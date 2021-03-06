/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;


import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

import java.util.Deque;
import java.util.List;

/**
 * Responsible to solve the constraints gathered during the reference phase.
 */
public interface IConstraintSolver
{
    void solveConstraints(List<IMethodSymbol> methodSymbols, IGlobalNamespaceScope globalDefaultNamespaceScope);

    List<WorkItemDto> solveConstraints(Deque<WorkItemDto> workDeque);
}
