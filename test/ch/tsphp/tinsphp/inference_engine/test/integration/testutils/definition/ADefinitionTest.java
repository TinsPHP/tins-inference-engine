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

import ch.tsphp.common.AstHelperRegistry;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.common.ParserUnitDto;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.core.Core;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ATest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestDefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestNamespaceScopeFactory;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestSymbolFactory;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.WriteExceptionToConsole;
import ch.tsphp.tinsphp.symbols.ModifierHelper;
import ch.tsphp.tinsphp.symbols.utils.OverloadResolver;
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
    protected TestDefinitionPhaseController definitionPhaseController;
    protected TestNamespaceScopeFactory scopeFactory;
    protected ITSPHPAst ast;
    protected CommonTreeNodeStream commonTreeNodeStream;
    protected ITSPHPAstAdaptor astAdaptor;

    protected ErrorReportingTinsPHPDefinitionWalker definition;
    protected TestSymbolFactory symbolFactory;
    protected IScopeHelper scopeHelper;
    protected IModifierHelper modifierHelper;
    protected IOverloadResolver overloadResolver;
    protected ICore core;

    public ADefinitionTest(String theTestString) {
        super();
        testString = theTestString;
        errorMessagePrefix = testString.replaceAll("\n", " ") + "\n" + testString;
        init();
    }

    private void init() {
        astAdaptor = createAstAdaptor();

        scopeHelper = createScopeHelper();
        scopeFactory = createTestScopeFactory(scopeHelper);
        modifierHelper = createModifierHelper();
        overloadResolver = createOverloadResolver();
        symbolFactory = createTestSymbolFactory(scopeHelper, modifierHelper, overloadResolver);


        definitionPhaseController = createTestDefiner(symbolFactory, scopeFactory);
        core = createCore(symbolFactory, overloadResolver);
    }

    public void runTest() {
        ParserUnitDto parserUnit = parser.parse("<?php" + testString + "?>");
        ast = parserUnit.compilationUnit;

        checkNoIssuesDuringParsing();

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

    protected ErrorReportingTinsPHPDefinitionWalker createDefinitionWalker() {
        return new ErrorReportingTinsPHPDefinitionWalker(commonTreeNodeStream, definitionPhaseController);
    }

    protected IScopeHelper createScopeHelper() {
        return new ScopeHelper();
    }

    protected ITSPHPAstAdaptor createAstAdaptor() {
        return new TSPHPAstAdaptor();
    }

    protected TestNamespaceScopeFactory createTestScopeFactory(IScopeHelper theScopeHelper) {
        return new TestNamespaceScopeFactory(theScopeHelper);
    }

    protected IModifierHelper createModifierHelper() {
        return new ModifierHelper();
    }

    protected IOverloadResolver createOverloadResolver() {
        return new OverloadResolver();
    }

    protected TestSymbolFactory createTestSymbolFactory(
            IScopeHelper theScopeHelper, IModifierHelper theModifierHelper, IOverloadResolver theOverloadResolver) {
        return new TestSymbolFactory(theScopeHelper, theModifierHelper, theOverloadResolver);
    }

    protected ICore createCore(TestSymbolFactory symbolFactory, IOverloadResolver overloadResolver) {
        return new Core(symbolFactory, overloadResolver, AstHelperRegistry.get());
    }

    protected TestDefinitionPhaseController createTestDefiner(TestSymbolFactory theSymbolFactory,
            TestNamespaceScopeFactory theScopeFactory) {
        return new TestDefinitionPhaseController(theSymbolFactory, theScopeFactory);
    }
}
