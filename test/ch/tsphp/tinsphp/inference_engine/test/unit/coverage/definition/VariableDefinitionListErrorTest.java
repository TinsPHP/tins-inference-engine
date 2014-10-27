/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.coverage.definition;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.TestTinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.test.unit.testutils.ADefinitionWalkerTest;
import org.antlr.runtime.EarlyExitException;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationTargetException;

import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.EOF;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.Identifier;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.TYPE;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.TYPE_MODIFIER;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.Try;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.VARIABLE_DECLARATION;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.VARIABLE_DECLARATION_LIST;
import static org.antlr.runtime.tree.TreeParser.UP;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class VariableDefinitionListErrorTest extends ADefinitionWalkerTest
{
    @Test
    public void empty_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(VARIABLE_DECLARATION_LIST);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.variableDeclarationList();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(EOF));
    }

    @Test
    public void missingType_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(VARIABLE_DECLARATION_LIST);
        ast.addChild(createAst(VARIABLE_DECLARATION));

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.variableDeclarationList();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(VARIABLE_DECLARATION));
    }

    @Test
    public void emptyType_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(VARIABLE_DECLARATION_LIST);
        ITSPHPAst type = createAst(TYPE);
        ast.addChild(type);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.variableDeclarationList();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(UP));
    }

    @Test
    public void missingTypeModifier_WithoutBacktracking_ReportEarlyExitException()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(VARIABLE_DECLARATION_LIST);
        ITSPHPAst type = createAst(TYPE);
        type.addChild(createAst(Identifier));
        ast.addChild(type);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.variableDeclarationList();

        ArgumentCaptor<EarlyExitException> captor = ArgumentCaptor.forClass(EarlyExitException.class);
        verify(walker).reportError(captor.capture());
        assertThat(captor.getValue().token.getType(), is(EOF));
    }

    @Test
    public void missingTypeModifier_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(VARIABLE_DECLARATION_LIST);
        ITSPHPAst type = createAst(TYPE);
        type.addChild(createAst(Identifier));
        ast.addChild(type);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.variableDeclarationList();

        assertThat(walker.getState().failed, is(true));
        //EOF due to matchAny
        assertThat(treeNodeStream.LA(1), is(EOF));
    }

    @Test
    public void superfluousChildInType_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(VARIABLE_DECLARATION_LIST);
        ITSPHPAst type = createAst(TYPE);
        type.addChild(createAst(TYPE_MODIFIER));
        type.addChild(createAst(Identifier));
        type.addChild(createAst(Try));
        ast.addChild(type);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.variableDeclarationList();

        assertThat(walker.getState().failed, is(true));
        //EOF due to matchAny
        assertThat(treeNodeStream.LA(1), is(Try));
    }

    @Test
    public void erroneousVariableDeclaration_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(VARIABLE_DECLARATION_LIST);
        ITSPHPAst type = createAst(TYPE);
        type.addChild(createAst(TYPE_MODIFIER));
        type.addChild(createAst(Identifier));
        ast.addChild(type);
        //should be VariableId
        ast.addChild(createAst(Try));

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.variableDeclarationList();

        assertThat(walker.getState().failed, is(true));
        //EOF due to matchAny
        assertThat(treeNodeStream.LA(1), is(Try));
    }
}

