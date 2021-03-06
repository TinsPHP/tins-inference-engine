/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.BoundException;
import ch.tsphp.tinsphp.common.inference.constraints.EBindingCollectionMode;
import ch.tsphp.tinsphp.common.inference.constraints.FixedTypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.OverloadApplicationDto;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IParametricTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IPolymorphicTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.utils.ERelation;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.common.utils.TypeHelperDto;
import ch.tsphp.tinsphp.inference_engine.constraints.AggregateBindingDto;
import ch.tsphp.tinsphp.inference_engine.constraints.IMostSpecificOverloadDecider;
import ch.tsphp.tinsphp.inference_engine.constraints.OverloadRankingDto;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void solveConstraints(IConstraintCollection constraintCollection, WorkItemDto workItemDto) {
        initWorkListDtoBindingCollection(workItemDto);

        //TODO TINS-535 improve precision in soft typing for unconstrained parameters
        //idea how precision of parametric parameters could be enhanced (instead of using mixed as above)
//        for (IVariableSymbol parameter : constraintCollection.getParameters()) {
//            String parameterId = parameter.getAbsoluteName();
//            String typeVariable = softTypingBindings.getTypeVariable(parameterId);
//            if (!softTypingBindings.hasLowerTypeBounds(typeVariable)) {
//                propagateParameterToParameters(
//                        typeVariable,
//                        typeVariable,
//                        parameterTypeVariables,
//                        softTypingBindings,
//                        workItemDto.param2LowerParams);
//            }
//        }

        workItemDto.bindingCollection.setMode(EBindingCollectionMode.Modification);
        workItemDto.isInSoftTypingMode = false;
        solveConstraintsAfterInit(constraintCollection, workItemDto);
        //Notice! workItemDto.bindingCollection is not the same binding collection as above
        workItemDto.bindingCollection.setMode(EBindingCollectionMode.Normal);
    }

    private void initWorkListDtoBindingCollection(WorkItemDto workItemDto) {

        IBindingCollection softTypingBindings = workItemDto.bindingCollection;
        workItemDto.bindingCollection = symbolFactory.createBindingCollection();

        for (String variableId : softTypingBindings.getVariableIds()) {
            if (variableId.contains("$")) {
                addVariableToLeftBindings(variableId, softTypingBindings, workItemDto.bindingCollection);
            }
        }
    }

    private void addVariableToLeftBindings(
            String parameterId,
            IBindingCollection softTypingBindings,
            IBindingCollection leftBindings) {

        String typeVariable;
        if (softTypingBindings.containsVariable(parameterId)) {
            typeVariable = softTypingBindings.getTypeVariable(parameterId);
        } else {
            //the parameter is not used at all, hence it can be mixed
            ITypeVariableReference reference = new FixedTypeVariableReference(softTypingBindings.getNextTypeVariable());
            typeVariable = reference.getTypeVariable();
            softTypingBindings.addVariable(parameterId, reference);
            softTypingBindings.addUpperTypeBound(typeVariable, mixedTypeSymbol);
            //TODO could generate a warning
        }
        ITypeVariableReference nextTypeVariable = leftBindings.getNextTypeVariable();
        leftBindings.addVariable(parameterId, new FixedTypeVariableReference(nextTypeVariable));
        if (softTypingBindings.hasLowerTypeBounds(typeVariable)) {
            //TODO TINS-534 type hints and soft-typing
            // we need to introduce a local variable here if a type hint was used and the inferred type is
            // different
            IUnionTypeSymbol lowerTypeBounds = softTypingBindings.getLowerTypeBounds(typeVariable);
            leftBindings.addLowerTypeBound(nextTypeVariable.getTypeVariable(), lowerTypeBounds);
        }
    }


    //TINS-535 improve precision in soft typing for unconstrained parameters
