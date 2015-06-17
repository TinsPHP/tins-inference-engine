/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.symbols.ITypeSymbol;

import java.util.List;

/**
 * Provides methods to determine a most specific overload.
 */
public interface IMostSpecificOverloadDecider
{
    OverloadRankingDto inNormalMode(
            WorklistDto worklistDto, List<OverloadRankingDto> applicableOverloads, List<ITypeSymbol> argumentTypes);

    //Warning! start code duplication, more or less the same as in inNormalMode
    List<OverloadRankingDto> inSoftTypingMode(
            WorklistDto worklistDto, List<OverloadRankingDto> applicableOverloads);
}
