/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;


import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstraintSolver implements IConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final IIterativeConstraintSolver iterativeConstraintSolver;
    private final ISoftTypingConstraintSolver softTypingConstraintSolver;
    private final IConstraintSolverHelper constraintSolverHelper;

    private final Map<String, Set<WorkItemDto>> unsolvedConstraints;

    @SuppressWarnings("checkstyle:parameternumber")
    public ConstraintSolver(
            ISymbolFactory theSymbolFactory,
            IIterativeConstraintSolver theIterativeConstraintSolver,
            ISoftTypingConstraintSolver theSoftTypingConstraintSolver,
            IConstraintSolverHelper theConstraintSolverHelper,
            Map<String, Set<WorkItemDto>> theUnsolvedConstraints) {

        symbolFactory = theSymbolFactory;
        iterativeConstraintSolver = theIterativeConstraintSolver;
        softTypingConstraintSolver = theSoftTypingConstraintSolver;
        constraintSolverHelper = theConstraintSolverHelper;
        unsolvedConstraints = theUnsolvedConstraints;
    }

    @Override
    public void solveConstraints(List<IMethodSymbol> methodSymbols, IGlobalNamespaceScope globalDefaultNamespaceScope) {
        for (IMethodSymbol methodSymbol : methodSymbols) {
            Deque<WorkItemDto> workDeque = createInitialWorklist(methodSymbol, true);
            solveMethodConstraints(methodSymbol, workDeque);
        }

        if (!unsolvedConstraints.isEmpty()) {
            iterativeConstraintSolver.solveConstraintsIteratively();
        }

        if (!globalDefaultNamespaceScope.getConstraints().isEmpty()) {
            Deque<WorkItemDto> workDeque = createInitialWorklist(globalDefaultNamespaceScope, false);
            solveGlobalDefaultNamespaceConstraints(globalDefaultNamespaceScope, workDeque);
        }
    }

    private Deque<WorkItemDto> createInitialWorklist(
            IConstraintCollection constraintCollection, boolean isSolvingMethod) {
        IBindingCollection bindings = symbolFactory.createBindingCollection();
        Deque<WorkItemDto> workDeque = new ArrayDeque<>();
        workDeque.add(new WorkItemDto(workDeque, constraintCollection, 0, isSolvingMethod, bindings));
        return workDeque;
    }

    public void solveGlobalDefaultNamespaceConstraints(
            IGlobalNamespaceScope globalDefaultNamespaceScope, Deque<WorkItemDto> workDeque) {
        List<IBindingCollection> bindings = solveConstraints(workDeque);
        if (bindings.isEmpty()) {
            softTypingConstraintSolver.fallBackToSoftTyping(globalDefaultNamespaceScope);
        } else {
            //Warning! start code duplication - same as in SoftTypingConstraintSolver
            globalDefaultNamespaceScope.setBindings(bindings);
            IBindingCollection bindingCollection = bindings.get(0);
            for (String variableId : bindingCollection.getVariableIds()) {
                bindingCollection.fixType(variableId);
            }
            //Warning! end code duplication - same as in SoftTypingConstraintSolver
        }
    }

    @Override
    public void solveMethodConstraints(IMethodSymbol methodSymbol, Deque<WorkItemDto> workDeque) {
        List<IBindingCollection> bindings = solveConstraints(workDeque);
        if (!bindings.isEmpty()) {
            constraintSolverHelper.finishingMethodConstraints(methodSymbol, bindings);
        } else if (!unsolvedConstraints.containsKey(methodSymbol.getAbsoluteName())) {
            //does not have any dependencies and still cannot be solved
            //need to fallback to soft typing
            softTypingConstraintSolver.fallBackToSoftTyping(methodSymbol);
        }
    }

    private List<IBindingCollection> solveConstraints(Deque<WorkItemDto> workDeque) {
        List<IBindingCollection> solvedBindings = new ArrayList<>();

        List<IConstraint> constraints = null;
        if (!workDeque.isEmpty()) {
            WorkItemDto workItemDto = workDeque.peek();
            constraints = workItemDto.constraintCollection.getConstraints();
        }

        while (!workDeque.isEmpty()) {
            WorkItemDto workItemDto = workDeque.removeFirst();

            if (workItemDto.pointer < constraints.size()) {
                IConstraint constraint = constraints.get(workItemDto.pointer);
                solveConstraint(workItemDto, constraint);
            } else if (workItemDto.dependentConstraints == null || workItemDto.dependentConstraints.isEmpty()) {
                solvedBindings.add(workItemDto.bindingCollection);
            } else if (!workItemDto.isSolvingDependency) {
                constraintSolverHelper.createDependencies(workItemDto);
            }
        }

        return solvedBindings;
    }

    private void solveConstraint(WorkItemDto workItemDto, IConstraint constraint) {
        IMinimalMethodSymbol refMethodSymbol = constraint.getMethodSymbol();
        if (refMethodSymbol.getOverloads().size() != 0) {
            constraintSolverHelper.solve(workItemDto, constraint);
        } else {
            //add to unresolved constraints
            if (workItemDto.dependentConstraints == null) {
                workItemDto.dependentConstraints = new ArrayList<>();
            }
            workItemDto.dependentConstraints.add(workItemDto.pointer);

            //proceed with the rest
            ++workItemDto.pointer;
            workItemDto.workDeque.add(workItemDto);
        }
    }
}
