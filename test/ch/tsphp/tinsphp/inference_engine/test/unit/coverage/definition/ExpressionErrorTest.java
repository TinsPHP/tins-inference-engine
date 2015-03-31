/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.coverage.definition;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.TestTinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.test.unit.testutils.ADefinitionWalkerTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.CAST;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.EOF;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.EXPRESSION;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.Instanceof;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.TYPE;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.Try;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.TypeInt;
import static org.antlr.runtime.tree.TreeParser.UP;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;

public class ExpressionErrorTest extends ADefinitionWalkerTest
{
    @Test
    public void emptyCast_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(CAST);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.expression();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(EOF));
    }

    @Test
    public void castWithoutTypeNode_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(CAST);
        //should be TYPE
        ast.addChild(createAst(Try));

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.expression();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(Try));
    }

    @Test
    public void castWithEmptyTypeNode_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(CAST);
        ast.addChild(createAst(TYPE));

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.expression();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(UP));
    }

    @Test
    public void castWithErroneousType_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(CAST);
        ITSPHPAst type = createAst(TYPE);
        //should be a primitive type
        type.addChild(createAst(Try));
        ast.addChild(type);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.expression();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(UP));
    }

    @Test
    public void castWithSuperfluousChildInType_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(CAST);
        ITSPHPAst type = createAst(TYPE);
        type.addChild(createAst(TypeInt));
        type.addChild(createAst(Try));
        ast.addChild(type);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.expression();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(Try));
    }


    @Test
    public void castWithSuperfluousChild_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(CAST);
        ITSPHPAst type = createAst(TYPE);
        type.addChild(createAst(TypeInt));
        ast.addChild(type);
        type.addChild(createAst(EXPRESSION));
        type.addChild(createAst(Try));

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.expression();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(EXPRESSION));
    }

    @Test
    public void emptyInstanceOf_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(Instanceof);

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.expression();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(EOF));
    }

    @Test
    public void instanceOfWithErroneousRHS_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(Instanceof);
        ast.addChild(createAst(EXPRESSION));
        ast.addChild(createAst(Try));

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.expression();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(Try));
    }


    @Test
    public void instanceOfWithSuperfluousChild_BacktrackingEnabled_StateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(Instanceof);
        ast.addChild(createAst(EXPRESSION));
        ast.addChild(createVariable());
        ast.addChild(createAst(Try));

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        walker.setBacktrackingLevel(1);
        walker.expression();

        assertThat(walker.getState().failed, is(true));
        assertThat(treeNodeStream.LA(1), is(Try));
    }
}

