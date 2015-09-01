/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.tinsphp.common.inference.constraints.EBindingCollectionMode;
import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.OverloadApplicationDto;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.inference_engine.constraints.TempFunctionType;
import ch.tsphp.tinsphp.inference_engine.constraints.TempMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static ch.tsphp.tinsphp.common.utils.Pair.pair;

public class ConstraintSolver implements IConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final ISoftTypingConstraintSolver softTypingConstraintSolver;
    private final IConstraintSolverHelper constraintSolverHelper;
    private final ExecutorService executorService;
    private final ConcurrentMap<String, Set<String>> methodsWithDependents;
    private final ConcurrentMap<String, Set<WorkItemDto>> dependentMethods;
    private final ConcurrentMap<String, ConcurrentMap<String, List<Integer>>> directDependencies;
    private final Map<String, TempMethodSymbol> tempMethodSymbols = new HashMap<>();

    private final ConcurrentLinkedQueue<Future> futures = new ConcurrentLinkedQueue<>();
    private final Set<IMethodSymbol> collectionsWhichChanged
            = Collections.synchronizedSet(new HashSet<IMethodSymbol>());


    @SuppressWarnings("checkstyle:parameternumber")
    public ConstraintSolver(
            ISymbolFactory theSymbolFactory,
            ISoftTypingConstraintSolver theSoftTypingConstraintSolver,
            IConstraintSolverHelper theConstraintSolverHelper,
            ExecutorService theExecutorService,
            ConcurrentMap<String, ConcurrentMap<String, List<Integer>>> theDirectDependencies,
            ConcurrentMap<String, Set<String>> theMethodsWithDependents,
            ConcurrentMap<String, Set<WorkItemDto>> theDependentMethods) {

        symbolFactory = theSymbolFactory;
        softTypingConstraintSolver = theSoftTypingConstraintSolver;
        constraintSolverHelper = theConstraintSolverHelper;
        executorService = theExecutorService;
        directDependencies = theDirectDependencies;
        methodsWithDependents = theMethodsWithDependents;
        dependentMethods = theDependentMethods;
    }

    @Override
    public void solveConstraints(List<IMethodSymbol> methodSymbols, IGlobalNamespaceScope globalDefaultNamespaceScope) {

        for (IMethodSymbol methodSymbol : methodSymbols) {
            Deque<WorkItemDto> workDeque = createInitialWorklist(methodSymbol, true);
            Future<?> future = executorService.submit(new MethodConstraintSolver(methodSymbol, workDeque));
            futures.add(future);
        }

        waitUntilAllFeaturesAreDone();

        if (!dependentMethods.isEmpty()) {
            solveConstraintsIteratively();
        }

        if (!globalDefaultNamespaceScope.getConstraints().isEmpty()) {
            Deque<WorkItemDto> workDeque = createInitialWorklist(globalDefaultNamespaceScope, false);
            solveGlobalDefaultNamespaceConstraints(globalDefaultNamespaceScope, workDeque);
        }
    }

    private void waitUntilAllFeaturesAreDone() {
        while (!futures.isEmpty()) {
            Future future = futures.remove();
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Deque<WorkItemDto> createInitialWorklist(
            IConstraintCollection constraintCollection, boolean isSolvingMethod) {
        IBindingCollection bindings = symbolFactory.createBindingCollection();
        Deque<WorkItemDto> workDeque = new ArrayDeque<>();
        workDeque.add(new WorkItemDto(workDeque, constraintCollection, 0, isSolvingMethod, bindings));
        return workDeque;
    }


    private void solveConstraintsIteratively() {

        solveIteratively();

        for (String indirectRecursiveMethod : methodsWithDependents.keySet()) {
            Iterator<WorkItemDto> iterator = dependentMethods.get(indirectRecursiveMethod).iterator();
            WorkItemDto firstWorkItemDto = iterator.next();
            IMethodSymbol methodSymbol = (IMethodSymbol) firstWorkItemDto.constraintCollection;
            createOverloadsForRecursiveMethod(iterator, firstWorkItemDto, methodSymbol);
        }

        for (Map.Entry<String, Set<String>> methodWithDependent : methodsWithDependents.entrySet()) {
            for (String dependentMethodName : methodWithDependent.getValue()) {
                if (!methodsWithDependents.containsKey(dependentMethodName)) {
                    //regular dependency solving for non indirect recursive methods
                    solveDependentMethod(dependentMethods.get(dependentMethodName), false, true);
                } else {
                    replaceAppliedOverload(dependentMethodName, methodWithDependent.getKey());
                }
            }
        }
        waitUntilAllFeaturesAreDone();

        dependentMethods.clear();
        methodsWithDependents.clear();
        directDependencies.clear();
    }

    private void replaceAppliedOverload(String dependentMethodName, String methodWithDependent) {
        Set<WorkItemDto> dependentWorkItems = dependentMethods.get(dependentMethodName);
        for (WorkItemDto workItemDto : dependentWorkItems) {
            workItemDto.isInIterativeMode = false;
            workItemDto.helperVariableMapping = null;
            for (List<Integer> pointers : directDependencies.get(methodWithDependent).values()) {
                for (Integer pointer : pointers) {
                    //exchange applied overload, currently it is still pointing to the temp overload
                    IConstraint constraint = workItemDto.constraintCollection.getConstraints().get(pointer);
                    constraintSolverHelper.addMostSpecificOverloadToWorklist(workItemDto, constraint);
                    if (workItemDto.workDeque.isEmpty()) {
                        //TODO TINS-524 erroneous overload - remove the debug info bellow
                        for (IFunctionType functionType : constraint.getMethodSymbol().getOverloads()) {
                            System.out.println(functionType.getSignature());
                        }

                        System.out.println("\n");

                        Collection<IFunctionType> operatorOverloads
                                = ((IMethodSymbol) workItemDto.constraintCollection).getOverloads();
                        for (IFunctionType functionType : operatorOverloads) {
                            System.out.println(functionType.getSignature());
                        }

                        System.out.println("\n----------- Bindings -------------\n");

                        for (IFunctionType functionType : constraint.getMethodSymbol().getOverloads()) {
                            System.out.println(functionType.getBindingCollection().toString());
                        }

                        System.out.println("\n");

                        for (IFunctionType functionType : operatorOverloads) {
                            System.out.println(functionType.getBindingCollection().toString());
                        }
                    }
                    WorkItemDto tempWorkItemDto = workItemDto.workDeque.removeFirst();
                    String lhsAbsoluteName = constraint.getLeftHandSide().getAbsoluteName();
                    OverloadApplicationDto dto = tempWorkItemDto.bindingCollection.getAppliedOverload(lhsAbsoluteName);
                    workItemDto.bindingCollection.setAppliedOverload(lhsAbsoluteName, dto);
                }
            }
        }
    }

    private void createOverloadsForRecursiveMethod(
            Iterator<WorkItemDto> iterator, WorkItemDto firstWorkItemDto, IMethodSymbol methodSymbol) {
        Map<IFunctionType, WorkItemDto> mapping = new HashMap<>();
        IFunctionType overload = constraintSolverHelper.createOverload(
                methodSymbol, firstWorkItemDto.bindingCollection);
        mapping.put(overload, firstWorkItemDto);
        while (iterator.hasNext()) {
            WorkItemDto workItemDto = iterator.next();
            overload = constraintSolverHelper.createOverload(methodSymbol, workItemDto.bindingCollection);
            mapping.put(overload, workItemDto);
        }

        for (IFunctionType functionType : methodSymbol.getOverloads()) {
            methodSymbol.addBindingCollection(functionType.getBindingCollection());
            mapping.remove(functionType);
        }

        // remove the WorklistDtos which were not chosen as overloads from the unresolved list.
        // Otherwise the applied overloads are recalculated for them nonetheless.
        String absoluteName = firstWorkItemDto.constraintCollection.getAbsoluteName();
        Set<WorkItemDto> workItemDtos = dependentMethods.get(absoluteName);
        for (WorkItemDto workItemDto : mapping.values()) {
            workItemDtos.remove(workItemDto);
        }
    }

    private void solveIteratively() {
        createTempOverloads();

        for (String indirectRecursiveMethod : methodsWithDependents.keySet()) {
            solveDependentMethod(dependentMethods.get(indirectRecursiveMethod), true, false);
        }

        waitUntilAllFeaturesAreDone();

        while (!collectionsWhichChanged.isEmpty()) {

            Map<String, Pair<IMethodSymbol, Deque<WorkItemDto>>> methodsToReIterate = new HashMap<>();

            Iterator<IMethodSymbol> collectionIterator = collectionsWhichChanged.iterator();
            while (collectionIterator.hasNext()) {
                IMethodSymbol methodSymbol = collectionIterator.next();
                collectionIterator.remove();

                String absoluteName = methodSymbol.getAbsoluteName();
                TempMethodSymbol tempMethodSymbol = tempMethodSymbols.get(absoluteName);
                List<IFunctionType> tempOverloads = createTempOverloads(
                        methodSymbol, dependentMethods.get(absoluteName));
                tempMethodSymbol.renewTempOverloads(tempOverloads);

                for (Map.Entry<String, List<Integer>> entry : directDependencies.get(absoluteName).entrySet()) {
                    String dependentMethodName = entry.getKey();
                    //Warning! start code duplication, very similar as solveDependentMethod
                    Iterator<WorkItemDto> iterator = dependentMethods.get(dependentMethodName).iterator();
                    WorkItemDto workItemDto = iterator.next();
                    List<Integer> dependentConstraints = entry.getValue();
                    if (!methodsToReIterate.containsKey(dependentMethodName)) {
                        IMethodSymbol dependentMethodSymbol = (IMethodSymbol) workItemDto.constraintCollection;
                        Deque<WorkItemDto> dependentWorkQueue = workItemDto.workDeque;
                        addToQueueInIterativeMode(workItemDto, dependentConstraints, dependentWorkQueue);
                        while (iterator.hasNext()) {
                            workItemDto = iterator.next();
                            addToQueueInIterativeMode(workItemDto, dependentConstraints, dependentWorkQueue);
                        }
                        methodsToReIterate.put(dependentMethodName, pair(dependentMethodSymbol, dependentWorkQueue));
                    } else {
                        workItemDto.dependentConstraints.addAll(dependentConstraints);
                    }
                    //Warning! end code duplication, very similar as solveDependentMethod
                }
            }

            for (Pair<IMethodSymbol, Deque<WorkItemDto>> pair : methodsToReIterate.values()) {
                futures.add(executorService.submit(new MethodConstraintSolver(pair.first, pair.second)));
            }

            waitUntilAllFeaturesAreDone();
        }
    }

    private void addToQueueInIterativeMode(
            WorkItemDto workItemDto, List<Integer> dependentConstraints, Deque<WorkItemDto> dependentWorkQueue) {
        workItemDto.dependentConstraints = dependentConstraints;
        workItemDto.pointer = 0;
        workItemDto.hasChanged = false;
        workItemDto.isSolvingDependency = true;
        dependentWorkQueue.add(workItemDto);
    }

    private void createTempOverloads() {
        for (Map.Entry<String, Set<String>> entry : methodsWithDependents.entrySet()) {
            String methodName = entry.getKey();
            Set<WorkItemDto> workItemDtos = dependentMethods.get(methodName);
            TempMethodSymbol tempMethodSymbol = createTempMethodSymbol(workItemDtos);
            tempMethodSymbols.put(tempMethodSymbol.getAbsoluteName(), tempMethodSymbol);

            for (String dependentMethodName : entry.getValue()) {
                //no need to change the constraint for non indirect recursive methods
                if (methodsWithDependents.containsKey(dependentMethodName)) {
                    Iterator<WorkItemDto> iterator = dependentMethods.get(dependentMethodName).iterator();
                    WorkItemDto workItemDto = iterator.next();
                    replaceWithTempMethodSymbol(workItemDto, dependentMethodName, methodName, tempMethodSymbol);
                }
            }
        }
    }

    private void replaceWithTempMethodSymbol(
            WorkItemDto workItemDto,
            String dependentMethodName,
            String methodWithDependent,
            TempMethodSymbol tempMethodSymbol) {

        List<IConstraint> constraints = workItemDto.constraintCollection.getConstraints();
        IConstraint constraint = constraints.get(workItemDto.pointer);
        IConstraint newConstraint = symbolFactory.createConstraint(
                constraint.getOperator(),
                constraint.getLeftHandSide(),
                constraint.getArguments(),
                tempMethodSymbol);
        constraints.set(workItemDto.pointer, newConstraint);

        ConcurrentMap<String, List<Integer>> dependentMethod = getOrInitAtomically(
                directDependencies, methodWithDependent, new ConcurrentHashMap<String, List<Integer>>());
        List<Integer> dependentConstraints = getOrInitAtomically(
                dependentMethod, dependentMethodName, Collections.synchronizedList(new ArrayList<Integer>()));
        dependentConstraints.add(workItemDto.pointer);
    }

    private TempMethodSymbol createTempMethodSymbol(Collection<WorkItemDto> workItemDtos) {
        IMethodSymbol methodSymbol = (IMethodSymbol) workItemDtos.iterator().next().constraintCollection;
        List<IFunctionType> tempOverloads = createTempOverloads(methodSymbol, workItemDtos);
        return new TempMethodSymbol(methodSymbol, tempOverloads);
    }

    private List<IFunctionType> createTempOverloads(
            IMethodSymbol methodSymbol, Collection<WorkItemDto> workItemDtos) {

        List<IVariable> parameterVariables = new ArrayList<IVariable>(methodSymbol.getParameters());
        IMinimalVariableSymbol returnVariable = methodSymbol.getReturnVariable();
        String absoluteName = methodSymbol.getAbsoluteName();

        List<IFunctionType> tempOverloads = new ArrayList<>();
        for (WorkItemDto workItemDto : workItemDtos) {
            IBindingCollection bindingCollection = workItemDto.bindingCollection;
            boolean notYetAllBindingsCreated = !bindingCollection.containsVariable(returnVariable.getAbsoluteName());
            if (!notYetAllBindingsCreated) {
                for (IVariable parameterVariable : parameterVariables) {
                    if (!bindingCollection.containsVariable(parameterVariable.getAbsoluteName())) {
                        notYetAllBindingsCreated = true;
                        break;
                    }
                }
            }

            if (notYetAllBindingsCreated) {
                IBindingCollection tmp = bindingCollection;
                bindingCollection = symbolFactory.createBindingCollection(workItemDto.bindingCollection);
                workItemDto.bindingCollection = bindingCollection;
                constraintSolverHelper.createBindingsIfNecessary(workItemDto, returnVariable, parameterVariables);
                workItemDto.bindingCollection = tmp;
            }

            IFunctionType overload =
                    symbolFactory.createFunctionType(absoluteName, bindingCollection, parameterVariables);
            TempFunctionType tempFunctionType = new TempFunctionType(overload);
            tempOverloads.add(tempFunctionType);
        }
        return tempOverloads;
    }


    private void solveDependentMethod(
            Collection<WorkItemDto> dependentWorkItems, boolean setIterativeMode, boolean resetToNormalMode) {
        Iterator<WorkItemDto> iterator = dependentWorkItems.iterator();
        WorkItemDto firstWorkItemDto = iterator.next();
        IMethodSymbol dependentMethodSymbol = (IMethodSymbol) firstWorkItemDto.constraintCollection;
        Deque<WorkItemDto> dependentWorkQueue = firstWorkItemDto.workDeque;
        addToQueue(dependentWorkQueue, firstWorkItemDto, setIterativeMode, resetToNormalMode);
        while (iterator.hasNext()) {
            WorkItemDto workItemDto = iterator.next();
            addToQueue(dependentWorkQueue, workItemDto, setIterativeMode, resetToNormalMode);
        }
        futures.add(executorService.submit(
                new MethodConstraintSolver(dependentMethodSymbol, dependentWorkQueue)));
    }

    private void addToQueue(
            Deque<WorkItemDto> dependentWorkQueue,
            WorkItemDto workItemDto,
            boolean setIterativeMode,
            boolean resetToNormalMode) {
        workItemDto.hasChanged = false;
        if (!resetToNormalMode) {
            workItemDto.isSolvingDependency = !setIterativeMode;
            if (setIterativeMode) {
                workItemDto.isInIterativeMode = true;
            }
        } else {
            workItemDto.isInIterativeMode = false;
            workItemDto.isSolvingDependency = false;
        }
        dependentWorkQueue.add(workItemDto);
    }

    private void solveGlobalDefaultNamespaceConstraints(
            IGlobalNamespaceScope globalDefaultNamespaceScope, Deque<WorkItemDto> workDeque) {
        List<WorkItemDto> workItemDtos = solveConstraints(workDeque);
        IBindingCollection bindingCollection;
        if (!workItemDtos.isEmpty()) {
            bindingCollection = workItemDtos.get(0).bindingCollection;
        } else {
            WorkItemDto softTypingWorkItem = getSoftTypingWorkItem(globalDefaultNamespaceScope, workDeque, false);
            solveConstraints(workDeque);
            softTypingConstraintSolver.solveConstraints(globalDefaultNamespaceScope, softTypingWorkItem);
            bindingCollection = softTypingWorkItem.bindingCollection;
        }
        globalDefaultNamespaceScope.addBindingCollection(bindingCollection);
        for (String variableId : bindingCollection.getVariableIds()) {
            bindingCollection.fixType(variableId);
        }
    }

    private WorkItemDto getSoftTypingWorkItem(
            IConstraintCollection constraintCollection, Deque<WorkItemDto> theWorkQueue, boolean isSolvingMethod) {
        IBindingCollection bindingCollection = symbolFactory.createBindingCollection();
        bindingCollection.setMode(EBindingCollectionMode.SoftTyping);
        WorkItemDto workItemDto = new WorkItemDto(
                theWorkQueue, constraintCollection, 0, isSolvingMethod, bindingCollection);
        workItemDto.isInSoftTypingMode = true;
        theWorkQueue.add(workItemDto);
        return workItemDto;
    }

    @Override
    public List<WorkItemDto> solveConstraints(Deque<WorkItemDto> workDeque) {
        List<WorkItemDto> solvedBindings = new ArrayList<>();
        WorkItemDto firstWorkItemDto = workDeque.peek();
        List<IConstraint> constraints = firstWorkItemDto.constraintCollection.getConstraints();

        while (!workDeque.isEmpty()) {
            WorkItemDto workItemDto = workDeque.removeFirst();
            if (workItemDto.pointer < constraints.size()) {
                int pointer;
                if (!workItemDto.isInIterativeMode || !workItemDto.isSolvingDependency) {
                    pointer = workItemDto.pointer;
                } else {
                    pointer = workItemDto.dependentConstraints.get(workItemDto.pointer);
                }
                IConstraint constraint = constraints.get(pointer);
                solveConstraint(workItemDto, constraint);
            } else {
                solvedBindings.add(workItemDto);
            }
        }

        return solvedBindings;
    }

    private void solveConstraint(WorkItemDto workItemDto, IConstraint theConstraint) {
        IConstraint constraint = theConstraint;
        IMinimalMethodSymbol refMethodSymbol = constraint.getMethodSymbol();

        Collection<IFunctionType> overloads = refMethodSymbol.getOverloads();
        boolean wasCreated = overloads.size() != 0;
        if (!wasCreated) {
            String dependentMethodName = workItemDto.constraintCollection.getAbsoluteName();
            String methodWithDependent = refMethodSymbol.getAbsoluteName();
            if (!workItemDto.isInIterativeMode) {
                //need to prevent that a dependency is created if the method is already solved
                synchronized (overloads) {
                    //could be solved by now
                    if (overloads.size() != 0) {
                        wasCreated = true;
                    } else {
                        registerDependency(workItemDto, dependentMethodName, methodWithDependent);
                    }
                }
            } else {
                TempMethodSymbol tempMethodSymbol = tempMethodSymbols.get(methodWithDependent);
                replaceWithTempMethodSymbol(workItemDto, dependentMethodName, methodWithDependent, tempMethodSymbol);
                constraint = workItemDto.constraintCollection.getConstraints().get(workItemDto.pointer);
                wasCreated = true;
            }
        }
        if (wasCreated) {
            constraintSolverHelper.solve(workItemDto, constraint);
        }
    }

    private void registerDependency(WorkItemDto workItemDto, String methodName, String refMethodName) {
        Set<String> dependentMethodNames = getOrInitAtomically(
                methodsWithDependents,
                refMethodName,
                new ConcurrentSkipListSet<String>());
        dependentMethodNames.add(methodName);
        Collection<WorkItemDto> workItemDtos = getOrInitAtomically(
                dependentMethods,
                methodName,
                Collections.synchronizedSet(new HashSet<WorkItemDto>()));
        workItemDtos.add(workItemDto);
    }


    private <TKey, TValue> TValue getOrInitAtomically(ConcurrentMap<TKey, TValue> map, TKey key, TValue initValue) {
        TValue value = map.get(key);
        if (value == null) {
            value = map.putIfAbsent(key, initValue);
            if (value == null) {
                value = map.get(key);
            }
        }
        return value;
    }


    private class MethodConstraintSolver implements Runnable
    {
        private final IMethodSymbol methodSymbol;
        private final Deque<WorkItemDto> workDeque;
        private boolean isInSoftTypingMode = false;

        public MethodConstraintSolver(
                IMethodSymbol theMethodSymbol, Deque<WorkItemDto> theWorkDeque) {
            methodSymbol = theMethodSymbol;
            workDeque = theWorkDeque;
        }

        @Override
        public void run() {
            int numberOfWorkItems = workDeque.size();
            List<WorkItemDto> workItemDtos = solveConstraints(workDeque);
            String methodName = methodSymbol.getAbsoluteName();
            if (!workItemDtos.isEmpty()) {
                WorkItemDto firstWorkItem = workItemDtos.get(0);
                if (isInSoftTypingMode) {
                    softTypingConstraintSolver.solveConstraints(methodSymbol, firstWorkItem);
                }

                //a method is not automatically solved if we are in the iterative mode
                if (!firstWorkItem.isInIterativeMode) {
                    //make sure dependencies to this method are not created anymore
                    synchronized (methodSymbol.getOverloads()) {
                        for (WorkItemDto workItemDto : workItemDtos) {
                            methodSymbol.addBindingCollection(workItemDto.bindingCollection);
                            constraintSolverHelper.createOverload(methodSymbol, workItemDto.bindingCollection);
                        }
                    }

                    Collection<String> dependentMethodNames = methodsWithDependents.remove(methodName);
                    if (dependentMethodNames != null) {
                        for (String dependentMethodName : dependentMethodNames) {
                            solveDependentMethod(dependentMethods.get(dependentMethodName), false, false);
                            dependentMethods.remove(dependentMethodName);
                        }
                    }
                    dependentMethods.remove(methodName);
                } else {
                    if (workItemDtos.size() != numberOfWorkItems || oneChanged(workItemDtos)) {
                        Set<WorkItemDto> dependentWorkItems = dependentMethods.get(methodName);
                        dependentWorkItems.clear();
                        for (WorkItemDto workItemDto : workItemDtos) {
                            dependentWorkItems.add(workItemDto);
                        }
                        collectionsWhichChanged.add(methodSymbol);
                    }
                }
            } else if (!isInSoftTypingMode && !dependentMethods.containsKey(methodName)) {
                //does not have any dependencies and still cannot be solved
                //need to fallback to soft typing
                isInSoftTypingMode = true;
                dependentMethods.remove(methodName);
                getSoftTypingWorkItem(methodSymbol, workDeque, true);
                futures.add(executorService.submit(this));
            }
        }

        private boolean oneChanged(List<WorkItemDto> workItemDtos) {
            boolean oneChanged = false;
            for (WorkItemDto workItemDto : workItemDtos) {
                if (workItemDto.hasChanged) {
                    oneChanged = true;
                    break;
                }
            }
            return oneChanged;
        }
    }
}
