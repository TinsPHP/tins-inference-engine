/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableConstraint;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class OverloadBindingsMatcher extends BaseMatcher<IOverloadBindings>
{
    final BindingMatcherDto[] dtos;

    public static Matcher<? super IOverloadBindings> withVariableBindings(BindingMatcherDto... dtos) {
        return new OverloadBindingsMatcher(dtos);
    }

    public static BindingMatcherDto varBinding(
            String theVariable,
            String theTypeVariable,
            List<String> theLowerBounds,
            List<String> theUpperBounds,
            boolean hasFixedType) {
        return new BindingMatcherDto(theVariable, theTypeVariable, theLowerBounds, theUpperBounds, hasFixedType);
    }

    public OverloadBindingsMatcher(BindingMatcherDto[] bindingDtos) {
        dtos = bindingDtos;
    }

    @Override
    public boolean matches(Object o) {
        IOverloadBindings overloadBindings = (IOverloadBindings) o;
        boolean ok = overloadBindings.getVariable2TypeVariable().size() == dtos.length;
        if (ok) {
            ok = matches(overloadBindings);
        }
        return ok;
    }

    public boolean matches(IOverloadBindings overloadBindings) {
        Map<String, ITypeVariableConstraint> variable2TypeVariable = overloadBindings.getVariable2TypeVariable();
        boolean ok = true;
        for (int i = 0; i < dtos.length; ++i) {
            BindingMatcherDto dto = dtos[i];
            ok = variable2TypeVariable.containsKey(dto.variableName);
            if (ok) {
                ITypeVariableConstraint typeVariableConstraint = variable2TypeVariable.get(dto.variableName);
                ok = typeVariableConstraint.getTypeVariable().equals(dto.typeVariable)
                        && typeVariableConstraint.hasFixedType() == dto.hasFixedType;
            }
            if (ok) {
                if (dto.lowerBounds == null) {
                    ok = !overloadBindings.hasLowerBounds(dto.typeVariable);
                } else {
                    ok = overloadBindings.hasLowerBounds(dto.typeVariable);
                    if (ok) {
                        Set<String> constraintIds = overloadBindings.getLowerBoundConstraintIds(dto.typeVariable);
                        ok = constraintIds.size() == dto.lowerBounds.size()
                                && constraintIds.containsAll(dto.lowerBounds);
                    }
                }
            }
            if (ok) {

                if (dto.upperBounds == null) {
                    ok = !overloadBindings.hasUpperBounds(dto.typeVariable);
                } else {
                    ok = overloadBindings.hasUpperBounds(dto.typeVariable);
                    if (ok) {
                        Set<String> constraintIds = overloadBindings.getUpperBoundConstraintIds(dto.typeVariable);
                        ok = constraintIds.size() == dto.upperBounds.size()
                                && constraintIds.containsAll(dto.upperBounds);
                    }
                }

            }

            if (!ok) {
                break;
            }
        }
        return ok;
    }

    @Override
    public void describeMismatch(Object item, org.hamcrest.Description description) {
        description.appendText(item.toString());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("[");
        for (int i = 0; i < dtos.length; ++i) {
            if (i != 0) {
                description.appendText(", ");
            }
            description.appendText(dtos[i].toString());
        }
        description.appendText("]");
    }

}
