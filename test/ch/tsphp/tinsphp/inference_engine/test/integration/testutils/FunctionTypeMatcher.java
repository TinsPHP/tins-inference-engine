/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class FunctionTypeMatcher extends BaseMatcher<IFunctionType>
{
    private final FunctionMatcherDto dto;
    private final OverloadBindingsMatcher overloadBindingsMatcher;

    public static Matcher<? super IFunctionType> isFunctionType(FunctionMatcherDto dto) {
        return new FunctionTypeMatcher(dto);
    }

    public FunctionTypeMatcher(FunctionMatcherDto functionMatcherDto) {
        dto = functionMatcherDto;
        overloadBindingsMatcher = new OverloadBindingsMatcher(dto.bindings);
    }

    @Override
    public boolean matches(Object o) {
        IFunctionType functionType = (IFunctionType) o;
        boolean ok = functionType.getNumberOfNonOptionalParameters() == dto.numberOfNonOptionalParameters
                && functionType.getName().equals(dto.name)
                && functionType.getParameters().size() + 1 == dto.bindings.length;
        if (ok) {
            ok = overloadBindingsMatcher.matches(functionType.getBindings());
        }
        return ok;
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        description.appendText(item.toString());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(dto.name).appendText("{")
                .appendText(String.valueOf(dto.numberOfNonOptionalParameters))
                .appendText("}");
        overloadBindingsMatcher.describeTo(description);
    }
}
