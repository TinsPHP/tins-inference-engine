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
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.utils.ERelation;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.common.utils.MapHelper;
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
        WorklistDto worklistDto = createAndInitSoftTypingWorklistDto(methodSymbol);
        if (worklistDto.unsolvedConstraints == null || worklistDto.unsolvedConstraints.isEmpty()) {
            solveConstraintsInSoftTyping(methodSymbol, worklistDto);
        } else {
            constraintSolverHelper.createDependencies(worklistDto);
        }
    }

    private WorklistDto createAndInitSoftTypingWorklistDto(IMethodSymbol methodSymbol) {
        IOverloadBindings leftBindings = symbolFactory.createOverloadBindings();
        leftBindings.changeToSoftTypingMode();
        WorklistDto worklistDto = new WorklistDto(null, methodSymbol, 0, true, leftBindings);
        worklistDto.isInSoftTypingMode = true;
        worklistDto.param2LowerParams = new HashMap<>();
        List<IConstraint> constraints = methodSymbol.getConstraints();
        int size = constraints.size();
        for (int i = 0; i < size; ++i) {
            worklistDto.pointer = i;
            aggregateLowerBoundsSoftTyping(worklistDto);
        }
        return worklistDto;
    }

    @Override
    public void aggregateLowerBoundsSoftTyping(WorklistDto worklistDto) {
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
    public void solveConstraintsInSoftTyping(IMethodSymbol methodSymbol, WorklistDto worklistDto) {
        IOverloadBindings softTypingBindings = worklistDto.overloadBindings;
        worklistDto.overloadBindings = symbolFactory.createOverloadBindings();

        //set parameters to mixed which do not have any lower bounds
        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            String typeVariable = softTypingBindings.getTypeVariable(parameter.getAbsoluteName());
            if (!softTypingBindings.hasLowerTypeBounds(typeVariable)) {
                softTypingBindings.addLowerTypeBound(typeVariable, mixedTypeSymbol);
            }
        }

        Set<String> parameterTypeVariables = new HashSet<>();
        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            String parameterId = parameter.getAbsoluteName();
            String typeVariable = softTypingBindings.getTypeVariable(parameterId);
            ITypeVariableReference nextTypeVariable = worklistDto.overloadBindings.getNextTypeVariable();
            worklistDto.overloadBindings.addVariable(parameterId, new FixedTypeVariableReference(nextTypeVariable));
            if (softTypingBindings.hasLowerTypeBounds(typeVariable)) {
                //TODO TINS-534 type hints and soft-typing
                // we need to introduce a local variable here if a type hint was used and the inferred type is
                // different

                IUnionTypeSymbol lowerTypeBounds = softTypingBindings.getLowerTypeBounds(typeVariable);
                worklistDto.overloadBindings.addLowerTypeBound(nextTypeVariable.getTypeVariable(), lowerTypeBounds);
            }
            parameterTypeVariables.add(typeVariable);
        }

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
        solveConstraintsSoftTyping(methodSymbol, worklistDto);

        worklistDto.overloadBindings.changeToNormalMode();
        List<IOverloadBindings> overloadBindingsList = new ArrayList<>(1);
        overloadBindingsList.add(worklistDto.overloadBindings);
        constraintSolverHelper.finishingMethodConstraints(methodSymbol, overloadBindingsList);
    }

    private boolean isNotDirectRecursiveAssignment(IConstraint constraint) {
        return !constraint.getMethodSymbol().getAbsoluteName().equals("=")
                || !constraint.getArguments().get(1).getAbsoluteName().equals(TinsPHPConstants.RETURN_VARIABLE_NAME);
    }

    //TODO TINS-535 improve precision in soft typing for unconstrained parameters
    private void propagateParameterToParameters(
            String refTypeVariable,
            String typeVariable,
            Set<String> parameterTypeVariables,
            IOverloadBindings softTypingBindings,
            Map<String, List<String>> parameter2LowerParameters) {
        if (softTypingBindings.hasUpperBounds(refTypeVariable)) {
            for (String refRefTypeVariable : softTypingBindings.getUpperRefBounds(refTypeVariable)) {
                if (!parameterTypeVariables.contains(refRefTypeVariable)) {
                    propagateParameterToParameters(
                            refRefTypeVariable,
                            typeVariable,
                            parameterTypeVariables,
                            softTypingBindings,
                            parameter2LowerParameters);
                } else {
                    MapHelper.addToListInMap(parameter2LowerParameters, refRefTypeVariable, typeVariable);
                }
            }
        }
    }

    private void solveConstraintsSoftTyping(IMethodSymbol methodSymbol, WorklistDto worklistDto) {
        for (IConstraint constraint : methodSymbol.getConstraints()) {
            List<IVariable> arguments = constraint.getArguments();
            int numberOfArguments = arguments.size();

            constraintSolverHelper.createBindingsIfNecessary(worklistDto, constraint.getLeftHandSide(),
                    constraint.getArguments());

            List<OverloadRankingDto> applicableOverloads = new ArrayList<>();
            for (IFunctionType overload : constraint.getMethodSymbol().getOverloads()) {
                if (numberOfArguments >= overload.getNumberOfNonOptionalParameters()) {
                    OverloadRankingDto dto = getOverloadRankingDtoInSoftTyping(
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
                        mergeMostSpecificOverloads(overloadRankingDto, constraint, mostSpecificOverloads);
                    }
                }

                worklistDto.overloadBindings = overloadRankingDto.bindings;
                if (numberOfApplicableOverloads == 1) {
                    worklistDto.overloadBindings.setAppliedOverload(leftHandSide, overloadRankingDto.overload);
                }
            } else {
                issueReporter.constraintViolation(worklistDto.overloadBindings, constraint);
                //TODO rstoll TINS-306 inference - runtime check insertion
                //I am not sure but maybe we do not need to do anything. see
                //TINS-399 save which overload was taken in AST
                //I think it is enough if the symbol does not contain any overload. The translator can then insert an
                // error in the output
            }
        }
    }

    private OverloadRankingDto getOverloadRankingDtoInSoftTyping(
            WorklistDto worklistDto, IConstraint constraint, IFunctionType overload) {

        IOverloadBindings leftBindings = symbolFactory.createOverloadBindings(worklistDto.overloadBindings);
        Map<String, Pair<ITypeSymbol, List<ITypeSymbol>>> explicitConversions = new HashMap<>();

        boolean overloadApplies = isApplicableInSoftTyping(
                worklistDto, constraint, overload, leftBindings, explicitConversions);

        if (overloadApplies) {
            return applyOverloadInSoftTyping(worklistDto, constraint, overload, leftBindings, explicitConversions);
        }
        return null;
    }

    private boolean isApplicableInSoftTyping(
            WorklistDto worklistDto,
            IConstraint constraint,
            IFunctionType overload,
            IOverloadBindings leftBindings,
            Map<String, Pair<ITypeSymbol, List<ITypeSymbol>>> explicitConversions) {

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
                String argumentTypeVariable = leftBindings.getTypeVariable(argumentId);
                IUnionTypeSymbol argumentType = leftBindings.getLowerTypeBounds(argumentTypeVariable);
                //TODO TINS-535 improve precision in soft typing for unconstrained parameters
//                if (argumentType == null || worklistDto.param2LowerParams.containsKey(argumentTypeVariable)) {
                if (argumentType == null) {
                    argumentType = symbolFactory.createUnionTypeSymbol();
                    argumentType.addTypeSymbol(mixedTypeSymbol);
                }

                boolean parameterApplies = false;
                IIntersectionTypeSymbol parameterType = rightBindings.getUpperTypeBounds(typeVariable);
                TypeHelperDto result = typeHelper.isFirstSameOrSubTypeOfSecond(argumentType, parameterType);

                switch (result.relation) {
                    case HAS_RELATION:
                    case HAS_COERCIVE_RELATION:
                        parameterApplies = true;
                        break;
                    case HAS_NO_RELATION:
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
                            parameterApplies = true;
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
                            explicitConversions.put(argumentId, pair(typeSymbol, typeSymbols));
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
                if (!parameterApplies) {
                    overloadApplies = false;
                    break;
                }
            }
        }
        return overloadApplies;
    }

    private OverloadRankingDto applyOverloadInSoftTyping(
            WorklistDto worklistDto,
            IConstraint constraint,
            IFunctionType overload,
            IOverloadBindings leftBindings,
            Map<String, Pair<ITypeSymbol, List<ITypeSymbol>>> explicitConversions) {

        Map<String, IUnionTypeSymbol> oldTypes = null;
        boolean requiresExplicit = !explicitConversions.isEmpty();
        if (requiresExplicit) {
            oldTypes = new HashMap<>(explicitConversions.size());
            for (Map.Entry<String, Pair<ITypeSymbol, List<ITypeSymbol>>> entry : explicitConversions.entrySet()) {
                String argumentId = entry.getKey();
                ITypeVariableReference reference = leftBindings.getTypeVariableReference(argumentId);
                String typeVariable = reference.getTypeVariable();

                IUnionTypeSymbol lowerTypeBounds = leftBindings.getLowerTypeBounds(typeVariable);
                oldTypes.put(argumentId, lowerTypeBounds);

                IUnionTypeSymbol unionTypeSymbol = symbolFactory.createUnionTypeSymbol();
                unionTypeSymbol.addTypeSymbol(entry.getValue().first);
                leftBindings.setLowerTypeBound(typeVariable, unionTypeSymbol);
            }
        }

        AggregateBindingDto dto =
                new AggregateBindingDto(constraint, overload, leftBindings, worklistDto);
        constraintSolverHelper.aggregateBinding(dto);

        OverloadRankingDto overloadRankingDto = new OverloadRankingDto(
                overload, leftBindings, dto.implicitConversionCounter, requiresExplicit);

        if (requiresExplicit) {
            overloadRankingDto.explicitConversions = explicitConversions;

            for (Map.Entry<String, IUnionTypeSymbol> entry : oldTypes.entrySet()) {
                String parameterId = entry.getKey();
                ITypeVariableReference reference = leftBindings.getTypeVariableReference(parameterId);
                String typeVariable = reference.getTypeVariable();
                IUnionTypeSymbol oldLowerTypeBound = entry.getValue();
                if (!reference.hasFixedType()) {
                    IIntersectionTypeSymbol newUpperTypeBound = leftBindings.getUpperTypeBounds(typeVariable);
                    if (newUpperTypeBound != null) {
                        IIntersectionTypeSymbol leastUpperBound = getLeastCommonUpperTypeBound(
                                oldLowerTypeBound, newUpperTypeBound);
                        leftBindings.setUpperTypeBound(typeVariable, leastUpperBound);
                    }
                } else {
                    IIntersectionTypeSymbol intersectionTypeSymbol = symbolFactory.createIntersectionTypeSymbol();
                    intersectionTypeSymbol.addTypeSymbol(oldLowerTypeBound);
                    leftBindings.setUpperTypeBound(typeVariable, intersectionTypeSymbol);
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

    private void mergeMostSpecificOverloads(
            OverloadRankingDto overloadRankingDto,
            IConstraint constraint,
            List<OverloadRankingDto> mostSpecificOverloads) {

        IUnionTypeSymbol upperUnion = symbolFactory.createUnionTypeSymbol();
        IOverloadBindings leftBindings = overloadRankingDto.bindings;
        String leftHandSide = constraint.getLeftHandSide().getAbsoluteName();
        String lhsTypeVariable = leftBindings.getTypeVariable(leftHandSide);
        leftBindings.removeUpperTypeBound(lhsTypeVariable);

        List<IVariable> arguments = constraint.getArguments();
        int numberOfArguments = arguments.size();
        List<IUnionTypeSymbol> upperArguments = new ArrayList<>(numberOfArguments);
        int numberOfApplicableOverloads = mostSpecificOverloads.size();

        for (int iMostSpecific = 0; iMostSpecific < numberOfApplicableOverloads; ++iMostSpecific) {
            IOverloadBindings rightBindings = mostSpecificOverloads.get(iMostSpecific).bindings;
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
                    leftBindings.removeUpperTypeBound(typeVariable);
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
        }

        leftBindings.addUpperTypeBound(lhsTypeVariable, upperUnion);
        for (int i = 0; i < numberOfArguments; ++i) {
            String argumentId = arguments.get(i).getAbsoluteName();
            String typeVariable = leftBindings.getTypeVariable(argumentId);
            leftBindings.addUpperTypeBound(typeVariable, upperArguments.get(i));
        }

        //TODO rstoll TINS-306 inference - runtime check insertion
        //need to rewrite the AST in order that it contains the dynamic dispatch mechanism
        //do not forget to set the applied overload for the new function calls as well
    }

}
