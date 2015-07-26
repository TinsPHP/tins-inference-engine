/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.tinsphp.common.TinsPHPConstants;
import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.HashSet;
import java.util.Set;

public class FunctionTypeMatcher extends BaseMatcher<IFunctionType>
{
    private final FunctionMatcherDto dto;
    private final BindingCollectionMatcher bindingCollectionMatcher;

    public static Matcher<? super IFunctionType> isFunctionType(FunctionMatcherDto dto) {
        return new FunctionTypeMatcher(dto);
    }

    public FunctionTypeMatcher(FunctionMatcherDto functionMatcherDto) {
        dto = functionMatcherDto;
        bindingCollectionMatcher = new BindingCollectionMatcher(dto.bindings);
    }

    @Override
    public boolean matches(Object o) {
        IFunctionType functionType = (IFunctionType) o;
        boolean ok = functionType.getNumberOfNonOptionalParameters() == dto.numberOfNonOptionalParameters
                && functionType.getName().equals(dto.name);
        if (ok) {
            Set<String> typeVariables = getTypeVariables(functionType);
            int numOfHelperTypeVariables = getNumberOfHelpTypeParameters(functionType, typeVariables);
            ok = functionType.getParameters().size() + 1 + numOfHelperTypeVariables == dto.bindings.length;
            if (ok) {
                ok = bindingCollectionMatcher.matches(functionType.getBindingCollection());
            }
        }
        return ok;
    }

    private Set<String> getTypeVariables(IFunctionType functionType) {
        IBindingCollection bindingCollection = functionType.getBindingCollection();
        Set<String> typeVariables = new HashSet<>();
        for (IVariable variable : functionType.getParameters()) {
            typeVariables.add(bindingCollection.getTypeVariable(variable.getAbsoluteName()));
        }
        typeVariables.add(bindingCollection.getTypeVariable(TinsPHPConstants.RETURN_VARIABLE_NAME));
        return typeVariables;
    }

    private int getNumberOfHelpTypeParameters(IFunctionType functionType, Set<String> typeVariables) {
        int numOfHelperTypeVariables = 0;
        for (String typeVariable : functionType.getNonFixedTypeParameters()) {
            if (!typeVariables.contains(typeVariable)) {
                typeVariables.add(typeVariable);
                ++numOfHelperTypeVariables;
            }
        }
        return numOfHelperTypeVariables;
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        IFunctionType functionType = (IFunctionType) item;
        description.appendText("\n").appendText(functionType.getName()).appendText("{")
                .appendText(String.valueOf(functionType.getNumberOfNonOptionalParameters()))
                .appendText("}[");
        IBindingCollection bindingCollection = functionType.getBindingCollection();
        Set<String> typeVariables = new HashSet<>();
        boolean isNotFirst = false;
        for (IVariable variable : functionType.getParameters()) {
            if (isNotFirst) {
                description.appendText(", ");
            } else {
                isNotFirst = true;
            }
            String variableId = variable.getAbsoluteName();
            addVariableToDescription(description, bindingCollection, variableId, typeVariables);
        }

        if (isNotFirst) {
            description.appendText(", ");
        }
        addVariableToDescription(description, bindingCollection, TinsPHPConstants.RETURN_VARIABLE_NAME, typeVariables);

        for (String typeVariable : functionType.getNonFixedTypeParameters()) {
            if (!typeVariables.contains(typeVariable)) {
                for (String variableId : bindingCollection.getVariableIds(typeVariable)) {
                    description.appendText(", ");
                    description.appendText("(");
                    addVariableToDescription(description, bindingCollection, variableId, typeVariables);
                    description.appendText(")");
                }
            }
        }
        description.appendText("]");


//        Set<String> typeVariables = getTypeVariables(functionType);
//        int numOfHelperTypeVariables = getNumberOfHelpTypeParameters(functionType, typeVariables);
//        if (functionType.getParameters().size() + 1 + numOfHelperTypeVariables < dto.bindings.length) {
//            description.appendText("Not all type variables or too many were specified: ");
//
//
//            description.appendText("Not all type variables or too many were specified: ");
//            boolean isNotFirst = false;
//            if (functionType.getParameters().size() + 1 + numOfHelperTypeVariables > dto.bindings.length) {
//                Set<String> expectedTypeVariables = new HashSet<>();
//                for (BindingMatcherDto binding : dto.bindings) {
//                    expectedTypeVariables.add(binding.typeVariable);
//                }
//                for (String typeVariable : typeVariables) {
//                    if (!expectedTypeVariables.contains(typeVariable)) {
//                        if (isNotFirst) {
//                            description.appendText(", ");
//                        } else {
//                            isNotFirst = true;
//                        }
//                        description.appendText(typeVariable);
//                    }
//                }
//            } else {
//                for (BindingMatcherDto binding : dto.bindings) {
//                    if (!typeVariables.contains(binding.typeVariable)) {
//                        if (isNotFirst) {
//                            description.appendText(", ");
//                        } else {
//                            isNotFirst = true;
//                        }
//                        description.appendText(binding.variableId + ":" + binding.typeVariable);
//                    }
//                }
//            }
//        } else {
//            description.appendText("\n").appendText(functionType.getName()).appendText("{")
//                    .appendText(String.valueOf(functionType.getNumberOfNonOptionalParameters()))
//                    .appendText("}");
//            bindingCollectionMatcher.describeMismatch(bindingCollection, description, false, false);
//        }
    }

    private void addVariableToDescription(
            Description description,
            IBindingCollection bindingCollection,
            String variableId,
            Set<String> typeVariables) {
        ITypeVariableReference reference = bindingCollection.getTypeVariableReference(variableId);
        String typeVariable = reference.getTypeVariable();
        typeVariables.add(typeVariable);
        description.appendText(variableId).appendText(":").appendText(typeVariable).appendText("<")
                .appendText(bindingCollection.getLowerBoundConstraintIds(typeVariable).toString())
                .appendText(",")
                .appendText(bindingCollection.getUpperBoundConstraintIds(typeVariable).toString())
                .appendText(">");
        if (reference.hasFixedType()) {
            description.appendText("#");
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("\n").appendText(dto.name).appendText("{")
                .appendText(String.valueOf(dto.numberOfNonOptionalParameters))
                .appendText("}");
        bindingCollectionMatcher.describeTo(description, false);
    }
}
