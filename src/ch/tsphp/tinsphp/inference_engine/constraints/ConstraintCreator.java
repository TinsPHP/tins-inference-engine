/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCreator;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.TypeVariableReference;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.utils.IOverloadResolver;
import ch.tsphp.tinsphp.symbols.TypeVariableNames;
import ch.tsphp.tinsphp.symbols.constraints.Constraint;
import ch.tsphp.tinsphp.symbols.constraints.OverloadBindings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConstraintCreator implements IConstraintCreator
{
    private final ISymbolFactory symbolFactory;
    private final IInferenceIssueReporter inferenceIssueReporter;
    private final IMinimalMethodSymbol assignFunction;


    public ConstraintCreator(
            ISymbolFactory theSymbolFactory,
            IOverloadResolver theOverloadResolver,
            IInferenceIssueReporter theInferenceErrorReporter) {
        symbolFactory = theSymbolFactory;
        inferenceIssueReporter = theInferenceErrorReporter;

        //Tlhs x Trhs -> Tlhs \ Tlhs > Trhs
        String tLhs = "Tlhs";
        String tRhs = "Trhs";
        IVariable lhs = symbolFactory.createVariable("$lhs", tLhs);
        IVariable rhs = symbolFactory.createVariable("$rhs", tRhs);
        IVariable rtn = symbolFactory.createVariable(TypeVariableNames.RETURN_VARIABLE_NAME, tLhs);
        IOverloadBindings overloadBindings = new OverloadBindings(theSymbolFactory, theOverloadResolver);
        overloadBindings.addVariable("$lhs", new TypeVariableReference(tLhs));
        overloadBindings.addVariable("$rhs", new TypeVariableReference(tRhs));
        overloadBindings.addVariable(TypeVariableNames.RETURN_VARIABLE_NAME, new TypeVariableReference(tLhs));
        overloadBindings.addLowerRefBound(tLhs, new TypeVariableReference(tRhs));
        IFunctionType identityOverload = symbolFactory.createFunctionType(
                "=", overloadBindings, Arrays.asList(lhs, rhs), rtn);
        assignFunction = symbolFactory.createMinimalMethodSymbol("=");
        assignFunction.addOverload(identityOverload);
    }

    @Override
    public void createTypeConstraint(ITSPHPAst literal) {
        IMinimalVariableSymbol typeVariableSymbol = symbolFactory.createExpressionTypeVariableSymbol(literal);
        typeVariableSymbol.setType(literal.getEvalType());
        typeVariableSymbol.setHasFixedType();
        literal.setSymbol(typeVariableSymbol);
    }

    @Override
    public void createRefConstraint(IConstraintCollection collection, ITSPHPAst identifier, ITSPHPAst rhs) {
        IVariable variableSymbol = (IVariable) identifier.getSymbol();
        List<IVariable> parameters = Arrays.asList(variableSymbol, (IVariable) rhs.getSymbol());
        IConstraint constraint = new Constraint(identifier, variableSymbol, parameters, assignFunction);
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
        IMinimalVariableSymbol expressionVariable = symbolFactory.createExpressionTypeVariableSymbol(identifierAst);
        expressionVariable.setDefinitionScope(identifierAst.getScope());
        IConstraint constraint = new Constraint(parentAst, expressionVariable, typeVariables, methodSymbol);
        collection.addConstraint(constraint);
        parentAst.setSymbol(expressionVariable);
    }

    @Override
    public void createFunctionCallConstraint(
            IConstraintCollection collection, ITSPHPAst functionCall, ITSPHPAst identifier, ITSPHPAst argumentList) {
        int size = argumentList.getChildCount();
        List<IVariable> typeVariables = new ArrayList<>(size);
        if (size > 0) {
            for (ITSPHPAst argument : argumentList.getChildren()) {
                typeVariables.add((IVariable) argument.getSymbol());
            }
        }

        IMinimalMethodSymbol methodSymbol = (IMinimalMethodSymbol) identifier.getSymbol();
        createConstraint(collection, functionCall, identifier, typeVariables, methodSymbol);
    }
}
