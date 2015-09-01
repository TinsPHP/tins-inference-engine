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

import ch.tsphp.common.IAstHelper;
import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.checking.ISymbolCheckController;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCreator;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolver;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolverController;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.inference_engine.ReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPReferenceWalker;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintCreator;
import ch.tsphp.tinsphp.inference_engine.constraints.IMostSpecificOverloadDecider;
import ch.tsphp.tinsphp.inference_engine.constraints.MostSpecificOverloadDecider;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;
import ch.tsphp.tinsphp.inference_engine.constraints.solvers.ConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.constraints.solvers.ConstraintSolverHelper;
import ch.tsphp.tinsphp.inference_engine.constraints.solvers.IConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.constraints.solvers.IConstraintSolverHelper;
import ch.tsphp.tinsphp.inference_engine.constraints.solvers.ISoftTypingConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.constraints.solvers.SoftTypingConstraintSolver;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertFalse;

@Ignore
public abstract class AReferenceTest extends ADefinitionTest
{
    public static int numberOfThreads = 1;
    protected ErrorReportingTinsPHPReferenceWalker reference;
    protected IReferencePhaseController referencePhaseController;
    protected IAstModificationHelper astModificationHelper;
    protected ISymbolResolver userSymbolResolver;
    protected ISymbolResolverController symbolResolverController;
    protected ISymbolCheckController symbolCheckController;
    protected IVariableDeclarationCreator variableDeclarationCreator;
    protected IConstraintCreator constraintCreator;
    protected IConstraintSolver constraintSolver;


    public AReferenceTest(String testString) {
        super(testString);

        init();
    }

    private void init() {
        IScopeHelper scopeHelper = symbolsInitialiser.getScopeHelper();
        ISymbolFactory symbolFactory = symbolsInitialiser.getSymbolFactory();
        ITypeHelper typeHelper = symbolsInitialiser.getTypeHelper();

        astModificationHelper = createAstModificationHelper(astHelper);

        userSymbolResolver = createUserSymbolResolver(
                scopeHelper,
                definitionPhaseController.getGlobalNamespaceScopes(),
                definitionPhaseController.getGlobalDefaultNamespace()
        );

        ArrayList<ISymbolResolver> resolvers = new ArrayList<>();
        resolvers.add(coreInitialiser.getCoreSymbolResolver());

        symbolResolverController = createSymbolResolverController(
                userSymbolResolver,
                resolvers,
                scopeHelper,
                symbolFactory,
                inferenceIssueReporter
        );

        symbolCheckController = createSymbolCheckController(userSymbolResolver, resolvers);

        variableDeclarationCreator = createVariableDeclarationCreator(
                symbolFactory, astModificationHelper, definitionPhaseController);

        IMostSpecificOverloadDecider mostSpecificOverloadDecider
                = new MostSpecificOverloadDecider(symbolFactory, typeHelper);

        ConcurrentMap<String, Set<String>> methodsWithDependents = new ConcurrentHashMap<>();
        ConcurrentMap<String, Set<WorkItemDto>> dependentMethods = new ConcurrentHashMap<>();
        ConcurrentMap<String, ConcurrentMap<String, List<Integer>>> directDependencies = new ConcurrentHashMap<>();


        IConstraintSolverHelper constraintSolverHelper = new ConstraintSolverHelper(
                symbolFactory,
                typeHelper,
                mostSpecificOverloadDecider
        );

        ISoftTypingConstraintSolver softTypingConstraintSolver = new SoftTypingConstraintSolver(
                symbolFactory,
                typeHelper,
                inferenceIssueReporter,
                constraintSolverHelper,
                mostSpecificOverloadDecider
        );

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        constraintSolver = createConstraintSolver(
                symbolFactory,
                softTypingConstraintSolver,
                constraintSolverHelper,
                executorService,
                directDependencies,
                methodsWithDependents,
                dependentMethods
        );

        constraintCreator = createConstraintCreator(symbolFactory, inferenceIssueReporter);

        referencePhaseController = createReferencePhaseController(
                symbolFactory,
                inferenceIssueReporter,
                astModificationHelper,
                symbolResolverController,
                symbolCheckController,
                variableDeclarationCreator,
                scopeHelper,
                symbolsInitialiser.getModifierHelper(),
                constraintCreator,
                constraintSolver,
                coreInitialiser.getCore(),
                definitionPhaseController.getGlobalDefaultNamespace()
        );
    }

