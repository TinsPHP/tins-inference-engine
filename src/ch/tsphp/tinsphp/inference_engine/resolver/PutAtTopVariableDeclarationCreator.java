/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.resolver;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.gen.TokenTypes;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.antlr.runtime.Token;

public class PutAtTopVariableDeclarationCreator implements IVariableDeclarationCreator
{
    private final ISymbolFactory symbolFactory;
    private final IAstModificationHelper astModificationHelper;
    private final IDefinitionPhaseController definitionPhaseController;
    private final IGlobalNamespaceScope globalDefaultNamespaceScope;

    public PutAtTopVariableDeclarationCreator(
            ISymbolFactory theSymbolFactory,
            IAstModificationHelper theAstModificationHelper,
            IDefinitionPhaseController theDefinitionPhaseController) {

        symbolFactory = theSymbolFactory;
        astModificationHelper = theAstModificationHelper;
        definitionPhaseController = theDefinitionPhaseController;
        globalDefaultNamespaceScope = definitionPhaseController.getGlobalDefaultNamespace();
    }

    @Override
    public IVariableSymbol create(ITSPHPAst variableId) {
        ITSPHPAst parent = (ITSPHPAst) variableId.getParent();
        while (isNotFunctionMethodOrNamespace(parent)) {
            parent = (ITSPHPAst) parent.getParent();
        }

        IVariableSymbol symbol;
        if (parent.getType() == TokenTypes.Namespace) {
            symbol = createVariableInNamespaceScope(parent, variableId);
            symbol.setDefinitionScope(parent.getScope());
        } else {
            symbol = createVariableInMethodScope(parent, variableId);
        }

        return symbol;
    }

    private boolean isNotFunctionMethodOrNamespace(ITSPHPAst parent) {
        int type = parent.getType();
        return type != TokenTypes.Function && type != TokenTypes.METHOD_DECLARATION && type != TokenTypes.Namespace;
    }

    private IVariableSymbol createVariableInNamespaceScope(ITSPHPAst namespace, ITSPHPAst variableId) {
        return createVariableAtTop(namespace.getChild(1), globalDefaultNamespaceScope, variableId);
    }

    private IVariableSymbol createVariableInMethodScope(ITSPHPAst functionOrMethod, ITSPHPAst variableId) {
        return createVariableAtTop(functionOrMethod.getChild(4), functionOrMethod.getScope(), variableId);
    }

    private IVariableSymbol createVariableAtTop(ITSPHPAst block, IScope scope, ITSPHPAst variableId) {
        ITSPHPAst variableDeclarationList = astModificationHelper.getVariableDeclaration(variableId.getText());
        ITSPHPAst variable = variableDeclarationList.getChild(1);
        Token token = variable.getToken();
        ITSPHPAst firstChild = block.getChild(0);
        token.setLine(firstChild.getLine());
        token.setCharPositionInLine(firstChild.getCharPositionInLine());
        insertVariableDeclarationList(block, variableDeclarationList);
        return definitionPhaseController.defineVariable(
                scope,
                null,
                variableDeclarationList.getChild(0),
                variable);
    }

    protected void insertVariableDeclarationList(ITSPHPAst block, ITSPHPAst variableDeclarationList) {
        astModificationHelper.insertChildAt(block, variableDeclarationList, 0);
    }

}
