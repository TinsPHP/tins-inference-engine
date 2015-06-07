/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            ok = overloadBindingsMatcher.matches(functionType.getOverloadBindings());
        }
        return ok;
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        IFunctionType functionType = (IFunctionType) item;
        List<IVariable> parameters = functionType.getParameters();
        if (parameters.size() + 1 != dto.bindings.length) {
            description.appendText("Not all parameters or too many were specified or the return variable was missing." +
                    " Missing were:\n");
            Set<String> variableIds = new HashSet<>();
            for (BindingMatcherDto binding : dto.bindings) {
                variableIds.add(binding.variableId);
            }
            boolean isNotFirst = false;
            for (IVariable variable : parameters) {
                if (!variableIds.contains(variable.getAbsoluteName())) {
                    if (isNotFirst) {
                        description.appendText(", ");
                    } else {
                        isNotFirst = true;
                    }
                    description.appendText(variable.getAbsoluteName());
                }
            }
        } else {
            description.appendText("\n").appendText(functionType.getName()).appendText("{")
                    .appendText(String.valueOf(functionType.getNumberOfNonOptionalParameters()))
                    .appendText("}");
            overloadBindingsMatcher.describeMismatch(functionType.getOverloadBindings(), description, false, false);
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("\n").appendText(dto.name).appendText("{")
                .appendText(String.valueOf(dto.numberOfNonOptionalParameters))
                .appendText("}");
        overloadBindingsMatcher.describeTo(description, false);
    }
}
