/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.utils.Pair;

import java.util.List;
import java.util.Map;

public class OverloadRankingDto
{
    public IFunctionType overload;
    public IBindingCollection bindings;
    public boolean hasNarrowedArguments;
    public boolean usesConvertibleTypes;
    public int numberOfTypeParameters;
    public int mostGeneralLowerCount;
    public int mostSpecificUpperCount;
    public List<Pair<IUnionTypeSymbol, IIntersectionTypeSymbol>> bounds;
    public Map<Integer, Pair<ITypeSymbol, ITypeSymbol>> implicitConversions;
    public Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> runtimeChecks;
    public Map<Integer, Map<String, ITypeVariableReference>> helperVariableMapping;
    public boolean hasChanged;

    public OverloadRankingDto(
            IFunctionType theOverload,
            IBindingCollection theBindings,
            Map<Integer, Pair<ITypeSymbol, ITypeSymbol>> theImplicitConversions,
            Map<Integer, Pair<ITypeSymbol, List<ITypeSymbol>>> theRuntimeChecks,
            Map<Integer, Map<String, ITypeVariableReference>> theHelperVariableMapping,
            boolean narrowedArguments,
            boolean hasItChanged) {
        overload = theOverload;
        bindings = theBindings;
        implicitConversions = theImplicitConversions;
        runtimeChecks = theRuntimeChecks;
        hasNarrowedArguments = narrowedArguments;
        helperVariableMapping = theHelperVariableMapping;
        hasChanged = hasItChanged;
    }

    public OverloadRankingDto(OverloadRankingDto dto, IFunctionType copyOverload) {
        this(copyOverload,
                dto.bindings,
                dto.implicitConversions,
                dto.runtimeChecks,
                dto.helperVariableMapping,
                dto.hasNarrowedArguments,
                dto.hasChanged);

    }

    public OverloadRankingDto(AggregateBindingDto dto) {
        this(dto.overload,
                dto.bindings,
                dto.implicitConversions,
                null,
                dto.helperVariableMapping,
                dto.hasNarrowedArguments,
                dto.hasChanged);
    }

    public String toString() {
        return "[hasNarrowed: " + hasNarrowedArguments
                + ", up: " + mostSpecificUpperCount
                + ", low: " + mostGeneralLowerCount
                + ", tp: " + numberOfTypeParameters
                + ", impl: " + (implicitConversions == null ? 0 : implicitConversions.size())
                + ", conv: " + usesConvertibleTypes
                + ", checks: " + (runtimeChecks == null ? 0 : runtimeChecks.size())
                + "]";
    }
}
