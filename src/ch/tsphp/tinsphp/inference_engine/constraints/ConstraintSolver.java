/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.TinsPHPConstants;
import ch.tsphp.tinsphp.common.inference.constraints.BoundException;
import ch.tsphp.tinsphp.common.inference.constraints.BoundResultDto;
import ch.tsphp.tinsphp.common.inference.constraints.FixedTypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IContainerTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.IParametricTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IPolymorphicTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.utils.ERelation;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.common.utils.MapHelper;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.common.utils.TypeHelperDto;

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

import static ch.tsphp.tinsphp.common.utils.Pair.pair;

public class ConstraintSolver implements IConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final ITypeHelper typeHelper;
    private final IInferenceIssueReporter issueReporter;
    private final ITypeSymbol mixedTypeSymbol;
    private final Map<String, Set<String>> dependencies = new HashMap<>();
    private final Map<String, List<Pair<WorklistDto, Integer>>> directDependencies = new ConcurrentHashMap<>();
    private final Map<String, Set<WorklistDto>> unresolvedConstraints = new ConcurrentHashMap<>();
    private final Map<String, TempMethodSymbol> tempMethodSymbols = new HashMap<>();

    public ConstraintSolver(
            ISymbolFactory theSymbolFactory,
            ITypeHelper theTypeHelper,
            IInferenceIssueReporter theIssueReporter) {
        symbolFactory = theSymbolFactory;
        typeHelper = theTypeHelper;
        issueReporter = theIssueReporter;
        mixedTypeSymbol = symbolFactory.getMixedTypeSymbol();
    }

    @Override
    public void solveConstraints(List<IMethodSymbol> methodSymbols, IGlobalNamespaceScope globalDefaultNamespaceScope) {
        for (IMethodSymbol methodSymbol : methodSymbols) {
            Deque<WorklistDto> workDeque = createInitialWorklist(methodSymbol, true);
            solveMethodConstraints(methodSymbol, workDeque);
        }

        if (!unresolvedConstraints.isEmpty()) {
            solveConstraintsIteratively();
        }

        if (!globalDefaultNamespaceScope.getConstraints().isEmpty()) {
            Deque<WorklistDto> workDeque = createInitialWorklist(globalDefaultNamespaceScope, false);
            solveGlobalDefaultNamespaceConstraints(globalDefaultNamespaceScope, workDeque);
        }
    }

    private Deque<WorklistDto> createInitialWorklist(
            IConstraintCollection constraintCollection, boolean isSolvingMethod) {
        IOverloadBindings bindings = symbolFactory.createOverloadBindings();
        Deque<WorklistDto> workDeque = new ArrayDeque<>();
        workDeque.add(new WorklistDto(workDeque, constraintCollection, 0, isSolvingMethod, bindings));
        return workDeque;
    }

    private void solveConstraintsIteratively() {

        Deque<WorklistDto> worklist = new ArrayDeque<>();

        createTempOverloadsAndPopulateWorklist(worklist);

        solveIteratively(worklist);

        for (Map.Entry<String, Set<WorklistDto>> entry : unresolvedConstraints.entrySet()) {
            String absoluteName = entry.getKey();
            if (directDependencies.containsKey(absoluteName)) {
                Iterator<WorklistDto> iterator = entry.getValue().iterator();
                WorklistDto firstWorkListDto = iterator.next();
                IMethodSymbol methodSymbol = (IMethodSymbol) firstWorkListDto.constraintCollection;
                createOverloadsForRecursiveMethod(iterator, firstWorkListDto, methodSymbol);
            }
        }

        //solveDependencies is not in the same loop on purpose since we already filter unresolvedConstraints which
        // have not been used as overloads
        for (String absoluteName : unresolvedConstraints.keySet()) {
            if (directDependencies.containsKey(absoluteName)) {
                solveDependenciesOfRecursiveMethod(absoluteName);
            }
        }

        dependencies.clear();
        directDependencies.clear();
        unresolvedConstraints.clear();
    }

    private void createTempOverloadsAndPopulateWorklist(Deque<WorklistDto> worklist) {
        for (Map.Entry<String, Set<WorklistDto>> entry : unresolvedConstraints.entrySet()) {
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
                overloadBindings = symbolFactory.createOverloadBindings(worklistDto.overloadBindings);
                createBindingsIfNecessary(overloadBindings, returnVariable, parameterVariables);
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
                Set<WorklistDto> unresolvedWorklistDtos = unresolvedConstraints.get(absoluteName);
                IMethodSymbol methodSymbol =
                        (IMethodSymbol) unresolvedWorklistDtos.iterator().next().constraintCollection;

                TempMethodSymbol tempMethodSymbol = tempMethodSymbols.get(absoluteName);
                List<IFunctionType> tempOverloads = createTempOverloads(methodSymbol, unresolvedWorklistDtos);
                tempMethodSymbol.renewTempOverloads(tempOverloads);

                for (String refAbsoluteName : dependencies.get(absoluteName)) {
                    for (WorklistDto dto : unresolvedConstraints.get(refAbsoluteName)) {
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
                    createDependencies(newWorklistDto);
                }
            } else if (overloadBindingsList.size() == 1) {
                IOverloadBindings overloadBindings = overloadBindingsList.get(0);
                if (hasChanged(worklistDto, overloadBindings)) {
                    collectionsWhichChanged.add(absoluteName);
                }
                //this work item will be re-added to the worklist if its collection is marked as has changed
                worklistDto.overloadBindings = overloadBindings;
            } else {
                Set<WorklistDto> dtos = unresolvedConstraints.get(absoluteName);
                if (dtos.remove(worklistDto)) {
                    collectionsWhichChanged.add(absoluteName);
                }
                if (dtos.isEmpty()) {
                    throw new UnsupportedOperationException("oho... indirect recursion and soft typing");
                }
            }
        }
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
        String oldTypeVariable = oldBindings.getTypeVariableReference(variableName).getTypeVariable();
        String newTypeVariable = oldBindings.getTypeVariableReference(variableName).getTypeVariable();
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
        IFunctionType overload = createOverload(methodSymbol, firstWorkListDto.overloadBindings);
        mapping.put(overload, firstWorkListDto);
        while (iterator.hasNext()) {
            WorklistDto worklistDto = iterator.next();
            IOverloadBindings overloadBindings = worklistDto.overloadBindings;
            overload = createOverload(methodSymbol, overloadBindings);
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
        Set<WorklistDto> worklistDtos = unresolvedConstraints.get(absoluteName);
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
                solveDependency(pair);
            } else if (unresolvedConstraints.get(refAbsoluteName).contains(worklistDto)) {
                //exchange applied overload, currently it is still pointing to the temp overload
                IConstraint constraint = worklistDto.constraintCollection.getConstraints().get(pair.second);

                worklistDto.isInIterativeMode = false;
                addMostSpecificOverloadToWorklist(worklistDto, constraint);

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

    public void solveGlobalDefaultNamespaceConstraints(
            IGlobalNamespaceScope globalDefaultNamespaceScope, Deque<WorklistDto> workDeque) {
        List<IOverloadBindings> bindings = solveConstraints(workDeque);
        if (bindings.isEmpty()) {
            //TODO rstoll TINS-306 inference - runtime check insertion
        } else {
            globalDefaultNamespaceScope.setBindings(bindings);
            IOverloadBindings overloadBindings = bindings.get(0);
            for (String variableId : overloadBindings.getVariableIds()) {
                overloadBindings.fixType(variableId);
            }
        }
    }

    private void solveMethodConstraints(IMethodSymbol methodSymbol, Deque<WorklistDto> workDeque) {
        List<IOverloadBindings> bindings = solveConstraints(workDeque);
        if (!bindings.isEmpty()) {
            methodSymbol.setBindings(bindings);
            createOverloads(methodSymbol, bindings);

            String methodName = methodSymbol.getAbsoluteName();
            if (directDependencies.containsKey(methodName)) {
                dependencies.remove(methodName);
                for (Pair<WorklistDto, Integer> element : directDependencies.remove(methodName)) {
                    solveDependency(element);
                }
            }
            unresolvedConstraints.remove(methodName);
        }
    }

    private void solveDependency(Pair<WorklistDto, Integer> pair) {
        WorklistDto worklistDto = pair.first;
        worklistDto.pointer = pair.second;
        //removing pointer not the element at index thus the cast to (Integer)
        worklistDto.unsolvedConstraints.remove((Integer) worklistDto.pointer);
        worklistDto.isSolvingDependency = true;
        worklistDto.workDeque.add(worklistDto);
        solveMethodConstraints((IMethodSymbol) worklistDto.constraintCollection, worklistDto.workDeque);
    }

    private List<IOverloadBindings> solveConstraints(Deque<WorklistDto> workDeque) {
        List<IOverloadBindings> solvedBindings = new ArrayList<>();

        List<IConstraint> constraints = null;
        if (!workDeque.isEmpty()) {
            WorklistDto worklistDto = workDeque.peek();
            constraints = worklistDto.constraintCollection.getConstraints();
        }

        boolean hasDependency = false;

        while (!workDeque.isEmpty()) {
            WorklistDto worklistDto = workDeque.removeFirst();

            if (worklistDto.pointer < constraints.size()) {
                IConstraint constraint = constraints.get(worklistDto.pointer);
                solveConstraint(worklistDto, constraint);
            } else if (worklistDto.unsolvedConstraints == null || worklistDto.unsolvedConstraints.isEmpty()) {
                solvedBindings.add(worklistDto.overloadBindings);
            } else {
                hasDependency = true;
                if (!worklistDto.isSolvingDependency) {
                    createDependencies(worklistDto);
                }
            }
        }

        if (solvedBindings.size() == 0 && !hasDependency) {
            //TODO error case if no overload could be found
        }

        return solvedBindings;
    }

    private void createDependencies(WorklistDto worklistDto) {
        List<IConstraint> constraints = worklistDto.constraintCollection.getConstraints();
        String absoluteName = worklistDto.constraintCollection.getAbsoluteName();
        for (Integer pointer : worklistDto.unsolvedConstraints) {
            String refAbsoluteName = constraints.get(pointer).getMethodSymbol().getAbsoluteName();
            MapHelper.addToListInMap(directDependencies, refAbsoluteName, pair(worklistDto, pointer));
            MapHelper.addToSetInMap(dependencies, refAbsoluteName, absoluteName);
        }
        MapHelper.addToSetInMap(
                unresolvedConstraints,
                absoluteName,
                worklistDto);
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
                solve(worklistDto, constraint);
            } else {
                solvedBindings.add(worklistDto.overloadBindings);
            }
        }

        return solvedBindings;
    }

    private void solveConstraint(WorklistDto worklistDto, IConstraint constraint) {
        IMinimalMethodSymbol refMethodSymbol = constraint.getMethodSymbol();
        if (refMethodSymbol.getOverloads().size() != 0) {
            solve(worklistDto, constraint);
        } else {
            //add to unresolved constraints
            if (worklistDto.unsolvedConstraints == null) {
                worklistDto.unsolvedConstraints = new ArrayList<>();
            }
            worklistDto.unsolvedConstraints.add(worklistDto.pointer);

            //proceed with the rest
            ++worklistDto.pointer;
            worklistDto.workDeque.add(worklistDto);
        }
    }

    private void solve(WorklistDto worklistDto, IConstraint constraint) {
        boolean atLeastOneBindingCreated = createBindingsIfNecessary(
                worklistDto.overloadBindings, constraint.getLeftHandSide(), constraint.getArguments());

        if (atLeastOneBindingCreated) {
            addApplicableOverloadsToWorklist(worklistDto, constraint);
        } else {
            try {
                addMostSpecificOverloadToWorklist(worklistDto, constraint);
            } catch (AmbiguousOverloadException ex) {
                if (!worklistDto.isSolvingMethod) {
                    //TODO report ambiguous overload
                    throw ex;
                } else {
                    List<OverloadRankingDto> overloadRankingDtos = ex.getOverloadRankingDtos();
                    //fine, as long as it has narrowed arguments. Then it should be covered by another workItem
                    if (!overloadRankingDtos.get(0).hasNarrowedArguments) {
                        throw ex;
                    }
                }
            }
        }
    }

    private boolean createBindingsIfNecessary(
            IOverloadBindings overloadBindings, IVariable leftHandSide, List<IVariable> arguments) {

        createBindingIfNecessary(overloadBindings, leftHandSide);
        boolean atLeastOneBindingCreated = false;
        for (IVariable parameterVariable : arguments) {
            boolean neededBinding = createBindingIfNecessary(overloadBindings, parameterVariable);
            atLeastOneBindingCreated = atLeastOneBindingCreated || neededBinding;
        }
        return atLeastOneBindingCreated;
    }


    private boolean createBindingIfNecessary(IOverloadBindings bindings, IVariable variable) {
        String absoluteName = variable.getAbsoluteName();
        boolean bindingDoesNotExist = !bindings.containsVariable(absoluteName);
        if (bindingDoesNotExist) {
            ITypeVariableReference reference = bindings.getNextTypeVariable();
            ITypeVariableReference typeVariableReference = reference;
            //if it is a literal then we know already the lower bound and it is a fix typed type variable
            ITypeSymbol typeSymbol = variable.getType();
            if (typeSymbol != null) {
                typeVariableReference = new FixedTypeVariableReference(reference);
            }
            bindings.addVariable(absoluteName, typeVariableReference);
            if (typeSymbol != null) {
                String typeVariable = typeVariableReference.getTypeVariable();
                bindings.addLowerTypeBound(typeVariable, typeSymbol);
                //TODO rstoll TINS-407 - store fixed type only in lower bound
                //TODO rstoll TINS-387 function application only consider upper bounds
//                bindings.addUpperTypeBound(typeVariable, typeSymbol);
                bindingDoesNotExist = false;
            }
        }
        return bindingDoesNotExist;
    }

    private void addApplicableOverloadsToWorklist(WorklistDto worklistDto, IConstraint constraint) {
        for (IFunctionType overload : constraint.getMethodSymbol().getOverloads()) {
            try {
                AggregateBindingDto dto = solveOverLoad(worklistDto, constraint, overload);

                //we do not create overloads with implicit conversions here.
                if (dto != null && dto.implicitConversionCounter == 0) {
                    worklistDto.workDeque.add(nextWorklistDto(worklistDto, dto.bindings));
                }
            } catch (BoundException ex) {
                //That is ok, we will deal with it in solveConstraints
            }
        }
    }

    private WorklistDto nextWorklistDto(WorklistDto worklistDto, IOverloadBindings bindings) {
        int pointer;
        if (!worklistDto.isSolvingDependency || worklistDto.isInIterativeMode) {
            pointer = worklistDto.pointer + 1;
        } else {
            pointer = Integer.MAX_VALUE;
        }
        return new WorklistDto(worklistDto, pointer, bindings);
    }

    private AggregateBindingDto solveOverLoad(
            WorklistDto worklistDto,
            IConstraint constraint,
            IFunctionType overload) {

        AggregateBindingDto dto = null;
        if (constraint.getArguments().size() >= overload.getNumberOfNonOptionalParameters()) {
            IOverloadBindings bindings = symbolFactory.createOverloadBindings(worklistDto.overloadBindings);
            dto = new AggregateBindingDto(constraint, overload, bindings, worklistDto.isInIterativeMode);
            aggregateBinding(dto);
        }
        return dto;
    }

    private void aggregateBinding(AggregateBindingDto dto) {
        List<IVariable> arguments = dto.constraint.getArguments();
        List<IVariable> parameters = dto.overload.getParameters();
        int numberOfParameters = parameters.size();

        dto.mapping = new HashMap<>(numberOfParameters + 1);

        int numberOfArguments = arguments.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;

        dto.needToReIterate = true;
        while (dto.needToReIterate) {
            dto.needToReIterate = false;

            IVariable leftHandSide = dto.constraint.getLeftHandSide();
            dto.bindingVariable = leftHandSide;
            dto.overloadVariableId = TinsPHPConstants.RETURN_VARIABLE_NAME;
            boolean hasNarrowed = dto.hasNarrowedArguments;
            mergeTypeVariables(dto);
            //reset because we are only interested in whether an argument was narrowed
            dto.hasNarrowedArguments = hasNarrowed;

            boolean argumentsAreAllFixed = true;
            for (int i = 0; i < count; ++i) {
                dto.bindingVariable = arguments.get(i);
                dto.overloadVariableId = parameters.get(i).getAbsoluteName();
                mergeTypeVariables(dto);
                argumentsAreAllFixed = argumentsAreAllFixed
                        && dto.bindings.getTypeVariableReference(dto.bindingVariable.getAbsoluteName()).hasFixedType();
            }

            if (!dto.needToReIterate) {
                String lhsAbsoluteName = leftHandSide.getAbsoluteName();
                ITypeVariableReference reference = dto.bindings.getTypeVariableReference(lhsAbsoluteName);
                if (!reference.hasFixedType() && argumentsAreAllFixed) {
                    dto.bindings.fixType(lhsAbsoluteName);
                }
                if (!dto.isInIterativeMode && !lhsAbsoluteName.equals(TinsPHPConstants.RETURN_VARIABLE_NAME)) {
                    dto.bindings.setAppliedOverload(lhsAbsoluteName, dto.overload);
                }
            }
            if (dto.iterateCount > 1) {
                throw new IllegalStateException("overload uses type variables "
                        + "which are not part of the signature.");
            }
            ++dto.iterateCount;
        }
    }

    private void mergeTypeVariables(AggregateBindingDto dto) {

        String bindingVariableName = dto.bindingVariable.getAbsoluteName();
        ITypeVariableReference bindingTypeVariableReference =
                dto.bindings.getTypeVariableReference(bindingVariableName);
        IOverloadBindings rightBindings = dto.overload.getOverloadBindings();
        String overloadTypeVariable =
                rightBindings.getTypeVariableReference(dto.overloadVariableId).getTypeVariable();

        String lhsTypeVariable;
        if (dto.mapping.containsKey(overloadTypeVariable)) {
            lhsTypeVariable = dto.mapping.get(overloadTypeVariable).getTypeVariable();
            String rhsTypeVariable = bindingTypeVariableReference.getTypeVariable();
            dto.bindings.mergeFirstIntoSecond(rhsTypeVariable, lhsTypeVariable);
        } else {
            lhsTypeVariable = bindingTypeVariableReference.getTypeVariable();
            dto.mapping.put(overloadTypeVariable, bindingTypeVariableReference);
        }

        applyRightToLeft(dto, lhsTypeVariable, overloadTypeVariable);
    }

    private void applyRightToLeft(AggregateBindingDto dto, String left, String right) {

        IOverloadBindings leftBindings = dto.bindings;
        IOverloadBindings rightBindings = dto.overload.getOverloadBindings();

        boolean usedImplicitConversions = false;

        if (rightBindings.hasUpperTypeBounds(right)) {
            IIntersectionTypeSymbol rightUpperTypeBounds = rightBindings.getUpperTypeBounds(right);
            ITypeSymbol copy = copyIfNotFixed(rightUpperTypeBounds, dto);
            if (copy != null) {
                BoundResultDto result = leftBindings.addUpperTypeBound(left, copy);
                if (result.usedImplicitConversion) {
                    usedImplicitConversions = true;
                    //implicit conversion always narrows
                    dto.hasNarrowedArguments = true;
                } else if (result.hasChanged) {
                    dto.hasNarrowedArguments = true;
                }
            }
        }

        if (rightBindings.hasLowerTypeBounds(right)) {
            IUnionTypeSymbol rightLowerTypeBounds = rightBindings.getLowerTypeBounds(right);
            ITypeSymbol copy = copyIfNotFixed(rightLowerTypeBounds, dto);
            if (copy != null) {
                BoundResultDto result = leftBindings.addLowerTypeBound(left, copy);
                if (result.usedImplicitConversion) {
                    usedImplicitConversions = true;
                }
                if (result.hasChanged) {
                    dto.hasNarrowedArguments = true;
                }
            }
        }

        if (usedImplicitConversions) {
            ++dto.implicitConversionCounter;
        }


        if (rightBindings.hasLowerRefBounds(right)) {
            for (String refTypeVariable : rightBindings.getLowerRefBounds(right)) {
                if (dto.mapping.containsKey(refTypeVariable)) {
                    leftBindings.addLowerRefBound(left, dto.mapping.get(refTypeVariable));
                } else if (!dto.isInIterativeMode && dto.iterateCount == 1) {
                    ITypeVariableReference typeVariableReference = addHelperVariable(dto, refTypeVariable);
                    leftBindings.addLowerRefBound(left, typeVariableReference);
                } else if (dto.isInIterativeMode && dto.iterateCount == 1) {
                    addLowerRefInIterativeMode(dto, left, refTypeVariable);
                } else {
                    dto.needToReIterate = true;
                    break;
                }
            }
        }
    }

    private IPolymorphicTypeSymbol copyIfNotFixed(
            IPolymorphicTypeSymbol polymorphicTypeSymbol, AggregateBindingDto dto) {

        IPolymorphicTypeSymbol copy = polymorphicTypeSymbol;
        if (!polymorphicTypeSymbol.isFixed()) {
            IOverloadBindings leftBindings = dto.bindings;
            Deque<IParametricTypeSymbol> parametricTypeSymbols = new ArrayDeque<>();
            copy = polymorphicTypeSymbol.copy(parametricTypeSymbols);

            parametricTypes:
            for (IParametricTypeSymbol parametricTypeSymbol : parametricTypeSymbols) {
                List<String> typeParameters = new ArrayList<>();
                for (String typeParameter : parametricTypeSymbol.getTypeParameters()) {
                    String typeVariable;
                    if (dto.mapping.containsKey(typeParameter)) {
                        typeVariable = dto.mapping.get(typeParameter).getTypeVariable();
                    } else if (!dto.isInIterativeMode && dto.iterateCount == 1) {
                        typeVariable = addHelperVariable(dto, typeParameter).getTypeVariable();
                    } else if (dto.isInIterativeMode && dto.iterateCount == 1) {
                        typeVariable = searchTypeVariable(dto, typeParameter);
                        if (typeVariable == null) {
                            typeVariable = addHelperVariable(dto, typeParameter).getTypeVariable();
                        }
                    } else {
                        dto.needToReIterate = true;
                        copy = null;
                        break parametricTypes;
                    }
                    typeParameters.add(typeVariable);
                }
                leftBindings.bind(parametricTypeSymbol, typeParameters);
            }
        }

        return copy;
    }

    private ITypeVariableReference addHelperVariable(AggregateBindingDto dto, String typeParameter) {
        ITypeVariableReference typeVariableReference = dto.bindings.createHelperVariable();
        dto.mapping.put(typeParameter, typeVariableReference);

        String typeVariable = typeVariableReference.getTypeVariable();
        applyRightToLeft(dto, typeVariable, typeParameter);

        return typeVariableReference;
    }

    //Warning! start code duplication - very similar as in addLowerRefInIterativeMode
    private String searchTypeVariable(AggregateBindingDto dto, String typeParameter) {
        IOverloadBindings rightBindings = dto.overload.getOverloadBindings();
        String typeVariable = null;
        if (rightBindings.hasLowerRefBounds(typeParameter)) {
            for (String refRefTypeVariable : rightBindings.getLowerRefBounds(typeParameter)) {
                if (dto.mapping.containsKey(refRefTypeVariable)) {
                    typeVariable = dto.mapping.get(refRefTypeVariable).getTypeVariable();
                    break;
                }
                searchTypeVariable(dto, refRefTypeVariable);
            }
        }
        return typeVariable;
    }
    //Warning! end code duplication - very similar as in addLowerRefInIterativeMode


    //Warning! start code duplication - very similar as in searchTypeVariable
    private void addLowerRefInIterativeMode(
            AggregateBindingDto dto, String left, String refTypeVariable) {
        IOverloadBindings rightBindings = dto.overload.getOverloadBindings();
        if (rightBindings.hasLowerRefBounds(refTypeVariable)) {
            for (String refRefTypeVariable : rightBindings.getLowerRefBounds(refTypeVariable)) {
                if (dto.mapping.containsKey(refRefTypeVariable)) {
                    dto.bindings.addLowerRefBound(left, dto.mapping.get(refRefTypeVariable));
                }
                addLowerRefInIterativeMode(dto, left, refRefTypeVariable);
            }
        }
    }
    //Warning! end code duplication - very similar as in searchTypeVariable

    private void addMostSpecificOverloadToWorklist(WorklistDto worklistDto, IConstraint constraint) {
        Collection<IFunctionType> overloads = constraint.getMethodSymbol().getOverloads();
        if (overloads.size() > 1) {
            List<IVariable> arguments = constraint.getArguments();
            List<ITypeSymbol> argumentTypes = calculateArgumentTypes(worklistDto, arguments);

            List<OverloadRankingDto> applicableOverloads
                    = getApplicableOverloads(worklistDto, constraint, overloads, argumentTypes);

            int numberOfApplicableOverloads = applicableOverloads.size();
            if (numberOfApplicableOverloads > 0) {
                OverloadRankingDto overloadRankingDto = applicableOverloads.get(0);
                if (numberOfApplicableOverloads > 1) {
                    overloadRankingDto = determineMostSpecific(
                            worklistDto, applicableOverloads, argumentTypes);
                }
                worklistDto.workDeque.add(nextWorklistDto(worklistDto, overloadRankingDto.bindings));
            } else if (!worklistDto.isSolvingMethod) {
                issueReporter.constraintViolation(worklistDto.overloadBindings, constraint);
                //TODO rstoll TINS-306 inference - runtime check insertion
                //I am not sure but maybe we do not need to do anything. see
                //TINS-399 save which overload was taken in AST
                //I think it is enough if the symbol does not contain any overload. The translator can then insert an
                // error in the output
            }
        } else {
            try {
                IFunctionType overload = overloads.iterator().next();
                AggregateBindingDto dto = solveOverLoad(worklistDto, constraint, overload);
                if (dto != null) {
                    worklistDto.workDeque.add(nextWorklistDto(worklistDto, dto.bindings));
                }
            } catch (BoundException ex) {
                if (!worklistDto.isSolvingMethod) {
                    issueReporter.constraintViolation(worklistDto.overloadBindings, constraint);
                }
            }
        }
    }

    private List<ITypeSymbol> calculateArgumentTypes(WorklistDto worklistDto, List<IVariable> arguments) {
        List<ITypeSymbol> argumentTypes = new ArrayList<>(arguments.size());

        IOverloadBindings overloadBindings = worklistDto.overloadBindings;
        for (IVariable variable : arguments) {
            String argumentId = variable.getAbsoluteName();
            ITypeVariableReference reference = overloadBindings.getTypeVariableReference(argumentId);
            String typeVariable = reference.getTypeVariable();

            //we prefer the upper type bound and when solving methods and the lower type bound when solving global
            // namespace scope
            IContainerTypeSymbol argumentType = null;
            if (worklistDto.isSolvingMethod && overloadBindings.hasUpperTypeBounds(typeVariable)) {
                argumentType = overloadBindings.getUpperTypeBounds(typeVariable);
            } else if (overloadBindings.hasLowerTypeBounds(typeVariable)) {
                argumentType = overloadBindings.getLowerTypeBounds(typeVariable);
            } else if (!worklistDto.isSolvingMethod && overloadBindings.hasUpperTypeBounds(typeVariable)) {
                argumentType = overloadBindings.getUpperTypeBounds(typeVariable);
            }

            if (argumentType != null && !argumentType.isFixed()) {
                //overload has non fixed arguments - since we manipulate it we have to copy it first...
                overloadBindings = symbolFactory.createOverloadBindings(overloadBindings);
                //.. then we fix all bounded types (in a brute force way - overloadBinding cannot be used for
                // constraint solving afterwards)
                overloadBindings.fixTypeParameters();
            }
            argumentTypes.add(argumentType);
        }
        return argumentTypes;
    }

    private List<OverloadRankingDto> getApplicableOverloads(
            WorklistDto worklistDto,
            IConstraint constraint,
            Collection<IFunctionType> overloads,
            List<ITypeSymbol> argumentTypes) {

        List<OverloadRankingDto> overloadBindingsList = new ArrayList<>();
        List<IVariable> arguments = constraint.getArguments();
        int numberOfArguments = arguments.size();

        for (IFunctionType overload : overloads) {
            if (numberOfArguments >= overload.getNumberOfNonOptionalParameters()) {
                try {
                    IOverloadBindings bindings = symbolFactory.createOverloadBindings(worklistDto.overloadBindings);
                    AggregateBindingDto dto = new AggregateBindingDto(
                            constraint, overload, bindings, worklistDto.isInIterativeMode);
                    aggregateBinding(dto);

                    //no need to check for one to one match if arguments were narrowed unless we solve constraints of
                    // the global default namespace scope - moreover, if the overload is not fixed,
                    // then we cannot use the check since there might be a fixed correspondence
                    // and last but not least, we cannot check whether an overload was fixed if it was not simplified
                    // yet which is only the case for indirect recursive functions which are still analysed (hence we
                    // are in iterative mode)
                    boolean isOneToOne = overload.wasSimplified() && overload.isFixed()
                            && (!dto.hasNarrowedArguments || !worklistDto.isSolvingMethod);

                    if (isOneToOne) {
                        isOneToOne = isOneToOneMatch(numberOfArguments, argumentTypes, overload);
                        if (isOneToOne) {
                            //reset list, we will break bellow
                            overloadBindingsList = new ArrayList<>(1);
                        }
                    }

                    overloadBindingsList.add(new OverloadRankingDto(
                            overload, dto.bindings, dto.implicitConversionCounter, dto.hasNarrowedArguments));

                    if (isOneToOne) {
                        break;
                    }
                } catch (BoundException ex) {
                    //That is ok, we are looking for applicable overloads
                }
            }
        }
        return overloadBindingsList;
    }

    private boolean isOneToOneMatch(int numberOfArguments, List<ITypeSymbol> argumentTypes, IFunctionType overload) {
        boolean isOneToOne = true;
        IOverloadBindings rightBindings = overload.getOverloadBindings();

        List<IVariable> parameters = overload.getParameters();
        int numberOfParameters = parameters.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;
        for (int i = 0; i < count; ++i) {
            IVariable variable = parameters.get(i);
            String argumentId = variable.getAbsoluteName();
            ITypeVariableReference reference = rightBindings.getTypeVariableReference(argumentId);
            String typeVariable = reference.getTypeVariable();
            //TODO TINS-418 function application only consider upper bounds 0.4.1
            ITypeSymbol argumentType = argumentTypes.get(i);
            if (rightBindings.hasUpperTypeBounds(typeVariable)) {
                IIntersectionTypeSymbol parameterType = rightBindings.getUpperTypeBounds(typeVariable);
                if (argumentType == null) {
                    argumentType = mixedTypeSymbol;
                }
                if (!typeHelper.areSame(argumentType, parameterType)) {
                    isOneToOne = false;
                    break;
                }
            } else if (argumentType != null) {
                isOneToOne = false;
                break;
            }
        }
        return isOneToOne;
    }

    private OverloadRankingDto determineMostSpecific(
            WorklistDto worklistDto, List<OverloadRankingDto> applicableOverloads, List<ITypeSymbol> argumentTypes) {

        List<OverloadRankingDto> overloadRankingDtos = filterOverload(applicableOverloads);
        OverloadRankingDto overloadRankingDto = overloadRankingDtos.get(0);
        if (overloadRankingDtos.size() > 1) {
            overloadRankingDtos = fixOverloads(worklistDto, overloadRankingDtos);

            int numberOfParameters = overloadRankingDto.overload.getParameters().size();
            List<Pair<ITypeSymbol, ITypeSymbol>> bounds = getParameterBounds(overloadRankingDtos, numberOfParameters);
            overloadRankingDtos = getMostSpecificApplicableOverload(overloadRankingDtos, bounds);

            overloadRankingDto = overloadRankingDtos.get(0);
            if (overloadRankingDtos.size() != 1) {
                if (!worklistDto.isInIterativeMode || overloadRankingDto.mostSpecificUpperCount < numberOfParameters) {
                    throw new AmbiguousOverloadException(argumentTypes, overloadRankingDtos);
                }
            }
        }
        return overloadRankingDto;
    }

    private List<OverloadRankingDto> filterOverload(List<OverloadRankingDto> applicableOverloads) {
        //can be null since the first applicable overload will certainly not have Interger.MAX_VALUE implicit
        // converions and hence initialise overloadRankingDtos
        List<OverloadRankingDto> overloadRankingDtos = null;
        int minNumberOfImplicitConversions = Integer.MAX_VALUE;
        boolean wereArgumentsNarrowed = true;

        for (OverloadRankingDto dto : applicableOverloads) {
            if (wereArgumentsNarrowed == dto.hasNarrowedArguments
                    && dto.numberOfImplicitConversions == minNumberOfImplicitConversions) {
                overloadRankingDtos.add(dto);
            } else if (wereArgumentsNarrowed && !dto.hasNarrowedArguments
                    || dto.numberOfImplicitConversions < minNumberOfImplicitConversions) {
                wereArgumentsNarrowed = dto.hasNarrowedArguments;
                minNumberOfImplicitConversions = dto.numberOfImplicitConversions;
                overloadRankingDtos = new ArrayList<>();
                overloadRankingDtos.add(dto);
            }
        }
        return overloadRankingDtos;
    }

    private List<OverloadRankingDto> fixOverloads(
            WorklistDto worklistDto, List<OverloadRankingDto> applicableOverloads) {
        List<OverloadRankingDto> overloadRankingDtos = new ArrayList<>(applicableOverloads.size());

        for (OverloadRankingDto dto : applicableOverloads) {
            OverloadRankingDto overloadRankingDto;
            if (dto.overload.wasSimplified()) {
                overloadRankingDto = fixOverload(dto);
            } else if (worklistDto.isInIterativeMode) {
                overloadRankingDto = fixOverloadInIterativeMode(dto);
            } else {
                throw new IllegalStateException("function " + dto.overload.getName() + " was not simplified "
                        + "and we are not in iterative mode.");
            }
            overloadRankingDto.bounds = new ArrayList<>();
            overloadRankingDtos.add(overloadRankingDto);
        }

        return overloadRankingDtos;
    }

    private OverloadRankingDto fixOverload(OverloadRankingDto dto) {
        IFunctionType overload = dto.overload;

        if (!overload.isFixed()) {
            IOverloadBindings overloadBindings = overload.getOverloadBindings();
            IOverloadBindings copyBindings = symbolFactory.createOverloadBindings(overloadBindings);
            IFunctionType copyOverload = symbolFactory.createFunctionType(
                    overload.getName(), copyBindings, overload.getParameters());

            Collection<String> nonFixedTypeParameters = new ArrayList<>(overload.getNonFixedTypeParameters());
            for (String nonFixedTypeParameter : nonFixedTypeParameters) {
                copyBindings.fixTypeParameter(nonFixedTypeParameter);
            }

            copyOverload.manuallySimplified(
                    Collections.<String>emptySet(),
                    overload.getNumberOfConvertibleApplications(),
                    overload.hasConvertibleParameterTypes());

            dto = new OverloadRankingDto(
                    copyOverload, dto.bindings, dto.numberOfImplicitConversions, dto.hasNarrowedArguments);
            dto.numberOfTypeParameters = nonFixedTypeParameters.size();
        }
        return dto;
    }

    private OverloadRankingDto fixOverloadInIterativeMode(OverloadRankingDto dto) {
        IOverloadBindings overloadBindings = symbolFactory.createOverloadBindings(dto.overload.getOverloadBindings());
        overloadBindings.fixTypeParameters();
        IFunctionType copyOverload = symbolFactory.createFunctionType(
                dto.overload.getName(), overloadBindings, dto.overload.getParameters());

        return new OverloadRankingDto(
                copyOverload, dto.bindings, dto.numberOfImplicitConversions, dto.hasNarrowedArguments);
    }

    private List<Pair<ITypeSymbol, ITypeSymbol>> getParameterBounds(List<OverloadRankingDto> fixedOverloads, int size) {

        List<Pair<ITypeSymbol, ITypeSymbol>> bounds = new ArrayList<>(size);

        for (int i = 0; i < size; ++i) {
            //the most general type is the most specific lower bound
            ITypeSymbol mostSpecificLowerBound = null;
            ITypeSymbol mostSpecificUpperBound = null;

            for (OverloadRankingDto dto : fixedOverloads) {
                IOverloadBindings bindings = dto.overload.getOverloadBindings();
                IVariable parameter = dto.overload.getParameters().get(i);
                ITypeVariableReference reference = bindings.getTypeVariableReference(parameter.getAbsoluteName());
                String parameterTypeVariable = reference.getTypeVariable();
                IUnionTypeSymbol lowerBound = null;
                IIntersectionTypeSymbol upperBound = null;
                if (bindings.hasLowerTypeBounds(parameterTypeVariable)) {
                    lowerBound = bindings.getLowerTypeBounds(parameterTypeVariable);
                    if (mostSpecificLowerBound != null) {
                        TypeHelperDto result = typeHelper.isFirstSameOrParentTypeOfSecond(
                                lowerBound, mostSpecificLowerBound);
                        ERelation relation = result.relation;
                        if (relation == ERelation.HAS_COERCIVE_RELATION) {
                            TypeHelperDto result2 = typeHelper.isFirstSameOrParentTypeOfSecond(
                                    mostSpecificLowerBound, lowerBound, false);
                            //we prefer a non coercive relation, hence if the current lower is a parent type of the
                            // new (without coercive subtyping) then it is more specific
                            if (result2.relation == ERelation.HAS_RELATION) {
                                relation = ERelation.HAS_NO_RELATION;
                            }
                        }
                        if (relation != ERelation.HAS_NO_RELATION) {
                            mostSpecificLowerBound = lowerBound;
                        }
                    } else {
                        mostSpecificLowerBound = lowerBound;
                    }
                }
                if (bindings.hasUpperTypeBounds(parameterTypeVariable)) {
                    upperBound = bindings.getUpperTypeBounds(parameterTypeVariable);
                    if (mostSpecificUpperBound != null) {
                        TypeHelperDto result = typeHelper.isFirstSameOrSubTypeOfSecond(
                                upperBound, mostSpecificUpperBound);
                        ERelation relation = result.relation;
                        if (relation == ERelation.HAS_COERCIVE_RELATION) {
                            TypeHelperDto result2 = typeHelper.isFirstSameOrSubTypeOfSecond(
                                    mostSpecificUpperBound, upperBound, false);
                            //we prefer a non coercive relation, hence if the current upper is a subtype of the
                            // new (without coercive subtyping) then it is more specific
                            if (result2.relation == ERelation.HAS_RELATION) {
                                relation = ERelation.HAS_NO_RELATION;
                            }
                        }
                        if (relation != ERelation.HAS_NO_RELATION) {
                            mostSpecificUpperBound = upperBound;
                        }
                    } else {
                        mostSpecificUpperBound = upperBound;
                    }
                }
                dto.bounds.add(pair(lowerBound, upperBound));
            }
            bounds.add(pair(mostSpecificLowerBound, mostSpecificUpperBound));
        }
        return bounds;
    }

    public List<OverloadRankingDto> getMostSpecificApplicableOverload(
            List<OverloadRankingDto> overloadBindingsList,
            List<Pair<ITypeSymbol, ITypeSymbol>> boundsList) {
        List<OverloadRankingDto> mostSpecificOverloads = new ArrayList<>(3);

        int numberOfParameters = boundsList.size();
        OverloadRankingDto mostSpecificDto = overloadBindingsList.get(0);

        int overloadSize = overloadBindingsList.size();
        for (int i = 0; i < overloadSize; ++i) {
            OverloadRankingDto dto = overloadBindingsList.get(i);
            for (int j = 0; j < numberOfParameters; ++j) {
                Pair<ITypeSymbol, ITypeSymbol> mostSpecificBound = boundsList.get(j);
                Pair<IUnionTypeSymbol, IIntersectionTypeSymbol> bound = dto.bounds.get(j);
                if (isMostGeneralLowerBound(bound.first, mostSpecificBound.first)) {
                    ++dto.mostGeneralLowerCount;
                }
                if (isMostSpecificUpperBound(bound.second, mostSpecificBound.second)) {
                    ++dto.mostSpecificUpperCount;
                }
            }

            int diff = compare(dto, mostSpecificDto);
            if (diff > 0) {
                if (mostSpecificOverloads.size() > 0) {
                    mostSpecificOverloads = new ArrayList<>(3);
                    mostSpecificOverloads.add(dto);
                }
                mostSpecificDto = dto;
                if (isMostSpecific(dto, numberOfParameters)) {
                    break;
                }
            } else if (diff == 0) {
                mostSpecificOverloads.add(dto);
            }
        }

        return mostSpecificOverloads;
    }

    private boolean isMostGeneralLowerBound(ITypeSymbol lowerBound, ITypeSymbol mostGeneralLowerBound) {
        if (lowerBound == null) {
            return mostGeneralLowerBound == null;
        } else {
            return typeHelper.areSame(lowerBound, mostGeneralLowerBound);
        }
    }

    private boolean isMostSpecificUpperBound(ITypeSymbol upperBound, ITypeSymbol mostSpecificUpperBound) {
        if (upperBound == null) {
            return mostSpecificUpperBound == null
                    || typeHelper.areSame(mixedTypeSymbol, mostSpecificUpperBound);
        } else {
            return typeHelper.areSame(upperBound, mostSpecificUpperBound);
        }
    }

    private boolean isMostSpecific(OverloadRankingDto dto, int numberOfParameters) {
        return dto.mostGeneralLowerCount == numberOfParameters
                && dto.mostSpecificUpperCount == numberOfParameters;
    }

    private int compare(OverloadRankingDto dto, OverloadRankingDto currentMostSpecific) {
        int diff = dto.mostSpecificUpperCount - currentMostSpecific.mostSpecificUpperCount;
        if (diff == 0) {
            diff = dto.mostGeneralLowerCount - currentMostSpecific.mostGeneralLowerCount;
            if (diff == 0) {
                diff = currentMostSpecific.numberOfTypeParameters - dto.numberOfTypeParameters;
                if (diff == 0) {
                    diff = currentMostSpecific.numberOfImplicitConversions - dto.numberOfImplicitConversions;
                }
            }
        }
        return diff;
    }

    private void createOverloads(IMethodSymbol methodSymbol, List<IOverloadBindings> bindingsList) {
        for (IOverloadBindings bindings : bindingsList) {
            createOverload(methodSymbol, bindings);
        }
    }

    private IFunctionType createOverload(IMethodSymbol methodSymbol, IOverloadBindings bindings) {
        List<IVariable> parameters = new ArrayList<>();
        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            String parameterId = parameter.getAbsoluteName();
            if (!bindings.containsVariable(parameterId)) {
                //the parameter is not used at all, hence it can be mixed
                ITypeVariableReference reference = new FixedTypeVariableReference(bindings.getNextTypeVariable());
                bindings.addVariable(parameterId, reference);
                //TODO rstoll TINS-407 - store fixed type only in lower bound
                //TODO rstoll TINS-387 function application only consider upper bounds
                bindings.addLowerTypeBound(reference.getTypeVariable(), mixedTypeSymbol);
                bindings.addUpperTypeBound(reference.getTypeVariable(), mixedTypeSymbol);
                //TODO could generate a warning
            }
            IMinimalVariableSymbol parameterVariable = symbolFactory.createMinimalVariableSymbol(
                    parameter.getDefinitionAst(), parameter.getName());
            parameterVariable.setDefinitionScope(parameter.getDefinitionScope());
            parameters.add(parameterVariable);
        }

        IFunctionType functionType = symbolFactory.createFunctionType(methodSymbol.getName(), bindings, parameters);
        functionType.simplify();
        methodSymbol.addOverload(functionType);
        return functionType;
    }

}
