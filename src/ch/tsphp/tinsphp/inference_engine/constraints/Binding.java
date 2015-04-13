/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.FixedTypeVariableConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.TypeVariableConstraint;
import ch.tsphp.tinsphp.symbols.constraints.TypeVariableCollection;

import java.util.HashMap;
import java.util.Map;

public class Binding implements IBinding
{
    private int count = 1;
    private Map<String, ITypeVariableConstraint> bindings;
    private TypeVariableCollection collection;

    public Binding(IOverloadResolver overloadResolver) {
        bindings = new HashMap<>();
        collection = new TypeVariableCollection(overloadResolver);
    }

    public Binding(IOverloadResolver overloadResolver, Binding binding) {
        count = binding.getTypeVariableCounter();
        bindings = new HashMap<>();
        Map<String, ITypeVariableConstraint> mapping = new HashMap<>();
        for (Map.Entry<String, ITypeVariableConstraint> entry : binding.getVariable2TypeVariable().entrySet()) {
            ITypeVariableConstraint constraint = createOrGetConstraint(mapping, entry);
            bindings.put(entry.getKey(), constraint);
        }
        collection = new TypeVariableCollection(overloadResolver, binding.collection, mapping);
    }

    private ITypeVariableConstraint createOrGetConstraint(
            Map<String, ITypeVariableConstraint> mapping,
            Map.Entry<String, ITypeVariableConstraint> entry) {
        ITypeVariableConstraint value = entry.getValue();
        ITypeVariableConstraint constraint;
        String constraintId = value.getId();
        boolean containsKey = mapping.containsKey(constraintId);
        if (containsKey) {
            constraint = mapping.get(constraintId);
        } else {
            constraint = new TypeVariableConstraint(value.getTypeVariable());
        }

        if (value.hasFixedType() && !constraint.hasFixedType()) {
            constraint = new FixedTypeVariableConstraint((TypeVariableConstraint) constraint);
        }

        if (!containsKey) {
            mapping.put(constraintId, constraint);
        }
        return constraint;
    }

    @Override
    public Map<String, ITypeVariableConstraint> getVariable2TypeVariable() {
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
        return new TypeVariableConstraint("T" + count++);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean isNotFirst = false;
        for (Map.Entry<String, ITypeVariableConstraint> entry : bindings.entrySet()) {
            if (isNotFirst) {
                sb.append(", ");
            } else {
                isNotFirst = true;
            }
            sb.append(entry.getKey()).append(":");
            String typeVariable = entry.getValue().getTypeVariable();
            sb.append(typeVariable)
                    .append("<")
                    .append(collection.hasLowerBounds(typeVariable)
                            ? collection.getLowerBoundConstraintIds(typeVariable).toString() : "[]")
                    .append(",")
                    .append(collection.hasUpperBounds(typeVariable)
                            ? collection.getUpperBoundConstraintIds(typeVariable).toString() : "[]")
                    .append(">");
            if (entry.getValue().hasFixedType()) {
                sb.append("#");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
