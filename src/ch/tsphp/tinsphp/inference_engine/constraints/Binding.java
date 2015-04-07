/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.symbols.constraints.TypeVariableCollection;

import java.util.HashMap;
import java.util.Map;

public class Binding implements IBinding
{
    private int count = 1;
    private Map<String, String> bindings;
    private TypeVariableCollection collection;

    public Binding(IOverloadResolver overloadResolver) {
        bindings = new HashMap<>();
        collection = new TypeVariableCollection(overloadResolver);
    }

    public Binding(IOverloadResolver overloadResolver, Binding binding) {
        count = binding.getTypeVariableCounter();
        bindings = new HashMap<>(binding.getVariable2TypeVariable());
        collection = new TypeVariableCollection(overloadResolver, binding.collection);
    }

    @Override
    public Map<String, String> getVariable2TypeVariable() {
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
    public String getNextTypeVariable() {
        return "T" + count++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(bindings.keySet().toString());
        sb.append(bindings.values().toString());
        sb.append("[");
        boolean notFirst = false;
        for (String typeVariable : bindings.values()) {
            if (notFirst) {
                sb.append(", ");
            } else {
                notFirst = true;
            }
            if (collection.hasLowerBounds(typeVariable)) {
                sb.append(collection.getLowerBoundConstraintIds(typeVariable).toString());
            } else {
                sb.append("null");
            }
        }
        sb.append("][");
        notFirst = false;
        for (String typeVariable : bindings.values()) {
            if (notFirst) {
                sb.append(", ");
            } else {
                notFirst = true;
            }
            if (collection.hasUpperBounds(typeVariable)) {
                sb.append(collection.getUpperBoundConstraintIds(typeVariable).toString());
            } else {
                sb.append("null");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
