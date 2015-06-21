/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.utils.ERelation;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.common.utils.TypeHelperDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static ch.tsphp.tinsphp.common.utils.Pair.pair;

public class MostSpecificOverloadDecider implements IMostSpecificOverloadDecider
{

    private final ISymbolFactory symbolFactory;
    private final ITypeHelper typeHelper;
    private final ITypeSymbol mixedTypeSymbol;

    public MostSpecificOverloadDecider(ISymbolFactory theSymbolFactory, ITypeHelper theTypeHelper) {
        symbolFactory = theSymbolFactory;
        typeHelper = theTypeHelper;
        mixedTypeSymbol = symbolFactory.getMixedTypeSymbol();
    }

    @Override
    public OverloadRankingDto inNormalMode(
            WorklistDto worklistDto, List<OverloadRankingDto> applicableOverloads, List<ITypeSymbol> argumentTypes) {

        List<OverloadRankingDto> overloadRankingDtos = preFilterOverloads(applicableOverloads);
        OverloadRankingDto overloadRankingDto = overloadRankingDtos.get(0);
        if (overloadRankingDtos.size() > 1) {
            overloadRankingDtos = fixOverloads(worklistDto, overloadRankingDtos);
            overloadRankingDtos = filterOverloads(overloadRankingDtos);
            overloadRankingDto = overloadRankingDtos.get(0);
            if (overloadRankingDtos.size() > 1) {

                int numberOfParameters = overloadRankingDto.overload.getParameters().size();
                List<Pair<ITypeSymbol, ITypeSymbol>> bounds = getParameterBounds(overloadRankingDtos,
                        numberOfParameters);

                overloadRankingDtos = getMostSpecificApplicableOverload(overloadRankingDtos, bounds);
                overloadRankingDto = overloadRankingDtos.get(0);
                if (overloadRankingDtos.size() != 1 && !worklistDto.isInIterativeMode) {
                    throw new AmbiguousOverloadException(argumentTypes, overloadRankingDtos);
                }
            }
        }
        return overloadRankingDto;
    }

    //Warning! start code duplication, more or less the same as in inNormalMode
    @Override
    public List<OverloadRankingDto> inSoftTypingMode(
            WorklistDto worklistDto, List<OverloadRankingDto> applicableOverloads) {

        List<OverloadRankingDto> overloadRankingDtos = preFilterOverloads(applicableOverloads);
        if (overloadRankingDtos.size() > 1) {
            OverloadRankingDto overloadRankingDto = overloadRankingDtos.get(0);
            //comparing overloads with explicit conversions require a different ranking
            if (!overloadRankingDto.hasNarrowedArguments) {
                overloadRankingDtos = fixOverloads(worklistDto, overloadRankingDtos);
                int numberOfParameters = overloadRankingDto.overload.getParameters().size();
                List<Pair<ITypeSymbol, ITypeSymbol>> bounds = getParameterBounds(overloadRankingDtos,
                        numberOfParameters);
                overloadRankingDtos = getMostSpecificApplicableOverload(overloadRankingDtos, bounds);
            } else {
                //TODO TINS-537 - most specific overload in soft typing
            }
        }
        return overloadRankingDtos;
    }
    //Warning! end code duplication, more or less the same as in inNormalMode


    private List<OverloadRankingDto> preFilterOverloads(List<OverloadRankingDto> applicableOverloads) {
        int size = applicableOverloads.size();

        List<OverloadRankingDto> overloadRankingDtos = new ArrayList<>(size);
        boolean wereArgumentsNarrowed = true;
        for (int i = 0; i < size; ++i) {
            OverloadRankingDto dto = applicableOverloads.get(i);
            if (wereArgumentsNarrowed == dto.hasNarrowedArguments) {
                overloadRankingDtos.add(dto);
            } else if (wereArgumentsNarrowed) {
                wereArgumentsNarrowed = dto.hasNarrowedArguments;
                overloadRankingDtos = new ArrayList<>(size - i);
                overloadRankingDtos.add(dto);
            }
        }
        return overloadRankingDtos;
    }

    private List<OverloadRankingDto> filterOverloads(List<OverloadRankingDto> fixedOverloads) {
        int size = fixedOverloads.size();

        List<OverloadRankingDto> overloadRankingDtos = new ArrayList<>(size);

        int minNumberOfImplicitConversions = Integer.MAX_VALUE;
        boolean usesConvertibleTypes = true;

        for (int i = 0; i < size; ++i) {
            OverloadRankingDto dto = fixedOverloads.get(i);
            int numberOfImplicitConversions = (dto.implicitConversions == null ? 0 : dto.implicitConversions.size());
            if (usesConvertibleTypes == dto.usesConvertibleTypes
                    && numberOfImplicitConversions == minNumberOfImplicitConversions) {

                overloadRankingDtos.add(dto);

            } else if ((usesConvertibleTypes && !dto.usesConvertibleTypes)
                    || (usesConvertibleTypes == dto.usesConvertibleTypes
                    && numberOfImplicitConversions < minNumberOfImplicitConversions)) {
                usesConvertibleTypes = dto.usesConvertibleTypes;
                minNumberOfImplicitConversions = numberOfImplicitConversions;
                overloadRankingDtos = new ArrayList<>(size - i);
                overloadRankingDtos.add(dto);
            }
        }
        return overloadRankingDtos;
    }

