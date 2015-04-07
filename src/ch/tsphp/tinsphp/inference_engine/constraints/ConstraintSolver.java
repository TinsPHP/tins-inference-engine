/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.BoundException;
import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IIntersectionConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.IReadOnlyConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.LowerBoundException;
import ch.tsphp.tinsphp.common.symbols.IFunctionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.symbols.constraints.TypeConstraint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstraintSolver implements IConstraintSolver
{
    private final ISymbolFactory symbolFactory;
    private final IOverloadResolver overloadResolver;
    Deque<ConstraintSolverDto> workDeque = new ArrayDeque<>();
    List<IBinding> solvedBindings = new ArrayList<>();

    public ConstraintSolver(
            ISymbolFactory theSymbolFactory,
            IOverloadResolver theOverloadResolver) {
        symbolFactory = theSymbolFactory;
        overloadResolver = theOverloadResolver;
    }

    public List<IBinding> solveConstraints(IReadOnlyConstraintCollection collection) {

        IBinding binding = new Binding(overloadResolver);
        workDeque.add(new ConstraintSolverDto(0, binding));
        List<IIntersectionConstraint> lowerBoundConstraints = collection.getLowerBoundConstraints();

        while (!workDeque.isEmpty()) {
            ConstraintSolverDto constraintSolver3Dto = workDeque.removeFirst();
            if (constraintSolver3Dto.pointer < lowerBoundConstraints.size()) {
                IIntersectionConstraint constraint = lowerBoundConstraints.get(constraintSolver3Dto.pointer);
                solve(constraintSolver3Dto, constraint);
            } else {
                solvedBindings.add(constraintSolver3Dto.binding);
            }
        }

        if (solvedBindings.size() == 0) {
            //TODO error case if no overload could be found
        }
        return solvedBindings;
    }

    private void solve(ConstraintSolverDto constraintSolver3Dto, IIntersectionConstraint constraint) {
        createBindingIfNecessary(constraintSolver3Dto.binding, constraint.getLeftHandSide());
        for (IVariable argument : constraint.getArguments()) {
            createBindingIfNecessary(constraintSolver3Dto.binding, argument);
        }

        for (IFunctionTypeSymbol overload : constraint.getOverloads()) {
            solveOverLoad(constraintSolver3Dto, constraint, overload);
        }
    }

    private void createBindingIfNecessary(IBinding binding, IVariable variable) {
        String absoluteName = variable.getAbsoluteName();
        Map<String, String> bindings = binding.getVariable2TypeVariable();
        if (!bindings.containsKey(absoluteName)) {
            String typeVariableName = binding.getNextTypeVariable();
            bindings.put(absoluteName, typeVariableName);
            //if it is a literal then the type variable is already set in stone and will not change anymore
            ITypeSymbol typeSymbol = variable.getType();
            if (typeSymbol != null) {
                try {
                    binding.getTypeVariables().addLowerBound(typeVariableName, new TypeConstraint(typeSymbol));
                } catch (LowerBoundException e) {
                    //should not happen, if it does, then throw exception
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private void solveOverLoad(ConstraintSolverDto constraintSolver3Dto, IIntersectionConstraint constraint,
            IFunctionTypeSymbol overload) {
        List<IVariable> arguments = constraint.getArguments();

        int numberOfArguments = arguments.size();
        if (numberOfArguments >= overload.getNumberOfNonOptionalParameters()) {

            Binding binding = new Binding(overloadResolver, (Binding) constraintSolver3Dto.binding);

            try {
                List<String> parameterTypeVariables = overload.getParameterTypeVariables();
                int numberOfParameters = parameterTypeVariables.size();
                Map<String, String> mapping = new HashMap<>(numberOfParameters + 1);
                String rhsTypeVariable = overload.getReturnTypeVariable();
                IVariable leftHandSide = constraint.getLeftHandSide();
                mergeTypeVariables(binding, overload, mapping, leftHandSide, rhsTypeVariable);

                int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;
                for (int i = 0; i < count; ++i) {
                    IVariable argument = arguments.get(i);
                    String parameterTypeVariable = parameterTypeVariables.get(i);
                    mergeTypeVariables(binding, overload, mapping, argument, parameterTypeVariable);
                }

                workDeque.add(new ConstraintSolverDto(constraintSolver3Dto.pointer + 1, binding));

            } catch (BoundException ex) {
                //That is ok, we will deal with it in solveConstraints
            }
        }
    }

    private void mergeTypeVariables(
            IBinding binding,
            IFunctionTypeSymbol overload,
            Map<String, String> mapping,
            IVariable bindingVariable,
            String overloadTypeVariable) throws BoundException {
        String bindingVariableName = bindingVariable.getAbsoluteName();
        String bindingTypeVariable = binding.getVariable2TypeVariable().get(bindingVariableName);
        ITypeVariableCollection collection = binding.getTypeVariables();

        String lhsTypeVariable;
        if (mapping.containsKey(overloadTypeVariable)) {
            lhsTypeVariable = mapping.get(overloadTypeVariable);
            binding.getVariable2TypeVariable().put(bindingVariableName, lhsTypeVariable);
            addRightToLeft(collection, lhsTypeVariable, collection, bindingTypeVariable);
        } else {
            lhsTypeVariable = bindingTypeVariable;
            mapping.put(overloadTypeVariable, lhsTypeVariable);
        }

        addRightToLeft(collection, lhsTypeVariable, overload.getTypeVariables(), overloadTypeVariable);
    }

    private void addRightToLeft(
            ITypeVariableCollection leftCollection, String left,
            ITypeVariableCollection rightCollection, String right) throws BoundException {
        if (rightCollection.hasUpperBounds(right)) {
            for (IConstraint upperBound : rightCollection.getUpperBounds(right)) {
                leftCollection.addUpperBound(left, upperBound);
            }
        }
        if (rightCollection.hasLowerBounds(right)) {
            for (IConstraint lowerBound : rightCollection.getLowerBounds(right)) {
                leftCollection.addLowerBound(left, lowerBound);
            }
        }
    }


}
