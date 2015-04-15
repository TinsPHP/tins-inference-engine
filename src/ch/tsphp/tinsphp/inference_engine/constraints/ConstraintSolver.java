/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.FixedTypeVariableConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IIntersectionConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.TypeVariableConstraint;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.utils.MapHelper;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.symbols.constraints.BoundException;
import ch.tsphp.tinsphp.symbols.constraints.OverloadBindings;
import ch.tsphp.tinsphp.symbols.constraints.TypeConstraint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.tsphp.tinsphp.common.utils.Pair.pair;
import static ch.tsphp.tinsphp.symbols.TypeVariableNames.RETURN_VARIABLE_NAME;

public class ConstraintSolver implements IConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final IOverloadResolver overloadResolver;
    private final TypeConstraint mixedTypeConstraint;

    public ConstraintSolver(
            ISymbolFactory theSymbolFactory,
            IOverloadResolver theOverloadResolver,
            ITypeSymbol theMixedTypeSymbol) {
        symbolFactory = theSymbolFactory;
        overloadResolver = theOverloadResolver;
        mixedTypeConstraint = new TypeConstraint(theMixedTypeSymbol);
    }

    @Override
    public void solveConstraints(List<IMethodSymbol> methodSymbols) {
        Map<String, List<Pair<IMethodSymbol, Deque<WorklistDto>>>> dependencies = new HashMap<>();
        for (IMethodSymbol methodSymbol : methodSymbols) {
            Deque<WorklistDto> workDeque = createInitialWorklist();
            solveMethodConstraints(dependencies, methodSymbol, workDeque);
        }

        if (!dependencies.isEmpty()) {
            //TODO need to iterate
        }
    }

    @Override
    public void solveConstraints(IGlobalNamespaceScope globalDefaultNamespaceScope) {
        if (!globalDefaultNamespaceScope.getLowerBoundConstraints().isEmpty()) {
            Deque<WorklistDto> workDeque = createInitialWorklist();
            List<IOverloadBindings> bindings = solveConstraints(globalDefaultNamespaceScope, workDeque);
            globalDefaultNamespaceScope.setBindings(bindings);
        }
    }

    private Deque<WorklistDto> createInitialWorklist() {
        IOverloadBindings bindings = new OverloadBindings(overloadResolver);
        Deque<WorklistDto> workDeque = new ArrayDeque<>();
        workDeque.add(new WorklistDto(workDeque, 0, bindings));
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

        List<IIntersectionConstraint> lowerBoundConstraints = constraintCollection.getLowerBoundConstraints();
        while (!workDeque.isEmpty()) {
            WorklistDto worklistDto = workDeque.removeFirst();
            if (worklistDto.pointer < lowerBoundConstraints.size()) {
                IIntersectionConstraint constraint = lowerBoundConstraints.get(worklistDto.pointer);
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
            WorklistDto worklistDto, IIntersectionConstraint constraint) {
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

    private void solve(WorklistDto worklistDto, IIntersectionConstraint constraint) {
        createBindingIfNecessary(worklistDto.overloadBindings, constraint.getLeftHandSide());
        boolean atLeastOneBindingCreated = false;
        for (IVariable argument : constraint.getArguments()) {
            boolean neededBinding = createBindingIfNecessary(worklistDto.overloadBindings, argument);
            atLeastOneBindingCreated = atLeastOneBindingCreated || neededBinding;
        }

        if (atLeastOneBindingCreated || constraint.getMethodSymbol().getOverloads().size() == 1) {
            addApplicableOverloadsToWorklist(worklistDto, constraint);
        } else {
            addMostSpecificOverloadToWorklist(worklistDto, constraint);
        }
    }

    private boolean createBindingIfNecessary(IOverloadBindings bindings, IVariable variable) {
        String absoluteName = variable.getAbsoluteName();
        Map<String, ITypeVariableConstraint> variable2TypeVariable = bindings.getVariable2TypeVariable();
        boolean bindingDoesNotExist = !variable2TypeVariable.containsKey(absoluteName);
        if (bindingDoesNotExist) {
            TypeVariableConstraint typeVariableConstraint = bindings.getNextTypeVariable();
            ITypeVariableConstraint constraint = typeVariableConstraint;
            //if it is a literal then we know already the lower bound and it is a fix typed type variable
            ITypeSymbol typeSymbol = variable.getType();
            if (typeSymbol != null) {
                constraint = new FixedTypeVariableConstraint(typeVariableConstraint);
                TypeConstraint typeConstraint = new TypeConstraint(typeSymbol);
                bindings.addLowerBound(constraint.getTypeVariable(), typeConstraint);
                bindingDoesNotExist = false;
            }
            variable2TypeVariable.put(absoluteName, constraint);
        }
        return bindingDoesNotExist;
    }

    private void addApplicableOverloadsToWorklist(WorklistDto worklistDto, IIntersectionConstraint constraint) {
        for (IFunctionType overload : constraint.getMethodSymbol().getOverloads()) {
            try {
                IOverloadBindings bindings = solveOverLoad(worklistDto, constraint, overload);
                if (bindings != null) {
                    worklistDto.workDeque.add(
                            new WorklistDto(worklistDto.workDeque, worklistDto.pointer + 1, bindings));
                }
            } catch (BoundException ex) {
                //That is ok, we will deal with it in solveConstraints
            }
        }
    }

    private IOverloadBindings solveOverLoad(
            WorklistDto worklistDto,
            IIntersectionConstraint constraint,
            IFunctionType overload) {

        IOverloadBindings bindings = null;
        if (constraint.getArguments().size() >= overload.getNumberOfNonOptionalParameters()) {
            bindings = new OverloadBindings(overloadResolver, (OverloadBindings) worklistDto.overloadBindings);
            aggregateBinding(constraint, overload, bindings);
        }
        return bindings;
    }

    private void aggregateBinding(
            IIntersectionConstraint constraint,
            IFunctionType overload,
            IOverloadBindings bindings) {

        Map<String, ITypeVariableConstraint> variable2TypeVariable = bindings.getVariable2TypeVariable();
        List<IVariable> arguments = constraint.getArguments();
        List<IVariable> parameters = overload.getParameters();
        int numberOfParameters = parameters.size();
        Map<String, ITypeVariableConstraint> mapping = new HashMap<>(numberOfParameters + 1);
        int numberOfArguments = arguments.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;

        int iterateCount = 0;
        boolean needToIterateOverload = true;
        while (needToIterateOverload) {

            IVariable leftHandSide = constraint.getLeftHandSide();
            IVariable rightHandSide = overload.getReturnVariable();
            needToIterateOverload = !mergeTypeVariables(bindings, overload, mapping, leftHandSide, rightHandSide);

            boolean argumentsAreAllFixed = true;
            for (int i = 0; i < count; ++i) {
                IVariable argument = arguments.get(i);
                IVariable parameter = parameters.get(i);
                boolean needToIterateParameter = !mergeTypeVariables(bindings, overload, mapping, argument, parameter);
                needToIterateOverload = needToIterateOverload || needToIterateParameter;
                argumentsAreAllFixed = argumentsAreAllFixed
                        && variable2TypeVariable.get(argument.getAbsoluteName()).hasFixedType();
            }

            if (!needToIterateOverload) {
                String lhsAbsoluteName = leftHandSide.getAbsoluteName();
                ITypeVariableConstraint typeVariableConstraint = variable2TypeVariable.get(lhsAbsoluteName);
                if (!typeVariableConstraint.hasFixedType() && (rightHandSide.hasFixedType() || argumentsAreAllFixed)) {
                    FixedTypeVariableConstraint fixedTypeVariableConstraint = fixType(typeVariableConstraint);
                    variable2TypeVariable.put(lhsAbsoluteName, fixedTypeVariableConstraint);
                }
            }

            if (iterateCount > 1) {
                throw new IllegalStateException("overload uses type variables "
                        + "which are not part of the signature.");
            }
            ++iterateCount;
        }
    }

    private FixedTypeVariableConstraint fixType(ITypeVariableConstraint typeVariableConstraint) {
        return new FixedTypeVariableConstraint((TypeVariableConstraint) typeVariableConstraint);
    }

    private boolean mergeTypeVariables(
            IOverloadBindings bindings,
            IFunctionType overload,
            Map<String, ITypeVariableConstraint> mapping,
            IVariable bindingVariable,
            IVariable overloadVariable) throws BoundException {
        String bindingVariableName = bindingVariable.getAbsoluteName();
        ITypeVariableConstraint bindingTypeVariableConstraint
                = bindings.getVariable2TypeVariable().get(bindingVariableName);
        String overloadTypeVariable = overloadVariable.getTypeVariable();

        String lhsTypeVariable;
        if (mapping.containsKey(overloadTypeVariable)) {
            ITypeVariableConstraint lhsTypeVariableConstraint = mapping.get(overloadTypeVariable);
            lhsTypeVariable = lhsTypeVariableConstraint.getTypeVariable();
            String rhsTypeVariable = bindingTypeVariableConstraint.getTypeVariable();
            if (!lhsTypeVariable.equals(rhsTypeVariable)) {
                bindings.renameTypeVariable(bindingTypeVariableConstraint, lhsTypeVariable);
                //TODO remove lower and upper bounds of unused type variable
            }
        } else {
            lhsTypeVariable = bindingTypeVariableConstraint.getTypeVariable();
            mapping.put(overloadTypeVariable, bindingTypeVariableConstraint);
        }

        IOverloadBindings overloadTypeVariables = overload.getBindings();
        return applyRightToLeft(
                mapping, bindings, lhsTypeVariable, overloadTypeVariables, overloadTypeVariable);
    }

    private boolean applyRightToLeft(
            Map<String, ITypeVariableConstraint> mapping,
            IOverloadBindings leftBindings, String left,
            IOverloadBindings rightBindings, String right) throws BoundException {

        if (rightBindings.hasUpperBounds(right)) {
            for (IConstraint upperBound : rightBindings.getUpperBounds(right)) {
                //upper bounds do not have TypeVariableConstraint
                leftBindings.addUpperBound(left, upperBound);
            }
        }

        boolean couldAddLower = true;
        if (rightBindings.hasLowerBounds(right)) {
            for (IConstraint lowerBound : rightBindings.getLowerBounds(right)) {
                if (lowerBound instanceof TypeConstraint) {
                    leftBindings.addLowerBound(left, lowerBound);
                } else if (lowerBound instanceof TypeVariableConstraint) {
                    String typeVariable = ((TypeVariableConstraint) lowerBound).getTypeVariable();
                    if (mapping.containsKey(typeVariable)) {
                        leftBindings.addLowerBound(left, mapping.get(typeVariable));
                    } else {
                        couldAddLower = false;
                        break;
                    }
                } else {
                    throw new UnsupportedOperationException(lowerBound.getClass().getName() + " not supported.");
                }
            }
        }

        return couldAddLower;
    }

    private void addMostSpecificOverloadToWorklist(WorklistDto worklistDto, IIntersectionConstraint constraint) {
        List<OverloadRankingDto> applicableOverloads = getApplicableOverloads(worklistDto, constraint);

        if (!applicableOverloads.isEmpty()) {
            List<OverloadRankingDto> overloadRankingDtos = getMostSpecificApplicableOverload(applicableOverloads);
            if (overloadRankingDtos.size() == 1) {
                OverloadRankingDto overloadRankingDto = overloadRankingDtos.get(0);
                worklistDto.workDeque.add(
                        new WorklistDto(worklistDto.workDeque, worklistDto.pointer + 1, overloadRankingDto.binding));
            } else {
                //TODO unify most specific
            }
        }
    }

    private List<OverloadRankingDto> getApplicableOverloads(
            WorklistDto worklistDto, IIntersectionConstraint constraint) {

        List<OverloadRankingDto> overloadRankingDtos = new ArrayList<>();
        for (IFunctionType overload : constraint.getMethodSymbol().getOverloads()) {
            try {
                IOverloadBindings bindings = solveOverLoad(worklistDto, constraint, overload);
                OverloadRankingDto dto = calculateOverloadRankingDto(worklistDto, constraint, overload);
                dto.binding = bindings;
                dto.overload = overload;
                overloadRankingDtos.add(dto);
                if (isMostSpecific(dto)) {
                    break;
                }
            } catch (BoundException ex) {
                //That is ok, we will deal with it in solveConstraints
            }
        }
        return overloadRankingDtos;
    }

    private boolean isMostSpecific(OverloadRankingDto dto) {
        return dto.parameterWithUpCastCount == 0
                && dto.parameterWithoutFixedTypeCount == 0
                && dto.parametersNeedExplicitConversion.size() == 0
                && dto.parametersNeedImplicitConversion.size() == 0;
    }

    private OverloadRankingDto calculateOverloadRankingDto(
            WorklistDto worklistDto,
            IIntersectionConstraint constraint,
            IFunctionType overload) {
        IOverloadBindings overloadBindings = overload.getBindings();
        Map<String, ITypeVariableConstraint> variable2TypeVariable = worklistDto.overloadBindings
                .getVariable2TypeVariable();
        List<IVariable> parameters = overload.getParameters();
        int numberOfParameters = parameters.size();
        List<IVariable> arguments = constraint.getArguments();
        int numberOfArguments = arguments.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;

        OverloadRankingDto dto = new OverloadRankingDto();

        for (int i = 0; i < count; ++i) {
            IVariable parameter = parameters.get(i);
            String argumentTypeVariable
                    = variable2TypeVariable.get(arguments.get(i).getAbsoluteName()).getTypeVariable();
            if (parameter.hasFixedType()) {
                aggregateOverloadRankingWithFixedType(
                        dto, overloadBindings, worklistDto.overloadBindings, argumentTypeVariable, parameter);
            } else {
                ++dto.parameterWithoutFixedTypeCount;
            }
        }

        return dto;
    }

    private void aggregateOverloadRankingWithFixedType(
            OverloadRankingDto dto,
            IOverloadBindings overloadBindings,
            IOverloadBindings worklistBindings, String argumentTypeVariable,
            IVariable parameter) {
        int maxUpCastLevel = 0;
        for (IConstraint paramUpperBound : overloadBindings.getUpperBounds(parameter.getTypeVariable())) {
            //we only support TypeConstraints as upper bounds, hence we can safely cast
            ITypeSymbol paramTypeSymbol = ((TypeConstraint) paramUpperBound).getTypeSymbol();

            for (IConstraint argumentLowerBound : worklistBindings.getLowerBounds(argumentTypeVariable)) {
                if (argumentLowerBound instanceof TypeConstraint) {
                    ITypeSymbol argumentTypeSymbol = ((TypeConstraint) argumentLowerBound).getTypeSymbol();
                    int upCastLevel = overloadResolver.getPromotionLevelFromTo(argumentTypeSymbol, paramTypeSymbol);
                    if (maxUpCastLevel > upCastLevel) {
                        maxUpCastLevel = upCastLevel;
                    }
                } else if (argumentLowerBound instanceof TypeVariableConstraint) {
                    String refTypeVariable = ((TypeVariableConstraint) argumentLowerBound).getTypeVariable();
                    Collection<IConstraint> refUpperBounds = worklistBindings.getUpperBounds(refTypeVariable);
                    for (IConstraint refUpperBound : refUpperBounds) {
                        ITypeSymbol refTypeSymbol = ((TypeConstraint) refUpperBound).getTypeSymbol();
                        int upCastLevel = overloadResolver.getPromotionLevelFromTo(refTypeSymbol, paramTypeSymbol);
                        if (maxUpCastLevel > upCastLevel) {
                            maxUpCastLevel = upCastLevel;
                        }
                    }
                }
            }
        }

        if (maxUpCastLevel != 0) {
            dto.upCastsTotal += maxUpCastLevel;
            ++dto.parameterWithUpCastCount;
        }
    }

    public List<OverloadRankingDto> getMostSpecificApplicableOverload(List<OverloadRankingDto> overloadRankingDtos) {

        List<OverloadRankingDto> mostSpecificOverloads = new ArrayList<>(3);
        OverloadRankingDto mostSpecificMethodDto = overloadRankingDtos.get(0);

        int overloadDtosSize = overloadRankingDtos.size();
        for (int i = 1; i < overloadDtosSize; ++i) {
            OverloadRankingDto overloadRankingDto = overloadRankingDtos.get(i);
            //TODO compare needs also take into account how many parameters are fix
            int diff = compare(mostSpecificMethodDto, overloadRankingDto);
            if (diff > 0) {
                mostSpecificMethodDto = overloadRankingDto;
                if (mostSpecificOverloads.size() > 0) {
                    mostSpecificOverloads = new ArrayList<>(3);
                }
            } else if (diff == 0) {
                mostSpecificOverloads.add(overloadRankingDto);
            }
        }
        mostSpecificOverloads.add(mostSpecificMethodDto);
        return mostSpecificOverloads;
    }

    private int compare(OverloadRankingDto mostSpecificMethodDto, OverloadRankingDto methodDto) {
        int diff = compareConversions(mostSpecificMethodDto, methodDto);
        if (diff == 0) {
            diff = compareUpCasts(mostSpecificMethodDto, methodDto);
            if (diff == 0) {
                diff = mostSpecificMethodDto.parameterWithoutFixedTypeCount - methodDto.parameterWithoutFixedTypeCount;
            }
        }
        return diff;
    }

    private int compareUpCasts(OverloadRankingDto mostSpecificMethodDto, OverloadRankingDto methodDto) {
        int diff = mostSpecificMethodDto.parameterWithUpCastCount - methodDto.parameterWithUpCastCount;
        if (diff == 0) {
            diff = mostSpecificMethodDto.upCastsTotal - methodDto.upCastsTotal;
        }
        return diff;
    }

    private int compareConversions(OverloadRankingDto mostSpecificMethodDto, OverloadRankingDto methodDto) {
        int diff = mostSpecificMethodDto.parametersNeedExplicitConversion.size()
                - methodDto.parametersNeedExplicitConversion.size();
        if (diff == 0) {
            diff = mostSpecificMethodDto.parametersNeedImplicitConversion.size()
                    - methodDto.parametersNeedImplicitConversion.size();
        }
        return diff;
    }


    private void createOverloads(IMethodSymbol methodSymbol, List<IOverloadBindings> bindingsList) {
        for (IOverloadBindings bindings : bindingsList) {
            createOverload(methodSymbol, bindings);
        }
    }

    private void createOverload(IMethodSymbol methodSymbol, IOverloadBindings bindings) {
        Map<String, ITypeVariableConstraint> variable2TypeVariable = bindings.getVariable2TypeVariable();

        Set<String> parameterConstraintIds = getParameterConstraintIds(methodSymbol, bindings);

        List<IVariable> parameters = new ArrayList<>();
        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            String parameterId = parameter.getAbsoluteName();
            String typeVariable = variable2TypeVariable.get(parameterId).getTypeVariable();
            IVariable parameterVariable = symbolFactory.createVariable(parameterId, typeVariable);
            parameters.add(parameterVariable);
            simplify(bindings, parameterConstraintIds, parameterId, parameterVariable);
        }

        ITypeVariableConstraint returnTypeVariableConstraint = variable2TypeVariable.get(RETURN_VARIABLE_NAME);
        String returnTypeVariable = returnTypeVariableConstraint.getTypeVariable();
        IVariable returnVariable = symbolFactory.createVariable(RETURN_VARIABLE_NAME, returnTypeVariable);
        simplify(bindings, parameterConstraintIds, RETURN_VARIABLE_NAME, returnVariable);

        IFunctionType functionType = symbolFactory.createFunctionType(
                methodSymbol.getName(), bindings, parameters, returnVariable);
        methodSymbol.addOverload(functionType);
    }

    private void simplify(
            IOverloadBindings bindings,
            Set<String> parameterConstraintIds,
            String parameterId,
            IVariable parameterVariable) {
        if (bindings.tryToFixType(parameterId)) {
            parameterVariable.setHasFixedType();
        } else {
            //could not fix the type, need to resolve lower bounds which are not type constraints
            bindings.resolveDependencies(parameterId, parameterConstraintIds);
        }
    }

    private Set<String> getParameterConstraintIds(IMethodSymbol methodSymbol, IOverloadBindings bindings) {
        Map<String, ITypeVariableConstraint> variable2TypeVariable = bindings.getVariable2TypeVariable();
        Set<String> parameterConstraintIds = new HashSet<>();
        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            String parameterId = parameter.getAbsoluteName();
            if (variable2TypeVariable.containsKey(parameterId)) {
                parameterConstraintIds.add(variable2TypeVariable.get(parameterId).getId());
            } else {
                //the parameter is not used at all, hence it can be mixed
                ITypeVariableConstraint constraint = new FixedTypeVariableConstraint(bindings.getNextTypeVariable());
                variable2TypeVariable.put(parameterId, constraint);
                bindings.addUpperBound(constraint.getTypeVariable(), mixedTypeConstraint);
                //TODO could generate a warning
            }
        }
        return parameterConstraintIds;
    }
}
