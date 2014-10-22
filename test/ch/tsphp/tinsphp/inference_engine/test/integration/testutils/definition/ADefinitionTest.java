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
import ch.tsphp.tinsphp.inference_engine.ITypeSystem;
import ch.tsphp.tinsphp.inference_engine.TypeSystem;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeHelper;
import ch.tsphp.tinsphp.inference_engine.symbols.IModifierHelper;
import ch.tsphp.tinsphp.inference_engine.symbols.ModifierHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ATest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestDefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestScopeFactory;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestSymbolFactory;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.WriteExceptionToConsole;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.junit.Assert;
import org.junit.Ignore;

import static org.junit.Assert.assertFalse;

@Ignore
public abstract class ADefinitionTest extends ATest
{

    protected String testString;
    protected String errorMessagePrefix;
    protected TestDefinitionPhaseController definitionPhaseController;
    protected TestScopeFactory scopeFactory;
    protected ITSPHPAst ast;
    protected CommonTreeNodeStream commonTreeNodeStream;
    protected ITSPHPAstAdaptor adaptor;

    protected ErrorReportingTinsPHPDefinitionWalker definition;
    protected TestSymbolFactory symbolFactory;
    protected IScopeHelper scopeHelper;
    protected IModifierHelper modifierHelper;
    protected ITypeSystem typeSystem;


    protected void verifyDefinitions() {
        assertFalse(testString.replaceAll("\n", " ") + " failed - definition phase throw exception",
                definition.hasFoundError());
    }

    public ADefinitionTest(String theTestString) {
        super();
        testString = theTestString;
        errorMessagePrefix = testString.replaceAll("\n", " ") + "\n" + testString;
        init();
    }

    private void init() {
        adaptor = createAstAdaptor();

        scopeHelper = createScopeHelper(inferenceErrorReporter);
        scopeFactory = createTestScopeFactory(scopeHelper, inferenceErrorReporter);
        modifierHelper = createModifierHelper();
        symbolFactory = createTestSymbolFactory(scopeHelper, modifierHelper);

        definitionPhaseController = createTestDefiner(symbolFactory, scopeFactory);
        typeSystem = createTypeSystem(symbolFactory, definitionPhaseController.getGlobalDefaultNamespace());
    }

    protected void verifyParser() {
        assertFalse(testString.replaceAll("\n", " ") + " failed - parser throw exception", parser.hasFoundError());
    }

    public void check() {
        ParserUnitDto parserUnit = parser.parse(testString);
        ast = parserUnit.compilationUnit;

        verifyParser();

        commonTreeNodeStream = new CommonTreeNodeStream(adaptor, ast);
        commonTreeNodeStream.setTokenStream(parserUnit.tokenStream);

        definition = new ErrorReportingTinsPHPDefinitionWalker(commonTreeNodeStream, definitionPhaseController);
        definition.registerErrorLogger(new WriteExceptionToConsole());
        try {
            definition.downup(ast);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(testString + " failed. Unexpected exception occurred in the definition phase.\n"
                    + e.getMessage());
        }

        assertFalse(testString.replaceAll("\n", " ") + " failed - definition throw exception",
                definition.hasFoundError());

        verifyDefinitions();
    }

    protected IScopeHelper createScopeHelper(IInferenceErrorReporter theInferenceErrorReporter) {
        return new ScopeHelper(theInferenceErrorReporter);
    }

    protected ITSPHPAstAdaptor createAstAdaptor() {
        return new TSPHPAstAdaptor();
    }

    protected TestScopeFactory createTestScopeFactory(
            IScopeHelper theScopeHelper, IInferenceErrorReporter theInferenceErrorReporter) {
        return new TestScopeFactory(theScopeHelper, theInferenceErrorReporter);
    }

    protected IModifierHelper createModifierHelper() {
        return new ModifierHelper();
    }

    protected TestSymbolFactory createTestSymbolFactory(
            IScopeHelper theScopeHelper, IModifierHelper theModifierHelper) {
        return new TestSymbolFactory(theScopeHelper, theModifierHelper);
    }

    protected ITypeSystem createTypeSystem(
            TestSymbolFactory symbolFactory, IGlobalNamespaceScope theGlobalNamespaceScope) {
        return new TypeSystem(symbolFactory, AstHelperRegistry.get(), theGlobalNamespaceScope);
    }

    protected TestDefinitionPhaseController createTestDefiner(TestSymbolFactory theSymbolFactory,
            TestScopeFactory theScopeFactory) {
        return new TestDefinitionPhaseController(theSymbolFactory, theScopeFactory);
    }
}
