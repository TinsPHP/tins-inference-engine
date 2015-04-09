/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IIntersectionConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.IReadOnlyConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.TypeVariableConstraint;
import ch.tsphp.tinsphp.common.symbols.IFunctionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.symbols.constraints.BoundException;
import ch.tsphp.tinsphp.symbols.constraints.TypeConstraint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstraintSolver implements IConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final IOverloadResolver overloadResolver;
    private Deque<WorkListDto> workDeque = new ArrayDeque<>();
    private List<IBinding> solvedBindings = new ArrayList<>();

    public ConstraintSolver(
            ISymbolFactory theSymbolFactory,
            IOverloadResolver theOverloadResolver) {
        symbolFactory = theSymbolFactory;
        overloadResolver = theOverloadResolver;
    }

    public List<IBinding> solveConstraints(IReadOnlyConstraintCollection collection) {

        IBinding binding = new Binding(overloadResolver);
        workDeque.add(new WorkListDto(0, binding));
        List<IIntersectionConstraint> lowerBoundConstraints = collection.getLowerBoundConstraints();

        while (!workDeque.isEmpty()) {
            WorkListDto constraintSolver3Dto = workDeque.removeFirst();
            if (constraintSolver3Dto.pointer < lowerBoundConstraints.size()) {
                IIntersectionConstraint constraint = lowerBoundConstraints.get(constraintSolver3Dto.pointer);
                solve(constraintSolver3Dto, constraint);
            } else {
                solvedBindings.add(constraintSolver3Dto.binding);
            }
        }

        if (solvedBindings.size() == 0) {
            //TODO error case if no overload could be found
        }
        return solvedBindings;
    }

    private void solve(WorkListDto worklistDto, IIntersectionConstraint constraint) {
        boolean atLeastOneBindingCreated
                = createBindingIfNecessary(worklistDto.binding, constraint.getLeftHandSide());
        for (IVariable argument : constraint.getArguments()) {
            boolean neededBinding = createBindingIfNecessary(worklistDto.binding, argument);
            atLeastOneBindingCreated = atLeastOneBindingCreated && neededBinding;
        }

//        if (atLeastOneBindingCreated || constraint.getOverloads().size() == 1) {
        addApplicableOverloadsToWorkList(worklistDto, constraint);
//        } else {
//            IBinding binding = determineMostSpecificOverload(worklistDto, constraint);
//            workDeque.add(new WorkListDto(worklistDto.pointer + 1, binding));
//        }
    }

    private boolean createBindingIfNecessary(IBinding binding, IVariable variable) {
        String absoluteName = variable.getAbsoluteName();
        Map<String, TypeVariableConstraint> bindings = binding.getVariable2TypeVariable();
        boolean wasNecessary = !bindings.containsKey(absoluteName);
        if (wasNecessary) {
            TypeVariableConstraint typeVariableConstraint = binding.getNextTypeVariable();
            bindings.put(absoluteName, typeVariableConstraint);
            //if it is a literal then we know already the lower bound.
            ITypeSymbol typeSymbol = variable.getType();
            if (typeSymbol != null) {
                typeVariableConstraint.setIsConstant();
                TypeConstraint constraint = new TypeConstraint(typeSymbol);
                ITypeVariableCollection typeVariables = binding.getTypeVariables();
                typeVariables.addLowerBound(typeVariableConstraint.getTypeVariable(), constraint);
                wasNecessary = false;
            }
        }
        return wasNecessary;
    }

    //
