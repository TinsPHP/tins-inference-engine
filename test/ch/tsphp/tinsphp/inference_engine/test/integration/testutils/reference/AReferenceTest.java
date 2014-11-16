/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AReferenceTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference;

import ch.tsphp.common.AstHelper;
import ch.tsphp.common.IAstHelper;
import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ISymbolResolver;
import ch.tsphp.tinsphp.common.symbols.ITypeSymbolResolver;
import ch.tsphp.tinsphp.inference_engine.IReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.ReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPReferenceWalker;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.resolver.UserSymbolResolver;
import ch.tsphp.tinsphp.inference_engine.resolver.UserTypeSymbolResolver;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.WriteExceptionToConsole;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ADefinitionTest;
import ch.tsphp.tinsphp.inference_engine.utils.AstModificationHelper;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.junit.Assert;
import org.junit.Ignore;

import static org.junit.Assert.assertFalse;

@Ignore
public abstract class AReferenceTest extends ADefinitionTest
{

    protected ErrorReportingTinsPHPReferenceWalker reference;
    protected IReferencePhaseController referencePhaseController;
    protected IAstModificationHelper astModificationHelper;
    protected ISymbolResolver symbolResolver;
    protected ITypeSymbolResolver typeSymbolResolver;

    protected ITSPHPAstAdaptor astAdaptor;
    protected IAstHelper astHelper;

    public AReferenceTest(String testString) {
        super(testString);

        init();
    }

    private void init() {

        astAdaptor = createAstAdaptor();
        astHelper = createAstHelper(astAdaptor);
        astModificationHelper = createAstModificationHelper(astHelper);
        symbolResolver = createUserSymbolResolver(
                scopeHelper,
                symbolFactory,
                inferenceErrorReporter,
                definitionPhaseController.getGlobalNamespaceScopes(),
                definitionPhaseController.getGlobalDefaultNamespace());

        typeSymbolResolver = createTypeSymbolResolver(
                scopeHelper,
                symbolFactory,
                inferenceErrorReporter,
                definitionPhaseController.getGlobalNamespaceScopes(),
                definitionPhaseController.getGlobalDefaultNamespace());

        referencePhaseController = createReferencePhaseController(
                symbolFactory,
                inferenceErrorReporter,
                astModificationHelper,
                symbolResolver,
                typeSymbolResolver,
                scopeHelper,
                core,
                modifierHelper,
                definitionPhaseController.getGlobalDefaultNamespace());
    }

    protected abstract void verifyReferences();

    protected void checkReferences() {
        assertFalse(testString + " failed. Exceptions occurred." + exceptions, inferenceErrorReporter.hasFoundError());
        assertFalse(testString + " failed. reference walker exceptions occurred.", reference.hasFoundError());

        verifyReferences();
    }

    @Override
    protected void verifyDefinitions() {
        super.verifyDefinitions();
        afterVerifyDefinitions();
    }

    protected void afterVerifyDefinitions() {
        commonTreeNodeStream.reset();
        reference = createReferenceWalker(commonTreeNodeStream, referencePhaseController);
        registerReferenceErrorLogger();

        try {
            reference.compilationUnit();
        } catch (RecognitionException e) {
            e.printStackTrace();
            Assert.fail(testString + " failed. Unexpected exception occurred, " +
                    "should be caught by the ErrorReportingTSPHPReferenceWalker.\n"
                    + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(testString + " failed. Unexpected exception occurred in the reference phase.\n"
                    + e.getMessage());
        }
        checkReferences();
    }

    protected void registerReferenceErrorLogger() {
        reference.registerErrorLogger(new WriteExceptionToConsole());
    }

    protected static String getAliasFullType(String type) {
        return type.substring(0, 1).equals("\\") ? type : "\\" + type;
    }

    protected static String getFullName(String namespace, String type) {
        String fullType = type;
        if (!type.substring(0, 1).equals("\\")) {
            fullType = namespace + type;
        }
        return fullType;
    }

    protected ITSPHPAstAdaptor createAstAdaptor() {
        return new TSPHPAstAdaptor();
    }

    protected IAstHelper createAstHelper(ITSPHPAstAdaptor theAstAdaptor) {
        return new AstHelper(theAstAdaptor);
    }

    protected IAstModificationHelper createAstModificationHelper(IAstHelper theAstHelper) {
        return new AstModificationHelper(theAstHelper);
    }

    protected ISymbolResolver createUserSymbolResolver(
            IScopeHelper theScopeHelper,
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter,
            ILowerCaseStringMap<IGlobalNamespaceScope> theGlobalNamespaceScopes,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        return new UserSymbolResolver(theScopeHelper, theSymbolFactory, theInferenceErrorReporter,
                theGlobalNamespaceScopes, theGlobalDefaultNamespace

        );
    }

    protected ITypeSymbolResolver createTypeSymbolResolver(
            IScopeHelper theScopeHelper,
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter,
            ILowerCaseStringMap<IGlobalNamespaceScope> theGlobalNamespaceScopes,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        return new UserTypeSymbolResolver(theScopeHelper, theSymbolFactory, theInferenceErrorReporter,
                theGlobalNamespaceScopes, theGlobalDefaultNamespace);
    }

    protected IReferencePhaseController createReferencePhaseController(
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter,
            IAstModificationHelper theAstModificationHelper,
            ISymbolResolver theSymbolResolver,
            ITypeSymbolResolver theTypeSymbolResolver,
            IScopeHelper theScopeHelper,
            ICore theCore,
            IModifierHelper theModifierHelper,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        return new ReferencePhaseController(
                theSymbolFactory,
                theInferenceErrorReporter,
                theAstModificationHelper,
                theSymbolResolver,
                theTypeSymbolResolver,
                theScopeHelper,
                theCore,
                theModifierHelper,
                theGlobalDefaultNamespace
        );
    }

    protected ErrorReportingTinsPHPReferenceWalker createReferenceWalker(
            CommonTreeNodeStream theCommonTreeNodeStream,
            IReferencePhaseController theController) {
        return new ErrorReportingTinsPHPReferenceWalker(theCommonTreeNodeStream, theController);
    }

}
