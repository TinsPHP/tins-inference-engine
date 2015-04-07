/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.symbols.IFunctionTypeSymbol;

import java.io.Serializable;
import java.util.List;

/**
 * Represents meta-data of an overload, e.g. method overload etc.
 */
public class OverloadRankingDto implements Serializable
{

    public IFunctionTypeSymbol overload;

    /**
     * Count which tells how many parameters require up casts.
     * <p/>
     * An up cast is happening when for instance Exception is required and ErrorException provided (this corresponds
     * to one promotion level)
     */
    public int parameterUpCastCount;

    /**
     * Summation of promotion levels.
     */
    public int upCastsTotal;

    /**
     * All the parameters which need an implicit conversion
     */
    public List<ConversionDto> parametersNeedImplicitConversion;

    /**
     * All the parameters which need an explicit conversion
     */
    public List<ConversionDto> parametersNeedExplicitConversion;

    public boolean isPartialApplication;

    public OverloadRankingDto(
            IFunctionTypeSymbol theOverload,
            int howManyParameterWerePromoted,
            int thePromotionsInTotal,
            List<ConversionDto> theseParametersNeedImplicitConversion,
            boolean isItPartialApplication) {
        overload = theOverload;
        parameterUpCastCount = howManyParameterWerePromoted;
        upCastsTotal = thePromotionsInTotal;
        parametersNeedImplicitConversion = theseParametersNeedImplicitConversion;
        isPartialApplication = isItPartialApplication;
    }
}