    protected abstract void assertsInReferencePhase();

    @Override
    protected void checkNoIssuesInDefinitionPhase() {
        super.checkNoIssuesInDefinitionPhase();
        afterVerifyDefinitions();
    }

    protected void afterVerifyDefinitions() {
        commonTreeNodeStream.reset();
        reference = createReferenceWalker(
                commonTreeNodeStream,
                referencePhaseController,
                astAdaptor,
                definitionPhaseController.getGlobalDefaultNamespace());

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

        checkNoErrorsInReferencePhase();

        assertsInReferencePhase();
    }

    protected void checkNoErrorsInReferencePhase() {
        assertFalse(testString + " failed. Exceptions occurred." + exceptions,
                inferenceIssueReporter.hasFound(EnumSet.allOf(EIssueSeverity.class)));
        assertFalse(testString + " failed. reference walker exceptions occurred.",
                reference.hasFound(EnumSet.allOf(EIssueSeverity.class)));
    }

    protected void registerReferenceErrorLogger() {
        reference.registerIssueLogger(new WriteExceptionToConsole());
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
            IInferenceIssueReporter theInferenceErrorReporter) {
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
            ISymbolFactory theSymbolFactory,
            IAstModificationHelper theAstModificationHelper,
            IDefinitionPhaseController theDefinitionPhaseController) {
        return new TestPutAtTopVariableDeclarationCreator(
                theSymbolFactory, theAstModificationHelper, theDefinitionPhaseController);
    }

    protected IConstraintCreator createConstraintCreator(
            ISymbolFactory theSymbolFactory,
            IInferenceIssueReporter theInferenceErrorReporter) {
        return new ConstraintCreator(theSymbolFactory, theInferenceErrorReporter);
    }

    protected IConstraintSolver createConstraintSolver(
            ISymbolFactory theSymbolFactory,
            ISoftTypingConstraintSolver theSoftTypingConstraintSolver,
            IConstraintSolverHelper theConstraintSolverHelper,
            ExecutorService theExecutorService,
            ConcurrentMap<String, ConcurrentMap<String, List<Integer>>> theDirectDependencies,
            ConcurrentMap<String, Set<String>> theMethodsWithDependents,
            ConcurrentMap<String, Set<WorkItemDto>> theDependentMethods) {
        return new ConstraintSolver(
                theSymbolFactory,
                theSoftTypingConstraintSolver,
                theConstraintSolverHelper,
                theExecutorService,
                theDirectDependencies,
                theMethodsWithDependents,
                theDependentMethods);
    }


    protected IReferencePhaseController createReferencePhaseController(
            ISymbolFactory theSymbolFactory,
            IInferenceIssueReporter theInferenceErrorReporter,
            IAstModificationHelper theAstModificationHelper,
            ISymbolResolverController theSymbolResolverController,
            ISymbolCheckController theSymbolCheckController,
            IVariableDeclarationCreator theVariableDeclarationCreator,
            IScopeHelper theScopeHelper,
            IModifierHelper theModifierHelper,
            IConstraintCreator theConstraintCreator,
            IConstraintSolver theConstraintSolver,
            ICore theCore,
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
                theConstraintCreator,
                theConstraintSolver,
                theCore, theGlobalDefaultNamespace
        );
    }

    protected ErrorReportingTinsPHPReferenceWalker createReferenceWalker(
            CommonTreeNodeStream theCommonTreeNodeStream,
            IReferencePhaseController theController,
            ITSPHPAstAdaptor theAstAdaptor,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        return new ErrorReportingTinsPHPReferenceWalker(
                theCommonTreeNodeStream, theController, theAstAdaptor, theGlobalDefaultNamespace);
    }
}
