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

import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents meta-data of an overload, e.g. method overload etc.
 */
public class OverloadRankingDto implements Serializable
{
    public IOverloadBindings binding;

    public IFunctionType overload;

    /**
     * Count which tells how many parameters require an up cast.
     * <p/>
     * An up cast is happening when for instance Exception is required and ErrorException provided (this corresponds
     * to one up cast level)
     */
    public int parameterWithUpCastCount;

    /**
     * Summation of the up casts levels over all parameters.
     */
    public int upCastsTotal;

    /**
     * Count which tells how many parameters had at least one lower bound
     */
    public int parameterWithoutFixedTypeCount;

    /**
     * All the parameters which need an implicit conversion
     */
    public List<ConversionDto> parametersNeedImplicitConversion;

    /**
     * All the parameters which need an explicit conversion
     */
    public List<ConversionDto> parametersNeedExplicitConversion;

    public OverloadRankingDto() {
        parametersNeedImplicitConversion = new ArrayList<>(5);
        parametersNeedExplicitConversion = new ArrayList<>(5);
    }
}
