/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Map;

public class BindingMatcher extends BaseMatcher<IBinding>
{
    final String[] variables;
    final String[] typeVariables;
    final List<String>[] lowerBoundLists;
    final List<String>[] upperBoundLists;

    public static Matcher<? super IBinding> isBinding(
            final String[] vars, final String[] typeVars, final List<String>[] lower,
            final List<String>[] upper) {
        return new BindingMatcher(vars, typeVars, lower, upper);
    }

    @SafeVarargs
    public static List<String>[] lowerConstraints(List<String>... constraints) {
        return constraints;
    }

    @SafeVarargs
    public static List<String>[] upperConstraints(List<String>... constraints) {
        return constraints;
    }

    public static String[] vars(String... vars) {
        return vars;
    }

    public static String[] typeVars(String... vars) {
        return vars;
    }

    public BindingMatcher(
            final String[] theVars,
            final String[] theTypeVars,
            final List<String>[] theLowerBounds,
            final List<String>[] theUpperBounds) {
        variables = theVars;
        typeVariables = theTypeVars;
        lowerBoundLists = theLowerBounds;
        upperBoundLists = theUpperBounds;
    }

    @Override
    public boolean matches(Object o) {
        IBinding binding = (IBinding) o;
        Map<String, String> variable2TypeVariable = binding.getVariable2TypeVariable();
        ITypeVariableCollection typeVariables = binding.getTypeVariables();

        int size = variable2TypeVariable.size();
        boolean ok = size == variables.length;
        if (ok) {
            for (int i = 0; i < size; ++i) {
                String variableName = variables[i];
                String typeVariable = this.typeVariables[i];
                List<String> lowerBounds = lowerBoundLists[i];
                List<String> upperBounds = upperBoundLists[i];
                ok = variable2TypeVariable.containsKey(variableName);
                if (ok) {
                    ok = variable2TypeVariable.get(variableName).equals(typeVariable);
                }
                if (ok) {
                    ok = lowerBounds == null && !typeVariables.hasLowerBounds(typeVariable)
                            || lowerBounds != null
                            && typeVariables.getLowerBoundConstraintIds(typeVariable).containsAll(lowerBounds);
                }
                if (ok) {
                    ok = upperBounds == null && !typeVariables.hasUpperBounds(typeVariable)
                            || upperBounds != null
                            && typeVariables.getUpperBoundConstraintIds(typeVariable).containsAll(upperBounds);
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
        for (int i = 0; i < typeVariables.length; ++i) {
            if (i != 0) {
                description.appendText(", ");
            }
            description.appendText(variables[i]).appendText(":");
            description.appendText(typeVariables[i]);
            description.appendText("<")
                    .appendText(lowerBoundLists[i] != null ? lowerBoundLists[i].toString() : "[]")
                    .appendText(",")
                    .appendText(upperBoundLists[i] != null ? upperBoundLists[i].toString() : "[]")
                    .appendText(">");
        }
        description.appendText("]");
    }
}
