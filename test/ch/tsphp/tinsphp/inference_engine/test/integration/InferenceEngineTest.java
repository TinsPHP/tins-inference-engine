/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration;

import ch.tsphp.common.IErrorLogger;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ParserUnitDto;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.tinsphp.common.IInferenceEngine;
import ch.tsphp.tinsphp.inference_engine.InferenceEngine;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ATest;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InferenceEngineTest extends ATest
{
    @Test
    public void registerErrorLogger_Standard_InformsLoggerWhenErrorOccurs() {
        //should cause an issue due to a double definition
        ParserUnitDto parserUnit = parser.parse("<?php function foo(){return;} function foo(){return;}");
        ITSPHPAst ast = parserUnit.compilationUnit;

        CommonTreeNodeStream commonTreeNodeStream = new CommonTreeNodeStream(new TSPHPAstAdaptor(), ast);
        commonTreeNodeStream.setTokenStream(parserUnit.tokenStream);

        IErrorLogger logger1 = mock(IErrorLogger.class);
        IErrorLogger logger2 = mock(IErrorLogger.class);

        //act
        IInferenceEngine inferenceEngine = createInferenceEngine();
        inferenceEngine.registerErrorLogger(logger1);
        inferenceEngine.registerErrorLogger(logger2);
        inferenceEngine.enrichWithDefinitions(ast, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(ast, commonTreeNodeStream);

        //verify
        verify(logger1).log(any(TSPHPException.class));
        verify(logger2).log(any(TSPHPException.class));
        assertThat(inferenceEngine.hasFoundError(), is(true));
    }

    @Test
    public void reset_RegisteredErrorLoggers_AreStillInformedWhenErrorOccurs() {
        //should cause an issue due to a double definition
        ParserUnitDto parserUnit = parser.parse("<?php function foo(){return;} function foo(){return;}");
        ITSPHPAst ast = parserUnit.compilationUnit;

        CommonTreeNodeStream commonTreeNodeStream = new CommonTreeNodeStream(new TSPHPAstAdaptor(), ast);
        commonTreeNodeStream.setTokenStream(parserUnit.tokenStream);

        IErrorLogger logger1 = mock(IErrorLogger.class);
        IErrorLogger logger2 = mock(IErrorLogger.class);

        //act
        IInferenceEngine inferenceEngine = createInferenceEngine();
        inferenceEngine.registerErrorLogger(logger1);
        inferenceEngine.registerErrorLogger(logger2);
        inferenceEngine.reset();
        inferenceEngine.enrichWithDefinitions(ast, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(ast, commonTreeNodeStream);

        //verify
        verify(logger1).log(any(TSPHPException.class));
        verify(logger2).log(any(TSPHPException.class));
        assertThat(inferenceEngine.hasFoundError(), is(true));
    }

    private IInferenceEngine createInferenceEngine() {
        return new InferenceEngine();
    }
}
