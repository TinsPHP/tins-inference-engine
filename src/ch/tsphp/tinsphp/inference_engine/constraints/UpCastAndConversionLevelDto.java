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

import java.io.Serializable;

/**
 * Represents a tuple composed of the promotion level and the casting level.
 * <p/>
 * The casting level tells how many castings (implicit and explicit) have to be applied until one type is casted to
 * another.
 */
public class UpCastAndConversionLevelDto implements Serializable
{

    public int upCastLevel;
    public int conversionLevel;

    public UpCastAndConversionLevelDto(int thePromotionLevel, int theConversionLevel) {
        upCastLevel = thePromotionLevel;
        conversionLevel = theConversionLevel;
    }
}
