/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.TinsPHPConstants;
import ch.tsphp.tinsphp.common.inference.constraints.FixedTypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.OverloadApplicationDto;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.utils.ERelation;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.common.utils.TypeHelperDto;
import ch.tsphp.tinsphp.inference_engine.constraints.AggregateBindingDto;
import ch.tsphp.tinsphp.inference_engine.constraints.IMostSpecificOverloadDecider;
import ch.tsphp.tinsphp.inference_engine.constraints.OverloadRankingDto;
import ch.tsphp.tinsphp.inference_engine.constraints.WorklistDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.tsphp.tinsphp.common.utils.Pair.pair;

public class SoftTypingConstraintSolver implements ISoftTypingConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final ITypeHelper typeHelper;
    private final IConstraintSolverHelper constraintSolverHelper;
    private final IMostSpecificOverloadDecider mostSpecificOverloadDecider;
    private final IInferenceIssueReporter issueReporter;
    private final ITypeSymbol mixedTypeSymbol;

    @SuppressWarnings("checkstyle:parameternumber")
    public SoftTypingConstraintSolver(
            ISymbolFactory theSymbolFactory,
            ITypeHelper theTypeHelper,
            IInferenceIssueReporter theIssueReporter,
            IConstraintSolverHelper theConstraintSolverHelper,
            IMostSpecificOverloadDecider theMostSpecificOverloadDecider) {
        symbolFactory = theSymbolFactory;
        typeHelper = theTypeHelper;
        issueReporter = theIssueReporter;
        constraintSolverHelper = theConstraintSolverHelper;
        mostSpecificOverloadDecider = theMostSpecificOverloadDecider;
        mixedTypeSymbol = symbolFactory.getMixedTypeSymbol();
    }

    @Override
    public void fallBackToSoftTyping(IMethodSymbol methodSymbol) {
        WorklistDto worklistDto = createAndInitWorklistDto(methodSymbol);
        if (worklistDto.unsolvedConstraints == null || worklistDto.unsolvedConstraints.isEmpty()) {
            solveConstraints(methodSymbol, worklistDto);
        } else {
            constraintSolverHelper.createDependencies(worklistDto);
        }
    }

    private WorklistDto createAndInitWorklistDto(IMethodSymbol methodSymbol) {
        IOverloadBindings leftBindings = symbolFactory.createOverloadBindings();
        leftBindings.changeToSoftTypingMode();
        WorklistDto worklistDto = new WorklistDto(null, methodSymbol, 0, true, leftBindings);
        worklistDto.isInSoftTypingMode = true;
        worklistDto.param2LowerParams = new HashMap<>();
        List<IConstraint> constraints = methodSymbol.getConstraints();
        int size = constraints.size();
        for (int i = 0; i < size; ++i) {
            worklistDto.pointer = i;
            aggregateLowerBounds(worklistDto);
        }
        return worklistDto;
    }

    @Override
    public void aggregateLowerBounds(WorklistDto worklistDto) {
        IConstraint constraint = worklistDto.constraintCollection.getConstraints().get(worklistDto.pointer);
        IMinimalMethodSymbol refMethodSymbol = constraint.getMethodSymbol();
        if (refMethodSymbol.getOverloads().size() > 0) {
            if (isNotDirectRecursiveAssignment(constraint)) {

                constraintSolverHelper.createBindingsIfNecessary(worklistDto, constraint.getLeftHandSide(),
                        constraint.getArguments());
                for (IFunctionType overload : refMethodSymbol.getOverloads()) {
                    if (constraint.getArguments().size() >= overload.getNumberOfNonOptionalParameters()) {
                        AggregateBindingDto dto = new AggregateBindingDto(
                                constraint, overload, worklistDto.overloadBindings, worklistDto);
                        constraintSolverHelper.aggregateBinding(dto);
                    }
                }
            }
        } else {
            //add to unresolved constraints
            if (worklistDto.unsolvedConstraints == null) {
                worklistDto.unsolvedConstraints = new ArrayList<>();
            }
            worklistDto.unsolvedConstraints.add(worklistDto.pointer);
        }
    }

    @Override
    public void solveConstraints(IMethodSymbol methodSymbol, WorklistDto worklistDto) {
        initWorkListDtoOverloadBindings(methodSymbol, worklistDto);

        //TODO TINS-535 improve precision in soft typing for unconstrained parameters
        //idea how precision of parametric parameters could be enhanced (instead of using mixed as above)
//        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
//            String parameterId = parameter.getAbsoluteName();
//            String typeVariable = softTypingBindings.getTypeVariable(parameterId);
//            if (!softTypingBindings.hasLowerTypeBounds(typeVariable)) {
//                propagateParameterToParameters(
//                        typeVariable,
//                        typeVariable,
//                        parameterTypeVariables,
//                        softTypingBindings,
//                        worklistDto.param2LowerParams);
//            }
//        }

        worklistDto.overloadBindings.changeToModificationMode();
        worklistDto.isInSoftTypingMode = false;
        solveConstraintsAfterInit(methodSymbol, worklistDto);

        worklistDto.overloadBindings.changeToNormalMode();
        List<IOverloadBindings> overloadBindingsList = new ArrayList<>(1);
        overloadBindingsList.add(worklistDto.overloadBindings);
        constraintSolverHelper.finishingMethodConstraints(methodSymbol, overloadBindingsList);
    }

    private void initWorkListDtoOverloadBindings(IMethodSymbol methodSymbol, WorklistDto worklistDto) {

        IOverloadBindings softTypingBindings = worklistDto.overloadBindings;
        worklistDto.overloadBindings = symbolFactory.createOverloadBindings();

        Set<String> parameterTypeVariables = new HashSet<>();
        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            String parameterId = parameter.getAbsoluteName();
            String typeVariable = addVariableToLeftBindings(
                    parameterId, softTypingBindings, worklistDto.overloadBindings);
            parameterTypeVariables.add(typeVariable);
        }

        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            String parameterId = parameter.getAbsoluteName();
            String typeVariable = softTypingBindings.getTypeVariable(parameterId);
            addUpperRefBoundsToWorklistBindings(worklistDto, softTypingBindings, parameterTypeVariables, typeVariable);
        }
    }

    private void addUpperRefBoundsToWorklistBindings(
            WorklistDto worklistDto,
            IOverloadBindings softTypingBindings,
            Set<String> parameterTypeVariables,
            String typeVariable) {

        if (softTypingBindings.hasUpperRefBounds(typeVariable)) {
            // param was probably assigned to a local variable (or a parameter), hence we need to add the local
            // variable to the binding as well because otherwise it does not have the lower type bound we want
            // which causes a LowerBoundException later on (see TINS-543 soft typing and cyclic variable references)
            for (String refTypeVariable : softTypingBindings.getUpperRefBounds(typeVariable)) {
                if (!parameterTypeVariables.contains(refTypeVariable)) {
                    Set<String> variableIds = softTypingBindings.getVariableIds(refTypeVariable);
                    for (String variableId : variableIds) {
                        if (variableId.contains("$") && !worklistDto.overloadBindings.containsVariable(variableId)) {
                            String newTypeVariable = addVariableToLeftBindings(
                                    variableId, softTypingBindings, worklistDto.overloadBindings);

                            addUpperRefBoundsToWorklistBindings(
                                    worklistDto, softTypingBindings, parameterTypeVariables, newTypeVariable);
                        }
                    }
                }
            }
        }
    }

    private String addVariableToLeftBindings(
            String variableId,
            IOverloadBindings softTypingBindings,
            IOverloadBindings leftBindings) {

        String typeVariable = softTypingBindings.getTypeVariable(variableId);
        ITypeVariableReference nextTypeVariable = leftBindings.getNextTypeVariable();
        leftBindings.addVariable(variableId, new FixedTypeVariableReference(nextTypeVariable));
        if (softTypingBindings.hasLowerTypeBounds(typeVariable)) {
            //TODO TINS-534 type hints and soft-typing
            // we need to introduce a local variable here if a type hint was used and the inferred type is
            // different
            IUnionTypeSymbol lowerTypeBounds = softTypingBindings.getLowerTypeBounds(typeVariable);
            leftBindings.addLowerTypeBound(nextTypeVariable.getTypeVariable(), lowerTypeBounds);
        }
        return typeVariable;
    }

    private boolean isNotDirectRecursiveAssignment(IConstraint constraint) {
        return !constraint.getMethodSymbol().getAbsoluteName().equals("=")
                || !constraint.getArguments().get(1).getAbsoluteName().equals(TinsPHPConstants.RETURN_VARIABLE_NAME);
    }

    //TINS-535 improve precision in soft typing for unconstrained parameters
