/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.utils.Pair;

import java.util.Map;
import java.util.SortedSet;

public class AggregateBindingDto
{
    public IConstraint constraint;
    public IFunctionType overload;
    public IBindingCollection bindings;
    public WorkItemDto workItemDto;
    public int iterateCount = 0;
    public Map<String, ITypeVariableReference> mapping;
    public IVariable bindingVariable;
    public String overloadVariableId;
    public boolean needToReIterate;
    public Map<Integer, Pair<ITypeSymbol, ITypeSymbol>> implicitConversions;
    public Integer argumentNumber;
    public boolean hasNarrowedArguments;
    public Map<String, SortedSet<ITypeSymbol>> lowerConstraints;
    public Map<String, SortedSet<ITypeSymbol>> upperConstraints;

    public AggregateBindingDto(
            IConstraint theConstraint,
            IFunctionType theOverload,
            IBindingCollection theBindings,
            WorkItemDto theWorkItemDto) {
        constraint = theConstraint;
        overload = theOverload;
        bindings = theBindings;
        workItemDto = theWorkItemDto;
    }
}