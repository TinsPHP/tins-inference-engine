/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

public class FunctionMatcherDto
{
    public final String name;
    public final int numberOfNonOptionalParameters;
    public final BindingMatcherDto[] bindings;

    public FunctionMatcherDto(String theName, int theNumberOfNonOptionalParameters, BindingMatcherDto[] theBindings) {
        this.bindings = theBindings;
        this.name = theName;
        this.numberOfNonOptionalParameters = theNumberOfNonOptionalParameters;
    }
}
