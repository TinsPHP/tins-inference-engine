/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.inference.constraints.TypeVariableConstraint;
import ch.tsphp.tinsphp.symbols.constraints.TypeVariableCollection;

import java.util.HashMap;
import java.util.Map;

public class Binding implements IBinding
{
    public static final char TYPE_VARIABLE_PREFIX = 'T';
    private int count = 1;
    private Map<String, TypeVariableConstraint> bindings;
    private TypeVariableCollection collection;

    public Binding(IOverloadResolver overloadResolver) {
        bindings = new HashMap<>();
        collection = new TypeVariableCollection(overloadResolver);
    }

    public Binding(IOverloadResolver overloadResolver, Binding binding) {
        count = binding.getTypeVariableCounter();
        bindings = new HashMap<>();
        Map<String, TypeVariableConstraint> mapping = new HashMap<>();
        for (Map.Entry<String, TypeVariableConstraint> entry : binding.getVariable2TypeVariable().entrySet()) {
            TypeVariableConstraint value = entry.getValue();
            TypeVariableConstraint constraint;
            String constraintId = value.getId();
            if (mapping.containsKey(constraintId)) {
                constraint = mapping.get(constraintId);
            } else {
                constraint = new TypeVariableConstraint(value.getTypeVariable());
                if (!value.isNotConstant()) {
                    constraint.setIsConstant();
                }
                mapping.put(constraintId, constraint);
            }
            bindings.put(entry.getKey(), constraint);
        }
        collection = new TypeVariableCollection(overloadResolver, binding.collection, mapping);
    }

    @Override
    public Map<String, TypeVariableConstraint> getVariable2TypeVariable() {
        return bindings;
    }

    @Override
    public ITypeVariableCollection getTypeVariables() {
        return collection;
    }

    @Override
    public int getTypeVariableCounter() {
        return count;
    }

    @Override
    public TypeVariableConstraint getNextTypeVariable() {
        return new TypeVariableConstraint(String.valueOf(TYPE_VARIABLE_PREFIX) + count++);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean isNotFirst = false;
        for (Map.Entry<String, TypeVariableConstraint> entry : bindings.entrySet()) {
            if (isNotFirst) {
                sb.append(", ");
            } else {
                isNotFirst = true;
            }
            sb.append(entry.getKey()).append(":");
            String typeVariable = entry.getValue().getTypeVariable();
            sb.append(typeVariable)
                    .append("<")
                    .append(collection.hasLowerBounds(typeVariable) ?
                            collection.getLowerBoundConstraintIds(typeVariable).toString() : "[]")
                    .append(",")
                    .append(collection.hasUpperBounds(typeVariable)
                            ? collection.getUpperBoundConstraintIds(typeVariable).toString() : "[]")
                    .append(">");
        }
        sb.append("]");
        return sb.toString();
    }
}