//    private void propagateParameterToParameters(
//            String refTypeVariable,
//            String typeVariable,
//            Set<String> parameterTypeVariables,
//            IOverloadBindings softTypingBindings,
//            Map<String, List<String>> parameter2LowerParameters) {
//        if (softTypingBindings.hasUpperBounds(refTypeVariable)) {
//            for (String refRefTypeVariable : softTypingBindings.getUpperRefBounds(refTypeVariable)) {
//                if (!parameterTypeVariables.contains(refRefTypeVariable)) {
//                    propagateParameterToParameters(
//                            refRefTypeVariable,
//                            typeVariable,
//                            parameterTypeVariables,
//                            softTypingBindings,
//                            parameter2LowerParameters);
//                } else {
//                    MapHelper.addToListInMap(parameter2LowerParameters, refRefTypeVariable, typeVariable);
//                }
//            }
//        }
//    }

    private void solveConstraintsAfterInit(IMethodSymbol methodSymbol, WorklistDto worklistDto) {
        for (IConstraint constraint : methodSymbol.getConstraints()) {
            List<IVariable> arguments = constraint.getArguments();
            int numberOfArguments = arguments.size();

            constraintSolverHelper.createBindingsIfNecessary(worklistDto, constraint.getLeftHandSide(),
                    constraint.getArguments());

            List<OverloadRankingDto> applicableOverloads = new ArrayList<>();
            for (IFunctionType overload : constraint.getMethodSymbol().getOverloads()) {
                if (numberOfArguments >= overload.getNumberOfNonOptionalParameters()) {
                    OverloadRankingDto dto = getOverloadRankingDto(
                            worklistDto, constraint, overload);
                    if (dto != null) {
                        applicableOverloads.add(dto);
                    }
                }
            }

            int numberOfApplicableOverloads = applicableOverloads.size();
            if (numberOfApplicableOverloads > 0) {
                OverloadRankingDto overloadRankingDto = applicableOverloads.get(0);
                String leftHandSide = constraint.getLeftHandSide().getAbsoluteName();
                if (numberOfApplicableOverloads > 1) {
                    List<OverloadRankingDto> mostSpecificOverloads
                            = mostSpecificOverloadDecider.inSoftTypingMode(worklistDto, applicableOverloads);
                    overloadRankingDto = mostSpecificOverloads.get(0);
                    numberOfApplicableOverloads = mostSpecificOverloads.size();
                    if (numberOfApplicableOverloads > 1) {
                        mergeMostSpecificOverloadsToNewOverloadBindings(worklistDto, constraint, mostSpecificOverloads);
                    }
                }

                if (numberOfApplicableOverloads == 1) {
                    worklistDto.overloadBindings = overloadRankingDto.bindings;
                    OverloadApplicationDto dto = new OverloadApplicationDto(
                            overloadRankingDto.overload,
                            overloadRankingDto.implicitConversions,
                            overloadRankingDto.runtimeChecks);
                    worklistDto.overloadBindings.setAppliedOverload(leftHandSide, dto);
                }
            } else {
                issueReporter.constraintViolation(worklistDto.overloadBindings, constraint);
            }
        }
    }

    private OverloadRankingDto getOverloadRankingDto(
            WorklistDto worklistDto, IConstraint constraint, IFunctionType overload) {

        IOverloadBindings leftBindings = symbolFactory.createOverloadBindings(worklistDto.overloadBindings);
        Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> runtimeChecks = new HashMap<>();

        boolean overloadApplies = isApplicable(
                constraint, overload, leftBindings, runtimeChecks);

        if (overloadApplies) {
            return applyOverload(worklistDto, constraint, overload, leftBindings, runtimeChecks);
        }
        return null;
    }

    private boolean isApplicable(
            IConstraint constraint,
            IFunctionType overload,
            IOverloadBindings leftBindings,
            Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> runtimeChecks) {

        IOverloadBindings rightBindings = overload.getOverloadBindings();
        List<IVariable> parameters = overload.getParameters();
        int numberOfParameters = parameters.size();
        List<IVariable> arguments = constraint.getArguments();
        int numberOfArguments = arguments.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;
        boolean overloadApplies = true;

        for (int i = 0; i < count; ++i) {
            String parameterId = parameters.get(i).getAbsoluteName();
            String typeVariable = rightBindings.getTypeVariable(parameterId);
            if (rightBindings.hasUpperTypeBounds(typeVariable)) {
                String argumentId = arguments.get(i).getAbsoluteName();
                boolean argumentApplies = doesArgumentApply(
                        leftBindings, argumentId, i, rightBindings, typeVariable, runtimeChecks);
                if (!argumentApplies) {
                    overloadApplies = false;
                    break;
                }
            }
        }
        return overloadApplies;
    }


    private boolean doesArgumentApply(
            IOverloadBindings leftBindings,
            String argumentId,
            int argumentNumber,
            IOverloadBindings rightBindings,
            String parameterTypeVariable,
            Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> runtimeChecks) {

        String argumentTypeVariable = leftBindings.getTypeVariable(argumentId);
        IUnionTypeSymbol argumentType = leftBindings.getLowerTypeBounds(argumentTypeVariable);

        //TODO TINS-535 improve precision in soft typing for unconstrained parameters
//                if (argumentType == null || worklistDto.param2LowerParams.containsKey(argumentTypeVariable)) {
        if (argumentType == null) {
            argumentType = symbolFactory.createUnionTypeSymbol();
            argumentType.addTypeSymbol(mixedTypeSymbol);
        }

        boolean argumentApplies = false;
        IIntersectionTypeSymbol parameterType = rightBindings.getUpperTypeBounds(parameterTypeVariable);
        TypeHelperDto result = typeHelper.isFirstSameOrSubTypeOfSecond(argumentType, parameterType);

        switch (result.relation) {
            case HAS_RELATION:
            case HAS_COERCIVE_RELATION:
                argumentApplies = true;
                break;
            case HAS_NO_RELATION:
            default:
                Map<String, ITypeSymbol> innerTypeSymbols = argumentType.getTypeSymbols();
                int size = innerTypeSymbols.size();

                List<ITypeSymbol> typeSymbols = new ArrayList<>(size);
                for (ITypeSymbol typeSymbol : innerTypeSymbols.values()) {
                    result = typeHelper.isFirstSameOrSubTypeOfSecond(typeSymbol, parameterType);
                    if (result.relation == ERelation.HAS_RELATION) {
                        typeSymbols.add(typeSymbol);
                    } else {
                        result = typeHelper.isFirstSameOrSubTypeOfSecond(parameterType, typeSymbol);
                        if (result.relation == ERelation.HAS_RELATION) {
                            typeSymbols.add(parameterType);
                        }
                    }
                }
                if (!typeSymbols.isEmpty()) {
                    argumentApplies = true;
                    ITypeSymbol typeSymbol;
                    if (typeSymbols.size() == 1) {
                        typeSymbol = typeSymbols.get(0);
                    } else {
                        IUnionTypeSymbol unionTypeSymbol = symbolFactory.createUnionTypeSymbol();
                        for (ITypeSymbol innerTypeSymbol : typeSymbols) {
                            unionTypeSymbol.addTypeSymbol(innerTypeSymbol);
                        }
                        typeSymbol = unionTypeSymbol;
                    }
                    runtimeChecks.put(argumentNumber, pair(typeSymbol, typeSymbols));
                    //TODO TINS-535 improve precision in soft typing for unconstrained parameters
//                            if (worklistDto.param2LowerParams.containsKey(argumentTypeVariable)) {
//                                for (String refTypeVariable : worklistDto.param2LowerParams.get
// (argumentTypeVariable)) {
//                                    leftBindings.addLowerTypeBound(refTypeVariable, parameterType);
//                                }
//                            }
                }

//                        }
                break;
        }
        return argumentApplies;
    }

    private OverloadRankingDto applyOverload(
            WorklistDto worklistDto,
            IConstraint constraint,
            IFunctionType overload,
            IOverloadBindings leftBindings,
            Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> theRuntimeChecks) {

        Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> runtimeChecks = theRuntimeChecks;
        Map<String, IUnionTypeSymbol> oldTypeBounds = null;
        boolean requiresExplicit = !runtimeChecks.isEmpty();
        if (requiresExplicit) {
            int size = runtimeChecks.size();
            oldTypeBounds = new HashMap<>(size);
            for (Map.Entry<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> entry : runtimeChecks.entrySet()) {
                String argumentId = constraint.getArguments().get(entry.getKey()).getAbsoluteName();
                String typeVariable = leftBindings.getTypeVariable(argumentId);

                IUnionTypeSymbol lowerTypeBounds = leftBindings.getLowerTypeBounds(typeVariable);
                oldTypeBounds.put(argumentId, lowerTypeBounds);

                IUnionTypeSymbol unionTypeSymbol = symbolFactory.createUnionTypeSymbol();
                unionTypeSymbol.addTypeSymbol(entry.getValue().first);
                leftBindings.setLowerTypeBounds(typeVariable, unionTypeSymbol);
                leftBindings.removeUpperTypeBounds(typeVariable);

            }
        } else {
            runtimeChecks = null;
        }

        AggregateBindingDto dto =
                new AggregateBindingDto(constraint, overload, leftBindings, worklistDto);
        constraintSolverHelper.aggregateBinding(dto);

        OverloadRankingDto overloadRankingDto = new OverloadRankingDto(
                overload, leftBindings, dto.implicitConversions, runtimeChecks, requiresExplicit);

        if (requiresExplicit) {
            for (Map.Entry<String, IUnionTypeSymbol> entry : oldTypeBounds.entrySet()) {
                String parameterId = entry.getKey();
                ITypeVariableReference reference = leftBindings.getTypeVariableReference(parameterId);
                String typeVariable = reference.getTypeVariable();
                IUnionTypeSymbol oldLowerTypeBound = entry.getValue();
                if (!reference.hasFixedType()) {
                    IIntersectionTypeSymbol newUpperTypeBound = leftBindings.getUpperTypeBounds(typeVariable);
                    if (newUpperTypeBound != null) {
                        IIntersectionTypeSymbol leastUpperBound = getLeastCommonUpperTypeBound(
                                oldLowerTypeBound, newUpperTypeBound);
                        leftBindings.setUpperTypeBounds(typeVariable, leastUpperBound);
                    }
                } else {
                    IIntersectionTypeSymbol intersectionTypeSymbol = symbolFactory.createIntersectionTypeSymbol();
                    intersectionTypeSymbol.addTypeSymbol(oldLowerTypeBound);
                    leftBindings.setUpperTypeBounds(typeVariable, intersectionTypeSymbol);
                }
                leftBindings.addLowerTypeBound(typeVariable, oldLowerTypeBound);
            }
        }

        return overloadRankingDto;
    }

    private IIntersectionTypeSymbol getLeastCommonUpperTypeBound(
            IUnionTypeSymbol oldLowerTypeBound, IIntersectionTypeSymbol newUpperTypeBound) {
        IUnionTypeSymbol unionTypeSymbol = symbolFactory.createUnionTypeSymbol();
        unionTypeSymbol.addTypeSymbol(oldLowerTypeBound);
        unionTypeSymbol.addTypeSymbol(newUpperTypeBound);
        IIntersectionTypeSymbol leastCommonUpperTypeBound = symbolFactory.createIntersectionTypeSymbol();
        leastCommonUpperTypeBound.addTypeSymbol(unionTypeSymbol);
        return leastCommonUpperTypeBound;
    }

    // I am aware of that this method is longer, but for now this is ok for efficiency reasons
    @SuppressWarnings("checkstyle:methodlength")
    private void mergeMostSpecificOverloadsToNewOverloadBindings(
            WorklistDto worklistDto, IConstraint constraint, List<OverloadRankingDto> mostSpecificOverloads) {

        IUnionTypeSymbol upperUnion = symbolFactory.createUnionTypeSymbol();

        IOverloadBindings leftBindings = mostSpecificOverloads.get(0).bindings;
        String leftHandSide = constraint.getLeftHandSide().getAbsoluteName();
        String lhsTypeVariable = leftBindings.getTypeVariable(leftHandSide);
        leftBindings.removeUpperTypeBounds(lhsTypeVariable);

        List<IVariable> arguments = constraint.getArguments();
        int numberOfArguments = arguments.size();
        List<IUnionTypeSymbol> upperArguments = new ArrayList<>(numberOfArguments);
        int numberOfApplicableOverloads = mostSpecificOverloads.size();

        Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> runtimeChecks = new HashMap<>();

        for (int iMostSpecific = 0; iMostSpecific < numberOfApplicableOverloads; ++iMostSpecific) {
            OverloadRankingDto overloadRankingDto = mostSpecificOverloads.get(iMostSpecific);
            IOverloadBindings rightBindings = overloadRankingDto.bindings;
            String rankingLhsTypeVariable = rightBindings.getTypeVariable(leftHandSide);
            if (rightBindings.hasUpperTypeBounds(rankingLhsTypeVariable)) {
                IIntersectionTypeSymbol upperTypeBounds = rightBindings.getUpperTypeBounds(rankingLhsTypeVariable);
                upperUnion.addTypeSymbol(upperTypeBounds);
            }

            if (rightBindings.hasLowerTypeBounds(rankingLhsTypeVariable)) {
                IUnionTypeSymbol lowerTypeBounds = rightBindings.getLowerTypeBounds(rankingLhsTypeVariable);
                leftBindings.addLowerTypeBound(lhsTypeVariable, lowerTypeBounds);
            }

            for (int iArgument = 0; iArgument < numberOfArguments; ++iArgument) {
                String argumentId = arguments.get(iArgument).getAbsoluteName();
                String typeVariable = leftBindings.getTypeVariable(argumentId);
                if (iMostSpecific == 0) {
                    leftBindings.removeUpperTypeBounds(typeVariable);
                    upperArguments.add(symbolFactory.createUnionTypeSymbol());
                }

                IUnionTypeSymbol unionTypeSymbol = upperArguments.get(iArgument);
                String rankingTypeVariable = rightBindings.getTypeVariable(argumentId);
                if (rightBindings.hasUpperTypeBounds(rankingTypeVariable)) {
                    IIntersectionTypeSymbol upperTypeBounds = rightBindings.getUpperTypeBounds(rankingTypeVariable);
                    unionTypeSymbol.addTypeSymbol(upperTypeBounds);
                }

                if (rightBindings.hasLowerTypeBounds(rankingTypeVariable)) {
                    IUnionTypeSymbol lowerTypeBounds = rightBindings.getLowerTypeBounds(rankingTypeVariable);
                    leftBindings.addLowerTypeBound(typeVariable, lowerTypeBounds);
                }
            }

            if (overloadRankingDto.runtimeChecks != null) {
                for (Map.Entry<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> entry
                        : overloadRankingDto.runtimeChecks.entrySet()) {
                    Integer argumentNumber = entry.getKey();
                    Pair<ITypeSymbol, List<ITypeSymbol>> pair = entry.getValue();
                    IUnionTypeSymbol unionTypeSymbol;
                    if (!runtimeChecks.containsKey(argumentNumber)) {
                        unionTypeSymbol = symbolFactory.createUnionTypeSymbol();
                        runtimeChecks.put(argumentNumber, pair((ITypeSymbol) unionTypeSymbol, pair.second));
                    } else {
                        unionTypeSymbol = (IUnionTypeSymbol) runtimeChecks.get(argumentNumber).first;
                    }
                    unionTypeSymbol.addTypeSymbol(pair.first);
                }
            }

        }

        runtimeChecks.put(-1, new Pair<ITypeSymbol, List<ITypeSymbol>>(upperUnion, null));

        leftBindings.addUpperTypeBound(lhsTypeVariable, upperUnion);
        for (int i = 0; i < numberOfArguments; ++i) {
            String argumentId = arguments.get(i).getAbsoluteName();
            String typeVariable = leftBindings.getTypeVariable(argumentId);
            leftBindings.addUpperTypeBound(typeVariable, upperArguments.get(i));
        }

        worklistDto.overloadBindings = leftBindings;
        //overload = null indicates that we need to fall back to the dynamic version
        worklistDto.overloadBindings.setAppliedOverload(
                leftHandSide, new OverloadApplicationDto(null, null, runtimeChecks));
    }

}
