/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine;


import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.inference.IInferencePhaseController;
import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IIntersectionConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IFunctionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IOverloadSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.symbols.constraints.IntersectionConstraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InferencePhaseController implements IInferencePhaseController
{
    private final ISymbolFactory symbolFactory;
    private final IInferenceIssueReporter inferenceIssueReporter;
    private final IConstraintSolver constraintSolver;
    private final List<IMethodSymbol> functionScopes = new ArrayList<>();
    private final IGlobalNamespaceScope globalDefaultNamespaceScope;
    private final IFunctionTypeSymbol assignFunction;


    public InferencePhaseController(
            ISymbolFactory theSymbolFactory,
            IInferenceIssueReporter theInferenceErrorReporter,
            IConstraintSolver theConstraintSolver,
            IGlobalNamespaceScope theGlobalDefaultNamespaceScope,
            IFunctionTypeSymbol theAssignFunction) {
        symbolFactory = theSymbolFactory;
        inferenceIssueReporter = theInferenceErrorReporter;
        constraintSolver = theConstraintSolver;
        globalDefaultNamespaceScope = theGlobalDefaultNamespaceScope;
        assignFunction = theAssignFunction;
    }

    @Override
    public void createTypeConstraint(ITSPHPAst literal) {
        ITypeVariableSymbol typeVariableSymbol = symbolFactory.createExpressionTypeVariableSymbol(literal);
        typeVariableSymbol.setType(literal.getEvalType());
        literal.setSymbol(typeVariableSymbol);
    }

    @Override
    public void createRefConstraint(IConstraintCollection collection, ITSPHPAst identifier, ITSPHPAst rhs) {
        IVariableSymbol variableSymbol = (IVariableSymbol) identifier.getSymbol();

        IIntersectionConstraint constraint = new IntersectionConstraint(
                variableSymbol,
                Arrays.asList(variableSymbol, (IVariable) rhs.getSymbol()),
                Arrays.asList(assignFunction));
        collection.addLowerBoundConstraint(constraint);
    }

    @Override
    public void createIntersectionConstraint(
            IConstraintCollection collection, ITSPHPAst operator, ITSPHPAst... arguments) {
        List<IVariable> typeVariables = new ArrayList<>(arguments.length);
        for (ITSPHPAst argument : arguments) {
            typeVariables.add((IVariable) argument.getSymbol());
        }

        IOverloadSymbol overloadSymbol = (IOverloadSymbol) operator.getSymbol();
        ITypeVariableSymbol expressionVariable = symbolFactory.createExpressionTypeVariableSymbol(operator);
        IIntersectionConstraint constraint = new IntersectionConstraint(
                expressionVariable, typeVariables, overloadSymbol.getOverloads());
        collection.addLowerBoundConstraint(constraint);
    }

    @Override
    public void addMethodSymbol(IMethodSymbol scope) {
        functionScopes.add(scope);
    }

    @Override
    public void solveAllConstraints() {
        for (IMethodSymbol scope : functionScopes) {
            List<IBinding> bindings = constraintSolver.solveConstraints(scope);
            scope.setBindings(bindings);
        }

        List<IBinding> bindings = constraintSolver.solveConstraints(globalDefaultNamespaceScope);
        globalDefaultNamespaceScope.setBindings(bindings);
    }
}