    private List<OverloadRankingDto> fixOverloads(
            WorklistDto worklistDto, List<OverloadRankingDto> applicableOverloads) {
        List<OverloadRankingDto> overloadRankingDtos = new ArrayList<>(applicableOverloads.size());

        for (OverloadRankingDto dto : applicableOverloads) {
            OverloadRankingDto overloadRankingDto;
            if (dto.overload.wasSimplified()) {
                overloadRankingDto = fixOverload(dto);
            } else if (worklistDto.isInIterativeMode) {
                overloadRankingDto = fixOverloadInIterativeMode(dto);
            } else {
                throw new IllegalStateException("function " + dto.overload.getName() + " was not simplified "
                        + "and we are not in iterative mode.");
            }
            overloadRankingDto.bounds = new ArrayList<>();
            overloadRankingDtos.add(overloadRankingDto);
        }

        return overloadRankingDtos;
    }


    private OverloadRankingDto fixOverload(OverloadRankingDto dto) {
        OverloadRankingDto fixedDto = dto;

        IFunctionType overload = dto.overload;
        if (!overload.isFixed()) {
            Collection<String> nonFixedTypeParameters = new ArrayList<>(overload.getNonFixedTypeParameters());
            IFunctionType copyOverload = copyAndFixOverload(overload);

            fixedDto = new OverloadRankingDto(
                    copyOverload, dto.bindings, dto.implicitConversions, dto.runtimeChecks, dto.hasNarrowedArguments);
            fixedDto.numberOfTypeParameters = nonFixedTypeParameters.size();
        }
        fixedDto.usesConvertibleTypes = overload.hasConvertibleParameterTypes();
        return fixedDto;
    }

    private IFunctionType copyAndFixOverload(IFunctionType overload) {
        Collection<String> nonFixedTypeParameters = new ArrayList<>(overload.getNonFixedTypeParameters());
        IOverloadBindings overloadBindings = overload.getOverloadBindings();
        IOverloadBindings copyBindings = symbolFactory.createOverloadBindings(overloadBindings);
        IFunctionType copyOverload = symbolFactory.createFunctionType(
                overload.getName(), copyBindings, overload.getParameters());

        for (String nonFixedTypeParameter : nonFixedTypeParameters) {
            copyBindings.fixTypeParameter(nonFixedTypeParameter);
        }

        copyOverload.manuallySimplified(
                Collections.<String>emptySet(),
                overload.getNumberOfConvertibleApplications(),
                overload.hasConvertibleParameterTypes());
        return copyOverload;
    }

    private OverloadRankingDto fixOverloadInIterativeMode(OverloadRankingDto dto) {
        IOverloadBindings overloadBindings = symbolFactory.createOverloadBindings(dto.overload.getOverloadBindings());
        overloadBindings.fixTypeParameters();
        IFunctionType copyOverload = symbolFactory.createFunctionType(
                dto.overload.getName(), overloadBindings, dto.overload.getParameters());

        return new OverloadRankingDto(
                copyOverload, dto.bindings, dto.implicitConversions, dto.runtimeChecks, dto.hasNarrowedArguments);
    }


