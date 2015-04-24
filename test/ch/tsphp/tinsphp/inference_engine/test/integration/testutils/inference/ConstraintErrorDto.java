/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

public class ConstraintErrorDto
{
    public String key;
    public String identifier;
    public int line;
    public int position;

    public ConstraintErrorDto(String theKey, String theIdentifier, int theLine, int thePosition) {
        key = theKey;
        identifier = theIdentifier;
        line = theLine;
        position = thePosition;
    }

}
