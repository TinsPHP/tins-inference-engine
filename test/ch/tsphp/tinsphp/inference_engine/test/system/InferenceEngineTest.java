/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.system;

import ch.tsphp.common.AstHelper;
import ch.tsphp.common.IAstHelper;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.common.ParserUnitDto;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.tinsphp.common.IInferenceEngine;
import ch.tsphp.tinsphp.common.IParser;
import ch.tsphp.tinsphp.common.config.IInferenceEngineInitialiser;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IIssueLogger;
import ch.tsphp.tinsphp.core.config.HardCodedCoreInitialiser;
import ch.tsphp.tinsphp.inference_engine.config.HardCodedInferenceEngineInitialiser;
import ch.tsphp.tinsphp.parser.ParserFacade;
import ch.tsphp.tinsphp.symbols.config.HardCodedSymbolsInitialiser;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.junit.Assert;
import org.junit.Test;

import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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

        IInferenceEngineInitialiser initialiser = createInitialiser();
        IInferenceEngine inferenceEngine = initialiser.getEngine();
        inferenceEngine.enrichWithDefinitions(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.solveMethodSymbolConstraints();
        inferenceEngine.solveGlobalDefaultNamespaceConstraints();

        assertThat(inferenceEngine.hasFound(EnumSet.allOf(EIssueSeverity.class)), is(false));
    }

    @Test
    public void allFine_Reset_AllFineAgain_DoesNotFindIssues() {
        IParser parser = new ParserFacade();
        ParserUnitDto parserUnitDto = parser.parse("<?php $a = 1;?>");
        CommonTreeNodeStream commonTreeNodeStream =
                new CommonTreeNodeStream(new TSPHPAstAdaptor(), parserUnitDto.compilationUnit);
        commonTreeNodeStream.setTokenStream(parserUnitDto.tokenStream);

        IInferenceEngineInitialiser initialiser = createInitialiser();
        IInferenceEngine inferenceEngine = initialiser.getEngine();
        inferenceEngine.enrichWithDefinitions(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.solveMethodSymbolConstraints();
        inferenceEngine.solveGlobalDefaultNamespaceConstraints();

        assertThat(inferenceEngine.hasFound(EnumSet.allOf(EIssueSeverity.class)), is(false));

        //second round
        parserUnitDto = parser.parse("<?php $a = 1;?>");
        commonTreeNodeStream = new CommonTreeNodeStream(new TSPHPAstAdaptor(), parserUnitDto.compilationUnit);
        commonTreeNodeStream.setTokenStream(parserUnitDto.tokenStream);

        inferenceEngine.reset();
        inferenceEngine.enrichWithDefinitions(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.solveMethodSymbolConstraints();
        inferenceEngine.solveGlobalDefaultNamespaceConstraints();

        assertThat(inferenceEngine.hasFound(EnumSet.allOf(EIssueSeverity.class)), is(false));
    }

    @Test
    public void uninitialisedVariable_FindsOneIssue() {
        IParser parser = new ParserFacade();
        ParserUnitDto parserUnitDto = parser.parse("<?php echo $a; ?>");
        CommonTreeNodeStream commonTreeNodeStream =
                new CommonTreeNodeStream(new TSPHPAstAdaptor(), parserUnitDto.compilationUnit);
        commonTreeNodeStream.setTokenStream(parserUnitDto.tokenStream);

        IInferenceEngineInitialiser initialiser = createInitialiser();
        IInferenceEngine inferenceEngine = initialiser.getEngine();
        inferenceEngine.enrichWithDefinitions(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.solveMethodSymbolConstraints();
        inferenceEngine.solveGlobalDefaultNamespaceConstraints();

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

        IInferenceEngineInitialiser initialiser = createInitialiser();
        IInferenceEngine inferenceEngine = initialiser.getEngine();
        inferenceEngine.registerIssueLogger(logger1);
        inferenceEngine.registerIssueLogger(logger2);
        inferenceEngine.enrichWithDefinitions(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(parserUnitDto.compilationUnit, commonTreeNodeStream);
        inferenceEngine.solveMethodSymbolConstraints();
        inferenceEngine.solveGlobalDefaultNamespaceConstraints();

        verify(logger1).log(any(TSPHPException.class), any(EIssueSeverity.class));
        verify(logger2).log(any(TSPHPException.class), any(EIssueSeverity.class));
    }

    @Test
    public void registerIssueLogger_Standard_InformsLoggerWhenErrorOccurs() {
        //should cause an issue with severity fatal error due to a double definition
        IParser parser = new ParserFacade();
        ParserUnitDto parserUnit = parser.parse("<?php function foo(){return;} function foo(){return;}");
        ITSPHPAst ast = parserUnit.compilationUnit;

        CommonTreeNodeStream commonTreeNodeStream = new CommonTreeNodeStream(new TSPHPAstAdaptor(), ast);
        commonTreeNodeStream.setTokenStream(parserUnit.tokenStream);

        IIssueLogger logger1 = mock(IIssueLogger.class);
        IIssueLogger logger2 = mock(IIssueLogger.class);

        //act
        IInferenceEngineInitialiser initialiser = createInitialiser();
        IInferenceEngine inferenceEngine = initialiser.getEngine();
        inferenceEngine.registerIssueLogger(logger1);
        inferenceEngine.registerIssueLogger(logger2);
        inferenceEngine.enrichWithDefinitions(ast, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(ast, commonTreeNodeStream);

        //verify
        verify(logger1).log(any(TSPHPException.class), eq(EIssueSeverity.FatalError));
        verify(logger2).log(any(TSPHPException.class), eq(EIssueSeverity.FatalError));
        Assert.assertThat(inferenceEngine.hasFound(EnumSet.of(EIssueSeverity.FatalError)), is(true));
    }

    @Test
    public void reset_RegisteredErrorLoggers_AreStillInformedWhenErrorOccurs() {
        //should cause an issue due to a double definition
        IParser parser = new ParserFacade();
        ParserUnitDto parserUnit = parser.parse("<?php function foo(){return;} function foo(){return;}");
        ITSPHPAst ast = parserUnit.compilationUnit;

        CommonTreeNodeStream commonTreeNodeStream = new CommonTreeNodeStream(new TSPHPAstAdaptor(), ast);
        commonTreeNodeStream.setTokenStream(parserUnit.tokenStream);

        IIssueLogger logger1 = mock(IIssueLogger.class);
        IIssueLogger logger2 = mock(IIssueLogger.class);

        //act
        IInferenceEngineInitialiser initialiser = createInitialiser();
        IInferenceEngine inferenceEngine = initialiser.getEngine();
        inferenceEngine.registerIssueLogger(logger1);
        inferenceEngine.registerIssueLogger(logger2);
        inferenceEngine.reset();
        inferenceEngine.enrichWithDefinitions(ast, commonTreeNodeStream);
        inferenceEngine.enrichWithReferences(ast, commonTreeNodeStream);

        //verify
        verify(logger1).log(any(TSPHPException.class), eq(EIssueSeverity.FatalError));
        verify(logger2).log(any(TSPHPException.class), eq(EIssueSeverity.FatalError));
        Assert.assertThat(inferenceEngine.hasFound(EnumSet.of(EIssueSeverity.FatalError)), is(true));
    }


    protected IInferenceEngineInitialiser createInitialiser() {
        ITSPHPAstAdaptor astAdaptor = new TSPHPAstAdaptor();
        IAstHelper astHelper = new AstHelper(astAdaptor);
        HardCodedSymbolsInitialiser symbolsInitialiser = new HardCodedSymbolsInitialiser();
        HardCodedCoreInitialiser coreInitialiser = new HardCodedCoreInitialiser(astHelper, symbolsInitialiser);

        return new HardCodedInferenceEngineInitialiser(astAdaptor, astHelper, symbolsInitialiser, coreInitialiser);
    }

}
