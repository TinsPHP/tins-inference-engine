/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ADefinitionTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition;

import ch.tsphp.common.AstHelper;
import ch.tsphp.common.IAstHelper;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.common.ParserUnitDto;
import ch.tsphp.tinsphp.common.config.ICoreInitialiser;
import ch.tsphp.tinsphp.common.config.ISymbolsInitialiser;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.core.config.HardCodedCoreInitialiser;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ATest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestDefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestNamespaceScopeFactory;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestSymbolFactory;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestSymbolsInitialiser;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.WriteExceptionToConsole;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.EnumSet;

import static org.junit.Assert.assertFalse;

@Ignore
public abstract class ADefinitionTest extends ATest
{

    protected String testString;
    protected String errorMessagePrefix;
    protected IAstHelper astHelper;

    protected TestDefinitionPhaseController definitionPhaseController;
    protected ITSPHPAst ast;
    protected CommonTreeNodeStream commonTreeNodeStream;


    protected ErrorReportingTinsPHPDefinitionWalker definition;
    protected ISymbolsInitialiser symbolsInitialiser;
    protected ICoreInitialiser coreInitialiser;

    public ADefinitionTest(String theTestString) {
        super();
        testString = theTestString;
        errorMessagePrefix = testString.replaceAll("\n", " ") + "\n" + testString;
        init();
    }

    private void init() {
        astHelper = createAstHelper(astAdaptor);

        symbolsInitialiser = createSymbolsInitialiser();
        coreInitialiser = createCoreInitialiser(astHelper, symbolsInitialiser);

        definitionPhaseController = createTestDefinitionPhaseController(
                (TestSymbolFactory) symbolsInitialiser.getSymbolFactory(),
                (TestNamespaceScopeFactory) symbolsInitialiser.getScopeFactory());
    }


    public void runTest() {
        try {
            run();
        } catch (Throwable ex) {
            if (!(ex instanceof AssertionError)) {
                System.err.println(testString + " failed - unexpected exception occurred.");
            }
            throw ex;
        }
    }

    private void run() {
        ParserUnitDto parserUnit = parser.parse("<?php" + testString + "?>");

        checkNoIssuesDuringParsing();

        ast = parserUnit.compilationUnit;
        commonTreeNodeStream = new CommonTreeNodeStream(astAdaptor, ast);
        commonTreeNodeStream.setTokenStream(parserUnit.tokenStream);

        definition = createDefinitionWalker();
        definition.registerIssueLogger(new WriteExceptionToConsole());
        try {
            definition.downup(ast);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(testString + " failed. Unexpected exception occurred in the definition phase.\n"
                    + e.getMessage());
        }

        checkNoIssuesInDefinitionPhase();
    }

    protected void checkNoIssuesDuringParsing() {
        assertFalse(testString.replaceAll("\n", " ") + " failed - parser throw exception",
                parser.hasFound(EnumSet.allOf(EIssueSeverity.class)));
    }

    protected void checkNoIssuesInDefinitionPhase() {
        assertFalse(testString.replaceAll("\n", " ") + " failed - definition phase found an issue",
                definition.hasFound(EnumSet.allOf(EIssueSeverity.class)));
    }


    protected IAstHelper createAstHelper(ITSPHPAstAdaptor theAstAdaptor) {
        return new AstHelper(theAstAdaptor);
    }

    protected ISymbolsInitialiser createSymbolsInitialiser() {
        return new TestSymbolsInitialiser();
    }

    protected ICoreInitialiser createCoreInitialiser(
            IAstHelper theAstHelper, ISymbolsInitialiser theSymbolsInitialiser) {
        return new HardCodedCoreInitialiser(theAstHelper, theSymbolsInitialiser);
    }


    protected ErrorReportingTinsPHPDefinitionWalker createDefinitionWalker() {
        return new ErrorReportingTinsPHPDefinitionWalker(commonTreeNodeStream, definitionPhaseController);
    }

    protected TestDefinitionPhaseController createTestDefinitionPhaseController(TestSymbolFactory theSymbolFactory,
            TestNamespaceScopeFactory theScopeFactory) {
        return new TestDefinitionPhaseController(theSymbolFactory, theScopeFactory);
    }
}
