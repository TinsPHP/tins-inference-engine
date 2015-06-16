/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.tinsphp.common.TinsPHPConstants;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.inference_engine.constraints.TempMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.WorklistDto;

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
    private final Map<String, List<Pair<WorklistDto, Integer>>> directDependencies;
    private final Map<String, Set<WorklistDto>> unsolvedConstraints;

    @SuppressWarnings("checkstyle:parameternumber")
    public IterativeConstraintSolver(
            ISymbolFactory theSymbolFactory,
            ITypeHelper theTypeHelper,
            IConstraintSolverHelper theConstraintSolverHelper,
            IDependencyConstraintSolver theDependencyConstraintSolver,
            Map<String, Set<String>> theDependencies,
            Map<String, List<Pair<WorklistDto, Integer>>> theDirectDependencies,
            Map<String, Set<WorklistDto>> theUnsolvedConstraints) {
        symbolFactory = theSymbolFactory;
        typeHelper = theTypeHelper;
        constraintSolverHelper = theConstraintSolverHelper;
        dependencyConstraintSolver = theDependencyConstraintSolver;
        dependencies = theDependencies;
        directDependencies = theDirectDependencies;
        unsolvedConstraints = theUnsolvedConstraints;
    }

    @Override
    public void solveConstraintsIteratively() {
        Deque<WorklistDto> worklist = new ArrayDeque<>();
        createTempOverloadsAndPopulateWorklist(worklist);

        solveIteratively(worklist);

        for (Map.Entry<String, Set<WorklistDto>> entry : unsolvedConstraints.entrySet()) {
            String absoluteName = entry.getKey();
            if (directDependencies.containsKey(absoluteName)) {
                Iterator<WorklistDto> iterator = entry.getValue().iterator();
                WorklistDto firstWorkListDto = iterator.next();
                IMethodSymbol methodSymbol = (IMethodSymbol) firstWorkListDto.constraintCollection;
                createOverloadsForRecursiveMethod(iterator, firstWorkListDto, methodSymbol);
            }
        }

        //solveDependencies is not in the same loop on purpose since we already filter unsolvedConstraints which
        // have not been used as overloads
        for (String absoluteName : unsolvedConstraints.keySet()) {
            if (directDependencies.containsKey(absoluteName)) {
                solveDependenciesOfRecursiveMethod(absoluteName);
            }
        }

        dependencies.clear();
        directDependencies.clear();
        unsolvedConstraints.clear();
    }

    private void createTempOverloadsAndPopulateWorklist(Deque<WorklistDto> worklist) {
        for (Map.Entry<String, Set<WorklistDto>> entry : unsolvedConstraints.entrySet()) {
            String absoluteName = entry.getKey();
            //we only solve recursive functions
            if (directDependencies.containsKey(absoluteName)) {
                Set<WorklistDto> worklistDtos = entry.getValue();
                TempMethodSymbol tempMethodSymbol = createTempMethodSymbol(worklistDtos);
                tempMethodSymbols.put(tempMethodSymbol.getAbsoluteName(), tempMethodSymbol);

                for (Pair<WorklistDto, Integer> dependency : directDependencies.get(absoluteName)) {
                    WorklistDto worklistDto = dependency.first;
                    List<IConstraint> constraints = worklistDto.constraintCollection.getConstraints();
                    int pointer = dependency.second;
                    IConstraint constraint = constraints.get(pointer);
                    IConstraint newConstraint = symbolFactory.createConstraint(
                            constraint.getOperator(),
                            constraint.getLeftHandSide(),
                            constraint.getArguments(),
                            tempMethodSymbol);
                    constraints.set(pointer, newConstraint);
                }

                for (WorklistDto worklistDto : entry.getValue()) {
                    worklistDto.pointer = 0;
                    worklistDto.isSolvingDependency = false;
                    worklistDto.isInIterativeMode = true;
                    worklist.add(worklistDto);
                }
            }
        }
    }

    private TempMethodSymbol createTempMethodSymbol(Collection<WorklistDto> worklistDtos) {
        IMethodSymbol methodSymbol = (IMethodSymbol) worklistDtos.iterator().next().constraintCollection;
        List<IFunctionType> tempOverloads = createTempOverloads(methodSymbol, worklistDtos);
        return new TempMethodSymbol(methodSymbol, tempOverloads);
    }

    private List<IFunctionType> createTempOverloads(
            IMethodSymbol methodSymbol, Collection<WorklistDto> worklistDtos) {

        List<IVariable> parameterVariables = new ArrayList<IVariable>(methodSymbol.getParameters());
        IMinimalVariableSymbol returnVariable = methodSymbol.getReturnVariable();
        String absoluteName = methodSymbol.getAbsoluteName();

        List<IFunctionType> tempOverloads = new ArrayList<>();
        for (WorklistDto worklistDto : worklistDtos) {
            IOverloadBindings overloadBindings = worklistDto.overloadBindings;
            boolean notYetAllBindingsCreated = !overloadBindings.containsVariable(returnVariable.getAbsoluteName());
            if (!notYetAllBindingsCreated) {
                for (IVariable parameterVariable : parameterVariables) {
                    if (!overloadBindings.containsVariable(parameterVariable.getAbsoluteName())) {
                        notYetAllBindingsCreated = true;
                        break;
                    }
                }
            }

            if (notYetAllBindingsCreated) {
                IOverloadBindings tmp = overloadBindings;
                overloadBindings = symbolFactory.createOverloadBindings(worklistDto.overloadBindings);
                worklistDto.overloadBindings = overloadBindings;
                constraintSolverHelper.createBindingsIfNecessary(worklistDto, returnVariable, parameterVariables);
                worklistDto.overloadBindings = tmp;
            }

            IFunctionType overload =
                    symbolFactory.createFunctionType(absoluteName, overloadBindings, parameterVariables);
            tempOverloads.add(overload);
        }
        return tempOverloads;
    }

    private void solveIteratively(Deque<WorklistDto> worklist) {
        Set<String> collectionsWhichChanged = new HashSet<>();
        solveIteratively(worklist, collectionsWhichChanged);
        while (!collectionsWhichChanged.isEmpty()) {
            Iterator<String> iterator = collectionsWhichChanged.iterator();
            while (iterator.hasNext()) {
                String absoluteName = iterator.next();
                iterator.remove();
                Set<WorklistDto> unresolvedWorklistDtos = unsolvedConstraints.get(absoluteName);
                IMethodSymbol methodSymbol =
                        (IMethodSymbol) unresolvedWorklistDtos.iterator().next().constraintCollection;

                TempMethodSymbol tempMethodSymbol = tempMethodSymbols.get(absoluteName);
                List<IFunctionType> tempOverloads = createTempOverloads(methodSymbol, unresolvedWorklistDtos);
                tempMethodSymbol.renewTempOverloads(tempOverloads);

                for (String refAbsoluteName : dependencies.get(absoluteName)) {
                    for (WorklistDto dto : unsolvedConstraints.get(refAbsoluteName)) {
                        worklist.add(dto);
                    }
                }
            }
            solveIteratively(worklist, collectionsWhichChanged);
        }
    }

    private void solveIteratively(Deque<WorklistDto> worklist, Set<String> collectionsWhichChanged) {
        while (!worklist.isEmpty()) {
            WorklistDto worklistDto = worklist.removeFirst();
            worklistDto.workDeque.add(worklistDto);
            List<IOverloadBindings> overloadBindingsList = solveConstraintsIterativeMode(worklistDto.workDeque);
            String absoluteName = worklistDto.constraintCollection.getAbsoluteName();

            if (overloadBindingsList.size() > 1) {
                collectionsWhichChanged.add(absoluteName);
                Iterator<IOverloadBindings> iterator = overloadBindingsList.iterator();
                //this work item will be re-added to the worklist since its collection is marked as has changed
                worklistDto.overloadBindings = iterator.next();
                while (iterator.hasNext()) {
                    //need to create more work items for the new overloads
                    WorklistDto newWorklistDto = new WorklistDto(worklistDto, 0, iterator.next());
                    constraintSolverHelper.createDependencies(newWorklistDto);
                }
            } else if (overloadBindingsList.size() == 1) {
                IOverloadBindings overloadBindings = overloadBindingsList.get(0);
                if (hasChanged(worklistDto, overloadBindings)) {
                    collectionsWhichChanged.add(absoluteName);
                }
                //this work item will be re-added to the worklist if its collection is marked as has changed
                worklistDto.overloadBindings = overloadBindings;
            } else {
                Set<WorklistDto> dtos = unsolvedConstraints.get(absoluteName);
                if (dtos.remove(worklistDto)) {
                    collectionsWhichChanged.add(absoluteName);
                }
                if (dtos.isEmpty()) {
                    throw new UnsupportedOperationException("oho... indirect recursion and soft typing");
                }
            }
        }
    }

    private List<IOverloadBindings> solveConstraintsIterativeMode(Deque<WorklistDto> workDeque) {
        List<IOverloadBindings> solvedBindings = new ArrayList<>();

        List<IConstraint> constraints = null;
        if (!workDeque.isEmpty()) {
            WorklistDto worklistDto = workDeque.peek();
            constraints = worklistDto.constraintCollection.getConstraints();
        }

        while (!workDeque.isEmpty()) {
            WorklistDto worklistDto = workDeque.removeFirst();
            if (worklistDto.pointer < worklistDto.unsolvedConstraints.size()) {
                int pointer = worklistDto.unsolvedConstraints.get(worklistDto.pointer);
                IConstraint constraint = constraints.get(pointer);
                constraintSolverHelper.solve(worklistDto, constraint);
            } else {
                solvedBindings.add(worklistDto.overloadBindings);
            }
        }

        return solvedBindings;
    }

    private boolean hasChanged(WorklistDto worklistDto, IOverloadBindings newBindings) {
        IOverloadBindings oldBindings = worklistDto.overloadBindings;
        boolean isNotTheSame = hasChanged(oldBindings, newBindings, TinsPHPConstants.RETURN_VARIABLE_NAME);
        if (!isNotTheSame) {
            IMethodSymbol methodSymbol = (IMethodSymbol) worklistDto.constraintCollection;
            for (IVariableSymbol parameter : methodSymbol.getParameters()) {
                if (hasChanged(oldBindings, newBindings, parameter.getAbsoluteName())) {
                    isNotTheSame = true;
                    break;
                }
            }
        }
        return isNotTheSame;
    }

    private boolean hasChanged(IOverloadBindings oldBindings, IOverloadBindings newBindings, String variableName) {
        String oldTypeVariable = oldBindings.getTypeVariable(variableName);
        String newTypeVariable = oldBindings.getTypeVariable(variableName);
        boolean isNotTheSame = !oldTypeVariable.equals(newTypeVariable);
        if (!isNotTheSame) {
            IUnionTypeSymbol oldLowerType = oldBindings.getLowerTypeBounds(newTypeVariable);
            IUnionTypeSymbol newLowerType = newBindings.getLowerTypeBounds(newTypeVariable);
            isNotTheSame = !(oldLowerType == null && newLowerType == null
                    || oldLowerType != null && typeHelper.areSame(oldLowerType, newLowerType));

            if (!isNotTheSame) {
                IIntersectionTypeSymbol oldUpperType = oldBindings.getUpperTypeBounds(newTypeVariable);
                IIntersectionTypeSymbol newUpperType = newBindings.getUpperTypeBounds(newTypeVariable);
                isNotTheSame = !(oldUpperType == null && newUpperType == null
                        || oldUpperType != null && typeHelper.areSame(oldUpperType, newUpperType));
                if (!isNotTheSame) {
                    Set<String> oldLowerRefBounds = oldBindings.getLowerRefBounds(newTypeVariable);
                    Set<String> newLowerRefBounds = newBindings.getLowerRefBounds(newTypeVariable);
                    isNotTheSame = !(oldLowerRefBounds == null && newLowerRefBounds == null
                            || oldLowerRefBounds != null && oldLowerRefBounds.equals(newLowerRefBounds));
                }
            }
        }
        return isNotTheSame;
    }

    private void createOverloadsForRecursiveMethod(
            Iterator<WorklistDto> iterator, WorklistDto firstWorkListDto, IMethodSymbol methodSymbol) {

        Map<IFunctionType, WorklistDto> mapping = new HashMap<>();
        IFunctionType overload = constraintSolverHelper.createOverload(methodSymbol, firstWorkListDto.overloadBindings);
        mapping.put(overload, firstWorkListDto);
        while (iterator.hasNext()) {
            WorklistDto worklistDto = iterator.next();
            IOverloadBindings overloadBindings = worklistDto.overloadBindings;
            overload = constraintSolverHelper.createOverload(methodSymbol, overloadBindings);
            mapping.put(overload, worklistDto);
        }

        List<IOverloadBindings> solvedBindings = new ArrayList<>();
        for (IFunctionType functionType : methodSymbol.getOverloads()) {
            solvedBindings.add(functionType.getOverloadBindings());
            mapping.remove(functionType);
        }
        methodSymbol.setBindings(solvedBindings);

        // remove the WorklistDtos which were not chosen as overloads from the unresolved list.
        // Otherwise the applied overloads are recalculated for them nonetheless.
        String absoluteName = firstWorkListDto.constraintCollection.getAbsoluteName();
        Set<WorklistDto> worklistDtos = unsolvedConstraints.get(absoluteName);
        for (WorklistDto worklistDto : mapping.values()) {
            worklistDtos.remove(worklistDto);
        }
    }

    private void solveDependenciesOfRecursiveMethod(String absoluteName) {
        for (Pair<WorklistDto, Integer> pair : directDependencies.get(absoluteName)) {
            WorklistDto worklistDto = pair.first;
            String refAbsoluteName = worklistDto.constraintCollection.getAbsoluteName();
            if (!directDependencies.containsKey(refAbsoluteName)) {
                //regular dependency solving for non recursive methods
                dependencyConstraintSolver.solveDependency(pair);
            } else if (unsolvedConstraints.get(refAbsoluteName).contains(worklistDto)) {
                //exchange applied overload, currently it is still pointing to the temp overload
                IConstraint constraint = worklistDto.constraintCollection.getConstraints().get(pair.second);

                worklistDto.isInIterativeMode = false;
                constraintSolverHelper.addMostSpecificOverloadToWorklist(worklistDto, constraint);

                if (worklistDto.workDeque.isEmpty()) {
                    //TODO TINS-524 erroneous overload - remove the debug info bellow
                    for (IFunctionType functionType : constraint.getMethodSymbol().getOverloads()) {
                        System.out.println(functionType.getSignature());
                    }

                    System.out.println("\n");

                    Collection<IFunctionType> operatorOverloads
                            = ((IMethodSymbol) worklistDto.constraintCollection).getOverloads();
                    for (IFunctionType functionType : operatorOverloads) {
                        System.out.println(functionType.getSignature());
                    }

                    System.out.println("\n----------- Bindings -------------\n");

                    for (IFunctionType functionType : constraint.getMethodSymbol().getOverloads()) {
                        System.out.println(functionType.getOverloadBindings().toString());
                    }

                    System.out.println("\n");

                    for (IFunctionType functionType : operatorOverloads) {
                        System.out.println(functionType.getOverloadBindings().toString());
                    }
                }
                WorklistDto tempWorklistDto = worklistDto.workDeque.removeFirst();
                String lhsAbsoluteName = constraint.getLeftHandSide().getAbsoluteName();
                IFunctionType appliedOverload
                        = tempWorklistDto.overloadBindings.getAppliedOverload(lhsAbsoluteName);
                worklistDto.overloadBindings.setAppliedOverload(lhsAbsoluteName, appliedOverload);
            }
        }
    }


}
