/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import java.util.List;

public class BindingMatcherDto
{
    public String variableId;
    public String typeVariable;
    public List<String> lowerBounds;
    public List<String> upperBounds;
    public boolean hasFixedType;

    BindingMatcherDto(
            String theVariable,
            String theTypeVariable,
            List<String> theLowerBounds,
            List<String> theUpperBounds,
            boolean hasItAFixedType) {
        variableId = theVariable;
        typeVariable = theTypeVariable;
        lowerBounds = theLowerBounds;
        upperBounds = theUpperBounds;
        hasFixedType = hasItAFixedType;
    }

    //Warning! start code duplication - same as in tins-symbols
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(variableId).append(":").append(typeVariable)
                .append("<")
                .append(lowerBounds != null ? lowerBounds.toString() : "[]")
                .append(",")
                .append(upperBounds != null ? upperBounds.toString() : "[]")
                .append(">");
        if (hasFixedType) {
            sb.append("#");
        }
        return sb.toString();
    }
    //Warning! end code duplication - same as in tins-symbols
}
