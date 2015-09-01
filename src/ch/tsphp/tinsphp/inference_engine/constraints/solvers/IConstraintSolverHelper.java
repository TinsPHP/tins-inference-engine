/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.AggregateBindingDto;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

import java.util.List;

/**
 * Provides constraint solving methods which are not directly related to a certain mode (iterative, soft typing etc.).
 */
public interface IConstraintSolverHelper
{
    boolean createBindingsIfNecessary(
            WorkItemDto workItemDto, IVariable leftHandSide, List<IVariable> arguments);

    void solve(WorkItemDto workItemDto, IConstraint constraint);

    void aggregateBinding(AggregateBindingDto dto);

    void addMostSpecificOverloadToWorklist(WorkItemDto workItemDto, IConstraint constraint);

    IFunctionType createOverload(IMethodSymbol methodSymbol, IBindingCollection bindings);
}
