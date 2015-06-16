/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.inference_engine.constraints.WorklistDto;

import java.util.Map;
import java.util.Set;

public class DependencyConstraintSolver implements IDependencyConstraintSolver
{
    private final Map<String, Set<WorklistDto>> unsolvedConstraints;
    private IConstraintSolver constraintSolver;
    private ISoftTypingConstraintSolver softTypingConstraintSolver;

    public DependencyConstraintSolver(Map<String, Set<WorklistDto>> theUnsolvedConstraints) {
        unsolvedConstraints = theUnsolvedConstraints;
    }

    @Override
    public void setConstraintSolver(IConstraintSolver theConstraintSolver) {
        constraintSolver = theConstraintSolver;
    }

    @Override
    public void solveDependency(Pair<WorklistDto, Integer> pair) {
        WorklistDto worklistDto = pair.first;
        worklistDto.pointer = pair.second;
        //removing pointer not the element at index thus the cast to (Integer)
        worklistDto.unsolvedConstraints.remove((Integer) worklistDto.pointer);
        worklistDto.isSolvingDependency = true;
        IMethodSymbol methodSymbol = (IMethodSymbol) worklistDto.constraintCollection;
        if (!worklistDto.isInSoftTypingMode) {
            worklistDto.workDeque.add(worklistDto);
            constraintSolver.solveMethodConstraints(methodSymbol, worklistDto.workDeque);
        } else {
            softTypingConstraintSolver.aggregateLowerBounds(worklistDto);
        }

        String absoluteName = methodSymbol.getAbsoluteName();
        if (worklistDto.unsolvedConstraints.isEmpty() && unsolvedConstraints.containsKey(absoluteName)) {
            Set<WorklistDto> remainingUnsolved = unsolvedConstraints.get(absoluteName);
            remainingUnsolved.remove(worklistDto);
            if (remainingUnsolved.isEmpty()) {
                if (worklistDto.isInSoftTypingMode) {
                    softTypingConstraintSolver.solveConstraints(methodSymbol, worklistDto);
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
