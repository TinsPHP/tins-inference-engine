/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

import java.util.Map;
import java.util.Set;

public class DependencyConstraintSolver implements IDependencyConstraintSolver
{
    private final Map<String, Set<WorkItemDto>> unsolvedConstraints;
    private IConstraintSolver constraintSolver;
    private ISoftTypingConstraintSolver softTypingConstraintSolver;

    public DependencyConstraintSolver(Map<String, Set<WorkItemDto>> theUnsolvedConstraints) {
        unsolvedConstraints = theUnsolvedConstraints;
    }

    @Override
    public void setConstraintSolver(IConstraintSolver theConstraintSolver) {
        constraintSolver = theConstraintSolver;
    }

    @Override
    public void solveDependency(Pair<WorkItemDto, Integer> pair) {
        WorkItemDto workItemDto = pair.first;
        workItemDto.pointer = pair.second;
        //removing pointer not the element at index thus the cast to (Integer)
        workItemDto.unsolvedConstraints.remove((Integer) workItemDto.pointer);
        workItemDto.isSolvingDependency = true;
        IMethodSymbol methodSymbol = (IMethodSymbol) workItemDto.constraintCollection;
        if (!workItemDto.isInSoftTypingMode) {
            workItemDto.workDeque.add(workItemDto);
            constraintSolver.solveMethodConstraints(methodSymbol, workItemDto.workDeque);
        } else {
            softTypingConstraintSolver.aggregateLowerBounds(workItemDto);
        }

        String absoluteName = methodSymbol.getAbsoluteName();
        if (workItemDto.unsolvedConstraints.isEmpty() && unsolvedConstraints.containsKey(absoluteName)) {
            Set<WorkItemDto> remainingUnsolved = unsolvedConstraints.get(absoluteName);
            remainingUnsolved.remove(workItemDto);
            if (remainingUnsolved.isEmpty()) {
                if (workItemDto.isInSoftTypingMode) {
                    softTypingConstraintSolver.solveConstraints(methodSymbol, workItemDto);
                } else if (methodSymbol.getOverloads().size() == 0) {
                    softTypingConstraintSolver.fallBackToSoftTyping(methodSymbol);
                }
            }
        }
    }

    @Override
    public void setSoftTypingConstraintSolver(ISoftTypingConstraintSolver theSoftTypingConstraintSolver) {
        softTypingConstraintSolver = theSoftTypingConstraintSolver;
    }
}
