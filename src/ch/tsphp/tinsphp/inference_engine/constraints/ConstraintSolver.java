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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
            Map<String, Integer> visitedVariables = new HashMap<>();
            Set<String> variablesToRevisit = new HashSet<>();
            String variableId = constraintsEntry.getKey();
            ScopeVariableDto variableToVisit = new ScopeVariableDto(currentScope, variableId);
            IUnionTypeSymbol unionTypeSymbol = getOrInitialiseVariableUnionTypeSymbol(variableToVisit);
            if (!unionTypeSymbol.isReadyForEval()) {
                ConstraintSolverDto dto = new ConstraintSolverDto(
                        visitedVariables,
                        variablesToRevisit,
                        variableToVisit,
                        constraintsEntry.getValue(),
                        unionTypeSymbol
                );
                addToVisitedAndSolve(variableToVisit, dto);
                unionTypeSymbol.seal();
            }
        }
    }

    private IUnionTypeSymbol getOrInitialiseVariableUnionTypeSymbol(ScopeVariableDto variable) {
        IUnionTypeSymbol unionTypeSymbol = variable.scope.getResultOfConstraintSolving(variable.variableId);
        if (unionTypeSymbol == null) {
            unionTypeSymbol = symbolFactory.createUnionTypeSymbol(new HashMap<String, ITypeSymbol>());
            variable.scope.setResultOfConstraintSolving(variable.variableId, unionTypeSymbol);
        }
        return unionTypeSymbol;
    }

    private IUnionTypeSymbol addToVisitedAndSolve(ScopeVariableDto variableToVisit, ConstraintSolverDto dto) {
        dto.visitedVariables.put(getVisitKey(variableToVisit), dto.visitedVariables.size());
        List<IConstraint> iterativeConstraints = null;
        Iterator<IConstraint> iterator = dto.constraints.listIterator();

        while (iterator.hasNext()) {
            IConstraint constraint = iterator.next();
            solveConstraint(dto, iterator, constraint);
            if (hasCircularReferenceAndIsBackAtTheBeginning(dto, variableToVisit)) {
                dto.hasNotCircularReference = true;
                if (iterativeConstraints == null) {
                    iterativeConstraints = new ArrayList<>();
                }
                iterativeConstraints.add(constraint);
            }
        }

        // might be that it is no longer the circular reference starting point, consider
        // $a -> $b -> $c -> $d -> $c    //$c will be the current starting point of the circular reference
        //        | -> $a                //$a will be the new starting point of the circular reference
        if (hasIterativeConstraintsAndIsStartingPoint(iterativeConstraints, dto)) {
            solveIterativeConstraints(dto, iterativeConstraints);
        }
        return dto.unionTypeSymbol;
    }

    private boolean hasIterativeConstraintsAndIsStartingPoint(
            List<IConstraint> iterativeConstraints, ConstraintSolverDto dto) {
        return iterativeConstraints != null
                && (dto.circularRefVariable == null
                || !isNotSelfReference(dto.circularRefVariable, dto.currentVariable));
    }

    private boolean hasCircularReferenceAndIsBackAtTheBeginning(
            ConstraintSolverDto dto, ScopeVariableDto variableToVisit) {
        return dto.circularRefVariable != null && !isNotSelfReference(dto.circularRefVariable, variableToVisit);
    }

    private boolean isNotSelfReference(ScopeVariableDto circularRefVariable, ScopeVariableDto variableToVisit) {
        return circularRefVariable.scope != variableToVisit.scope
                || !circularRefVariable.variableId.equals(variableToVisit.variableId);
    }

    private String getVisitKey(ScopeVariableDto currentVariable) {
        return currentVariable.scope.getScopeName() + currentVariable.variableId;
    }

    private void solveIterativeConstraints(ConstraintSolverDto dto, List<IConstraint> constraints) {
        dto.notInIterativeMode = false;

        //TODO recursive generic functions
        //Sure that it terminates? nope I do not think so, need to add a guard, fall back to mixed after 10 attempts
        // or similar

        dto.hasUnionChanged = true;
        while (dto.hasUnionChanged) {

            resetIterativeInformation(dto);
            Iterator<IConstraint> iterator = constraints.listIterator();
            while (iterator.hasNext()) {
                IConstraint constraint = iterator.next();
                solveConstraint(dto, iterator, constraint);
            }
        }

        if (isNotBeginningVariable(dto, dto.currentVariable)) {
            IScope scope = dto.currentVariable.scope;
            IUnionTypeSymbol unionTypeSymbol = scope.getResultOfConstraintSolving(dto.currentVariable.variableId);
            dto.unionTypeSymbol.merge(unionTypeSymbol);
        }

        resetIterativeInformation(dto);
        dto.circularRefVariable = null;
        dto.notInIterativeMode = true;
    }

    private boolean isNotBeginningVariable(ConstraintSolverDto dto, ScopeVariableDto currentVariable) {
        return dto.visitedVariables.get(getVisitKey(currentVariable)) != 0;
    }

    private void resetIterativeInformation(ConstraintSolverDto dto) {
        for (String visitKey : dto.revisitVariables) {
            dto.visitedVariables.remove(visitKey);
        }
        dto.revisitVariables.clear();
        dto.hasUnionChanged = false;
        dto.hasNotCircularReference = true;
    }

    private void solveConstraint(ConstraintSolverDto dto, Iterator<IConstraint> iterator, IConstraint constraint) {
        if (constraint instanceof TypeConstraint) {
            resolveTypeConstraint(dto, iterator, (TypeConstraint) constraint);
        } else if (constraint instanceof RefConstraint) {
            resolveReferenceConstraint(dto, iterator, (RefConstraint) constraint);
        } else if (constraint instanceof IntersectionConstraint) {
            resolveIntersectionConstraint(dto, iterator, (IntersectionConstraint) constraint);
        }
    }

    private void resolveTypeConstraint(ConstraintSolverDto dto, Iterator<IConstraint> iterator,
            TypeConstraint constraint) {
        ITypeSymbol typeSymbol = constraint.getTypeSymbol();
        addToDtoUnionAndCurrentVariable(dto, typeSymbol);
        iterator.remove();
    }

    private void resolveReferenceConstraint(
            ConstraintSolverDto dto, Iterator<IConstraint> iterator, RefConstraint refConstraint) {
        ScopeVariableDto refVariable = refConstraint.getScopeVariableDto();
        if (isNotSelfReference(dto.currentVariable, refVariable)) {
            IUnionTypeSymbol unionTypeSymbol = getOrInitialiseVariableUnionTypeSymbol(refVariable);
            if (!unionTypeSymbol.isReadyForEval()) {
                String visitKey = getVisitKey(refVariable);
                if (!dto.visitedVariables.containsKey(visitKey)) {
                    ConstraintSolverDto refDto = new ConstraintSolverDto(
                            dto,
                            refVariable,
                            refVariable.scope.getConstraintsForVariable(refVariable.variableId),
                            dto.unionTypeSymbol
                    );
                    addToVisitedAndSolve(refVariable, refDto);
                    if (refDto.hasNotCircularReference) {
                        unionTypeSymbol.seal();
                        iterator.remove();
                    } else {
                        propagateCircularReferenceFromTo(refDto, dto);
                    }
                } else {
                    reportCircularReference(dto, refVariable);
                }
            } else {
                iterator.remove();
            }
            mergeWithDtoAndCurrentVariable(dto, unionTypeSymbol);
        } else {
            iterator.remove();
        }
    }

    private void mergeWithDtoAndCurrentVariable(ConstraintSolverDto dto, IUnionTypeSymbol unionTypeSymbol) {
        IScope scope = dto.currentVariable.scope;
        scope.getResultOfConstraintSolving(dto.currentVariable.variableId).merge(unionTypeSymbol);
        boolean hasChanged = dto.unionTypeSymbol.merge(unionTypeSymbol);
        dto.hasUnionChanged = dto.hasUnionChanged || hasChanged;
    }

    private void propagateCircularReferenceFromTo(ConstraintSolverDto constraintSolverDto, ConstraintSolverDto dto) {
        dto.hasNotCircularReference = constraintSolverDto.hasNotCircularReference;
        dto.hasUnionChanged = dto.hasUnionChanged || constraintSolverDto.hasUnionChanged;
        setCircularRefVariable(dto, constraintSolverDto.circularRefVariable);
    }

    private void setCircularRefVariable(ConstraintSolverDto dto, ScopeVariableDto circularRefVariable) {
        if (dto.circularRefVariable == null) {
            dto.circularRefVariable = circularRefVariable;
        } else if (wasVisitedLaterThanCurrentCircularRefVariable(dto, circularRefVariable)) {
            addToRevisitVariable(dto, circularRefVariable);
        } else if (isNotSelfReference(dto.circularRefVariable, circularRefVariable)) {
            //exchange circular reference variable with one which was visited earlier, revisit the old one
            addToRevisitVariable(dto, dto.circularRefVariable);
            dto.circularRefVariable = circularRefVariable;
        }
    }

    private boolean wasVisitedLaterThanCurrentCircularRefVariable(
            ConstraintSolverDto dto, ScopeVariableDto circularRefVariable) {
        Map<String, Integer> visitedVariables = dto.visitedVariables;
        Integer currentIndex = visitedVariables.get(getVisitKey(dto.circularRefVariable));
        Integer newDetectedCircularRefIndex = visitedVariables.get(getVisitKey(circularRefVariable));
        return currentIndex < newDetectedCircularRefIndex;
    }

    private void reportCircularReference(ConstraintSolverDto dto, ScopeVariableDto refVariableId) {
        if (isNotStartingPointNorSelfRefNorDefinedEarlierThanCurrentCircularRefVariable(dto, refVariableId)) {
            addToRevisitVariable(dto, dto.currentVariable);
        }
        dto.hasNotCircularReference = false;
        setCircularRefVariable(dto, refVariableId);
    }

    private boolean isNotStartingPointNorSelfRefNorDefinedEarlierThanCurrentCircularRefVariable(
            ConstraintSolverDto dto, ScopeVariableDto refVariableId) {
        // The variable which detected the circular reference needs to be revisited during iterative mode unless
        // - the variable is already the starting point of the circular reference, then we do not need to revisit it.
        //   Adding the starting point to the revisit list would cause endless loop. An example:
        //   $a -> $b -> $a // circle one
        //   $a -> $b //we have already seen $b, hence a circle as well, yet we do not need to revisit $a otherwise we
        //   would endlessly do $a -> $b -> $a in iterative mode
        //
        // - the variable detected a circle with itself (self-reference). There is no need to revisit a self-reference
        //   $a -> $b -> $b (no need to add $b, $b does not add any information to $b)
        //   Actually it would be wrong since it would close the union during the revisit and hence throw an exception
        //   afterwards (cannot add a type to a closed union)
        //
        // - the variable was visited earlier than the starting point of the current circular reference. For instance,
        //   $a -> $b -> $c -> $d -> $c    //$c will be the current starting point of the circular reference
        //          | -> $a                //$a will be the new
        //
        //   Similar to self-references it will lead to exceptions since unions were closed too early.

        return isNotBeginningVariable(dto, dto.currentVariable)
                && isNotSelfReference(dto.currentVariable, refVariableId)
                && (dto.circularRefVariable == null
                || wasVisitedLaterThanCurrentCircularRefVariable(dto, dto.currentVariable));
    }

    private void addToRevisitVariable(ConstraintSolverDto dto, ScopeVariableDto variable) {
        dto.revisitVariables.add(getVisitKey(variable));
    }

    private boolean inIterativeMode(ConstraintSolverDto dto) {
        return !dto.notInIterativeMode;
    }

    private void resolveIntersectionConstraint(ConstraintSolverDto dto, Iterator<IConstraint> iterator,
            IntersectionConstraint intersectionConstraint) {
        List<OverloadRankingDto> applicableOverloads = getApplicableOverloads(dto, intersectionConstraint);
        if (!applicableOverloads.isEmpty()) {
            OverloadRankingDto overloadRankingDto;
            try {
                overloadRankingDto = getMostSpecificApplicableOverload(applicableOverloads);
            } catch (AmbiguousCallException ex) {
                //TODO if several calls are valid due to data polymorphism, then we need to take
                // the union of all result Types. For Instance float V array + array => once float once array both
                // with promotion level = 1 (one cast from float V array to float and one from float V array to array)
                // another example without casting is float V array + float V array => float V array
                overloadRankingDto = ex.getAmbiguousOverloads().get(0);
            }

            if (overloadRankingDto != null) {
                ITypeSymbol returnTypeSymbol = overloadRankingDto.overloadDto.returnTypeSymbol;
                addToDtoUnionAndCurrentVariable(dto, returnTypeSymbol);
                if (dto.hasNotCircularReference) {
                    iterator.remove();
                }
            }
        }
    }

    private void addToDtoUnionAndCurrentVariable(ConstraintSolverDto dto, ITypeSymbol typeSymbol) {
        IScope scope = dto.currentVariable.scope;
        scope.getResultOfConstraintSolving(dto.currentVariable.variableId).addTypeSymbol(typeSymbol);
        boolean hasChanged = dto.unionTypeSymbol.addTypeSymbol(typeSymbol);
        dto.hasUnionChanged = dto.hasUnionChanged || hasChanged;
    }

    private List<OverloadRankingDto> getApplicableOverloads(
            ConstraintSolverDto dto, IntersectionConstraint intersectionConstraint) {
        List<OverloadRankingDto> applicableOverloads = new ArrayList<>();
        List<IUnionTypeSymbol> refVariableTypes = resolveRefVariableTypes(dto, intersectionConstraint);
        if (dto.hasNotCircularReference || inIterativeMode(dto)) {
            for (OverloadDto overload : intersectionConstraint.getOverloads()) {
                OverloadRankingDto overloadRankingDto = getApplicableOverload(refVariableTypes, overload);
                if (overloadRankingDto != null) {
                    applicableOverloads.add(overloadRankingDto);
                    if (isOverloadWithoutPromotionNorConversion(overloadRankingDto)) {
                        break;
                    }
                }
            }
        }
        return applicableOverloads;
    }

    private List<IUnionTypeSymbol> resolveRefVariableTypes(
            ConstraintSolverDto dto, IntersectionConstraint intersectionConstraint) {
        List<IUnionTypeSymbol> refVariableTypes = new ArrayList<>();
        for (RefConstraint refConstraint : intersectionConstraint.getVariables()) {
            ScopeVariableDto refVariable = refConstraint.getScopeVariableDto();
            IUnionTypeSymbol unionTypeSymbol = getOrInitialiseVariableUnionTypeSymbol(refVariable);
            if (!unionTypeSymbol.isReadyForEval()) {
                String visitKey = getVisitKey(refVariable);
                if (!dto.visitedVariables.containsKey(visitKey)) {
                    Map<String, ITypeSymbol> predefinedTypes = new HashMap<>();
                    if (inIterativeMode(dto)) {
                        predefinedTypes.putAll(unionTypeSymbol.getTypeSymbols());
//                        predefinedTypes.putAll(dto.unionTypeSymbol.getTypeSymbols());
                    }
                    IUnionTypeSymbol accumulatorUnion = symbolFactory.createUnionTypeSymbol(predefinedTypes);
                    ConstraintSolverDto refDto = new ConstraintSolverDto(
                            dto,
                            refVariable,
                            refVariable.scope.getConstraintsForVariable(refVariable.variableId),
                            accumulatorUnion
                    );
                    addToVisitedAndSolve(refVariable, refDto);
                    if (refDto.hasNotCircularReference) {
                        unionTypeSymbol.seal();
                        refVariableTypes.add(accumulatorUnion);
                    } else {
                        propagateCircularReferenceFromTo(refDto, dto);
                        if (dto.notInIterativeMode) {
                            break;
                        }
                        refVariableTypes.add(accumulatorUnion);
                    }
                } else {

                    reportCircularReference(dto, refVariable);

                    if (inIterativeMode(dto)) {
//                        if (isNotSelfReference(dto.currentVariable, refVariable)) {
//                            unionTypeSymbol.merge(dto.unionTypeSymbol);
//                        }
                        // In iterative mode, we can use the union type as it is now
                        // (will be re-iterate if it causes a change)
                        refVariableTypes.add(unionTypeSymbol);
                    }
                }
            } else {
                refVariableTypes.add(unionTypeSymbol);
            }
        }
        return refVariableTypes;
    }

    private boolean isOverloadWithoutPromotionNorConversion(OverloadRankingDto dto) {
        return dto.parameterPromotedCount == 0 && dto.parametersNeedImplicitConversion.size() == 0
                && (dto.parametersNeedExplicitConversion == null || dto.parametersNeedExplicitConversion.size() == 0);
    }

    private OverloadRankingDto getApplicableOverload(
            List<IUnionTypeSymbol> refVariableTypes, OverloadDto overloadDto) {
        List<List<IConstraint>> parametersConstraints = overloadDto.parametersConstraints;
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
                    overloadDto, promotionParameterCount, promotionTotalCount, parametersNeedImplicitConversion);
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
}