//    private void propagateParameterToParameters(
//            String refTypeVariable,
//            String typeVariable,
//            Set<String> parameterTypeVariables,
//            IBindingCollection softTypingBindings,
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

    private void solveConstraintsAfterInit(IConstraintCollection constraintCollection, WorkItemDto workItemDto) {
        for (IConstraint constraint : constraintCollection.getConstraints()) {
            List<IVariable> arguments = constraint.getArguments();
            int numberOfArguments = arguments.size();

            constraintSolverHelper.createBindingsIfNecessary(
                    workItemDto, constraint.getLeftHandSide(), constraint.getArguments());

            List<OverloadRankingDto> applicableOverloads = new ArrayList<>();
            for (IFunctionType overload : constraint.getMethodSymbol().getOverloads()) {
                if (numberOfArguments >= overload.getNumberOfNonOptionalParameters()) {
                    IBindingCollection leftBindings = symbolFactory.createBindingCollection(
                            workItemDto.bindingCollection);
                    Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> runtimeChecks = new HashMap<>();
                    boolean overloadApplies = isApplicable(constraint, overload, leftBindings, runtimeChecks);
                    if (overloadApplies) {
                        try {
                            OverloadRankingDto dto = applyOverload(
                                    workItemDto, constraint, overload, leftBindings, runtimeChecks);
                            applicableOverloads.add(dto);
                        } catch (BoundException ex) {
                            //ok, must be a lower type exception
                        }
                    }
                }
            }

            int numberOfApplicableOverloads = applicableOverloads.size();
            if (numberOfApplicableOverloads > 0) {
                OverloadRankingDto overloadRankingDto = applicableOverloads.get(0);
                String leftHandSide = constraint.getLeftHandSide().getAbsoluteName();
                if (numberOfApplicableOverloads > 1) {
                    List<OverloadRankingDto> mostSpecificOverloads
                            = mostSpecificOverloadDecider.inSoftTypingMode(workItemDto, applicableOverloads);
                    overloadRankingDto = mostSpecificOverloads.get(0);
                    numberOfApplicableOverloads = mostSpecificOverloads.size();
                    if (numberOfApplicableOverloads > 1) {
                        mergeMostSpecificOverloadsToNewBindingCollection(
                                workItemDto, constraint, mostSpecificOverloads);
                    }
                }

                if (numberOfApplicableOverloads == 1) {
                    workItemDto.bindingCollection = overloadRankingDto.bindings;
                    OverloadApplicationDto dto = new OverloadApplicationDto(
                            overloadRankingDto.overload,
                            overloadRankingDto.implicitConversions,
                            overloadRankingDto.runtimeChecks);
                    workItemDto.bindingCollection.setAppliedOverload(leftHandSide, dto);
                }
            } else {
                issueReporter.constraintViolation(workItemDto.bindingCollection, constraint);
            }
        }
    }

    private boolean isApplicable(
            IConstraint constraint,
            IFunctionType overload,
            IBindingCollection leftBindings,
            Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> runtimeChecks) {

        IBindingCollection rightBindings = overload.getBindingCollection();
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
            IBindingCollection leftBindings,
            String argumentId,
            int argumentNumber,
            IBindingCollection rightBindings,
            String parameterTypeVariable,
            Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> runtimeChecks) {

        String argumentTypeVariable = leftBindings.getTypeVariable(argumentId);
        IUnionTypeSymbol argumentType = leftBindings.getLowerTypeBounds(argumentTypeVariable);

        //TODO TINS-535 improve precision in soft typing for unconstrained parameters
//                if (argumentType == null || workItemDto.param2LowerParams.containsKey(argumentTypeVariable)) {
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
                        typeSymbols.add(copyIfRequired(typeSymbol));
                    } else {
                        result = typeHelper.isFirstSameOrSubTypeOfSecond(parameterType, typeSymbol);
                        if (result.relation == ERelation.HAS_RELATION) {
                            typeSymbols.add(copyIfRequired(parameterType));
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
//                                for (String refTypeVariable : workItemDto.param2LowerParams.get
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

    private ITypeSymbol copyIfRequired(ITypeSymbol typeSymbol) {
        ITypeSymbol fixedTypeSymbol = typeSymbol;
        if (typeSymbol instanceof IPolymorphicTypeSymbol) {
            IPolymorphicTypeSymbol polymorphicType = (IPolymorphicTypeSymbol) typeSymbol;
            if (!polymorphicType.isFixed()) {
                Collection<IParametricTypeSymbol> parametricTypeSymbols = new ArrayDeque<>();
                fixedTypeSymbol = polymorphicType.copy(parametricTypeSymbols);
                for (IParametricTypeSymbol parametricTypeSymbol : parametricTypeSymbols) {
                    IBindingCollection copyBindings = symbolFactory.createBindingCollection(
                            parametricTypeSymbol.getBindingCollection());
                    copyBindings.bind(parametricTypeSymbol, parametricTypeSymbol.getTypeParameters());
                    copyBindings.fixTypeParameters();
                }
            }
        }
        return fixedTypeSymbol;
    }

    private OverloadRankingDto applyOverload(
            WorkItemDto workItemDto,
            IConstraint constraint,
            IFunctionType overload,
            IBindingCollection leftBindings,
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
                new AggregateBindingDto(constraint, overload, leftBindings, workItemDto);
        constraintSolverHelper.aggregateBinding(dto);

        OverloadRankingDto overloadRankingDto = new OverloadRankingDto(
                overload,
                leftBindings,
                dto.implicitConversions,
                runtimeChecks,
                dto.helperVariableMapping,
                requiresExplicit,
                dto.hasChanged);

        if (requiresExplicit) {
            for (Map.Entry<String, IUnionTypeSymbol> entry : oldTypeBounds.entrySet()) {
                String parameterId = entry.getKey();
                ITypeVariableReference reference = leftBindings.getTypeVariableReference(parameterId);
                String typeVariable = reference.getTypeVariable();
                IUnionTypeSymbol oldLowerTypeBound = entry.getValue();
                if (!reference.hasFixedType()) {
                    IIntersectionTypeSymbol newUpperTypeBound = leftBindings.getUpperTypeBounds(typeVariable);
                    if (newUpperTypeBound != null) {
                        IIntersectionTypeSymbol leastUpperBound;
                        if (oldLowerTypeBound != null) {
                            leastUpperBound = getLeastCommonUpperTypeBound(oldLowerTypeBound, newUpperTypeBound);
                        } else {
                            leastUpperBound = newUpperTypeBound;
                        }
                        leftBindings.setUpperTypeBounds(typeVariable, leastUpperBound);
                    }
                } else {
                    IIntersectionTypeSymbol intersectionTypeSymbol = symbolFactory.createIntersectionTypeSymbol();
                    intersectionTypeSymbol.addTypeSymbol(oldLowerTypeBound);
                    leftBindings.setUpperTypeBounds(typeVariable, intersectionTypeSymbol);
                }
                if (oldLowerTypeBound != null) {
                    leftBindings.addLowerTypeBound(typeVariable, oldLowerTypeBound);
                }
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
    private void mergeMostSpecificOverloadsToNewBindingCollection(
            WorkItemDto workItemDto, IConstraint constraint, List<OverloadRankingDto> mostSpecificOverloads) {

        IUnionTypeSymbol upperUnion = symbolFactory.createUnionTypeSymbol();

        IBindingCollection leftBindings = mostSpecificOverloads.get(0).bindings;
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
            IBindingCollection rightBindings = overloadRankingDto.bindings;
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
                    IUnionTypeSymbol unionTypeSymbol = symbolFactory.createUnionTypeSymbol();
                    if (runtimeChecks != null) {
                        runtimeChecks.put(iArgument, new Pair<ITypeSymbol, List<ITypeSymbol>>(unionTypeSymbol, null));
                    }
                }

                IUnionTypeSymbol upperArgument = upperArguments.get(iArgument);
                String rankingTypeVariable = rightBindings.getTypeVariable(argumentId);
                if (rightBindings.hasUpperTypeBounds(rankingTypeVariable)) {
                    IIntersectionTypeSymbol upperTypeBounds = rightBindings.getUpperTypeBounds(rankingTypeVariable);
                    upperArgument.addTypeSymbol(upperTypeBounds);
                }

                if (rightBindings.hasLowerTypeBounds(rankingTypeVariable)) {
                    IUnionTypeSymbol lowerTypeBounds = rightBindings.getLowerTypeBounds(rankingTypeVariable);
                    leftBindings.addLowerTypeBound(typeVariable, lowerTypeBounds);
                }

                if (runtimeChecks != null && runtimeChecks.containsKey(iArgument)) {
                    if (overloadRankingDto.runtimeChecks != null) {
                        if (overloadRankingDto.runtimeChecks.containsKey(iArgument)) {
                            Pair<ITypeSymbol, List<ITypeSymbol>> pair = overloadRankingDto.runtimeChecks.get(iArgument);
                            IUnionTypeSymbol unionTypeSymbol = (IUnionTypeSymbol) runtimeChecks.get(iArgument).first;
                            unionTypeSymbol.addTypeSymbol(pair.first);
                        } else {
                            runtimeChecks.remove(iArgument);
                        }
                    } else {
                        runtimeChecks = null;
                    }
                }
            }
        }

        if (!upperUnion.getTypeSymbols().isEmpty()) {
            leftBindings.addUpperTypeBound(lhsTypeVariable, upperUnion);
        }
        for (int i = 0; i < numberOfArguments; ++i) {
            String argumentId = arguments.get(i).getAbsoluteName();
            String typeVariable = leftBindings.getTypeVariable(argumentId);
            IUnionTypeSymbol typeSymbol = upperArguments.get(i);
            if (!typeSymbol.getTypeSymbols().isEmpty()) {
                leftBindings.addUpperTypeBound(typeVariable, typeSymbol);
            }
        }

        workItemDto.bindingCollection = leftBindings;
        //overload = null indicates that we need to fall back to the dynamic version
        workItemDto.bindingCollection.setAppliedOverload(
                leftHandSide, new OverloadApplicationDto(null, null, runtimeChecks));
    }

}
