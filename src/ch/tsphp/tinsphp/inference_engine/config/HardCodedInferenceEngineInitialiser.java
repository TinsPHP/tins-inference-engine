/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.config;

import ch.tsphp.common.AstHelperRegistry;
import ch.tsphp.common.IAstHelper;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.checking.ISymbolCheckController;
import ch.tsphp.tinsphp.common.inference.IConstraintCreator;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.inference.IInferenceEngineInitialiser;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolver;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolverController;
import ch.tsphp.tinsphp.common.scopes.IScopeFactory;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.core.Core;
import ch.tsphp.tinsphp.inference_engine.ConstraintCreator;
import ch.tsphp.tinsphp.inference_engine.DefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.ReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.issues.HardCodedIssueMessageProvider;
import ch.tsphp.tinsphp.inference_engine.issues.InferenceIssueReporter;
import ch.tsphp.tinsphp.inference_engine.resolver.PutAtTopVariableDeclarationCreator;
import ch.tsphp.tinsphp.inference_engine.resolver.SymbolCheckController;
import ch.tsphp.tinsphp.inference_engine.resolver.SymbolResolverController;
import ch.tsphp.tinsphp.inference_engine.resolver.UserSymbolResolver;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeFactory;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeHelper;
import ch.tsphp.tinsphp.inference_engine.utils.AstModificationHelper;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import ch.tsphp.tinsphp.symbols.ModifierHelper;
import ch.tsphp.tinsphp.symbols.SymbolFactory;
import ch.tsphp.tinsphp.symbols.utils.OverloadResolver;

import java.util.ArrayList;
import java.util.List;

public class HardCodedInferenceEngineInitialiser implements IInferenceEngineInitialiser
{
    private final IScopeHelper scopeHelper;
    private final IModifierHelper modifierHelper;
    private final ISymbolFactory symbolFactory;
    private final IScopeFactory scopeFactory;
    private final IAstModificationHelper astModificationHelper;
    private final IConstraintCreator constraintCreator;
    private final IConstraintSolver constraintSolver;


    private IDefinitionPhaseController definitionPhaseController;
    private IReferencePhaseController referencePhaseController;
    private IConstraintCreator inferencePhaseController;
    private InferenceIssueReporter inferenceErrorReporter;
    private ICore core;
    private final List<ISymbolResolver> additionalSymbolResolvers;
    private final IOverloadResolver overloadResolver;

    public HardCodedInferenceEngineInitialiser() {
        scopeHelper = new ScopeHelper();
        modifierHelper = new ModifierHelper();
        overloadResolver = new OverloadResolver();

        symbolFactory = new SymbolFactory(scopeHelper, modifierHelper, overloadResolver);
        scopeFactory = new ScopeFactory(scopeHelper);
        constraintSolver = new ConstraintSolver(overloadResolver);
        inferenceErrorReporter = new InferenceIssueReporter(new HardCodedIssueMessageProvider());
        constraintCreator = new ConstraintCreator(symbolFactory, overloadResolver, inferenceErrorReporter);

        IAstHelper astHelper = AstHelperRegistry.get();
        astModificationHelper = new AstModificationHelper(astHelper);
        additionalSymbolResolvers = new ArrayList<>();

        core = new Core(symbolFactory, overloadResolver, astHelper);
        additionalSymbolResolvers.add(core.getCoreSymbolResolver());

        init();
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
                inferenceErrorReporter);
        ISymbolCheckController symbolCheckController = new SymbolCheckController(userSymbolResolver,
                additionalSymbolResolvers);

        IVariableDeclarationCreator variableDeclarationCreator =
                new PutAtTopVariableDeclarationCreator(symbolFactory, astModificationHelper, definitionPhaseController);

        referencePhaseController = new ReferencePhaseController(
                symbolFactory,
                inferenceErrorReporter,
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

        inferencePhaseController = new ConstraintCreator(
                symbolFactory,
                overloadResolver,
                inferenceErrorReporter
        );
    }

    @Override
    public IDefinitionPhaseController getDefinitionPhaseController() {
        return definitionPhaseController;
    }

    @Override
    public IReferencePhaseController getReferencePhaseController() {
        return referencePhaseController;
    }

    @Override
    public IInferenceIssueReporter getInferenceErrorReporter() {
        return inferenceErrorReporter;
    }

    @Override
    public void reset() {
        inferenceErrorReporter.reset();
        init();
    }
}
