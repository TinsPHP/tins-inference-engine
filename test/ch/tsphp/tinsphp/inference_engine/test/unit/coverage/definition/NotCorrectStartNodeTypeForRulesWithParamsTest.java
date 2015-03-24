/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.coverage.definition;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.TestTinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.test.unit.testutils.ADefinitionWalkerTest;
import org.antlr.runtime.MismatchedTreeNodeException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationTargetException;

import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.Else;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class NotCorrectStartNodeTypeForRulesWithParamsTest extends ADefinitionWalkerTest
{
    @Test
    public void expression_withoutBacktracking_reportNoViableAltException()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.expression(false);

        ArgumentCaptor<NoViableAltException> captor = ArgumentCaptor.forClass(NoViableAltException.class);
        verify(walker).reportError(captor.capture());
        assertThat(captor.getValue().token.getType(), is(Else));
    }

    @Test
    public void expression_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.expression(false);

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(Else));
    }

    @Test
    public void variableDeclaration_withoutBacktracking_reportNoViableAltException()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.variableDeclaration(mock(ITSPHPAst.class), mock(ITSPHPAst.class));

        ArgumentCaptor<NoViableAltException> captor = ArgumentCaptor.forClass(NoViableAltException.class);
        verify(walker).reportError(captor.capture());
        assertThat(captor.getValue().token.getType(), is(Else));
    }

    @Test
    public void variableDeclaration_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.variableDeclaration(mock(ITSPHPAst.class), mock(ITSPHPAst.class));

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(Else));
    }

    @Test
    public void constantDeclaration_withoutBacktracking_reportMismatchedTreeNodeException()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.constantDeclaration(mock(ITSPHPAst.class), mock(ITSPHPAst.class));

        ArgumentCaptor<MismatchedTreeNodeException> captor = ArgumentCaptor.forClass(MismatchedTreeNodeException.class);
        verify(walker).reportError(captor.capture());
        assertThat(captor.getValue().token.getType(), is(Else));
    }

    @Test
    public void constantDeclaration_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.constantDeclaration(mock(ITSPHPAst.class), mock(ITSPHPAst.class));

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(Else));
    }
}

