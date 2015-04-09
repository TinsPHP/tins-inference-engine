/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IIntersectionConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.IReadOnlyConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.TypeVariableConstraint;
import ch.tsphp.tinsphp.common.symbols.IFunctionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.symbols.constraints.BoundException;
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
        Map<String, TypeVariableConstraint> bindings = binding.getVariable2TypeVariable();
        if (!bindings.containsKey(absoluteName)) {
            TypeVariableConstraint typeVariableConstraint = binding.getNextTypeVariable();
            bindings.put(absoluteName, typeVariableConstraint);
            //if it is a literal then we know already the lower bound.
            ITypeSymbol typeSymbol = variable.getType();
            if (typeSymbol != null) {
                typeVariableConstraint.setIsConstant();
                ITypeVariableCollection typeVariables = binding.getTypeVariables();
                TypeConstraint constraint = new TypeConstraint(typeSymbol);
                String typeVariable = typeVariableConstraint.getTypeVariable();
                typeVariables.addLowerBound(typeVariable, constraint);

            }
        }
    }

    private void solveOverLoad(ConstraintSolverDto constraintSolver3Dto, IIntersectionConstraint constraint,
            IFunctionTypeSymbol overload) {
        if (constraint.getArguments().size() >= overload.getNumberOfNonOptionalParameters()) {
            Binding binding = new Binding(overloadResolver, (Binding) constraintSolver3Dto.binding);
            aggregateBinding(constraintSolver3Dto, constraint, overload, binding);
        }
    }

    private void aggregateBinding(
            ConstraintSolverDto constraintSolver3Dto,
            IIntersectionConstraint constraint,
            IFunctionTypeSymbol overload,
            Binding binding) {

        List<IVariable> arguments = constraint.getArguments();
        List<String> parameterTypeVariables = overload.getParameterTypeVariables();
        int numberOfParameters = parameterTypeVariables.size();
        Map<String, TypeVariableConstraint> mapping = new HashMap<>(numberOfParameters + 1);
        int numberOfArguments = arguments.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;
        try {
            int iterateCount = 0;
            boolean needToIterateOverload = true;
            while (needToIterateOverload) {

                String rhsTypeVariable = overload.getReturnTypeVariable();
                IVariable leftHandSide = constraint.getLeftHandSide();
                needToIterateOverload = !mergeTypeVariables(binding, overload, mapping, leftHandSide, rhsTypeVariable);

                for (int i = 0; i < count; ++i) {
                    IVariable argument = arguments.get(i);
                    String parameterTypeVariable = parameterTypeVariables.get(i);
                    boolean needToIterateParameter = !mergeTypeVariables(
                            binding, overload, mapping, argument, parameterTypeVariable);
                    needToIterateOverload = needToIterateOverload || needToIterateParameter;
                }

                if (iterateCount > 1) {
                    throw new IllegalStateException("overload uses type variables "
                            + "which are not part of the signature.");
                }
                ++iterateCount;
            }
            workDeque.add(new ConstraintSolverDto(constraintSolver3Dto.pointer + 1, binding));
        } catch (BoundException ex) {
            //That is ok, we will deal with it in solveConstraints
        }
    }

    private boolean mergeTypeVariables(
            IBinding binding,
            IFunctionTypeSymbol overload,
            Map<String, TypeVariableConstraint> mapping,
            IVariable bindingVariable,
            String overloadTypeVariable) throws BoundException {
        ITypeVariableCollection bindingTypeVariables = binding.getTypeVariables();
        String bindingVariableName = bindingVariable.getAbsoluteName();
        TypeVariableConstraint bindingTypeVariableConstraint
                = binding.getVariable2TypeVariable().get(bindingVariableName);

        boolean canMergeBinding;

        String lhsTypeVariable;
        if (mapping.containsKey(overloadTypeVariable)) {
            lhsTypeVariable = mapping.get(overloadTypeVariable).getTypeVariable();
            String rhsTypeVariable = bindingTypeVariableConstraint.getTypeVariable();
            canMergeBinding = lhsTypeVariable.equals(rhsTypeVariable);
            if (!canMergeBinding) {
                canMergeBinding = addRightToLeft(
                        mapping, bindingTypeVariables, lhsTypeVariable, bindingTypeVariables, rhsTypeVariable);
                if (canMergeBinding) {
                    bindingTypeVariableConstraint.setTypeVariable(lhsTypeVariable);
                }
                //TODO remove lower and upper bounds of unused type variable
            }
        } else {
            lhsTypeVariable = bindingTypeVariableConstraint.getTypeVariable();
            mapping.put(overloadTypeVariable, bindingTypeVariableConstraint);
            canMergeBinding = true;
        }

        ITypeVariableCollection overloadTypeVariables = overload.getTypeVariables();
        boolean canMergeOverload = addRightToLeft(
                mapping, bindingTypeVariables, lhsTypeVariable, overloadTypeVariables, overloadTypeVariable);

        return canMergeBinding && canMergeOverload;
    }

    private boolean addRightToLeft(
            Map<String, TypeVariableConstraint> mapping,
            ITypeVariableCollection leftCollection, String left,
            ITypeVariableCollection rightCollection, String right) throws BoundException {

        if (rightCollection.hasUpperBounds(right)) {
            for (IConstraint upperBound : rightCollection.getUpperBounds(right)) {
                //upper bounds do not have TypeVariableConstraint
                leftCollection.addUpperBound(left, upperBound);
            }
        }

        boolean couldAddLower = true;
        if (rightCollection.hasLowerBounds(right)) {
            for (IConstraint lowerBound : rightCollection.getLowerBounds(right)) {
                if (lowerBound instanceof TypeConstraint) {
                    leftCollection.addLowerBound(left, lowerBound);
                } else if (lowerBound instanceof TypeVariableConstraint) {
                    String typeVariable = ((TypeVariableConstraint) lowerBound).getTypeVariable();
                    if (typeVariable.charAt(0) != Binding.TYPE_VARIABLE_PREFIX) {
                        if (mapping.containsKey(typeVariable)) {
                            leftCollection.addLowerBound(left, mapping.get(typeVariable));
                        } else {
                            couldAddLower = false;
                            break;
                        }
                    } else {
                        leftCollection.addLowerBound(left, lowerBound);
                    }
                } else {
                    throw new UnsupportedOperationException(lowerBound.getClass().getName() + " not supported.");
                }
            }
        }

        return couldAddLower;
    }
}
