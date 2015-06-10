/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.utils.Pair;

import java.util.List;

public class OverloadRankingDto
{
    public IFunctionType overload;
    public IOverloadBindings bindings;
    public boolean hasNarrowedArguments;
    public int numberOfImplicitConversions;
    public int numberOfTypeParameter;
    public int mostGeneralLowerCount;
    public int mostSpecificUpperCount;
    public List<Pair<IUnionTypeSymbol, IIntersectionTypeSymbol>> bounds;

    public OverloadRankingDto(
            IFunctionType theOverload,
            IOverloadBindings theBindings,
            int implicitConversionsCount,
            boolean narrowedArguments) {
        overload = theOverload;
        bindings = theBindings;
        numberOfImplicitConversions = implicitConversionsCount;
        hasNarrowedArguments = narrowedArguments;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[hasNarrowed: ").append(hasNarrowedArguments)
                .append(", impl: ").append(numberOfImplicitConversions)
                .append(", low: ").append(mostGeneralLowerCount)
                .append(", up: ").append(mostSpecificUpperCount)
                .append("]");
        return stringBuilder.toString();
    }
}
