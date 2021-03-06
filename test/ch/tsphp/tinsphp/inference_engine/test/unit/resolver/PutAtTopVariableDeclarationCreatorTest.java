/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.resolver;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.gen.TokenTypes;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.resolver.PutAtTopVariableDeclarationCreator;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.antlr.runtime.Token;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PutAtTopVariableDeclarationCreatorTest
{
    @Test
    public void create_InFunction_DelegateToAstModificationHelper() {
        ITSPHPAst function = createAst(TokenTypes.Function, null);
        IMethodSymbol scope = mock(IMethodSymbol.class);
        when(function.getScope()).thenReturn(scope);
        ITSPHPAst block = createAst(TokenTypes.BLOCK, function);
        when(function.getChild(4)).thenReturn(block);
        ITSPHPAst ast = createAst(TokenTypes.VariableId, block);
        String variableId = "$a";
        when(ast.getText()).thenReturn(variableId);

        //variableDeclarationList
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst variableDeclarationList = mock(ITSPHPAst.class);
        when(astModificationHelper.getVariableDeclaration(variableId)).thenReturn(variableDeclarationList);
        ITSPHPAst type = createAst(TokenTypes.TYPE, variableDeclarationList);
        when(variableDeclarationList.getChild(0)).thenReturn(type);
        ITSPHPAst variable = createAst(TokenTypes.VariableId, variableDeclarationList);
        when(variableDeclarationList.getChild(1)).thenReturn(variable);

        //transfer line and position
        ITSPHPAst firstChild = mock(ITSPHPAst.class);
        when(block.getChild(0)).thenReturn(firstChild);
        Token token = mock(Token.class);
        when(variable.getToken()).thenReturn(token);

        //VariableSymbol
        IDefinitionPhaseController definitionPhaseController = mock(IDefinitionPhaseController.class);
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
        when(definitionPhaseController.defineVariable(
                any(IScope.class), any(ITSPHPAst.class), any(ITSPHPAst.class), any(ITSPHPAst.class)))
                .thenReturn(variableSymbol);


        //act
        IVariableDeclarationCreator variableDeclarationCreator
                = createVariableDeclarationCreator(astModificationHelper, definitionPhaseController);
        variableDeclarationCreator.create(ast);


        verify(astModificationHelper).getVariableDeclaration(variableId);
    }

    @Test
    public void create_InFunction_DelegateToDefinitionPhaseControllerAndReturnsCorrespondingSymbol() {
        ITSPHPAst function = createAst(TokenTypes.Function, null);
        IMethodSymbol scope = mock(IMethodSymbol.class);
        when(function.getScope()).thenReturn(scope);
        ITSPHPAst block = createAst(TokenTypes.BLOCK, function);
        when(function.getChild(4)).thenReturn(block);
        ITSPHPAst ast = createAst(TokenTypes.VariableId, block);
        String variableId = "$a";
        when(ast.getText()).thenReturn(variableId);

        //variableDeclarationList
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst variableDeclarationList = mock(ITSPHPAst.class);
        when(astModificationHelper.getVariableDeclaration(variableId)).thenReturn(variableDeclarationList);
        ITSPHPAst type = createAst(TokenTypes.TYPE, variableDeclarationList);
        when(variableDeclarationList.getChild(0)).thenReturn(type);
        ITSPHPAst variable = createAst(TokenTypes.VariableId, variableDeclarationList);
        when(variableDeclarationList.getChild(1)).thenReturn(variable);

        //transfer line and position
        ITSPHPAst firstChild = mock(ITSPHPAst.class);
        when(block.getChild(0)).thenReturn(firstChild);
        Token token = mock(Token.class);
        when(variable.getToken()).thenReturn(token);

        //VariableSymbol
        IDefinitionPhaseController definitionPhaseController = mock(IDefinitionPhaseController.class);
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
        when(definitionPhaseController.defineVariable(
                any(IScope.class), any(ITSPHPAst.class), any(ITSPHPAst.class), any(ITSPHPAst.class)))
                .thenReturn(variableSymbol);


        //act
        IVariableDeclarationCreator variableDeclarationCreator
                = createVariableDeclarationCreator(astModificationHelper, definitionPhaseController);
        IVariableSymbol result = variableDeclarationCreator.create(ast);


        verify(definitionPhaseController).defineVariable(scope, null, type, variable);
        assertThat(result, is(variableSymbol));
    }

    @Test
    public void create_InFunction_CreatesAsFirstElementInBlock() {
        ITSPHPAst function = createAst(TokenTypes.Function, null);
        IMethodSymbol scope = mock(IMethodSymbol.class);
        when(function.getScope()).thenReturn(scope);
        ITSPHPAst block = createAst(TokenTypes.BLOCK, function);
        when(function.getChild(4)).thenReturn(block);
        ITSPHPAst ast = createAst(TokenTypes.VariableId, block);
        String variableId = "$a";
        when(ast.getText()).thenReturn(variableId);

        //variableDeclarationList
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst variableDeclarationList = mock(ITSPHPAst.class);
        when(astModificationHelper.getVariableDeclaration(variableId)).thenReturn(variableDeclarationList);
        ITSPHPAst type = createAst(TokenTypes.TYPE, variableDeclarationList);
        when(variableDeclarationList.getChild(0)).thenReturn(type);
        ITSPHPAst variable = createAst(TokenTypes.VariableId, variableDeclarationList);
        when(variableDeclarationList.getChild(1)).thenReturn(variable);

        //transfer line and position
        ITSPHPAst firstChild = mock(ITSPHPAst.class);
        when(block.getChild(0)).thenReturn(firstChild);
        Token token = mock(Token.class);
        when(variable.getToken()).thenReturn(token);

        //VariableSymbol
        IDefinitionPhaseController definitionPhaseController = mock(IDefinitionPhaseController.class);
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
        when(definitionPhaseController.defineVariable(
                any(IScope.class), any(ITSPHPAst.class), any(ITSPHPAst.class), any(ITSPHPAst.class)))
                .thenReturn(variableSymbol);


        //act
        IVariableDeclarationCreator variableDeclarationCreator
                = createVariableDeclarationCreator(astModificationHelper, definitionPhaseController);
        variableDeclarationCreator.create(ast);


        verify(astModificationHelper).insertChildAt(block, variableDeclarationList, 0);
    }

    @Test
    public void create_InFunction_TransfersLineAndPositionsFromFirstChildToVariableDeclarationList() {
        ITSPHPAst function = createAst(TokenTypes.Function, null);
        IMethodSymbol scope = mock(IMethodSymbol.class);
        when(function.getScope()).thenReturn(scope);
        ITSPHPAst block = createAst(TokenTypes.BLOCK, function);
        when(function.getChild(4)).thenReturn(block);
        ITSPHPAst ast = createAst(TokenTypes.VariableId, block);
        String variableId = "$a";
        when(ast.getText()).thenReturn(variableId);

        //variableDeclarationList
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst variableDeclarationList = mock(ITSPHPAst.class);
        when(astModificationHelper.getVariableDeclaration(variableId)).thenReturn(variableDeclarationList);
        ITSPHPAst type = createAst(TokenTypes.TYPE, variableDeclarationList);
        when(variableDeclarationList.getChild(0)).thenReturn(type);
        ITSPHPAst variable = createAst(TokenTypes.VariableId, variableDeclarationList);
        when(variableDeclarationList.getChild(1)).thenReturn(variable);

        //transfer line and position
        ITSPHPAst firstChild = mock(ITSPHPAst.class);
        when(block.getChild(0)).thenReturn(firstChild);
        int line = 12;
        when(firstChild.getLine()).thenReturn(line);
        int position = 98;
        when(firstChild.getCharPositionInLine()).thenReturn(position);
        Token token = mock(Token.class);
        when(variable.getToken()).thenReturn(token);

        //VariableSymbol
        IDefinitionPhaseController definitionPhaseController = mock(IDefinitionPhaseController.class);
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
        when(definitionPhaseController.defineVariable(
                any(IScope.class), any(ITSPHPAst.class), any(ITSPHPAst.class), any(ITSPHPAst.class)))
                .thenReturn(variableSymbol);


        //act
        IVariableDeclarationCreator variableDeclarationCreator
                = createVariableDeclarationCreator(astModificationHelper, definitionPhaseController);
        variableDeclarationCreator.create(ast);

        verify(firstChild).getLine();
        verify(firstChild).getCharPositionInLine();
        verify(token).setLine(line);
        verify(token).setCharPositionInLine(position);
    }

    @Test
    public void create_InMethod_DelegateToAstModificationHelper() {
        ITSPHPAst function = createAst(TokenTypes.METHOD_DECLARATION, null);
        IMethodSymbol scope = mock(IMethodSymbol.class);
        when(function.getScope()).thenReturn(scope);
        ITSPHPAst block = createAst(TokenTypes.BLOCK, function);
        when(function.getChild(4)).thenReturn(block);
        ITSPHPAst ast = createAst(TokenTypes.VariableId, block);
        String variableId = "$a";
        when(ast.getText()).thenReturn(variableId);

        //variableDeclarationList
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst variableDeclarationList = mock(ITSPHPAst.class);
        when(astModificationHelper.getVariableDeclaration(variableId)).thenReturn(variableDeclarationList);
        ITSPHPAst type = createAst(TokenTypes.TYPE, variableDeclarationList);
        when(variableDeclarationList.getChild(0)).thenReturn(type);
        ITSPHPAst variable = createAst(TokenTypes.VariableId, variableDeclarationList);
        when(variableDeclarationList.getChild(1)).thenReturn(variable);

        //transfer line and position
        ITSPHPAst firstChild = mock(ITSPHPAst.class);
        when(block.getChild(0)).thenReturn(firstChild);
        Token token = mock(Token.class);
        when(variable.getToken()).thenReturn(token);

        //VariableSymbol
        IDefinitionPhaseController definitionPhaseController = mock(IDefinitionPhaseController.class);
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
        when(definitionPhaseController.defineVariable(
                any(IScope.class), any(ITSPHPAst.class), any(ITSPHPAst.class), any(ITSPHPAst.class)))
                .thenReturn(variableSymbol);


        //act
        IVariableDeclarationCreator variableDeclarationCreator
                = createVariableDeclarationCreator(astModificationHelper, definitionPhaseController);
        variableDeclarationCreator.create(ast);


        verify(astModificationHelper).getVariableDeclaration(variableId);
    }

    @Test
    public void create_InMethod_DelegateToDefinitionPhaseControllerAndReturnsCorrespondingSymbol() {
        ITSPHPAst function = createAst(TokenTypes.METHOD_DECLARATION, null);
        IMethodSymbol scope = mock(IMethodSymbol.class);
        when(function.getScope()).thenReturn(scope);
        ITSPHPAst block = createAst(TokenTypes.BLOCK, function);
        when(function.getChild(4)).thenReturn(block);
        ITSPHPAst ast = createAst(TokenTypes.VariableId, block);
        String variableId = "$a";
        when(ast.getText()).thenReturn(variableId);

        //variableDeclarationList
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst variableDeclarationList = mock(ITSPHPAst.class);
        when(astModificationHelper.getVariableDeclaration(variableId)).thenReturn(variableDeclarationList);
        ITSPHPAst type = createAst(TokenTypes.TYPE, variableDeclarationList);
        when(variableDeclarationList.getChild(0)).thenReturn(type);
        ITSPHPAst variable = createAst(TokenTypes.VariableId, variableDeclarationList);
        when(variableDeclarationList.getChild(1)).thenReturn(variable);

        //transfer line and position
        ITSPHPAst firstChild = mock(ITSPHPAst.class);
        when(block.getChild(0)).thenReturn(firstChild);
        Token token = mock(Token.class);
        when(variable.getToken()).thenReturn(token);

        //VariableSymbol
        IDefinitionPhaseController definitionPhaseController = mock(IDefinitionPhaseController.class);
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
        when(definitionPhaseController.defineVariable(
                any(IScope.class), any(ITSPHPAst.class), any(ITSPHPAst.class), any(ITSPHPAst.class)))
                .thenReturn(variableSymbol);


        //act
        IVariableDeclarationCreator variableDeclarationCreator
                = createVariableDeclarationCreator(astModificationHelper, definitionPhaseController);
        IVariableSymbol result = variableDeclarationCreator.create(ast);


        verify(definitionPhaseController).defineVariable(scope, null, type, variable);
        assertThat(result, is(variableSymbol));
    }

    @Test
    public void create_InMethod_CreatesAsFirstElementInBlock() {
        ITSPHPAst function = createAst(TokenTypes.METHOD_DECLARATION, null);
        IMethodSymbol scope = mock(IMethodSymbol.class);
        when(function.getScope()).thenReturn(scope);
        ITSPHPAst block = createAst(TokenTypes.BLOCK, function);
        when(function.getChild(4)).thenReturn(block);
        ITSPHPAst ast = createAst(TokenTypes.VariableId, block);
        String variableId = "$a";
        when(ast.getText()).thenReturn(variableId);

        //variableDeclarationList
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst variableDeclarationList = mock(ITSPHPAst.class);
        when(astModificationHelper.getVariableDeclaration(variableId)).thenReturn(variableDeclarationList);
        ITSPHPAst type = createAst(TokenTypes.TYPE, variableDeclarationList);
        when(variableDeclarationList.getChild(0)).thenReturn(type);
        ITSPHPAst variable = createAst(TokenTypes.VariableId, variableDeclarationList);
        when(variableDeclarationList.getChild(1)).thenReturn(variable);

        //transfer line and position
        ITSPHPAst firstChild = mock(ITSPHPAst.class);
        when(block.getChild(0)).thenReturn(firstChild);
        Token token = mock(Token.class);
        when(variable.getToken()).thenReturn(token);

        //VariableSymbol
        IDefinitionPhaseController definitionPhaseController = mock(IDefinitionPhaseController.class);
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
        when(definitionPhaseController.defineVariable(
                any(IScope.class), any(ITSPHPAst.class), any(ITSPHPAst.class), any(ITSPHPAst.class)))
                .thenReturn(variableSymbol);


        //act
        IVariableDeclarationCreator variableDeclarationCreator
                = createVariableDeclarationCreator(astModificationHelper, definitionPhaseController);
        variableDeclarationCreator.create(ast);


        verify(astModificationHelper).insertChildAt(block, variableDeclarationList, 0);
    }

    @Test
    public void create_InMethod_TransfersLineAndPositionsFromFirstChildToVariableDeclarationList() {
        ITSPHPAst function = createAst(TokenTypes.METHOD_DECLARATION, null);
        IMethodSymbol scope = mock(IMethodSymbol.class);
        when(function.getScope()).thenReturn(scope);
        ITSPHPAst block = createAst(TokenTypes.BLOCK, function);
        when(function.getChild(4)).thenReturn(block);
        ITSPHPAst ast = createAst(TokenTypes.VariableId, block);
        String variableId = "$a";
        when(ast.getText()).thenReturn(variableId);

        //variableDeclarationList
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst variableDeclarationList = mock(ITSPHPAst.class);
        when(astModificationHelper.getVariableDeclaration(variableId)).thenReturn(variableDeclarationList);
        ITSPHPAst type = createAst(TokenTypes.TYPE, variableDeclarationList);
        when(variableDeclarationList.getChild(0)).thenReturn(type);
        ITSPHPAst variable = createAst(TokenTypes.VariableId, variableDeclarationList);
        when(variableDeclarationList.getChild(1)).thenReturn(variable);

        //transfer line and position
        ITSPHPAst firstChild = mock(ITSPHPAst.class);
        when(block.getChild(0)).thenReturn(firstChild);
        int line = 12;
        when(firstChild.getLine()).thenReturn(line);
        int position = 98;
        when(firstChild.getCharPositionInLine()).thenReturn(position);
        Token token = mock(Token.class);
        when(variable.getToken()).thenReturn(token);

        //VariableSymbol
        IDefinitionPhaseController definitionPhaseController = mock(IDefinitionPhaseController.class);
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
        when(definitionPhaseController.defineVariable(
                any(IScope.class), any(ITSPHPAst.class), any(ITSPHPAst.class), any(ITSPHPAst.class)))
                .thenReturn(variableSymbol);


        //act
        IVariableDeclarationCreator variableDeclarationCreator
                = createVariableDeclarationCreator(astModificationHelper, definitionPhaseController);
        variableDeclarationCreator.create(ast);

        verify(firstChild).getLine();
        verify(firstChild).getCharPositionInLine();
        verify(token).setLine(line);
        verify(token).setCharPositionInLine(position);
    }


    @Test
    public void create_InNamespaceScope_TransfersLineAndPositionsFromFirstChildToVariableDeclarationList() {
        ITSPHPAst namespace = createAst(TokenTypes.Namespace, null);
        ITSPHPAst block = createAst(TokenTypes.BLOCK, namespace);
        when(namespace.getChild(1)).thenReturn(block);
        ITSPHPAst ast = createAst(TokenTypes.VariableId, block);
        String variableId = "$a";
        when(ast.getText()).thenReturn(variableId);

        //variableDeclarationList
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst variableDeclarationList = mock(ITSPHPAst.class);
        when(astModificationHelper.getVariableDeclaration(variableId)).thenReturn(variableDeclarationList);
        ITSPHPAst type = createAst(TokenTypes.TYPE, variableDeclarationList);
        when(variableDeclarationList.getChild(0)).thenReturn(type);
        ITSPHPAst variable = createAst(TokenTypes.VariableId, variableDeclarationList);
        when(variableDeclarationList.getChild(1)).thenReturn(variable);

        //transfer line and position
        ITSPHPAst firstChild = mock(ITSPHPAst.class);
        when(block.getChild(0)).thenReturn(firstChild);
        int line = 12;
        when(firstChild.getLine()).thenReturn(line);
        int position = 98;
        when(firstChild.getCharPositionInLine()).thenReturn(position);
        Token token = mock(Token.class);
        when(variable.getToken()).thenReturn(token);

        //VariableSymbol
        IDefinitionPhaseController definitionPhaseController = mock(IDefinitionPhaseController.class);
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
        when(definitionPhaseController.defineVariable(
                any(IScope.class), any(ITSPHPAst.class), any(ITSPHPAst.class), any(ITSPHPAst.class)))
                .thenReturn(variableSymbol);
        IGlobalNamespaceScope globalDefaultNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(definitionPhaseController.getGlobalDefaultNamespace()).thenReturn(globalDefaultNamespaceScope);


        //act
        IVariableDeclarationCreator variableDeclarationCreator
                = createVariableDeclarationCreator(astModificationHelper, definitionPhaseController);
        variableDeclarationCreator.create(ast);

        verify(firstChild).getLine();
        verify(firstChild).getCharPositionInLine();
        verify(token).setLine(line);
        verify(token).setCharPositionInLine(position);
    }

    private ITSPHPAst createAst(int tokenTypes, ITSPHPAst parent) {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getType()).thenReturn(tokenTypes);
        when(ast.getParent()).thenReturn(parent);
        return ast;
    }

    private IVariableDeclarationCreator createVariableDeclarationCreator(
            IAstModificationHelper astModificationHelper,
            IDefinitionPhaseController definitionPhaseController) {
        return createVariableDeclarationCreator(
                mock(ISymbolFactory.class), astModificationHelper, definitionPhaseController);
    }

    protected IVariableDeclarationCreator createVariableDeclarationCreator(
            ISymbolFactory theSymbolFactory,
            IAstModificationHelper astModificationHelper,
            IDefinitionPhaseController definitionPhaseController) {
        return new PutAtTopVariableDeclarationCreator(
                theSymbolFactory, astModificationHelper, definitionPhaseController);
    }

}
