/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.coverage.definition;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.TestTinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.test.unit.testutils.ADefinitionWalkerTest;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationTargetException;

import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.EOF;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.VariableId;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class VariableDefinitionErrorTest extends ADefinitionWalkerTest
{
    @Test
    public void EOFAfterVariableId_WithoutBacktracking_ReportNoViableAltException()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createVariable();

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.variableDefinition(mock(ITSPHPAst.class), mock(ITSPHPAst.class));

        ArgumentCaptor<NoViableAltException> captor = ArgumentCaptor.forClass(NoViableAltException.class);
        verify(walker).reportError(captor.capture());
        assertThat(captor.getValue().token.getType(), is(EOF));
    }

    @Test
    public void EOFAfterVariableId_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createVariable();

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.variableDefinition(mock(ITSPHPAst.class), mock(ITSPHPAst.class));

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(VariableId));
    }
}

