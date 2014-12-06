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
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.resolver.ISymbolCheckController;
import ch.tsphp.tinsphp.common.symbols.resolver.ISymbolResolver;
import ch.tsphp.tinsphp.common.symbols.resolver.ISymbolResolverController;
import ch.tsphp.tinsphp.common.symbols.resolver.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.inference_engine.IDefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.IReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.ReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPReferenceWalker;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.resolver.SymbolCheckController;
import ch.tsphp.tinsphp.inference_engine.resolver.SymbolResolverController;
import ch.tsphp.tinsphp.inference_engine.resolver.UserSymbolResolver;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.WriteExceptionToConsole;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ADefinitionTest;
import ch.tsphp.tinsphp.inference_engine.utils.AstModificationHelper;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;

@Ignore
public abstract class AReferenceTest extends ADefinitionTest
{

    protected ErrorReportingTinsPHPReferenceWalker reference;
    protected IReferencePhaseController referencePhaseController;
    protected IAstModificationHelper astModificationHelper;
    protected ISymbolResolver userSymbolResolver;
    protected ISymbolResolver coreSymbolResolver;
    protected ISymbolResolverController symbolResolverController;
    protected ISymbolCheckController symbolCheckController;
    protected IVariableDeclarationCreator variableDeclarationCreator;

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

        userSymbolResolver = createUserSymbolResolver(
                scopeHelper,
                definitionPhaseController.getGlobalNamespaceScopes(),
                definitionPhaseController.getGlobalDefaultNamespace()
        );

        coreSymbolResolver = core.getCoreSymbolResolver();
        ArrayList<ISymbolResolver> resolvers = new ArrayList<>();
        resolvers.add(coreSymbolResolver);

        symbolResolverController = createSymbolResolverController(
                userSymbolResolver,
                resolvers,
                scopeHelper,
                symbolFactory,
                inferenceErrorReporter
        );

        ArrayList<ISymbolResolver> symbolResolvers = new ArrayList<>();
        symbolResolvers.add(core.getCoreSymbolResolver());
        symbolCheckController = createSymbolCheckController(userSymbolResolver, symbolResolvers);

        variableDeclarationCreator = createVariableDeclarationCreator(astModificationHelper, definitionPhaseController);

        referencePhaseController = createReferencePhaseController(
                symbolFactory,
                inferenceErrorReporter,
                astModificationHelper,
                symbolResolverController,
                symbolCheckController,
                variableDeclarationCreator,
                scopeHelper,
                modifierHelper,
                core.getPrimitiveTypes(),
                definitionPhaseController.getGlobalDefaultNamespace()
        );
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
            ILowerCaseStringMap<IGlobalNamespaceScope> theGlobalNamespaceScopes,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        return new UserSymbolResolver(theScopeHelper, theGlobalNamespaceScopes, theGlobalDefaultNamespace);
    }

    protected ISymbolResolverController createSymbolResolverController(
            ISymbolResolver theUserSymbolResolver,
            List<ISymbolResolver> additionalSymbolResolvers,
            IScopeHelper theScopeHelper,
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter) {
        return new SymbolResolverController(
                theUserSymbolResolver,
                additionalSymbolResolvers,
                theScopeHelper,
                theSymbolFactory,
                theInferenceErrorReporter
        );
    }

    protected ISymbolCheckController createSymbolCheckController(
            ISymbolResolver theUserSymbolResolver,
            List<ISymbolResolver> additionalSymbolResolvers) {
        return new SymbolCheckController(theUserSymbolResolver, additionalSymbolResolvers);
    }

    protected IVariableDeclarationCreator createVariableDeclarationCreator(
            IAstModificationHelper theAstModificationHelper, IDefinitionPhaseController theDefinitionPhaseController) {
        return new TestPutAtTopVariableDeclarationCreator(theAstModificationHelper, theDefinitionPhaseController);
    }

    protected IReferencePhaseController createReferencePhaseController(
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter,
            IAstModificationHelper theAstModificationHelper,
            ISymbolResolverController theSymbolResolverController,
            ISymbolCheckController theSymbolCheckController,
            IVariableDeclarationCreator theVariableDeclarationCreator,
            IScopeHelper theScopeHelper,
            IModifierHelper theModifierHelper,
            Map<String, ITypeSymbol> thePrimitiveTypes,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        return new ReferencePhaseController(
                theSymbolFactory,
                theInferenceErrorReporter,
                theAstModificationHelper,
                theSymbolResolverController,
                theSymbolCheckController,
                theVariableDeclarationCreator,
                theScopeHelper,
                theModifierHelper,
                thePrimitiveTypes,
                theGlobalDefaultNamespace
        );
    }

    protected ErrorReportingTinsPHPReferenceWalker createReferenceWalker(
            CommonTreeNodeStream theCommonTreeNodeStream,
            IReferencePhaseController theController) {
        return new ErrorReportingTinsPHPReferenceWalker(theCommonTreeNodeStream, theController);
    }

}
