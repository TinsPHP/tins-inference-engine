/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.TinsPHPConstants;
import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCreator;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.TypeVariableReference;
import ch.tsphp.tinsphp.common.symbols.IExpressionVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConstraintCreator implements IConstraintCreator
{
    private final ISymbolFactory symbolFactory;
    private final IMinimalMethodSymbol assignFunction;


    public ConstraintCreator(ISymbolFactory theSymbolFactory) {
        symbolFactory = theSymbolFactory;

        //Tlhs x Trhs -> Tlhs \ Trhs <: Tlhs
        String tLhs = "Tlhs";
        String tRhs = "Trhs";
        String varLhs = "$lhs";
        String varRhs = "$rhs";
        IVariable lhs = symbolFactory.createVariable(varLhs);
        IVariable rhs = symbolFactory.createVariable(varRhs);
        IBindingCollection bindingCollection = symbolFactory.createBindingCollection();
        bindingCollection.addVariable(varLhs, new TypeVariableReference(tLhs));
        bindingCollection.addVariable(varRhs, new TypeVariableReference(tRhs));
        bindingCollection.addVariable(TinsPHPConstants.RETURN_VARIABLE_NAME, new TypeVariableReference(tLhs));
        bindingCollection.addLowerRefBound(tLhs, new TypeVariableReference(tRhs));
        IFunctionType identityOverload = symbolFactory.createFunctionType(
                "=", bindingCollection, Arrays.asList(lhs, rhs));
        Set<String> nonFixedTypeParameters = new HashSet<>();
        nonFixedTypeParameters.add("Tlhs");
        nonFixedTypeParameters.add("Trhs");
        identityOverload.manuallySimplified(nonFixedTypeParameters, 0, false);
        assignFunction = symbolFactory.createMinimalMethodSymbol("=");
        List<IFunctionType> overloads = new ArrayList<>(1);
        overloads.add(identityOverload);
        assignFunction.setOverloads(overloads);
    }

    @Override
    public void createTypeConstraint(ITSPHPAst literal) {
        IMinimalVariableSymbol typeVariableSymbol = symbolFactory.createExpressionVariableSymbol(literal);
        typeVariableSymbol.setType(literal.getEvalType());
        literal.setSymbol(typeVariableSymbol);
    }

    @Override
    public void createRefConstraint(IConstraintCollection collection, ITSPHPAst identifier, ITSPHPAst rhs) {
        IVariable variableSymbol = (IVariable) identifier.getSymbol();
        List<IVariable> parameters = Arrays.asList(variableSymbol, (IVariable) rhs.getSymbol());
        IConstraint constraint = symbolFactory.createConstraint(identifier, variableSymbol, parameters, assignFunction);
        collection.addConstraint(constraint);
    }

    @Override
    public void createOperatorConstraint(IConstraintCollection collection, ITSPHPAst operator, ITSPHPAst... arguments) {
        List<IVariable> typeVariables = new ArrayList<>(arguments.length);
        for (ITSPHPAst argument : arguments) {
            typeVariables.add((IVariable) argument.getSymbol());
        }

        IMinimalMethodSymbol methodSymbol = (IMinimalMethodSymbol) operator.getSymbol();
        createConstraint(collection, operator, operator, typeVariables, methodSymbol);
    }

    private void createConstraint(
            IConstraintCollection collection,
            ITSPHPAst parentAst,
            ITSPHPAst identifierAst,
            List<IVariable> typeVariables,
            IMinimalMethodSymbol methodSymbol) {
        IExpressionVariableSymbol expressionVariable = createExpressionVariable(parentAst, identifierAst);
        expressionVariable.setMethodSymbol(methodSymbol);
        IConstraint constraint = symbolFactory.createConstraint(
                parentAst, expressionVariable, typeVariables, methodSymbol);
        collection.addConstraint(constraint);
    }

    private IExpressionVariableSymbol createExpressionVariable(ITSPHPAst parentAst, ITSPHPAst identifierAst) {
        IExpressionVariableSymbol expressionVariable = symbolFactory.createExpressionVariableSymbol(identifierAst);
        expressionVariable.setDefinitionScope(identifierAst.getScope());
        parentAst.setSymbol(expressionVariable);
        return expressionVariable;
    }

    @Override
    public void createFunctionCallConstraint(
            IConstraintCollection collection, ITSPHPAst functionCall, ITSPHPAst identifier, ITSPHPAst argumentList) {

        IMinimalMethodSymbol methodSymbol = (IMinimalMethodSymbol) identifier.getSymbol();
        boolean isNotDirectRecursion = collection != methodSymbol;

        if (isNotDirectRecursion) {
            int size = argumentList.getChildCount();
            List<IVariable> typeVariables = new ArrayList<>(size);
            if (size > 0) {
                for (ITSPHPAst argument : argumentList.getChildren()) {
                    typeVariables.add((IVariable) argument.getSymbol());
                }
            }
            createConstraint(collection, functionCall, identifier, typeVariables, methodSymbol);
        } else {
            // direct recursion can be replaced with the assign function with expressionVariable as lhs and the return
            // variable of the method as rhs
            IMinimalVariableSymbol expressionVariable = createExpressionVariable(functionCall, identifier);
            IVariable variableSymbol = ((IMethodSymbol) methodSymbol).getReturnVariable();
            List<IVariable> arguments = Arrays.asList(expressionVariable, variableSymbol);
            IConstraint constraint = symbolFactory.createConstraint(
                    functionCall, expressionVariable, arguments, assignFunction);
            collection.addConstraint(constraint);
        }
    }
}
