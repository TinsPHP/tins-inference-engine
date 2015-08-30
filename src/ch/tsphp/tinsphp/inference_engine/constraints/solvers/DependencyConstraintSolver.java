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
    private final Map<String, Set<WorkItemDto>> unsolvedWorkItems;
    private IConstraintSolver constraintSolver;
    private ISoftTypingConstraintSolver softTypingConstraintSolver;
    private IConstraintSolverHelper constraintSolverHelper;

    public DependencyConstraintSolver(Map<String, Set<WorkItemDto>> theUnsolvedWorkItems) {
        unsolvedWorkItems = theUnsolvedWorkItems;
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
            // the work item might already be invalid due to another dependency. In such a case we remove it from
            // unsolvedWorkItems but not from directDependencies (see ConstraintSolverHelper)
            if (unsolvedWorkItems.containsKey(absoluteName)
                    && unsolvedWorkItems.get(absoluteName).contains(workItemDto)) {
                solveDependency(methodSymbol, workItemDto);
            }
        } else {
            softTypingConstraintSolver.aggregateLowerBounds(workItemDto);
            if (workItemDto.dependentConstraints.isEmpty() && unsolvedWorkItems.containsKey(absoluteName)) {
                Set<WorkItemDto> remainingUnsolved = unsolvedWorkItems.get(absoluteName);
                remainingUnsolved.remove(workItemDto);
                if (remainingUnsolved.isEmpty()) {
                    softTypingConstraintSolver.solveConstraints(methodSymbol, workItemDto);
                }
            }
        }
    }

    private void solveDependency(IMethodSymbol methodSymbol, WorkItemDto workItemDto) {
        String absoluteName = methodSymbol.getAbsoluteName();
        workItemDto.workDeque.add(workItemDto);
        List<WorkItemDto> solvedWorkItems = constraintSolver.solveConstraints(workItemDto.workDeque);
        Iterator<WorkItemDto> iterator = solvedWorkItems.iterator();
        WorkItemDto newWorkItem = null;
        if (iterator.hasNext()) {
            newWorkItem = iterator.next();
            // renew binding in order that the registered unsolved work item is up-to-date
            workItemDto.bindingCollection = newWorkItem.bindingCollection;
        } else {
            unsolvedWorkItems.get(absoluteName).remove(workItemDto);
        }
        boolean createDependencies = true;
        if (workItemDto.dependentConstraints.isEmpty()) {
            Set<WorkItemDto> remainingUnsolved = unsolvedWorkItems.get(absoluteName);
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