//    private boolean resolveIntersectionConstraint(
//            ConstraintSolverDto dto, IntersectionConstraint intersectionConstraint) {
//        boolean allArgumentsReady = true;
//        List<IUnionTypeSymbol> refVariableTypes = new ArrayList<>();
//        List<ITypeVariableSymbol> refTypeVariables = intersectionConstraint.getTypeVariables();
//        for (ITypeVariableSymbol refTypeVariable : refTypeVariables) {
//            IUnionTypeSymbol unionTypeSymbol = refTypeVariable.getType();
////            if (!unionTypeSymbol.isReadyForEval()) {
////                allArgumentsReady = false;
////                break;
////            }
//            refVariableTypes.add(unionTypeSymbol);
//        }
//
//        if (allArgumentsReady) {
//            List<OverloadRankingDto> applicableOverloads = getApplicableOverloads(
//                    dto, refVariableTypes, intersectionConstraint);
//
//            if (!applicableOverloads.isEmpty()) {
//                OverloadRankingDto overloadRankingDto;
//                try {
//                    overloadRankingDto = getMostSpecificApplicableOverload(applicableOverloads);
//                } catch (AmbiguousCallException ex) {
//                    //TODO if several calls are valid due to data polymorphism, then we need to take
//                    // the union of all result Types. For Instance float V array + array => once float once array both
//                    // with promotion level = 1 (one cast from float V array to float and one from float V array to
//                    // array)
//
//                    // another example without casting is float V array + float V array => float V array
//                    overloadRankingDto = ex.getAmbiguousOverloads().get(0);
//                }
//
//                //TODO apply needs to retrieve visited functions, otherwise recursive functions would loop
// indefinitely
//                ITypeSymbol returnTypeSymbol = overloadRankingDto.overload.apply(refTypeVariables);
//                addToDtoUnionAndCurrentVariable(dto, returnTypeSymbol);
//            } else {
//                //TODO deal with the error case, how to proceed if no applicable overload was found?
//                //Data flow analysis should abort from this point on
//            }
//            return true;
//        }
//        return false;
//    }
//
//    private void addToDtoUnionAndCurrentVariable(ConstraintSolverDto dto, ITypeSymbol typeSymbol) {
//        IUnionTypeSymbol newType = symbolFactory.createUnionTypeSymbol();
//        newType.addTypeSymbol(typeSymbol);
//        newType.seal();
//        dto.currentTypeVariable.setType(newType);
//        //TODO right now I am not sure how I am going to use dto.unionTypeSymbol, will see when dealing with
//        //recursive functions
//        boolean hasChanged = dto.unionTypeSymbol.addTypeSymbol(typeSymbol);
//        dto.hasUnionChanged = dto.hasUnionChanged || hasChanged;
//    }
//
//    private List<OverloadRankingDto> getApplicableOverloads(
//            ConstraintSolverDto dto,
//            List<IUnionTypeSymbol> refVariableTypes,
//            IntersectionConstraint intersectionConstraint) {
//
//        List<OverloadRankingDto> applicableOverloads = new ArrayList<>();
//        for (IFunctionTypeSymbol overload : intersectionConstraint.getOverloads()) {
//            OverloadRankingDto overloadRankingDto = getApplicableOverload(refVariableTypes, overload);
//            if (overloadRankingDto != null) {
//                applicableOverloads.add(overloadRankingDto);
//                if (isOverloadWithoutPromotionNorConversion(overloadRankingDto)) {
//                    break;
//                }
//            }
//        }
//
//        return applicableOverloads;
//    }
//
//    private boolean isOverloadWithoutPromotionNorConversion(OverloadRankingDto dto) {
//        return dto.parameterPromotedCount == 0 && dto.parametersNeedImplicitConversion.size() == 0
//                && (dto.parametersNeedExplicitConversion == null || dto.parametersNeedExplicitConversion.size() == 0);
//    }
//
//    private OverloadRankingDto getApplicableOverload(
//            List<IUnionTypeSymbol> refVariableTypes, IFunctionTypeSymbol functionTypeSymbol) {
//        List<List<IConstraint>> parametersConstraints = functionTypeSymbol.getInputConstraints();
//        int parameterCount = parametersConstraints.size();
//        int promotionTotalCount = 0;
//        int promotionParameterCount = 0;
//        ConversionDto conversionDto = null;
//        List<ConversionDto> parametersNeedImplicitConversion = new ArrayList<>();
//
//        for (int i = 0; i < parameterCount; ++i) {
//            conversionDto = getConversionDto(refVariableTypes.get(i), parametersConstraints.get(i));
//            if (conversionDto != null) {
//                if (conversionDto.promotionLevel != 0) {
//                    ++promotionParameterCount;
//                    promotionTotalCount += conversionDto.promotionLevel;
//                }
//                //TODO conversions
////                if (conversionDto.castingMethods != null) {
////                    parametersNeedImplicitConversion.add(castingDto);
////                }
//            } else {
//                break;
//            }
//        }
//
//        if (conversionDto != null) {
//            return new OverloadRankingDto(
//                    functionTypeSymbol, promotionParameterCount, promotionTotalCount,
// parametersNeedImplicitConversion);
//        }
//        return null;
//    }
//
//    private ConversionDto getConversionDto(IUnionTypeSymbol refVariableType, List<IConstraint> parameterConstraints) {
//
//        //TODO take conversions into account - implicit conversions should be preferred over explicit conversions
//        ConversionDto conversionDto = null;
//        int highestPromotionLevel = -1;
//        for (IConstraint constraint : parameterConstraints) {
//            if (constraint instanceof TypeConstraint) {
//                conversionDto = getConversionDto(refVariableType, (TypeConstraint) constraint);
//            }
//            if (conversionDto != null) {
//                if (highestPromotionLevel < conversionDto.promotionLevel) {
//                    highestPromotionLevel = conversionDto.promotionLevel;
//                }
//            } else {
//                break;
//            }
//        }
//        if (conversionDto != null) {
//            conversionDto.promotionLevel = highestPromotionLevel;
//        }
//        return conversionDto;
//    }
//
//    private ConversionDto getConversionDto(IUnionTypeSymbol refVariableType, TypeConstraint constraint) {
//        ConversionDto conversionDto;
//        int promotionLevel = overloadResolver.getPromotionLevelFromTo(refVariableType, constraint.getTypeSymbol());
//        if (overloadResolver.isSameOrSubType(promotionLevel)) {
//            conversionDto = new ConversionDto(promotionLevel, 0);
//        } else {
//            //TODO castingDto = getImplicitCastingDto();
//            conversionDto = null;
//        }
//        return conversionDto;
//    }
//
//
//    public OverloadRankingDto getMostSpecificApplicableOverload(List<OverloadRankingDto> overloadRankingDtos) throws
//            AmbiguousCallException {
//
//        List<OverloadRankingDto> ambiguousOverloadRankingDtos = new ArrayList<>();
//
//        OverloadRankingDto mostSpecificMethodDto = overloadRankingDtos.get(0);
//
//        int overloadDtosSize = overloadRankingDtos.size();
//        for (int i = 1; i < overloadDtosSize; ++i) {
//            OverloadRankingDto overloadRankingDto = overloadRankingDtos.get(i);
//            if (isSecondBetter(mostSpecificMethodDto, overloadRankingDto)) {
//                mostSpecificMethodDto = overloadRankingDto;
//                if (ambiguousOverloadRankingDtos.size() > 0) {
//                    ambiguousOverloadRankingDtos = new ArrayList<>();
//                }
//            } else if (isSecondEqual(mostSpecificMethodDto, overloadRankingDto)) {
//                ambiguousOverloadRankingDtos.add(overloadRankingDto);
//            }
//        }
//        if (!ambiguousOverloadRankingDtos.isEmpty()) {
//            ambiguousOverloadRankingDtos.add(mostSpecificMethodDto);
//            throw new AmbiguousCallException(ambiguousOverloadRankingDtos);
//        }
//
//        return mostSpecificMethodDto;
//    }
//
//    private boolean isSecondBetter(OverloadRankingDto mostSpecificMethodDto, OverloadRankingDto methodDto) {
//
//        int mostSpecificCastingSize = mostSpecificMethodDto.parametersNeedImplicitConversion.size();
//        int challengerCastingSize = methodDto.parametersNeedImplicitConversion.size();
//        boolean isSecondBetter = mostSpecificCastingSize > challengerCastingSize;
//        if (!isSecondBetter && mostSpecificCastingSize == challengerCastingSize) {
//            int mostSpecificParameterCount = mostSpecificMethodDto.parameterPromotedCount;
//            int challengerParameterCount = methodDto.parameterPromotedCount;
//            isSecondBetter = mostSpecificParameterCount > challengerParameterCount
//                    || (mostSpecificParameterCount == challengerParameterCount
//                    && mostSpecificMethodDto.promotionsTotal > methodDto.promotionsTotal);
//        }
//        return isSecondBetter;
//    }
//
//    private boolean isSecondEqual(OverloadRankingDto mostSpecificMethodDto, OverloadRankingDto methodDto) {
//        return mostSpecificMethodDto.parametersNeedImplicitConversion.size() == methodDto
//                .parametersNeedImplicitConversion.size()
//                && mostSpecificMethodDto.parameterPromotedCount == methodDto.parameterPromotedCount
//                && mostSpecificMethodDto.promotionsTotal == methodDto.promotionsTotal;
//    }
//
//
    private void addApplicableOverloadsToWorkList(WorkListDto worklistDto, IIntersectionConstraint constraint) {
        for (IFunctionTypeSymbol overload : constraint.getOverloads()) {
            try {
                IBinding binding = solveOverLoad(worklistDto, constraint, overload);
                workDeque.add(new WorkListDto(worklistDto.pointer + 1, binding));
            } catch (BoundException ex) {
                //That is ok, we will deal with it in solveConstraints
            }
        }
    }

    private IBinding solveOverLoad(
            WorkListDto worklistDto,
            IIntersectionConstraint constraint,
            IFunctionTypeSymbol overload) {

        IBinding binding = null;
        if (constraint.getArguments().size() >= overload.getNumberOfNonOptionalParameters()) {
            binding = new Binding(overloadResolver, (Binding) worklistDto.binding);
            aggregateBinding(constraint, overload, binding);
        }

        return binding;
    }

    private IBinding aggregateBinding(
            IIntersectionConstraint constraint,
            IFunctionTypeSymbol overload,
            IBinding binding) {

        List<IVariable> arguments = constraint.getArguments();
        List<String> parameterTypeVariables = overload.getParameterTypeVariables();
        int numberOfParameters = parameterTypeVariables.size();
        Map<String, TypeVariableConstraint> mapping = new HashMap<>(numberOfParameters + 1);
        int numberOfArguments = arguments.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;

        int iterateCount = 0;
        boolean needToIterateOverload = true;
        while (needToIterateOverload) {

            String rhsTypeVariable = overload.getReturnTypeVariable();
            IVariable leftHandSide = constraint.getLeftHandSide();
            needToIterateOverload = !mergeTypeVariables(binding, overload, mapping, leftHandSide, rhsTypeVariable);

            for (int i = 0; i < count; ++i) {
                IVariable argument = arguments.get(i);
                String parameterTypeVariable = parameterTypeVariables.get(i);
                boolean needToIterateParameter = !mergeTypeVariables(
                        binding, overload, mapping, argument, parameterTypeVariable);
                needToIterateOverload = needToIterateOverload || needToIterateParameter;
            }

            if (iterateCount > 1) {
                throw new IllegalStateException("overload uses type variables "
                        + "which are not part of the signature.");
            }
            ++iterateCount;
        }
        return binding;
    }

    private boolean mergeTypeVariables(
            IBinding binding,
            IFunctionTypeSymbol overload,
            Map<String, TypeVariableConstraint> mapping,
            IVariable bindingVariable,
            String overloadTypeVariable) throws BoundException {
        ITypeVariableCollection bindingTypeVariables = binding.getTypeVariables();
        String bindingVariableName = bindingVariable.getAbsoluteName();
        TypeVariableConstraint bindingTypeVariableConstraint
                = binding.getVariable2TypeVariable().get(bindingVariableName);


        String lhsTypeVariable;
        if (mapping.containsKey(overloadTypeVariable)) {
            lhsTypeVariable = mapping.get(overloadTypeVariable).getTypeVariable();
            String rhsTypeVariable = bindingTypeVariableConstraint.getTypeVariable();
            if (!lhsTypeVariable.equals(rhsTypeVariable)) {
                transferConstraints(bindingTypeVariables, lhsTypeVariable, rhsTypeVariable);
                bindingTypeVariableConstraint.setTypeVariable(lhsTypeVariable);
                //TODO remove lower and upper bounds of unused type variable
            }
        } else {
            lhsTypeVariable = bindingTypeVariableConstraint.getTypeVariable();
            mapping.put(overloadTypeVariable, bindingTypeVariableConstraint);
        }

        ITypeVariableCollection overloadTypeVariables = overload.getTypeVariables();
        return applyRightToLeft(
                mapping, bindingTypeVariables, lhsTypeVariable, overloadTypeVariables, overloadTypeVariable);
    }

    private void transferConstraints(ITypeVariableCollection typeVariables, String lhs, String rhs) {
        if (typeVariables.hasUpperBounds(rhs)) {
            for (IConstraint upperBound : typeVariables.getUpperBounds(rhs)) {
                typeVariables.addUpperBound(lhs, upperBound);
            }
        }
        if (typeVariables.hasLowerBounds(rhs)) {
            for (IConstraint upperBound : typeVariables.getLowerBounds(rhs)) {
                typeVariables.addLowerBound(lhs, upperBound);
            }
        }
    }

    private boolean applyRightToLeft(
            Map<String, TypeVariableConstraint> mapping,
            ITypeVariableCollection leftCollection, String left,
            ITypeVariableCollection rightCollection, String right) throws BoundException {

        if (rightCollection.hasUpperBounds(right)) {
            for (IConstraint upperBound : rightCollection.getUpperBounds(right)) {
                //upper bounds do not have TypeVariableConstraint
                leftCollection.addUpperBound(left, upperBound);
            }
        }

        boolean couldAddLower = true;
        if (rightCollection.hasLowerBounds(right)) {
            for (IConstraint lowerBound : rightCollection.getLowerBounds(right)) {
                if (lowerBound instanceof TypeConstraint) {
                    leftCollection.addLowerBound(left, lowerBound);
                } else if (lowerBound instanceof TypeVariableConstraint) {
                    String typeVariable = ((TypeVariableConstraint) lowerBound).getTypeVariable();
                    if (mapping.containsKey(typeVariable)) {
                        leftCollection.addLowerBound(left, mapping.get(typeVariable));
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
}
