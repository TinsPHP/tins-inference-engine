/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.AggregateBindingDto;
import ch.tsphp.tinsphp.inference_engine.constraints.WorklistDto;

import java.util.List;

public interface IConstraintSolverHelper
{
    boolean createBindingsIfNecessary(
            WorklistDto worklistDto, IVariable leftHandSide, List<IVariable> arguments);

    void solve(WorklistDto worklistDto, IConstraint constraint);

    void aggregateBinding(AggregateBindingDto dto);

    void addMostSpecificOverloadToWorklist(WorklistDto worklistDto, IConstraint constraint);

    void createDependencies(WorklistDto worklistDto);

    void finishingMethodConstraints(IMethodSymbol methodSymbol, List<IOverloadBindings> bindings);

    IFunctionType createOverload(IMethodSymbol methodSymbol, IOverloadBindings bindings);
}
