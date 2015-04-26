/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.symbols.ITypeSymbol;
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
import ch.tsphp.tinsphp.symbols.TypeVariableNames;
import ch.tsphp.tinsphp.symbols.constraints.BoundException;
import ch.tsphp.tinsphp.symbols.constraints.OverloadBindings;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.tsphp.tinsphp.common.utils.Pair.pair;

public class ConstraintSolver implements IConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final IOverloadResolver overloadResolver;
    private final IInferenceIssueReporter issueReporter;
    private final ITypeSymbol mixedTypeSymbol;

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
    public void solveConstraints(List<IMethodSymbol> methodSymbols) {
        Map<String, List<Pair<IMethodSymbol, Deque<WorklistDto>>>> dependencies = new HashMap<>();
        for (IMethodSymbol methodSymbol : methodSymbols) {
            Deque<WorklistDto> workDeque = createInitialWorklist(false);
            solveMethodConstraints(dependencies, methodSymbol, workDeque);
        }

        if (!dependencies.isEmpty()) {
            //TODO need to iterate
        }
    }

    @Override
    public void solveConstraints(IGlobalNamespaceScope globalDefaultNamespaceScope) {
        if (!globalDefaultNamespaceScope.getConstraints().isEmpty()) {
            Deque<WorklistDto> workDeque = createInitialWorklist(true);
            List<IOverloadBindings> bindings = solveConstraints(globalDefaultNamespaceScope, workDeque);
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
    }

    private Deque<WorklistDto> createInitialWorklist(boolean isSolvingGlobalDefaultNamespace) {
        IOverloadBindings bindings = new OverloadBindings(symbolFactory, overloadResolver);
        Deque<WorklistDto> workDeque = new ArrayDeque<>();
        workDeque.add(new WorklistDto(workDeque, 0, isSolvingGlobalDefaultNamespace, bindings));
        return workDeque;
    }

    private void solveMethodConstraints(
            Map<String, List<Pair<IMethodSymbol, Deque<WorklistDto>>>> dependencies,
            IMethodSymbol methodSymbol,
            Deque<WorklistDto> workDeque) {
        try {
            List<IOverloadBindings> bindings = solveConstraints(methodSymbol, workDeque);
            methodSymbol.setBindings(bindings);
            createOverloads(methodSymbol, bindings);

            String methodName = methodSymbol.getAbsoluteName();
            if (dependencies.containsKey(methodName)) {
                for (Pair<IMethodSymbol, Deque<WorklistDto>> pair : dependencies.remove(methodName)) {
                    solveMethodConstraints(dependencies, pair.first, pair.second);
                }
            }
        } catch (NoOverloadsException ex) {
            MapHelper.addToListMap(dependencies, ex.getDependency(), pair(ex.getMethodSymbol(), ex.getWorklist()));
            //register dependency
        }
    }

    private List<IOverloadBindings> solveConstraints(IConstraintCollection constraintCollection,
            Deque<WorklistDto> workDeque) {
        List<IOverloadBindings> solvedBindings = new ArrayList<>();

        List<IConstraint> constraints = constraintCollection.getConstraints();
        while (!workDeque.isEmpty()) {
            WorklistDto worklistDto = workDeque.removeFirst();
            if (worklistDto.pointer < constraints.size()) {
                IConstraint constraint = constraints.get(worklistDto.pointer);
                solveConstraint(constraintCollection, workDeque, worklistDto, constraint);
            } else {
                solvedBindings.add(worklistDto.overloadBindings);
            }
        }

        if (solvedBindings.size() == 0) {
            //TODO error case if no overload could be found
        }

        return solvedBindings;
    }

    private void solveConstraint(IConstraintCollection constraintCollection, Deque<WorklistDto> workDeque,
            WorklistDto worklistDto, IConstraint constraint) {
        IMinimalMethodSymbol refMethodSymbol = constraint.getMethodSymbol();
        if (refMethodSymbol.getOverloads().size() != 0) {
            solve(worklistDto, constraint);
        } else {
            workDeque.add(worklistDto);
            //global default scope is solved after methods symbols have been solved, hence constraintCollection
            //must be an IMethodSymbol
            throw new NoOverloadsException(
                    workDeque, (IMethodSymbol) constraintCollection, refMethodSymbol.getAbsoluteName());
        }
    }

    private void solve(WorklistDto worklistDto, IConstraint constraint) {
        createBindingIfNecessary(worklistDto.overloadBindings, constraint.getLeftHandSide());
        boolean atLeastOneBindingCreated = false;
        for (IVariable argument : constraint.getArguments()) {
            boolean neededBinding = createBindingIfNecessary(worklistDto.overloadBindings, argument);
            atLeastOneBindingCreated = atLeastOneBindingCreated || neededBinding;
        }

        if (atLeastOneBindingCreated) {
            addApplicableOverloadsToWorklist(worklistDto, constraint);
        } else {
            addMostSpecificOverloadToWorklist(worklistDto, constraint);
        }
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
                    worklistDto.workDeque.add(new WorklistDto(worklistDto, bindings));
                }
            } catch (BoundException ex) {
                //That is ok, we will deal with it in solveConstraints
            }
        }
    }

    private IOverloadBindings solveOverLoad(
            WorklistDto worklistDto,
            IConstraint constraint,
            IFunctionType overload) {

        IOverloadBindings bindings = null;
        if (constraint.getArguments().size() >= overload.getNumberOfNonOptionalParameters()) {
            bindings = new OverloadBindings((OverloadBindings) worklistDto.overloadBindings);
            aggregateBinding(constraint, overload, bindings);
        }
        return bindings;
    }

    private void aggregateBinding(
            IConstraint constraint,
            IFunctionType overload,
            IOverloadBindings bindings) {

        List<IVariable> arguments = constraint.getArguments();
        List<IVariable> parameters = overload.getParameters();
        int numberOfParameters = parameters.size();
        Map<String, ITypeVariableReference> mapping = new HashMap<>(numberOfParameters + 1);
        int numberOfArguments = arguments.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;

        int iterateCount = 0;
        boolean needToIterateOverload = true;
        while (needToIterateOverload) {

            IVariable leftHandSide = constraint.getLeftHandSide();
            needToIterateOverload = !mergeTypeVariables(
                    bindings, overload, mapping, leftHandSide, TypeVariableNames.RETURN_VARIABLE_NAME);

            boolean argumentsAreAllFixed = true;
            for (int i = 0; i < count; ++i) {
                IVariable argument = arguments.get(i);
                String parameterId = parameters.get(i).getAbsoluteName();
                boolean needToIterateParameter = !mergeTypeVariables(
                        bindings, overload, mapping, argument, parameterId);

                needToIterateOverload = needToIterateOverload || needToIterateParameter;
                argumentsAreAllFixed = argumentsAreAllFixed
                        && bindings.getTypeVariableReference(argument.getAbsoluteName()).hasFixedType();
            }

            if (!needToIterateOverload) {
                String lhsAbsoluteName = leftHandSide.getAbsoluteName();
                ITypeVariableReference reference = bindings.getTypeVariableReference(lhsAbsoluteName);
                if (!reference.hasFixedType() && argumentsAreAllFixed) {
                    bindings.fixType(lhsAbsoluteName);
                }
                if (!lhsAbsoluteName.equals(TypeVariableNames.RETURN_VARIABLE_NAME)) {
                    bindings.setAppliedOverload(lhsAbsoluteName, overload);
                }
            }

            if (iterateCount > 1) {
                throw new IllegalStateException("overload uses type variables "
                        + "which are not part of the signature.");
            }
            ++iterateCount;
        }
    }

    private boolean mergeTypeVariables(
            IOverloadBindings bindings,
            IFunctionType overload,
            Map<String, ITypeVariableReference> mapping,
            IVariable bindingVariable,
            String overloadVariableId) throws BoundException {
        String bindingVariableName = bindingVariable.getAbsoluteName();
        ITypeVariableReference bindingTypeVariableReference = bindings.getTypeVariableReference(bindingVariableName);
        IOverloadBindings overloadBindings = overload.getBindings();
        String overloadTypeVariable = overloadBindings.getTypeVariableReference(overloadVariableId).getTypeVariable();

        String lhsTypeVariable;
        if (mapping.containsKey(overloadTypeVariable)) {
            lhsTypeVariable = mapping.get(overloadTypeVariable).getTypeVariable();
            String rhsTypeVariable = bindingTypeVariableReference.getTypeVariable();
            bindings.renameTypeVariable(rhsTypeVariable, lhsTypeVariable);
        } else {
            lhsTypeVariable = bindingTypeVariableReference.getTypeVariable();
            mapping.put(overloadTypeVariable, bindingTypeVariableReference);
        }


        return applyRightToLeft(
                mapping, bindings, lhsTypeVariable, overloadBindings, overloadTypeVariable);
    }

    private boolean applyRightToLeft(
            Map<String, ITypeVariableReference> mapping,
            IOverloadBindings leftBindings, String left,
            IOverloadBindings rightBindings, String right) throws BoundException {

        if (rightBindings.hasUpperTypeBounds(right)) {
            leftBindings.addUpperTypeBound(left, rightBindings.getUpperTypeBounds(right));
        }

        if (rightBindings.hasLowerTypeBounds(right)) {
            leftBindings.addLowerTypeBound(left, rightBindings.getLowerTypeBounds(right));
        }

        boolean couldAddLower = true;
        if (rightBindings.hasLowerRefBounds(right)) {
            for (String refTypeVariable : rightBindings.getLowerRefBounds(right)) {
                if (mapping.containsKey(refTypeVariable)) {
                    leftBindings.addLowerRefBound(left, mapping.get(refTypeVariable));
                } else {
                    couldAddLower = false;
                    break;
                }
            }
        }

        return couldAddLower;
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
            worklistDto.workDeque.add(new WorklistDto(worklistDto, overloadRankingDto.bindings));
        } else if (worklistDto.isSolvingGlobalDefaultNamespace) {
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