    private List<Pair<ITypeSymbol, ITypeSymbol>> getParameterBounds(List<OverloadRankingDto> fixedOverloads, int size) {

        List<Pair<ITypeSymbol, ITypeSymbol>> bounds = new ArrayList<>(size);

        for (int i = 0; i < size; ++i) {
            //the most general type is the most specific lower bound
            ITypeSymbol mostSpecificLowerBound = null;
            ITypeSymbol mostSpecificUpperBound = null;

            for (OverloadRankingDto dto : fixedOverloads) {
                IOverloadBindings bindings = dto.overload.getOverloadBindings();
                IVariable parameter = dto.overload.getParameters().get(i);
                String parameterTypeVariable = bindings.getTypeVariable(parameter.getAbsoluteName());
                IUnionTypeSymbol lowerBound = null;
                IIntersectionTypeSymbol upperBound = null;
                if (bindings.hasLowerTypeBounds(parameterTypeVariable)) {
                    lowerBound = bindings.getLowerTypeBounds(parameterTypeVariable);
                    if (mostSpecificLowerBound != null) {
                        TypeHelperDto result = typeHelper.isFirstSameOrParentTypeOfSecond(
                                lowerBound, mostSpecificLowerBound);
                        ERelation relation = result.relation;
                        if (relation == ERelation.HAS_COERCIVE_RELATION) {
                            TypeHelperDto result2 = typeHelper.isFirstSameOrParentTypeOfSecond(
                                    mostSpecificLowerBound, lowerBound, false);
                            //we prefer a non coercive relation, hence if the current lower is a parent type of the
                            // new (without coercive subtyping) then it is more specific
                            if (result2.relation == ERelation.HAS_RELATION) {
                                relation = ERelation.HAS_NO_RELATION;
                            }
                        }
                        if (relation != ERelation.HAS_NO_RELATION) {
                            mostSpecificLowerBound = lowerBound;
                        }
                    } else {
                        mostSpecificLowerBound = lowerBound;
                    }
                }
                if (bindings.hasUpperTypeBounds(parameterTypeVariable)) {
                    upperBound = bindings.getUpperTypeBounds(parameterTypeVariable);
                    if (mostSpecificUpperBound != null) {
                        TypeHelperDto result = typeHelper.isFirstSameOrSubTypeOfSecond(
                                upperBound, mostSpecificUpperBound);
                        ERelation relation = result.relation;
                        if (relation == ERelation.HAS_COERCIVE_RELATION) {
                            TypeHelperDto result2 = typeHelper.isFirstSameOrSubTypeOfSecond(
                                    mostSpecificUpperBound, upperBound, false);
                            //we prefer a non coercive relation, hence if the current upper is a subtype of the
                            // new (without coercive subtyping) then it is more specific
                            if (result2.relation == ERelation.HAS_RELATION) {
                                relation = ERelation.HAS_NO_RELATION;
                            }
                        }
                        if (relation != ERelation.HAS_NO_RELATION) {
                            mostSpecificUpperBound = upperBound;
                        }
                    } else {
                        mostSpecificUpperBound = upperBound;
                    }
                }
                dto.bounds.add(pair(lowerBound, upperBound));
            }
            bounds.add(pair(mostSpecificLowerBound, mostSpecificUpperBound));
        }
        return bounds;
    }

    private List<OverloadRankingDto> getMostSpecificApplicableOverload(
            List<OverloadRankingDto> overloadBindingsList,
            List<Pair<ITypeSymbol, ITypeSymbol>> boundsList) {
        List<OverloadRankingDto> mostSpecificOverloads = new ArrayList<>(3);

        int numberOfParameters = boundsList.size();
        OverloadRankingDto mostSpecificDto = overloadBindingsList.get(0);

        int overloadSize = overloadBindingsList.size();
        for (int i = 0; i < overloadSize; ++i) {
            OverloadRankingDto dto = overloadBindingsList.get(i);
            for (int j = 0; j < numberOfParameters; ++j) {
                Pair<ITypeSymbol, ITypeSymbol> mostSpecificBound = boundsList.get(j);
                Pair<IUnionTypeSymbol, IIntersectionTypeSymbol> bound = dto.bounds.get(j);
                if (isMostGeneralLowerBound(bound.first, mostSpecificBound.first)) {
                    ++dto.mostGeneralLowerCount;
                }
                if (isMostSpecificUpperBound(bound.second, mostSpecificBound.second)) {
                    ++dto.mostSpecificUpperCount;
                }
            }

            int diff = compare(dto, mostSpecificDto);
            if (diff > 0) {
                if (mostSpecificOverloads.size() > 0) {
                    mostSpecificOverloads = new ArrayList<>(3);
                    mostSpecificOverloads.add(dto);
                }
                mostSpecificDto = dto;
                if (isMostSpecific(dto, numberOfParameters)) {
                    break;
                }
            } else if (diff == 0) {
                mostSpecificOverloads.add(dto);
                if (i == 0 && isMostSpecific(dto, numberOfParameters)) {
                    break;
                }
            }
        }

        return mostSpecificOverloads;
    }

    private boolean isMostGeneralLowerBound(ITypeSymbol lowerBound, ITypeSymbol mostGeneralLowerBound) {
        if (lowerBound == null) {
            return mostGeneralLowerBound == null;
        } else {
            return typeHelper.areSame(lowerBound, mostGeneralLowerBound);
        }
    }

    private boolean isMostSpecificUpperBound(ITypeSymbol upperBound, ITypeSymbol mostSpecificUpperBound) {
        if (upperBound == null) {
            return mostSpecificUpperBound == null
                    || typeHelper.areSame(mixedTypeSymbol, mostSpecificUpperBound);
        } else {
            return typeHelper.areSame(upperBound, mostSpecificUpperBound);
        }
    }

    private boolean isMostSpecific(OverloadRankingDto dto, int numberOfParameters) {
        return dto.mostGeneralLowerCount == numberOfParameters
                && dto.mostSpecificUpperCount == numberOfParameters;
    }

    /**
     * Returns a number less than 0 if dto is less specific than the currentMostSpecific,
     * 0 if they are equal and a bigger number if it is more specific.
     */
    private int compare(OverloadRankingDto dto, OverloadRankingDto currentMostSpecific) {
        int diff = dto.mostSpecificUpperCount - currentMostSpecific.mostSpecificUpperCount;
        if (diff == 0) {
            diff = dto.mostGeneralLowerCount - currentMostSpecific.mostGeneralLowerCount;
            if (diff == 0) {
                diff = currentMostSpecific.numberOfTypeParameters - dto.numberOfTypeParameters;
            }
        }
        return diff;
    }
}
