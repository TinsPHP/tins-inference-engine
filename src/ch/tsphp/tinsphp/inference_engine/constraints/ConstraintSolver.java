/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.TinsPHPConstants;
import ch.tsphp.tinsphp.common.inference.constraints.BoundException;
import ch.tsphp.tinsphp.common.inference.constraints.FixedTypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.TypeVariableReference;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.utils.IOverloadResolver;
import ch.tsphp.tinsphp.common.utils.MapHelper;
import ch.tsphp.tinsphp.common.utils.Pair;

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
import java.util.concurrent.ConcurrentHashMap;

import static ch.tsphp.tinsphp.common.utils.Pair.pair;

public class ConstraintSolver implements IConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final IOverloadResolver overloadResolver;
    private final IInferenceIssueReporter issueReporter;
    private final ITypeSymbol mixedTypeSymbol;
    private final Map<String, Set<String>> dependencies = new HashMap<>();
    private final Map<String, List<Pair<WorklistDto, Integer>>> directDependencies = new ConcurrentHashMap<>();
    private final Map<String, Set<WorklistDto>> unresolvedConstraints = new ConcurrentHashMap<>();
    private final Map<String, TempMethodSymbol> tempMethodSymbols = new HashMap<>();

    public ConstraintSolver(
            ISymbolFactory theSymbolFactory,
            IOverloadResolver theOverloadResolver,
            IInferenceIssueReporter theIssueReporter) {
        symbolFactory = theSymbolFactory;
        overloadResolver = theOverloadResolver;
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

        for (Set<WorklistDto> worklistDtos : unresolvedConstraints.values()) {
            Iterator<WorklistDto> iterator = worklistDtos.iterator();
            WorklistDto firstWorkListDto = iterator.next();
            IMethodSymbol methodSymbol = (IMethodSymbol) firstWorkListDto.constraintCollection;
            String absoluteName = methodSymbol.getAbsoluteName();
            if (directDependencies.containsKey(absoluteName)) {

                createOverloadsForRecursiveMethod(iterator, firstWorkListDto, methodSymbol);

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
                worklistDto.overloadBindings = iterator.next();
                while (iterator.hasNext()) {
                    WorklistDto newWorklistDto = new WorklistDto(worklistDto, 0, iterator.next());
                    createDependencies(newWorklistDto);
                }
            } else if (overloadBindingsList.size() == 1) {
                IOverloadBindings overloadBindings = overloadBindingsList.get(0);
                if (hasChanged(worklistDto, overloadBindings)) {
                    collectionsWhichChanged.add(absoluteName);
                }
                worklistDto.overloadBindings = overloadBindings;
            } else {
                Set<WorklistDto> dtos = unresolvedConstraints.get(absoluteName);
                if (dtos.remove(worklistDto)) {
                    collectionsWhichChanged.add(absoluteName);
                }
                if (dtos.isEmpty()) {
                    throw new UnsupportedOperationException("oho... recursion and soft typing");
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
                    || oldLowerType != null && overloadResolver.areSame(oldLowerType, newLowerType));

            if (!isNotTheSame) {
                IIntersectionTypeSymbol oldUpperType = oldBindings.getUpperTypeBounds(newTypeVariable);
                IIntersectionTypeSymbol newUpperType = newBindings.getUpperTypeBounds(newTypeVariable);
                isNotTheSame = !(oldUpperType == null && newUpperType == null
                        || oldUpperType != null && overloadResolver.areSame(oldUpperType, newUpperType));
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

        List<IOverloadBindings> solvedBindings = new ArrayList<>();
        solvedBindings.add(firstWorkListDto.overloadBindings);
        createOverload(methodSymbol, firstWorkListDto.overloadBindings);
        while (iterator.hasNext()) {
            WorklistDto worklistDto = iterator.next();
            IOverloadBindings overloadBindings = worklistDto.overloadBindings;
            solvedBindings.add(overloadBindings);
            createOverload(methodSymbol, overloadBindings);
        }
        methodSymbol.setBindings(solvedBindings);

    }

    private void solveDependenciesOfRecursiveMethod(String absoluteName) {
        for (Pair<WorklistDto, Integer> pair : directDependencies.get(absoluteName)) {
            WorklistDto worklistDto = pair.first;
            String refAbsoluteName = worklistDto.constraintCollection.getAbsoluteName();
            if (!directDependencies.containsKey(refAbsoluteName)) {
                //regular dependency solving for non recursive methods
                solveDependency(pair);
            } else {
                if (unresolvedConstraints.get(refAbsoluteName).contains(worklistDto)) {
                    //exchange applied overload, currently it is still pointing to the temp overload
                    IConstraint constraint = worklistDto.constraintCollection.getConstraints().get(pair.second);

                    addMostSpecificOverloadToWorklist(worklistDto, constraint);
                    WorklistDto tempWorklistDto = worklistDto.workDeque.removeFirst();
                    String lhsAbsoluteName = constraint.getLeftHandSide().getAbsoluteName();
                    IFunctionType appliedOverload
                            = tempWorklistDto.overloadBindings.getAppliedOverload(lhsAbsoluteName);
                    worklistDto.overloadBindings.setAppliedOverload(lhsAbsoluteName, appliedOverload);
                }
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
            addMostSpecificOverloadToWorklist(worklistDto, constraint);
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
            TypeVariableReference reference = bindings.getNextTypeVariable();
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
                IOverloadBindings bindings = solveOverLoad(worklistDto, constraint, overload);
                if (bindings != null) {
                    worklistDto.workDeque.add(nextWorklistDto(worklistDto, bindings));
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

    private IOverloadBindings solveOverLoad(
            WorklistDto worklistDto,
            IConstraint constraint,
            IFunctionType overload) {

        IOverloadBindings bindings = null;
        if (constraint.getArguments().size() >= overload.getNumberOfNonOptionalParameters()) {
            bindings = symbolFactory.createOverloadBindings(worklistDto.overloadBindings);
            AggregateBindingDto dto = new AggregateBindingDto(
                    constraint, overload, bindings, worklistDto.isInIterativeMode);
            aggregateBinding(dto);
        }
        return bindings;
    }

    private void aggregateBinding(AggregateBindingDto dto) {
        List<IVariable> arguments = dto.constraint.getArguments();
        List<IVariable> parameters = dto.overload.getParameters();
        int numberOfParameters = parameters.size();

        dto.mapping = new HashMap<>(numberOfParameters + 1);

        int numberOfArguments = arguments.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;

        boolean needToIterateOverload = true;
        while (needToIterateOverload) {

            IVariable leftHandSide = dto.constraint.getLeftHandSide();
            dto.bindingVariable = leftHandSide;
            dto.overloadVariableId = TinsPHPConstants.RETURN_VARIABLE_NAME;
            needToIterateOverload = !mergeTypeVariables(dto);

            boolean argumentsAreAllFixed = true;
            for (int i = 0; i < count; ++i) {
                dto.bindingVariable = arguments.get(i);
                dto.overloadVariableId = parameters.get(i).getAbsoluteName();
                boolean needToIterateParameter = !mergeTypeVariables(dto);

                needToIterateOverload = needToIterateOverload || needToIterateParameter;
                argumentsAreAllFixed = argumentsAreAllFixed
                        && dto.bindings.getTypeVariableReference(dto.bindingVariable.getAbsoluteName()).hasFixedType();
            }

            if (!needToIterateOverload) {
                String lhsAbsoluteName = leftHandSide.getAbsoluteName();
                ITypeVariableReference reference = dto.bindings.getTypeVariableReference(lhsAbsoluteName);
                if (!reference.hasFixedType() && argumentsAreAllFixed) {
                    dto.bindings.fixType(lhsAbsoluteName);
                }
                if (!lhsAbsoluteName.equals(TinsPHPConstants.RETURN_VARIABLE_NAME)) {
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

    private boolean mergeTypeVariables(AggregateBindingDto dto) {

        String bindingVariableName = dto.bindingVariable.getAbsoluteName();
        ITypeVariableReference bindingTypeVariableReference =
                dto.bindings.getTypeVariableReference(bindingVariableName);
        IOverloadBindings overloadBindings = dto.overload.getBindings();
        String overloadTypeVariable =
                overloadBindings.getTypeVariableReference(dto.overloadVariableId).getTypeVariable();

        String lhsTypeVariable;
        if (dto.mapping.containsKey(overloadTypeVariable)) {
            lhsTypeVariable = dto.mapping.get(overloadTypeVariable).getTypeVariable();
            String rhsTypeVariable = bindingTypeVariableReference.getTypeVariable();
            dto.bindings.renameTypeVariable(rhsTypeVariable, lhsTypeVariable);
        } else {
            lhsTypeVariable = bindingTypeVariableReference.getTypeVariable();
            dto.mapping.put(overloadTypeVariable, bindingTypeVariableReference);
        }

        return applyRightToLeft(dto, lhsTypeVariable, overloadBindings, overloadTypeVariable);
    }

    private boolean applyRightToLeft(
            AggregateBindingDto dto, String left, IOverloadBindings rightBindings, String right) {

        IOverloadBindings leftBindings = dto.bindings;

        if (rightBindings.hasUpperTypeBounds(right)) {
            leftBindings.addUpperTypeBound(left, rightBindings.getUpperTypeBounds(right));
        }

        if (rightBindings.hasLowerTypeBounds(right)) {
            leftBindings.addLowerTypeBound(left, rightBindings.getLowerTypeBounds(right));
        }

        boolean couldAddLower = true;
        if (rightBindings.hasLowerRefBounds(right)) {
            for (String refTypeVariable : rightBindings.getLowerRefBounds(right)) {
                if (dto.mapping.containsKey(refTypeVariable)) {
                    leftBindings.addLowerRefBound(left, dto.mapping.get(refTypeVariable));
                } else if (dto.isInIterativeMode && dto.iterateCount == 1) {
                    addLowerRefInIterativeMode(dto, left, rightBindings, refTypeVariable);
                } else {
                    couldAddLower = false;
                    break;
                }
            }
        }

        return couldAddLower;
    }

    private void addLowerRefInIterativeMode(
            AggregateBindingDto dto, String left, IOverloadBindings rightBindings, String refTypeVariable) {
        if (rightBindings.hasLowerRefBounds(refTypeVariable)) {
            for (String refRefTypeVariable : rightBindings.getLowerRefBounds(refTypeVariable)) {
                if (dto.mapping.containsKey(refRefTypeVariable)) {
                    dto.bindings.addLowerRefBound(left, dto.mapping.get(refRefTypeVariable));
                }
                addLowerRefInIterativeMode(dto, left, rightBindings, refRefTypeVariable);
            }
        }
    }

    private void addMostSpecificOverloadToWorklist(WorklistDto worklistDto, IConstraint constraint) {
        List<OverloadRankingDto> applicableOverloads = getApplicableOverloads(worklistDto, constraint);

        int numberOfApplicableOverloads = applicableOverloads.size();
        if (numberOfApplicableOverloads > 0) {
            OverloadRankingDto overloadRankingDto = applicableOverloads.get(0);
            if (numberOfApplicableOverloads > 1) {
                int size = overloadRankingDto.overload.getParameters().size();
                List<Pair<IUnionTypeSymbol, IIntersectionTypeSymbol>> bounds
                        = getParameterBounds(applicableOverloads, size);

                List<OverloadRankingDto> overloadRankingDtos
                        = getMostSpecificApplicableOverload(applicableOverloads, bounds);

                if (overloadRankingDtos.size() == 1) {
                    overloadRankingDto = overloadRankingDtos.get(0);
                } else {
                    //TODO unify most specific
                }
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
    }

    private List<OverloadRankingDto> getApplicableOverloads(WorklistDto worklistDto, IConstraint constraint) {

        List<OverloadRankingDto> overloadBindingsList = new ArrayList<>();

        for (IFunctionType overload : constraint.getMethodSymbol().getOverloads()) {
            try {
                IOverloadBindings bindings = solveOverLoad(worklistDto, constraint, overload);
                if (bindings != null) {
                    overloadBindingsList.add(new OverloadRankingDto(overload, bindings));
                }
            } catch (BoundException ex) {
                //That is ok, we are looking for applicable overloads
            }
        }
        return overloadBindingsList;
    }

    private List<Pair<IUnionTypeSymbol, IIntersectionTypeSymbol>> getParameterBounds(
            List<OverloadRankingDto> overloadBindingsList, int size) {

        List<Pair<IUnionTypeSymbol, IIntersectionTypeSymbol>> bounds = new ArrayList<>(size);

        for (int i = 0; i < size; ++i) {
            IUnionTypeSymbol unionTypeSymbol = symbolFactory.createUnionTypeSymbol();
            IIntersectionTypeSymbol intersectionTypeSymbol = symbolFactory.createIntersectionTypeSymbol();

            for (OverloadRankingDto dto : overloadBindingsList) {
                IOverloadBindings bindings = dto.overload.getBindings();
                IVariable parameter = dto.overload.getParameters().get(i);
                ITypeVariableReference reference = bindings.getTypeVariableReference(parameter.getAbsoluteName());
                String parameterTypeVariable = reference.getTypeVariable();
                if (bindings.hasLowerTypeBounds(parameterTypeVariable)) {
                    dto.lowerBound = bindings.getLowerTypeBounds(parameterTypeVariable);
                    unionTypeSymbol.addTypeSymbol(dto.lowerBound);
                }
                if (bindings.hasUpperTypeBounds(parameterTypeVariable)) {
                    dto.upperBound = bindings.getUpperTypeBounds(parameterTypeVariable);
                    intersectionTypeSymbol.addTypeSymbol(dto.upperBound);
                }
            }
            bounds.add(pair(unionTypeSymbol, intersectionTypeSymbol));
        }
        return bounds;
    }


    public List<OverloadRankingDto> getMostSpecificApplicableOverload(
            List<OverloadRankingDto> overloadBindingsList,
            List<Pair<IUnionTypeSymbol, IIntersectionTypeSymbol>> boundsList) {
        List<OverloadRankingDto> mostSpecificOverloads = new ArrayList<>(3);

        int numberOfParameters = boundsList.size();
        OverloadRankingDto mostSpecificDto = overloadBindingsList.get(0);

        int overloadSize = overloadBindingsList.size();
        for (int i = 0; i < overloadSize; ++i) {
            OverloadRankingDto dto = overloadBindingsList.get(i);
            for (int j = 0; j < numberOfParameters; ++j) {
                Pair<IUnionTypeSymbol, IIntersectionTypeSymbol> bounds = boundsList.get(j);
                if (isMostGeneralLowerBound(dto, bounds.first)) {
                    ++dto.mostGeneralLowerCount;
                }
                if (isMostSpecificUpperBound(dto, bounds.second)) {
                    ++dto.mostSpecificUpperCount;
                }
            }

            int diff = compare(dto, mostSpecificDto);
            if (diff > 0) {
                if (mostSpecificOverloads.size() > 0) {
                    mostSpecificOverloads = new ArrayList<>(3);
                }
                mostSpecificDto = dto;
                if (isMostSpecific(dto, numberOfParameters)) {
                    break;
                }
            } else if (diff == 0) {
                mostSpecificOverloads.add(dto);
            }
        }
        mostSpecificOverloads.add(mostSpecificDto);
        return mostSpecificOverloads;
    }

    private boolean isMostGeneralLowerBound(OverloadRankingDto dto, IUnionTypeSymbol mostGeneralLowerBound) {
        if (dto.lowerBound == null) {
            return mostGeneralLowerBound.getTypeSymbols().size() == 0;
        } else {
            return overloadResolver.areSame(dto.lowerBound, mostGeneralLowerBound);
        }
    }

    private boolean isMostSpecificUpperBound(OverloadRankingDto dto, IIntersectionTypeSymbol mostSpecificUpperBound) {
        if (dto.upperBound == null) {
            return overloadResolver.areSame(mixedTypeSymbol, mostSpecificUpperBound);
        } else {
            return overloadResolver.areSame(dto.upperBound, mostSpecificUpperBound);
        }
    }

    private boolean isMostSpecific(OverloadRankingDto dto, int numberOfParameters) {
        return dto.mostGeneralLowerCount == numberOfParameters
                && dto.mostSpecificUpperCount == numberOfParameters;
    }

    private int compare(OverloadRankingDto dto, OverloadRankingDto currentMostSpecific) {
        int diff = dto.mostSpecificUpperCount - currentMostSpecific.mostSpecificUpperCount;
        if (diff == 0) {
            diff = currentMostSpecific.numberOfTypeParameter - dto.numberOfTypeParameter;
        }
        return diff;
    }

    private void createOverloads(IMethodSymbol methodSymbol, List<IOverloadBindings> bindingsList) {
        for (IOverloadBindings bindings : bindingsList) {
            createOverload(methodSymbol, bindings);
        }
    }

    private void createOverload(IMethodSymbol methodSymbol, IOverloadBindings bindings) {
        Set<String> parameterTypeVariables = getParameterTypeVariables(methodSymbol, bindings);

        bindings.tryToFix(parameterTypeVariables);

        List<IVariable> parameters = new ArrayList<>();
        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            IMinimalVariableSymbol parameterVariable = symbolFactory.createMinimalVariableSymbol(
                    parameter.getDefinitionAst(), parameter.getName());
            parameterVariable.setDefinitionScope(parameter.getDefinitionScope());
            parameters.add(parameterVariable);
        }

        IFunctionType functionType = symbolFactory.createFunctionType(methodSymbol.getName(), bindings, parameters);
        methodSymbol.addOverload(functionType);
    }

    private Set<String> getParameterTypeVariables(IMethodSymbol methodSymbol, IOverloadBindings bindings) {

        Set<String> parameterTypeVariables = new HashSet<>();
        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            String parameterId = parameter.getAbsoluteName();
            if (bindings.containsVariable(parameterId)) {
                parameterTypeVariables.add(bindings.getTypeVariableReference(parameterId).getTypeVariable());
            } else {
                //the parameter is not used at all, hence it can be mixed
                ITypeVariableReference reference = new FixedTypeVariableReference(bindings.getNextTypeVariable());
                bindings.addVariable(parameterId, reference);
                //TODO rstoll TINS-407 - store fixed type only in lower bound
                //TODO rstoll TINS-387 function application only consider upper bounds
                bindings.addLowerTypeBound(reference.getTypeVariable(), mixedTypeSymbol);
                bindings.addUpperTypeBound(reference.getTypeVariable(), mixedTypeSymbol);
                //TODO could generate a warning
            }
        }
        return parameterTypeVariables;
    }
}
