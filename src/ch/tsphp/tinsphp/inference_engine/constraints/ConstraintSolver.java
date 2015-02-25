/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.IConstraint;
import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstraintSolver implements IConstraintSolver
{
    private ITypeSymbol nothingTypeSymbol;
    private ISymbolFactory symbolFactory;
    private IOverloadResolver overloadResolver;

    public ConstraintSolver(
            ITypeSymbol theNothingTypeSymbol, ISymbolFactory theSymbolFactory, IOverloadResolver theOverloadResolver) {
        nothingTypeSymbol = theNothingTypeSymbol;
        symbolFactory = theSymbolFactory;
        overloadResolver = theOverloadResolver;
    }

    @Override
    public void solveConstraintsOfScope(IScope currentScope) {
        for (Map.Entry<String, List<IConstraint>> constraintsEntry : currentScope.getConstraints().entrySet()) {
            Set<String> visitedVariables = new HashSet<>();
            solveAndAddToResultsIfNotAlreadySolved(
                    currentScope,
                    constraintsEntry.getKey(),
                    new ConstraintSolverDto(
                            visitedVariables,
                            constraintsEntry.getValue(),
                            symbolFactory.createUnionTypeSymbol(new HashMap<String, ITypeSymbol>())
                    ));

        }
    }

    private IUnionTypeSymbol solveAndAddToResultsIfNotAlreadySolved(
            IScope currentScope, String variableId, ConstraintSolverDto dto) {
        IUnionTypeSymbol unionTypeSymbol = currentScope.getResultOfConstraintSolving(variableId);
        if (unionTypeSymbol == null) {
            unionTypeSymbol = addToVisitedAndSolve(getVisitKey(currentScope, variableId), dto);
            unionTypeSymbol.seal();
            currentScope.setResultOfConstraintSolving(variableId, unionTypeSymbol);
        }
        return unionTypeSymbol;
    }

    private IUnionTypeSymbol addToVisitedAndSolve(String visitKey, ConstraintSolverDto dto) {
        dto.visitedVariables.add(visitKey);
        return solveConstraints(dto);
    }

    private String getVisitKey(IScope currentScope, String variableId) {
        return currentScope.getScopeName() + variableId;
    }

    private IUnionTypeSymbol solveConstraints(ConstraintSolverDto dto) {
        List<IConstraint> iterativeConstraints = new ArrayList<>();
        for (IConstraint constraint : dto.constraints) {
            try {
                solveConstraint(dto, constraint);
            } catch (SelfReferenceInIntersectionException e) {
                iterativeConstraints.add(constraint);
            }
        }

        if (iterativeConstraints.size() != 0) {
            solveIterativeConstraints(dto, iterativeConstraints);
        }
        return dto.unionTypeSymbol;
    }

    private void solveConstraint(ConstraintSolverDto dto, IConstraint constraint) {
        if (constraint instanceof TypeConstraint) {
            dto.unionTypeSymbol.addTypeSymbol(((TypeConstraint) constraint).getType());
        } else if (constraint instanceof RefConstraint) {
            resolveReferenceConstraint(dto, (RefConstraint) constraint);
        } else if (constraint instanceof IntersectionConstraint) {
            resolveIntersectionConstraint(dto, (IntersectionConstraint) constraint);
        }
    }

    private void solveIterativeConstraints(ConstraintSolverDto dto, List<IConstraint> constraints) {
        dto.hasUnionChanged = true;
        //TODO recursive functions
        //Sure that it terminates? nope I do not think so, need to add a guard, fall back to mixed after 10 attempts
        // or similar
        while (dto.hasUnionChanged) {
            dto.hasUnionChanged = false;
            for (IConstraint constraint : constraints) {
                solveConstraint(dto, constraint);
            }
        }
    }

    private void resolveReferenceConstraint(ConstraintSolverDto dto, RefConstraint refConstraint) {
        IScope refScope = refConstraint.getRefScope();
        String refVariableId = refConstraint.getRefVariableId();
        IUnionTypeSymbol unionTypeSymbol = refScope.getResultOfConstraintSolving(refVariableId);
        if (unionTypeSymbol == null) {
            String visitKey = getVisitKey(refScope, refVariableId);
            if (!dto.visitedVariables.contains(visitKey)) {
                addToVisitedAndSolve(
                        visitKey,
                        new ConstraintSolverDto(
                                dto.visitedVariables,
                                refScope.getConstraintsForVariable(refVariableId),
                                dto.unionTypeSymbol
                        ));
            }
        } else {
            dto.unionTypeSymbol.merge(unionTypeSymbol);
        }
    }

    private void resolveIntersectionConstraint(ConstraintSolverDto dto, IntersectionConstraint intersectionConstraint) {
        OverloadDto overloadDto = null;

        List<OverloadDto> goodMethods = getApplicableOverloads(dto, intersectionConstraint);
        if (!goodMethods.isEmpty()) {
            try {
                overloadDto = getMostSpecificApplicableOverload(goodMethods);
            } catch (AmbiguousCallException ex) {
                //TODO report error
//                ambiguousCallReporter.report(ex);
                overloadDto = ex.getAmbiguousOverloads().get(0);
            }
        }

        if (overloadDto != null) {
            addConversionsToAstIfNecessary(overloadDto);
            dto.hasUnionChanged = dto.unionTypeSymbol.addTypeSymbol(overloadDto.overload.getValue());
        }
    }

    private List<OverloadDto> getApplicableOverloads(
            ConstraintSolverDto dto, IntersectionConstraint intersectionConstraint) {
        List<OverloadDto> applicableOverloads = new ArrayList<>();
        for (Map.Entry<List<RefTypeConstraint>, ITypeSymbol> overload : intersectionConstraint.getOverloads()) {
            OverloadDto overloadDto = getApplicableOverload(dto, overload);
            if (overloadDto != null) {
                applicableOverloads.add(overloadDto);
                if (isOverloadWithoutPromotionNorConversion(overloadDto)) {
                    break;
                }
            }
        }
        return applicableOverloads;
    }

    private boolean isOverloadWithoutPromotionNorConversion(OverloadDto dto) {
        return dto.parameterPromotedCount == 0 && dto.parametersNeedImplicitConversion.size() == 0
                && (dto.parametersNeedExplicitConversion == null || dto.parametersNeedExplicitConversion.size() == 0);
    }

    private OverloadDto getApplicableOverload(
            ConstraintSolverDto dto, Map.Entry<List<RefTypeConstraint>, ITypeSymbol> overload) {
        List<RefTypeConstraint> parameterConstraints = overload.getKey();
        int promotionTotalCount = 0;
        int promotionParameterCount = 0;
        ConversionDto conversionDto = null;
        List<ConversionDto> parametersNeedImplicitConversion = new ArrayList<>();

        for (RefTypeConstraint parameterConstraint : parameterConstraints) {
            conversionDto = getCastingDto(dto, parameterConstraint);
            if (conversionDto != null) {
                if (conversionDto.promotionLevel != 0) {
                    ++promotionParameterCount;
                    promotionTotalCount += conversionDto.promotionLevel;
                }
                //TODO casting
//                if (castingDto.castingMethods != null) {
//                    parametersNeedImplicitConversion.add(castingDto);
//                }
            } else {
                break;
            }
        }

        if (conversionDto != null) {
            return new OverloadDto(
                    overload, promotionParameterCount, promotionTotalCount, parametersNeedImplicitConversion);
        }
        return null;
    }

    private ConversionDto getCastingDto(ConstraintSolverDto dto, RefTypeConstraint parameterConstraint) {
        IVariableSymbol variableSymbol = (IVariableSymbol) parameterConstraint.getVariableIdAst().getSymbol();
        if (!variableSymbol.isAlwaysCasting()) {
            return getCastingDtoInNormalMode(dto, parameterConstraint);
        }
        return getCastingDtoInAlwaysCastingMode(dto, parameterConstraint);
    }

    private ConversionDto getCastingDtoInNormalMode(ConstraintSolverDto dto, RefTypeConstraint parameterConstraint) {
        ConversionDto conversionDto;

        IScope refScope = parameterConstraint.getRefScope();
        String refVariableId = parameterConstraint.getRefVariableId();
        IUnionTypeSymbol unionTypeSymbol = refScope.getResultOfConstraintSolving(refVariableId);
        if (unionTypeSymbol == null) {
            String visitKey = getVisitKey(refScope, refVariableId);
            if (!dto.visitedVariables.contains(visitKey)) {
//                unionTypeSymbol = addToVisitedAndSolve(
//                        visitKey,
//                        new ConstraintSolverDto(
//                                constraintSolverDto.constraintSolverDto,
//                                refScope.getConstraintsForVariable(refVariableId),
//                                constraintSolverDto.unionTypeSymbol
//                        ));
                unionTypeSymbol = solveAndAddToResultsIfNotAlreadySolved(
                        refScope,
                        refVariableId,
                        new ConstraintSolverDto(
                                dto.visitedVariables,
                                refScope.getConstraintsForVariable(refVariableId),
                                symbolFactory.createUnionTypeSymbol(new HashMap<String, ITypeSymbol>())
                        ));
            } else if (dto.notInIterativeMode) {
                //cannot solve this yet, have to solve other first, requires iterative approach
                dto.notInIterativeMode = false;
                throw new SelfReferenceInIntersectionException();
            } else {
                //In iterative mode, hence use the existing union type as argument type
                unionTypeSymbol = dto.unionTypeSymbol;
            }
        }

        ITypeSymbol overloadType = parameterConstraint.getVariableIdAst().getSymbol().getType();
        int promotionLevel = overloadResolver.getPromotionLevelFromTo(unionTypeSymbol, overloadType);
        if (overloadResolver.isSameOrSubType(promotionLevel)) {
            conversionDto = new ConversionDto(promotionLevel, 0);
        } else {
            //TODO castingDto = getImplicitCastingDto();
            conversionDto = null;
        }
        return conversionDto;
    }


    private ConversionDto getCastingDtoInAlwaysCastingMode(
            ConstraintSolverDto dto, RefTypeConstraint parameterConstraint) {
        //TODO implement always casting mode
        return null;
    }

    public OverloadDto getMostSpecificApplicableOverload(List<OverloadDto> overloadDtos) throws AmbiguousCallException {

        List<OverloadDto> ambiguousOverloadDtos = new ArrayList<>();

        OverloadDto mostSpecificMethodDto = overloadDtos.get(0);

        int overloadDtosSize = overloadDtos.size();
        for (int i = 1; i < overloadDtosSize; ++i) {
            OverloadDto overloadDto = overloadDtos.get(i);
            if (isSecondBetter(mostSpecificMethodDto, overloadDto)) {
                mostSpecificMethodDto = overloadDto;
                if (ambiguousOverloadDtos.size() > 0) {
                    ambiguousOverloadDtos = new ArrayList<>();
                }
            } else if (isSecondEqual(mostSpecificMethodDto, overloadDto)) {
                ambiguousOverloadDtos.add(overloadDto);
            }
        }
        if (!ambiguousOverloadDtos.isEmpty()) {
            ambiguousOverloadDtos.add(mostSpecificMethodDto);
            throw new AmbiguousCallException(ambiguousOverloadDtos);
        }

        return mostSpecificMethodDto;
    }

    private boolean isSecondBetter(OverloadDto mostSpecificMethodDto, OverloadDto methodDto) {

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

    private boolean isSecondEqual(OverloadDto mostSpecificMethodDto, OverloadDto methodDto) {
        return mostSpecificMethodDto.parametersNeedImplicitConversion.size() == methodDto
                .parametersNeedImplicitConversion.size()
                && mostSpecificMethodDto.parameterPromotedCount == methodDto.parameterPromotedCount
                && mostSpecificMethodDto.promotionsTotal == methodDto.promotionsTotal;
    }

    private void addConversionsToAstIfNecessary(OverloadDto dto) {
        for (ConversionDto parameterPromotionDto : dto.parametersNeedImplicitConversion) {
            //TODO insert casting
//            astHelper.prependImplicitCasting(parameterPromotionDto);
        }

        if (dto.parametersNeedExplicitConversion != null) {

            for (ConversionDto parameterPromotionDto : dto.parametersNeedExplicitConversion) {
                //TODO insert casting
//            astHelper.prependExplicitConversion(parameterPromotionDto);
            }
        }
    }

}
