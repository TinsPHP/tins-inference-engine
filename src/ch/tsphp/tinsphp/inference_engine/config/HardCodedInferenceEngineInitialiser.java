/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.config;

import ch.tsphp.common.IAstHelper;
import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.IInferenceEngine;
import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.checking.ISymbolCheckController;
import ch.tsphp.tinsphp.common.config.ICoreInitialiser;
import ch.tsphp.tinsphp.common.config.IInferenceEngineInitialiser;
import ch.tsphp.tinsphp.common.config.ISymbolsInitialiser;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCreator;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolver;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolverController;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeFactory;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.utils.IOverloadResolver;
import ch.tsphp.tinsphp.inference_engine.DefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.InferenceEngine;
import ch.tsphp.tinsphp.inference_engine.ReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintCreator;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.issues.HardCodedIssueMessageProvider;
import ch.tsphp.tinsphp.inference_engine.issues.InferenceIssueReporter;
import ch.tsphp.tinsphp.inference_engine.resolver.PutAtTopVariableDeclarationCreator;
import ch.tsphp.tinsphp.inference_engine.resolver.SymbolCheckController;
import ch.tsphp.tinsphp.inference_engine.resolver.SymbolResolverController;
import ch.tsphp.tinsphp.inference_engine.resolver.UserSymbolResolver;
import ch.tsphp.tinsphp.inference_engine.utils.AstModificationHelper;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;

import java.util.ArrayList;
import java.util.List;

public class HardCodedInferenceEngineInitialiser implements IInferenceEngineInitialiser
{
    private final IScopeHelper scopeHelper;
    private final IModifierHelper modifierHelper;
    private final ISymbolFactory symbolFactory;
    private final IScopeFactory scopeFactory;
    private final IInferenceIssueReporter inferenceIssueReporter;
    private final IConstraintCreator constraintCreator;
    private final IAstModificationHelper astModificationHelper;
    private final ICore core;
    private final IConstraintSolver constraintSolver;
    private final List<ISymbolResolver> additionalSymbolResolvers;

    private InferenceEngine engine;
    private IDefinitionPhaseController definitionPhaseController;
    private IReferencePhaseController referencePhaseController;

    public HardCodedInferenceEngineInitialiser(
            ITSPHPAstAdaptor theAstAdaptor,
            IAstHelper astHelper,
            ISymbolsInitialiser symbolsInitialiser,
            ICoreInitialiser coreInitialiser) {

        scopeHelper = symbolsInitialiser.getScopeHelper();
        modifierHelper = symbolsInitialiser.getModifierHelper();
        IOverloadResolver overloadResolver = symbolsInitialiser.getOverloadResolver();
        core = coreInitialiser.getCore();

        symbolFactory = symbolsInitialiser.getSymbolFactory();
        scopeFactory = symbolsInitialiser.getScopeFactory();

        inferenceIssueReporter = new InferenceIssueReporter(new HardCodedIssueMessageProvider());
        constraintCreator = new ConstraintCreator(symbolFactory, overloadResolver, inferenceIssueReporter);

        astModificationHelper = new AstModificationHelper(astHelper);

        additionalSymbolResolvers = new ArrayList<>();
        additionalSymbolResolvers.add(coreInitialiser.getCoreSymbolResolver());

        constraintSolver = new ConstraintSolver(symbolFactory, overloadResolver, inferenceIssueReporter);

        init();

        engine = new InferenceEngine(
                theAstAdaptor, inferenceIssueReporter, definitionPhaseController, referencePhaseController);
    }

    private void init() {
        definitionPhaseController = new DefinitionPhaseController(symbolFactory, scopeFactory);

        ISymbolResolver userSymbolResolver = new UserSymbolResolver(
                scopeHelper,
                definitionPhaseController.getGlobalNamespaceScopes(),
                definitionPhaseController.getGlobalDefaultNamespace());

        ISymbolResolverController symbolResolverController = new SymbolResolverController(
                userSymbolResolver,
                additionalSymbolResolvers,
                scopeHelper,
                symbolFactory,
                inferenceIssueReporter);
        ISymbolCheckController symbolCheckController = new SymbolCheckController(userSymbolResolver,
                additionalSymbolResolvers);

        IVariableDeclarationCreator variableDeclarationCreator =
                new PutAtTopVariableDeclarationCreator(symbolFactory, astModificationHelper, definitionPhaseController);

        referencePhaseController = new ReferencePhaseController(
                symbolFactory,
                inferenceIssueReporter,
                astModificationHelper,
                symbolResolverController,
                symbolCheckController,
                variableDeclarationCreator,
                scopeHelper,
                modifierHelper,
                constraintCreator,
                constraintSolver,
                core,
                definitionPhaseController.getGlobalDefaultNamespace()
        );
    }

    @Override
    public void reset() {
        inferenceIssueReporter.reset();
        init();
        engine.setDefinitionPhaseController(definitionPhaseController);
        engine.setReferencePhaseController(referencePhaseController);
    }

    @Override
    public IInferenceEngine getEngine() {
        return engine;
    }

    @Override
    public IGlobalNamespaceScope getGlobalDefaultNamespace() {
        return definitionPhaseController.getGlobalDefaultNamespace();
    }
}
