/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.OverloadApplicationDto;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.inference_engine.constraints.TempFunctionType;
import ch.tsphp.tinsphp.inference_engine.constraints.TempMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IterativeConstraintSolver implements IIterativeConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final ITypeHelper typeHelper;
    private final IConstraintSolverHelper constraintSolverHelper;
    private final IDependencyConstraintSolver dependencyConstraintSolver;

    private final Map<String, TempMethodSymbol> tempMethodSymbols = new HashMap<>();

    private final Map<String, Set<String>> dependencies;
    private final Map<String, List<Pair<WorkItemDto, Integer>>> directDependencies;
    private final Map<String, Set<WorkItemDto>> unsolvedWorkItems;

    @SuppressWarnings("checkstyle:parameternumber")
    public IterativeConstraintSolver(
            ISymbolFactory theSymbolFactory,
            ITypeHelper theTypeHelper,
            IConstraintSolverHelper theConstraintSolverHelper,
            IDependencyConstraintSolver theDependencyConstraintSolver,
            Map<String, Set<String>> theDependencies,
            Map<String, List<Pair<WorkItemDto, Integer>>> theDirectDependencies,
            Map<String, Set<WorkItemDto>> theUnsolvedWorkItems) {
        symbolFactory = theSymbolFactory;
        typeHelper = theTypeHelper;
        constraintSolverHelper = theConstraintSolverHelper;
        dependencyConstraintSolver = theDependencyConstraintSolver;
        dependencies = theDependencies;
        directDependencies = theDirectDependencies;
        unsolvedWorkItems = theUnsolvedWorkItems;
    }

    @Override
    public void solveConstraintsIteratively() {
        Deque<WorkItemDto> worklist = new ArrayDeque<>();
        createTempOverloadsAndPopulateWorklist(worklist);

        solveIteratively(worklist);

        for (Map.Entry<String, Set<WorkItemDto>> entry : unsolvedWorkItems.entrySet()) {
            String absoluteName = entry.getKey();
            if (directDependencies.containsKey(absoluteName)) {
                Iterator<WorkItemDto> iterator = entry.getValue().iterator();
                WorkItemDto firstWorkItemDto = iterator.next();
                IMethodSymbol methodSymbol = (IMethodSymbol) firstWorkItemDto.constraintCollection;
                createOverloadsForRecursiveMethod(iterator, firstWorkItemDto, methodSymbol);
            }
        }

        //solveDependencies is not in the same loop on purpose since we already filter dependentConstraints which
        // have not been used as overloads
        for (String absoluteName : unsolvedWorkItems.keySet()) {
            if (directDependencies.containsKey(absoluteName)) {
                solveDependenciesOfRecursiveMethod(absoluteName);
            }
        }

        dependencies.clear();
        directDependencies.clear();
        unsolvedWorkItems.clear();
    }

    private void createTempOverloadsAndPopulateWorklist(Deque<WorkItemDto> worklist) {
        for (Map.Entry<String, Set<WorkItemDto>> entry : unsolvedWorkItems.entrySet()) {
            String absoluteName = entry.getKey();
            //we only solve recursive functions
            if (directDependencies.containsKey(absoluteName)) {
                Set<WorkItemDto> workItemDtos = entry.getValue();
                TempMethodSymbol tempMethodSymbol = createTempMethodSymbol(workItemDtos);
                tempMethodSymbols.put(tempMethodSymbol.getAbsoluteName(), tempMethodSymbol);

                for (Pair<WorkItemDto, Integer> dependency : directDependencies.get(absoluteName)) {
                    WorkItemDto workItemDto = dependency.first;
                    List<IConstraint> constraints = workItemDto.constraintCollection.getConstraints();
                    int pointer = dependency.second;
                    IConstraint constraint = constraints.get(pointer);
                    IConstraint newConstraint = symbolFactory.createConstraint(
                            constraint.getOperator(),
                            constraint.getLeftHandSide(),
                            constraint.getArguments(),
                            tempMethodSymbol);
                    constraints.set(pointer, newConstraint);
                }

                for (WorkItemDto workItemDto : entry.getValue()) {
                    workItemDto.pointer = 0;
                    workItemDto.isSolvingDependency = false;
                    workItemDto.isInIterativeMode = true;
                    workItemDto.hasChanged = false;
                    worklist.add(workItemDto);
                }
            }
        }
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

    private void solveIteratively(Deque<WorkItemDto> worklist) {
        Set<String> collectionsWhichChanged = new HashSet<>();
        solveIteratively(worklist, collectionsWhichChanged);
        while (!collectionsWhichChanged.isEmpty()) {
            Iterator<String> iterator = collectionsWhichChanged.iterator();
            while (iterator.hasNext()) {
                String absoluteName = iterator.next();
                iterator.remove();
                Set<WorkItemDto> unsolvedWorkItemDtos = unsolvedWorkItems.get(absoluteName);
                IMethodSymbol methodSymbol =
                        (IMethodSymbol) unsolvedWorkItemDtos.iterator().next().constraintCollection;

                TempMethodSymbol tempMethodSymbol = tempMethodSymbols.get(absoluteName);
                List<IFunctionType> tempOverloads = createTempOverloads(methodSymbol, unsolvedWorkItemDtos);
                tempMethodSymbol.renewTempOverloads(tempOverloads);

                for (String refAbsoluteName : dependencies.get(absoluteName)) {
                    if (directDependencies.containsKey(refAbsoluteName)) {
                        for (WorkItemDto dto : unsolvedWorkItems.get(refAbsoluteName)) {
                            dto.hasChanged = false;
                            worklist.add(dto);
                        }
                    }
                }
            }
            solveIteratively(worklist, collectionsWhichChanged);
        }
    }

    private void solveIteratively(Deque<WorkItemDto> worklist, Set<String> collectionsWhichChanged) {
        while (!worklist.isEmpty()) {
            WorkItemDto workItemDto = worklist.removeFirst();
            workItemDto.workDeque.add(workItemDto);
            String absoluteName = workItemDto.constraintCollection.getAbsoluteName();
            List<WorkItemDto> workItemDtos = solveConstraintsIterativeMode(workItemDto.workDeque);

            Iterator<WorkItemDto> iterator = workItemDtos.iterator();
            if (iterator.hasNext()) {
                //update the registered work item
                WorkItemDto newWorkItem = iterator.next();
                if (newWorkItem.hasChanged || iterator.hasNext()) {
                    collectionsWhichChanged.add(absoluteName);
                }

                workItemDto.bindingCollection = newWorkItem.bindingCollection;
                workItemDto.helperVariableMapping = newWorkItem.helperVariableMapping;

                while (iterator.hasNext()) {
                    //need to register a dependency for the new overloads
                    newWorkItem = iterator.next();
                    // need to set the pointer to the constraint which needs to be solved
                    // in order that the correct dependency is registered
                    newWorkItem.pointer = workItemDto.pointer;
                    constraintSolverHelper.createDependencies(newWorkItem);
                }
            } else {
                Set<WorkItemDto> dtos = unsolvedWorkItems.get(absoluteName);
                if (dtos.remove(workItemDto)) {
                    collectionsWhichChanged.add(absoluteName);
                }
                if (dtos.isEmpty()) {
                    throw new UnsupportedOperationException("oho... indirect recursion and soft typing");
                }
            }
        }
    }

    private List<WorkItemDto> solveConstraintsIterativeMode(Deque<WorkItemDto> workDeque) {
        List<WorkItemDto> solvedWorkItems = new ArrayList<>();

        List<IConstraint> constraints = null;
        if (!workDeque.isEmpty()) {
            WorkItemDto workItemDto = workDeque.peek();
            constraints = workItemDto.constraintCollection.getConstraints();
        }

        while (!workDeque.isEmpty()) {
            WorkItemDto workItemDto = workDeque.removeFirst();
            if (workItemDto.pointer < workItemDto.dependentConstraints.size()) {
                int pointer = workItemDto.dependentConstraints.get(workItemDto.pointer);
                IConstraint constraint = constraints.get(pointer);
                constraintSolverHelper.solve(workItemDto, constraint);
            } else {
                solvedWorkItems.add(workItemDto);
            }
        }

        return solvedWorkItems;
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

        List<IBindingCollection> solvedBindings = new ArrayList<>();
        for (IFunctionType functionType : methodSymbol.getOverloads()) {
            solvedBindings.add(functionType.getBindingCollection());
            mapping.remove(functionType);
        }
        methodSymbol.setBindings(solvedBindings);

        // remove the WorklistDtos which were not chosen as overloads from the unresolved list.
        // Otherwise the applied overloads are recalculated for them nonetheless.
        String absoluteName = firstWorkItemDto.constraintCollection.getAbsoluteName();
        Set<WorkItemDto> workItemDtos = unsolvedWorkItems.get(absoluteName);
        for (WorkItemDto workItemDto : mapping.values()) {
            workItemDtos.remove(workItemDto);
        }
    }

    private void solveDependenciesOfRecursiveMethod(String absoluteName) {
        for (Pair<WorkItemDto, Integer> pair : directDependencies.get(absoluteName)) {
            WorkItemDto workItemDto = pair.first;
            workItemDto.isInIterativeMode = false;
            workItemDto.helperVariableMapping = null;
            String refAbsoluteName = workItemDto.constraintCollection.getAbsoluteName();
            if (!directDependencies.containsKey(refAbsoluteName)) {
                //regular dependency solving for non recursive methods
                dependencyConstraintSolver.solveDependency(pair);
            } else if (unsolvedWorkItems.get(refAbsoluteName).contains(workItemDto)) {
                //exchange applied overload, currently it is still pointing to the temp overload
                IConstraint constraint = workItemDto.constraintCollection.getConstraints().get(pair.second);

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
