/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.symbols.ITypeSymbol;

import java.util.List;

public class AmbiguousOverloadException extends RuntimeException
{

    private final List<OverloadRankingDto> overloadRankingDtos;
    private final List<ITypeSymbol> argumentTypes;

    public AmbiguousOverloadException(
            List<ITypeSymbol> theArgumentTypes, List<OverloadRankingDto> theOverloadRankingDtos) {
        argumentTypes = theArgumentTypes;
        overloadRankingDtos = theOverloadRankingDtos;
    }

    public List<ITypeSymbol> getArgumentTypes() {
        return argumentTypes;
    }

    public List<OverloadRankingDto> getOverloadRankingDtos() {
        return overloadRankingDtos;
    }
}
