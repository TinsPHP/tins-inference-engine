/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyConstraintSolver implements IDependencyConstraintSolver
{
    private final Map<String, Set<WorkItemDto>> unsolvedConstraints;
    private IConstraintSolver constraintSolver;
    private ISoftTypingConstraintSolver softTypingConstraintSolver;
    private IConstraintSolverHelper constraintSolverHelper;

    public DependencyConstraintSolver(Map<String, Set<WorkItemDto>> theUnsolvedConstraints) {
        unsolvedConstraints = theUnsolvedConstraints;
    }

    @Override
    public void setDependencies(
            IConstraintSolver theConstraintSolver,
            IConstraintSolverHelper theConstraintSolverHelper,
            ISoftTypingConstraintSolver theSoftTypingConstraintSolver) {
        constraintSolver = theConstraintSolver;
        constraintSolverHelper = theConstraintSolverHelper;
        softTypingConstraintSolver = theSoftTypingConstraintSolver;
    }

    @Override
    public void solveDependency(Pair<WorkItemDto, Integer> pair) {
        WorkItemDto workItemDto = pair.first;
        workItemDto.pointer = pair.second;
        //removing pointer not the element at index thus the cast to (Integer)
        workItemDto.dependentConstraints.remove((Integer) workItemDto.pointer);
        workItemDto.isSolvingDependency = true;
        IMethodSymbol methodSymbol = (IMethodSymbol) workItemDto.constraintCollection;
        String absoluteName = methodSymbol.getAbsoluteName();

        if (!workItemDto.isInSoftTypingMode) {
            workItemDto.workDeque.add(workItemDto);
            List<WorkItemDto> solvedWorkItems = constraintSolver.solveConstraints(workItemDto.workDeque);
            Iterator<WorkItemDto> iterator = solvedWorkItems.iterator();
            WorkItemDto newWorkItem = null;
            if (iterator.hasNext()) {
                newWorkItem = iterator.next();
                // renew binding in order that the registered unsolved work item is up-to-date
                workItemDto.bindingCollection = newWorkItem.bindingCollection;
            }
            boolean createDependencies = true;
            if (workItemDto.dependentConstraints.isEmpty() && unsolvedConstraints.containsKey(absoluteName)) {
                Set<WorkItemDto> remainingUnsolved = unsolvedConstraints.get(absoluteName);
                remainingUnsolved.remove(workItemDto);
                if (newWorkItem != null) {
                    List<IBindingCollection> bindings = initOrGetBindings(methodSymbol);
                    bindings.add(workItemDto.bindingCollection);

                    // if we do not have remaining work items left which need to be solved,
                    // then we can finalise the method.
                    if (remainingUnsolved.isEmpty()) {
                        createDependencies = false;
                        while (iterator.hasNext()) {
                            bindings.add(iterator.next().bindingCollection);
                        }
                        constraintSolverHelper.finishingMethodConstraints(methodSymbol, bindings);
                    }
                } else if (remainingUnsolved.isEmpty()) {
                    createDependencies = false;
                    softTypingConstraintSolver.fallBackToSoftTyping(methodSymbol);
                }
            }
            if (createDependencies) {
                while (iterator.hasNext()) {
                    newWorkItem = iterator.next();
                    newWorkItem.pointer = workItemDto.pointer;
                    constraintSolverHelper.createDependencies(newWorkItem);
                }
            }
        } else {
            softTypingConstraintSolver.aggregateLowerBounds(workItemDto);
            if (workItemDto.dependentConstraints.isEmpty() && unsolvedConstraints.containsKey(absoluteName)) {
                Set<WorkItemDto> remainingUnsolved = unsolvedConstraints.get(absoluteName);
                remainingUnsolved.remove(workItemDto);
                if (remainingUnsolved.isEmpty()) {
                    softTypingConstraintSolver.solveConstraints(methodSymbol, workItemDto);
                }
            }
        }

    }

    private List<IBindingCollection> initOrGetBindings(IMethodSymbol methodSymbol) {
        List<IBindingCollection> bindings = methodSymbol.getBindings();
        if (bindings == null) {
            bindings = new ArrayList<>();
            methodSymbol.setBindings(bindings);
        }
        return bindings;
    }
}
