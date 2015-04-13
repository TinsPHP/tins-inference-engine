/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableConstraint;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Map;

public class BindingsMatcher extends BaseMatcher<IBinding>
{
    final BindingMatcherDto[] dtos;

    public static Matcher<? super IBinding> withVariableBindings(BindingMatcherDto... dtos) {
        return new BindingsMatcher(dtos);
    }

    public static BindingMatcherDto varBinding(
            String theVariable,
            String theTypeVariable,
            List<String> theLowerBounds,
            List<String> theUpperBounds,
            boolean hasFixedType) {
        return new BindingMatcherDto(theVariable, theTypeVariable, theLowerBounds, theUpperBounds, hasFixedType);
    }

    public BindingsMatcher(BindingMatcherDto[] bindingDtos) {
        dtos = bindingDtos;
    }

    @Override
    public boolean matches(Object o) {
        IBinding binding = (IBinding) o;
        Map<String, ITypeVariableConstraint> variable2TypeVariable = binding.getVariable2TypeVariable();
        ITypeVariableCollection typeVariables = binding.getTypeVariables();

        return matches(variable2TypeVariable, typeVariables);
    }

    public boolean matches(
            Map<String, ITypeVariableConstraint> variable2TypeVariable,
            ITypeVariableCollection typeVariables) {
        int size = variable2TypeVariable.size();
        boolean ok = size == dtos.length;
        if (ok) {
            for (int i = 0; i < size; ++i) {
                BindingMatcherDto dto = dtos[i];
                ok = variable2TypeVariable.containsKey(dto.variableName);
                if (ok) {
                    ITypeVariableConstraint typeVariableConstraint = variable2TypeVariable.get(dto.variableName);
                    ok = typeVariableConstraint.getTypeVariable().equals(dto.typeVariable)
                            && typeVariableConstraint.hasFixedType() == dto.hasFixedType;
                }
                if (ok) {
                    ok = dto.lowerBounds == null && !typeVariables.hasLowerBounds(dto.typeVariable)
                            || dto.lowerBounds != null
                            && typeVariables.hasLowerBounds(dto.typeVariable)
                            && typeVariables.getLowerBoundConstraintIds(dto.typeVariable).containsAll(dto.lowerBounds);
                }
                if (ok) {
                    ok = dto.upperBounds == null && !typeVariables.hasUpperBounds(dto.typeVariable)
                            || dto.upperBounds != null
                            && typeVariables.hasUpperBounds(dto.typeVariable)
                            && typeVariables.getUpperBoundConstraintIds(dto.typeVariable).containsAll(dto.upperBounds);
                }
                if (!ok) {
                    break;
                }
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
