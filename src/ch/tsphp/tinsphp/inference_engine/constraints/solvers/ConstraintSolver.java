/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;


import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.inference_engine.constraints.WorklistDto;

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

    private final Map<String, Set<WorklistDto>> unsolvedConstraints;

    public ConstraintSolver(
            ISymbolFactory theSymbolFactory,
            IIterativeConstraintSolver theIterativeConstraintSolver,
            ISoftTypingConstraintSolver theSoftTypingConstraintSolver,
            IConstraintSolverHelper theConstraintSolverHelper,
            Map<String, Set<WorklistDto>> theUnsolvedConstraints) {

        symbolFactory = theSymbolFactory;
        iterativeConstraintSolver = theIterativeConstraintSolver;
        softTypingConstraintSolver = theSoftTypingConstraintSolver;
        constraintSolverHelper = theConstraintSolverHelper;
        unsolvedConstraints = theUnsolvedConstraints;
    }

    @Override
    public void solveConstraints(List<IMethodSymbol> methodSymbols, IGlobalNamespaceScope globalDefaultNamespaceScope) {
        for (IMethodSymbol methodSymbol : methodSymbols) {
            Deque<WorklistDto> workDeque = createInitialWorklist(methodSymbol, true);
            solveMethodConstraints(methodSymbol, workDeque);
        }

        if (!unsolvedConstraints.isEmpty()) {
            iterativeConstraintSolver.solveConstraintsIteratively();
        }

        if (!globalDefaultNamespaceScope.getConstraints().isEmpty()) {
            Deque<WorklistDto> workDeque = createInitialWorklist(globalDefaultNamespaceScope, false);
            solveGlobalDefaultNamespaceConstraints(globalDefaultNamespaceScope, workDeque);
        }
    }

    private Deque<WorklistDto> createInitialWorklist(
            IConstraintCollection constraintCollection, boolean isSolvingMethod) {
        IOverloadBindings bindings = symbolFactory.createOverloadBindings();
        Deque<WorklistDto> workDeque = new ArrayDeque<>();
        workDeque.add(new WorklistDto(workDeque, constraintCollection, 0, isSolvingMethod, bindings));
        return workDeque;
    }

    public void solveGlobalDefaultNamespaceConstraints(
            IGlobalNamespaceScope globalDefaultNamespaceScope, Deque<WorklistDto> workDeque) {
        List<IOverloadBindings> bindings = solveConstraints(workDeque);
        if (bindings.isEmpty()) {
            //TODO rstoll TINS-306 inference - runtime check insertion
        } else {
            globalDefaultNamespaceScope.setBindings(bindings);
            IOverloadBindings overloadBindings = bindings.get(0);
            for (String variableId : overloadBindings.getVariableIds()) {
                overloadBindings.fixType(variableId);
            }
        }
    }

    @Override
    public void solveMethodConstraints(IMethodSymbol methodSymbol, Deque<WorklistDto> workDeque) {
        List<IOverloadBindings> bindings = solveConstraints(workDeque);
        if (!bindings.isEmpty()) {
            constraintSolverHelper.finishingMethodConstraints(methodSymbol, bindings);
        } else if (!unsolvedConstraints.containsKey(methodSymbol.getAbsoluteName())) {
            //does not have any dependencies and still cannot be solved
            //need to fallback to soft typing
            softTypingConstraintSolver.fallBackToSoftTyping(methodSymbol);
        }
    }

    private List<IOverloadBindings> solveConstraints(Deque<WorklistDto> workDeque) {
        List<IOverloadBindings> solvedBindings = new ArrayList<>();

        List<IConstraint> constraints = null;
        if (!workDeque.isEmpty()) {
            WorklistDto worklistDto = workDeque.peek();
            constraints = worklistDto.constraintCollection.getConstraints();
        }

        while (!workDeque.isEmpty()) {
            WorklistDto worklistDto = workDeque.removeFirst();

            if (worklistDto.pointer < constraints.size()) {
                IConstraint constraint = constraints.get(worklistDto.pointer);
                solveConstraint(worklistDto, constraint);
            } else if (worklistDto.unsolvedConstraints == null || worklistDto.unsolvedConstraints.isEmpty()) {
                solvedBindings.add(worklistDto.overloadBindings);
            } else if (!worklistDto.isSolvingDependency) {
                constraintSolverHelper.createDependencies(worklistDto);
            }
        }

        return solvedBindings;
    }

    private void solveConstraint(WorklistDto worklistDto, IConstraint constraint) {
        IMinimalMethodSymbol refMethodSymbol = constraint.getMethodSymbol();
        if (refMethodSymbol.getOverloads().size() != 0) {
            constraintSolverHelper.solve(worklistDto, constraint);
        } else {
            //add to unresolved constraints
            if (worklistDto.unsolvedConstraints == null) {
                worklistDto.unsolvedConstraints = new ArrayList<>();
            }
            worklistDto.unsolvedConstraints.add(worklistDto.pointer);

            //proceed with the rest
            ++worklistDto.pointer;
            worklistDto.workDeque.add(worklistDto);
        }
    }
}
