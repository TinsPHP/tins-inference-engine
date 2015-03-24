/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.IReadOnlyTypeVariableCollection;
import ch.tsphp.tinsphp.common.symbols.IFunctionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;
import ch.tsphp.tinsphp.symbols.constraints.TransferConstraint;
import ch.tsphp.tinsphp.symbols.constraints.TypeConstraint;
import ch.tsphp.tinsphp.symbols.constraints.UnionConstraint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstraintSolver implements IConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final IOverloadResolver overloadResolver;

    public ConstraintSolver(
            ISymbolFactory theSymbolFactory,
            IOverloadResolver theOverloadResolver) {
        symbolFactory = theSymbolFactory;
        overloadResolver = theOverloadResolver;
    }

    @Override
    public void solveConstraints(IReadOnlyTypeVariableCollection currentScope) {
        Deque<ITypeVariableSymbol> typeVariables = new ArrayDeque<>(currentScope.getTypeVariables());
        while (!typeVariables.isEmpty()) {
            Map<String, Integer> visitedTypeVariables = new HashMap<>();
            Set<String> typeVariablesToRevisit = new HashSet<>();
            ITypeVariableSymbol constraintSymbol = typeVariables.removeFirst();
            IUnionTypeSymbol unionTypeSymbol = constraintSymbol.getType();

            ConstraintSolverDto dto = new ConstraintSolverDto(
                    typeVariables,
                    visitedTypeVariables,
                    typeVariablesToRevisit,
                    constraintSymbol,
                    unionTypeSymbol
            );

            IConstraint constraint = constraintSymbol.getConstraint();
            solveConstraint(dto, constraint);
        }

        for (ITypeVariableSymbol typeVariableSymbol : currentScope.getTypeVariablesWithRef()) {
            typeVariableSymbol.getType().seal();
        }
    }

    private boolean solveConstraint(ConstraintSolverDto dto, IConstraint constraint) {
        if (constraint instanceof TransferConstraint) {
            return resolveTransferConstraint(dto, (TransferConstraint) constraint);
        } else if (constraint instanceof IntersectionConstraint) {
            return resolveIntersectionConstraint(dto, (IntersectionConstraint) constraint);
        } else if (constraint instanceof ITypeVariableSymbol) {
            return resolveReferenceConstraint(dto, (ITypeVariableSymbol) constraint);
        } else if (constraint instanceof UnionConstraint) {
            return resolveUnionConstraint(dto, (UnionConstraint) constraint);
        }
        throw new IllegalArgumentException("Unknown TypeConstraint " + constraint.getClass());
    }

    private boolean resolveTransferConstraint(ConstraintSolverDto dto, TransferConstraint constraint) {
        IUnionTypeSymbol unionTypeSymbol = constraint.getTypeVariableSymbol().getType();
        if (unionTypeSymbol.isReadyForEval()) {
            dto.currentTypeVariable.getType().merge(unionTypeSymbol);
            //TODO right now I am not sure how I am going to use dto.unionTypeSymbol, will see when dealing with
            //recursive functions
            boolean hasChanged = dto.unionTypeSymbol.merge(unionTypeSymbol);
            dto.hasUnionChanged = dto.hasUnionChanged || hasChanged;
            return true;
        }
        return false;
    }

    private boolean resolveIntersectionConstraint(
            ConstraintSolverDto dto, IntersectionConstraint intersectionConstraint) {
        boolean allArgumentsReady = true;
        List<IUnionTypeSymbol> refVariableTypes = new ArrayList<>();
        List<ITypeVariableSymbol> refTypeVariables = intersectionConstraint.getTypeVariables();
        for (ITypeVariableSymbol refTypeVariable : refTypeVariables) {
            IUnionTypeSymbol unionTypeSymbol = refTypeVariable.getType();
//            if (!unionTypeSymbol.isReadyForEval()) {
//                allArgumentsReady = false;
//                break;
//            }
            refVariableTypes.add(unionTypeSymbol);
        }

        if (allArgumentsReady) {
            List<OverloadRankingDto> applicableOverloads = getApplicableOverloads(
                    dto, refVariableTypes, intersectionConstraint);

            if (!applicableOverloads.isEmpty()) {
                OverloadRankingDto overloadRankingDto;
                try {
                    overloadRankingDto = getMostSpecificApplicableOverload(applicableOverloads);
                } catch (AmbiguousCallException ex) {
                    //TODO if several calls are valid due to data polymorphism, then we need to take
                    // the union of all result Types. For Instance float V array + array => once float once array both
                    // with promotion level = 1 (one cast from float V array to float and one from float V array to
                    // array)

                    // another example without casting is float V array + float V array => float V array
                    overloadRankingDto = ex.getAmbiguousOverloads().get(0);
                }

                //TODO apply needs to retrieve visited functions, otherwise recursive functions would loop indefinitely
                ITypeSymbol returnTypeSymbol = overloadRankingDto.overload.apply(refTypeVariables);
                addToDtoUnionAndCurrentVariable(dto, returnTypeSymbol);
            } else {
                //TODO deal with the error case, how to proceed if no applicable overload was found?
                //Data flow analysis should abort from this point on
            }
            return true;
        }
        return false;
    }

    private void addToDtoUnionAndCurrentVariable(ConstraintSolverDto dto, ITypeSymbol typeSymbol) {
        IUnionTypeSymbol newType = symbolFactory.createUnionTypeSymbol();
        newType.addTypeSymbol(typeSymbol);
        newType.seal();
        dto.currentTypeVariable.setType(newType);
        //TODO right now I am not sure how I am going to use dto.unionTypeSymbol, will see when dealing with
        //recursive functions
        boolean hasChanged = dto.unionTypeSymbol.addTypeSymbol(typeSymbol);
        dto.hasUnionChanged = dto.hasUnionChanged || hasChanged;
    }

    private List<OverloadRankingDto> getApplicableOverloads(
            ConstraintSolverDto dto,
            List<IUnionTypeSymbol> refVariableTypes,
            IntersectionConstraint intersectionConstraint) {

        List<OverloadRankingDto> applicableOverloads = new ArrayList<>();
        for (IFunctionTypeSymbol overload : intersectionConstraint.getOverloads()) {
            OverloadRankingDto overloadRankingDto = getApplicableOverload(refVariableTypes, overload);
            if (overloadRankingDto != null) {
                applicableOverloads.add(overloadRankingDto);
                if (isOverloadWithoutPromotionNorConversion(overloadRankingDto)) {
                    break;
                }
            }
        }

        return applicableOverloads;
    }

    private boolean isOverloadWithoutPromotionNorConversion(OverloadRankingDto dto) {
        return dto.parameterPromotedCount == 0 && dto.parametersNeedImplicitConversion.size() == 0
                && (dto.parametersNeedExplicitConversion == null || dto.parametersNeedExplicitConversion.size() == 0);
    }

    private OverloadRankingDto getApplicableOverload(
            List<IUnionTypeSymbol> refVariableTypes, IFunctionTypeSymbol functionTypeSymbol) {
        List<List<IConstraint>> parametersConstraints = functionTypeSymbol.getInputConstraints();
        int parameterCount = parametersConstraints.size();
        int promotionTotalCount = 0;
        int promotionParameterCount = 0;
        ConversionDto conversionDto = null;
        List<ConversionDto> parametersNeedImplicitConversion = new ArrayList<>();

        for (int i = 0; i < parameterCount; ++i) {
            conversionDto = getConversionDto(refVariableTypes.get(i), parametersConstraints.get(i));
            if (conversionDto != null) {
                if (conversionDto.promotionLevel != 0) {
                    ++promotionParameterCount;
                    promotionTotalCount += conversionDto.promotionLevel;
                }
                //TODO conversions
//                if (conversionDto.castingMethods != null) {
//                    parametersNeedImplicitConversion.add(castingDto);
//                }
            } else {
                break;
            }
        }

        if (conversionDto != null) {
            return new OverloadRankingDto(
                    functionTypeSymbol, promotionParameterCount, promotionTotalCount, parametersNeedImplicitConversion);
        }
        return null;
    }

    private ConversionDto getConversionDto(IUnionTypeSymbol refVariableType, List<IConstraint> parameterConstraints) {

        //TODO take conversions into account - implicit conversions should be preferred over explicit conversions
        ConversionDto conversionDto = null;
        int highestPromotionLevel = -1;
        for (IConstraint constraint : parameterConstraints) {
            if (constraint instanceof TypeConstraint) {
                conversionDto = getConversionDto(refVariableType, (TypeConstraint) constraint);
            }
            if (conversionDto != null) {
                if (highestPromotionLevel < conversionDto.promotionLevel) {
                    highestPromotionLevel = conversionDto.promotionLevel;
                }
            } else {
                break;
            }
        }
        if (conversionDto != null) {
            conversionDto.promotionLevel = highestPromotionLevel;
        }
        return conversionDto;
    }

    private ConversionDto getConversionDto(IUnionTypeSymbol refVariableType, TypeConstraint constraint) {
        ConversionDto conversionDto;
        int promotionLevel = overloadResolver.getPromotionLevelFromTo(refVariableType, constraint.getTypeSymbol());
        if (overloadResolver.isSameOrSubType(promotionLevel)) {
            conversionDto = new ConversionDto(promotionLevel, 0);
        } else {
            //TODO castingDto = getImplicitCastingDto();
            conversionDto = null;
        }
        return conversionDto;
    }


    public OverloadRankingDto getMostSpecificApplicableOverload(List<OverloadRankingDto> overloadRankingDtos) throws
            AmbiguousCallException {

        List<OverloadRankingDto> ambiguousOverloadRankingDtos = new ArrayList<>();

        OverloadRankingDto mostSpecificMethodDto = overloadRankingDtos.get(0);

        int overloadDtosSize = overloadRankingDtos.size();
        for (int i = 1; i < overloadDtosSize; ++i) {
            OverloadRankingDto overloadRankingDto = overloadRankingDtos.get(i);
            if (isSecondBetter(mostSpecificMethodDto, overloadRankingDto)) {
                mostSpecificMethodDto = overloadRankingDto;
                if (ambiguousOverloadRankingDtos.size() > 0) {
                    ambiguousOverloadRankingDtos = new ArrayList<>();
                }
            } else if (isSecondEqual(mostSpecificMethodDto, overloadRankingDto)) {
                ambiguousOverloadRankingDtos.add(overloadRankingDto);
            }
        }
        if (!ambiguousOverloadRankingDtos.isEmpty()) {
            ambiguousOverloadRankingDtos.add(mostSpecificMethodDto);
            throw new AmbiguousCallException(ambiguousOverloadRankingDtos);
        }

        return mostSpecificMethodDto;
    }

    private boolean isSecondBetter(OverloadRankingDto mostSpecificMethodDto, OverloadRankingDto methodDto) {

        int mostSpecificCastingSize = mostSpecificMethodDto.parametersNeedImplicitConversion.size();
        int challengerCastingSize = methodDto.parametersNeedImplicitConversion.size();
        boolean isSecondBetter = mostSpecificCastingSize > challengerCastingSize;
        if (!isSecondBetter && mostSpecificCastingSize == challengerCastingSize) {
            int mostSpecificParameterCount = mostSpecificMethodDto.parameterPromotedCount;
            int challengerParameterCount = methodDto.parameterPromotedCount;
            isSecondBetter = mostSpecificParameterCount > challengerParameterCount
                    || (mostSpecificParameterCount == challengerParameterCount
                    && mostSpecificMethodDto.promotionsTotal > methodDto.promotionsTotal);
        }
        return isSecondBetter;
    }

    private boolean isSecondEqual(OverloadRankingDto mostSpecificMethodDto, OverloadRankingDto methodDto) {
        return mostSpecificMethodDto.parametersNeedImplicitConversion.size() == methodDto
                .parametersNeedImplicitConversion.size()
                && mostSpecificMethodDto.parameterPromotedCount == methodDto.parameterPromotedCount
                && mostSpecificMethodDto.promotionsTotal == methodDto.promotionsTotal;
    }

    private boolean resolveReferenceConstraint(ConstraintSolverDto dto, ITypeVariableSymbol refTypeVariable) {
        IUnionTypeSymbol unionTypeSymbol = refTypeVariable.getType();
        if (unionTypeSymbol.isReadyForEval()) {
            IUnionTypeSymbol newType = symbolFactory.createUnionTypeSymbol();
            newType.merge(unionTypeSymbol);
            newType.seal();
            dto.currentTypeVariable.setType(newType);
            //TODO right now I am not sure how I am going to use dto.unionTypeSymbol, will see when dealing with
            //recursive functions
            boolean hasChanged = dto.unionTypeSymbol.merge(unionTypeSymbol);
            dto.hasUnionChanged = dto.hasUnionChanged || hasChanged;
            return true;
        }
        return false;
    }


    private boolean resolveUnionConstraint(ConstraintSolverDto dto, UnionConstraint unionConstraint) {
        boolean allTypesReady = true;
        for (ITypeVariableSymbol refTypeVariable : unionConstraint.getTypeVariables()) {
            if (!resolveReferenceConstraint(dto, refTypeVariable)) {
                allTypesReady = false;
                break;
            }
        }
        return allTypesReady;
    }
}
