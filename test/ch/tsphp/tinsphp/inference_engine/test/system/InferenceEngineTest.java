/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.system;

import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.common.ParserUnitDto;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.tinsphp.common.IParser;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IIssueLogger;
import ch.tsphp.tinsphp.inference_engine.InferenceEngine;
import ch.tsphp.tinsphp.parser.ParserFacade;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.junit.Test;

import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InferenceEngineTest
{

    @Test
    public void allFine_DoesNotFindIssues() {
        IParser parser = new ParserFacade();
        ParserUnitDto parserUnitDto = parser.parse("<?php $a = 1;?>");
        CommonTreeNodeStream commonTreeNodeStream =
                new CommonTreeNodeStream(new TSPHPAstAdaptor(), parserUnitDto.compilationUnit);
        commonTreeNodeStream.setTokenStream(parserUnitDto.tokenStream);

        InferenceEngine inferenceEngine = createInferenceEngine();
        inferenceEngine.enrichWithDefinitions(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichtWithTypes(parserUnitDto.compilationUnit, commonTreeNodeStream);

        assertThat(inferenceEngine.hasFound(EnumSet.allOf(EIssueSeverity.class)), is(false));
    }

    @Test
    public void allFine_Reset_AllFineAgain_DoesNotFindIssues() {
        IParser parser = new ParserFacade();
        ParserUnitDto parserUnitDto = parser.parse("<?php $a = 1;?>");
        CommonTreeNodeStream commonTreeNodeStream =
                new CommonTreeNodeStream(new TSPHPAstAdaptor(), parserUnitDto.compilationUnit);
        commonTreeNodeStream.setTokenStream(parserUnitDto.tokenStream);

        InferenceEngine inferenceEngine = createInferenceEngine();
        inferenceEngine.enrichWithDefinitions(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichtWithTypes(parserUnitDto.compilationUnit, commonTreeNodeStream);

        assertThat(inferenceEngine.hasFound(EnumSet.allOf(EIssueSeverity.class)), is(false));

        //second round
        parserUnitDto = parser.parse("<?php $a = 1;?>");
        commonTreeNodeStream = new CommonTreeNodeStream(new TSPHPAstAdaptor(), parserUnitDto.compilationUnit);
        commonTreeNodeStream.setTokenStream(parserUnitDto.tokenStream);

        inferenceEngine.reset();
        inferenceEngine.enrichWithDefinitions(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichtWithTypes(parserUnitDto.compilationUnit, commonTreeNodeStream);

        assertThat(inferenceEngine.hasFound(EnumSet.allOf(EIssueSeverity.class)), is(false));
    }

    @Test
    public void uninitialisedVariable_FindsOneIssue() {
        IParser parser = new ParserFacade();
        ParserUnitDto parserUnitDto = parser.parse("<?php echo $a; ?>");
        CommonTreeNodeStream commonTreeNodeStream =
                new CommonTreeNodeStream(new TSPHPAstAdaptor(), parserUnitDto.compilationUnit);
        commonTreeNodeStream.setTokenStream(parserUnitDto.tokenStream);

        InferenceEngine inferenceEngine = createInferenceEngine();
        inferenceEngine.enrichWithDefinitions(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichtWithTypes(parserUnitDto.compilationUnit, commonTreeNodeStream);

        assertThat(inferenceEngine.hasFound(EnumSet.allOf(EIssueSeverity.class)), is(true));
    }

    @Test
    public void uninitialisedVariable_FindsOneIssue_InformsLoggers() {
        IParser parser = new ParserFacade();
        ParserUnitDto parserUnitDto = parser.parse("<?php echo $a; ?>");
        CommonTreeNodeStream commonTreeNodeStream =
                new CommonTreeNodeStream(new TSPHPAstAdaptor(), parserUnitDto.compilationUnit);
        commonTreeNodeStream.setTokenStream(parserUnitDto.tokenStream);
        IIssueLogger logger1 = mock(IIssueLogger.class);
        IIssueLogger logger2 = mock(IIssueLogger.class);

        InferenceEngine inferenceEngine = createInferenceEngine();
        inferenceEngine.registerIssueLogger(logger1);
        inferenceEngine.registerIssueLogger(logger2);
        inferenceEngine.enrichWithDefinitions(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(parserUnitDto.compilationUnit, commonTreeNodeStream);
        //inferenceEngine.enrichtWithTypes(parserUnitDto.compilationUnit, commonTreeNodeStream);

        verify(logger1).log(any(TSPHPException.class), any(EIssueSeverity.class));
        verify(logger2).log(any(TSPHPException.class), any(EIssueSeverity.class));
    }

    private InferenceEngine createInferenceEngine() {
        return createInferenceEngine(new TSPHPAstAdaptor());
    }

    protected InferenceEngine createInferenceEngine(ITSPHPAstAdaptor astAdaptor) {
        return new InferenceEngine(astAdaptor);
    }
}
