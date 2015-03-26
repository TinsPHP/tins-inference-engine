/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine;


import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.inference.IInferencePhaseController;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IOverloadSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.IntersectionConstraint;
import ch.tsphp.tinsphp.symbols.constraints.TransferConstraint;

import java.util.ArrayList;
import java.util.List;

public class InferencePhaseController implements IInferencePhaseController
{
    private final ISymbolFactory symbolFactory;
    private final IInferenceIssueReporter inferenceIssueReporter;
    private final IConstraintSolver constraintSolver;
    private final List<IMethodSymbol> functionScopes = new ArrayList<>();
    private final IGlobalNamespaceScope globalDefaultNamespaceScope;


    public InferencePhaseController(
            ISymbolFactory theSymbolFactory,
            IInferenceIssueReporter theInferenceErrorReporter,
            IConstraintSolver theConstraintSolver,
            IGlobalNamespaceScope theGlobalDefaultNamespaceScope) {
        symbolFactory = theSymbolFactory;
        inferenceIssueReporter = theInferenceErrorReporter;
        constraintSolver = theConstraintSolver;
        globalDefaultNamespaceScope = theGlobalDefaultNamespaceScope;
    }

    @Override
    public void createRefVariable(ITypeVariableCollection collection, ITSPHPAst variableId) {
        IVariableSymbol variableSymbol = (IVariableSymbol) variableId.getSymbol();
        ITypeVariableSymbol currentVariableSymbol = variableSymbol.getCurrentTypeVariable();

        variableId.setText(variableId.getText() + variableId.getLine() + "|" + variableId.getCharPositionInLine());
        IVariableSymbol refVariableSymbol = symbolFactory.createVariableSymbol(null, variableId);
        variableId.setText(variableSymbol.getName());
        refVariableSymbol.setType(symbolFactory.createUnionTypeSymbol());
        refVariableSymbol.setDefinitionScope(variableId.getScope());
        //TODO not sure if this bidirectional relationship is necessary
        refVariableSymbol.setPreviousTypeVariable(currentVariableSymbol);
        refVariableSymbol.setConstraint(new TransferConstraint(currentVariableSymbol));
        variableId.setSymbol(refVariableSymbol);

        variableSymbol.addRefVariable(refVariableSymbol);
        collection.addTypeVariable(refVariableSymbol);
    }

    @Override
    public void createTypeConstraint(ITSPHPAst literal) {
        ITypeVariableSymbol typeVariableSymbol = symbolFactory.createExpressionTypeVariableSymbol(literal);
        typeVariableSymbol.setType(literal.getEvalType());
        literal.setSymbol(typeVariableSymbol);
    }

    @Override
    public void createRefConstraint(ITypeVariableCollection collection, ITSPHPAst identifier, ITSPHPAst rhs) {
        IVariableSymbol variableSymbol = (IVariableSymbol) identifier.getSymbol();
        variableSymbol.setConstraint((ITypeVariableSymbol) rhs.getSymbol());
        collection.addTypeVariable(variableSymbol);
    }

    @Override
    public void createIntersectionConstraint(
            ITypeVariableCollection collection, ITSPHPAst operator, ITSPHPAst... arguments) {
        List<ITypeVariableSymbol> typeVariables = new ArrayList<>(arguments.length);
        for (ITSPHPAst argument : arguments) {
            typeVariables.add((ITypeVariableSymbol) argument.getSymbol());
        }
        IOverloadSymbol overloadSymbol = (IOverloadSymbol) operator.getSymbol();
        ITypeVariableSymbol typeVariableSymbol = symbolFactory.createExpressionTypeVariableSymbol(operator);
        typeVariableSymbol.setType(symbolFactory.createUnionTypeSymbol());
        typeVariableSymbol.setConstraint(new IntersectionConstraint(typeVariables, overloadSymbol.getOverloads()));
        operator.setSymbol(typeVariableSymbol);
        collection.addTypeVariable(typeVariableSymbol);
    }

    @Override
    public void addTypeVariableCollection(IMethodSymbol scope) {
        functionScopes.add(scope);
    }

    @Override
    public void solveAllConstraints() {
        for (ITypeVariableCollection scope : functionScopes) {
            constraintSolver.solveConstraints(scope);
        }

        constraintSolver.solveConstraints(globalDefaultNamespaceScope);
    }
}
