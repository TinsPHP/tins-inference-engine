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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    //Warning! start code duplication - same as in tins-symbols
    @Override
    public boolean matches(Object o) {
        IOverloadBindings overloadBindings = (IOverloadBindings) o;
        boolean ok = overloadBindings.getVariableIds().size() == dtos.length;
        if (ok) {
            ok = matches(overloadBindings);
        }
        return ok;
    }
    //Warning! end code duplication - same as in tins-symbols


    //Warning! start code duplication - same as in tins-symbols
    public boolean matches(IOverloadBindings bindings) {
        Set<String> variableIds = bindings.getVariableIds();
        boolean ok = true;
        for (BindingMatcherDto dto : dtos) {
            ok = variableIds.contains(dto.variableId);
            if (ok) {
                ITypeVariableConstraint typeVariableConstraint = bindings.getTypeVariableConstraint(dto.variableId);
                ok = typeVariableConstraint.getTypeVariable().equals(dto.typeVariable)
                        && typeVariableConstraint.hasFixedType() == dto.hasFixedType;
            }
            if (ok) {
                if (dto.lowerBounds == null) {
                    ok = !bindings.hasLowerBounds(dto.typeVariable);
                } else {
                    ok = bindings.hasLowerBounds(dto.typeVariable);
                    if (ok) {
                        Set<String> constraintIds = bindings.getLowerBoundConstraintIds(dto.typeVariable);
                        ok = constraintIds.size() == dto.lowerBounds.size()
                                && constraintIds.containsAll(dto.lowerBounds);
                    }
                }
            }
            if (ok) {

                if (dto.upperBounds == null) {
                    ok = !bindings.hasUpperBounds(dto.typeVariable);
                } else {
                    ok = bindings.hasUpperBounds(dto.typeVariable);
                    if (ok) {
                        Set<String> constraintIds = bindings.getUpperBoundConstraintIds(dto.typeVariable);
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
    //Warning! end code duplication - same as in tins-symbols


    //Warning! start code duplication - same as in tins-symbols
    @Override
    public void describeMismatch(Object item, org.hamcrest.Description description) {
        describeMismatch((IOverloadBindings) item, description, true, true);
    }

    public void describeMismatch(
            IOverloadBindings bindings, Description description,
            boolean withBeginningNewLine, boolean reportAdditionalBindings) {
        boolean variableMissing = false;

        StringBuilder sb = new StringBuilder();
        if (withBeginningNewLine) {
            description.appendText("\n");
        }
        description.appendText("[");

        boolean notFirst = false;

        Set<String> variableIds = new HashSet<>(bindings.getVariableIds());
        for (BindingMatcherDto dto : dtos) {
            String variableId = dto.variableId;
            if (!variableIds.contains(variableId)) {
                if (!variableMissing) {
                    sb.append("\nThe following variables where not defined in the bindings: [");
                    variableMissing = true;
                } else {
                    sb.append(", ");
                }
                sb.append(variableId);
            } else {
                variableIds.remove(variableId);
                notFirst = appendVariable(bindings, description, notFirst, variableId);
            }
        }
        if (variableMissing) {
            sb.append("]\n");
        }
        if (reportAdditionalBindings && !variableIds.isEmpty()) {

            if (!variableMissing) {
                sb.append("\n");
            }
            sb.append("The following variables where defined additionally in the bindings: [");

            Iterator<String> iterator = variableIds.iterator();
            if (iterator.hasNext()) {
                String variableId = iterator.next();
                sb.append(variableId);
                notFirst = appendVariable(bindings, description, notFirst, variableId);
            }
            while (iterator.hasNext()) {
                String variableId = iterator.next();
                sb.append(", ").append(variableId);
                notFirst = appendVariable(bindings, description, notFirst, variableId);
            }
            sb.append("]\n");
        }
        description.appendText("]");
        description.appendText(sb.toString());
    }
    //Warning! end code duplication - same as in tins-symbols


    //Warning! start code duplication - same as in tins-symbols
    private boolean appendVariable(
            IOverloadBindings bindings, Description description, boolean notFirst, String variableId) {
        if (notFirst) {
            description.appendText(", ");
        }
        ITypeVariableConstraint typeVariableConstraint = bindings.getTypeVariableConstraint(variableId);
        String typeVariable = typeVariableConstraint.getTypeVariable();
        description.appendText(variableId).appendText(":").appendText(typeVariable)
                .appendText("<")
                .appendText(bindings.getLowerBoundConstraintIds(typeVariable).toString())
                .appendText(",")
                .appendText(bindings.getUpperBoundConstraintIds(typeVariable).toString())
                .appendText(">");
        if (typeVariableConstraint.hasFixedType()) {
            description.appendText("#");
        }
        return true;
    }
    //Warning! end code duplication - same as in tins-symbols


    //Warning! start code duplication - same as in tins-symbols
    @Override
    public void describeTo(Description description) {
        describeTo(description, true);
    }

    public void describeTo(Description description, boolean withBeginningNewLine) {
        if (withBeginningNewLine) {
            description.appendText("\n");
        }
        description.appendText("[");
        for (int i = 0; i < dtos.length; ++i) {
            if (i != 0) {
                description.appendText(", ");
            }
            description.appendText(dtos[i].toString());
        }
        description.appendText("]");
    }
    //Warning! end code duplication - same as in tins-symbols

}
