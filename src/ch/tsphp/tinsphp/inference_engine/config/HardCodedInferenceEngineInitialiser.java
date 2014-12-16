/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.config;

import ch.tsphp.common.AstHelperRegistry;
import ch.tsphp.common.IAstHelper;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.inference.IInferenceEngineInitialiser;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.common.inference.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolver;
import ch.tsphp.tinsphp.common.scopes.IScopeFactory;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.core.Core;
import ch.tsphp.tinsphp.inference_engine.DefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.ReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.error.InferenceErrorReporter;
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

import java.util.ArrayList;
import java.util.List;

public class HardCodedInferenceEngineInitialiser implements IInferenceEngineInitialiser
{
    private final IScopeHelper scopeHelper;
    private final IModifierHelper modifierHelper;
    private final ISymbolFactory symbolFactory;
    private final IScopeFactory scopeFactory;
    private final IAstModificationHelper astModificationHelper;
    private final SymbolResolverController symbolResolverController;
    private final SymbolCheckController symbolCheckController;
    private final PutAtTopVariableDeclarationCreator variableDeclarationCreator;

    private IDefinitionPhaseController definitionPhaseController;
    private IReferencePhaseController referencePhaseController;
    private InferenceErrorReporter inferenceErrorReporter;
    private ICore core;

    public HardCodedInferenceEngineInitialiser() {
        scopeHelper = new ScopeHelper();
        modifierHelper = new ModifierHelper();
        symbolFactory = new SymbolFactory(scopeHelper, modifierHelper);
        scopeFactory = new ScopeFactory(scopeHelper);
        inferenceErrorReporter = new InferenceErrorReporter();

        createDefinitionPhaseController();

        IAstHelper astHelper = AstHelperRegistry.get();
        astModificationHelper = new AstModificationHelper(astHelper);
        ISymbolResolver userSymbolResolver = new UserSymbolResolver(
                scopeHelper,
                definitionPhaseController.getGlobalNamespaceScopes(),
                definitionPhaseController.getGlobalDefaultNamespace());
        List<ISymbolResolver> additionalSymbolResolvers = new ArrayList<>();
        core = new Core(symbolFactory, astHelper);
        additionalSymbolResolvers.add(core.getCoreSymbolResolver());
        symbolResolverController = new SymbolResolverController(
                userSymbolResolver,
                additionalSymbolResolvers,
                scopeHelper,
                symbolFactory,
                inferenceErrorReporter);
        symbolCheckController = new SymbolCheckController(userSymbolResolver, additionalSymbolResolvers);
        variableDeclarationCreator =
                new PutAtTopVariableDeclarationCreator(astModificationHelper, definitionPhaseController);

        createReferencePhaseController();
    }

    private void createDefinitionPhaseController() {
        definitionPhaseController = new DefinitionPhaseController(symbolFactory, scopeFactory);
    }

    private void createReferencePhaseController() {
        referencePhaseController = new ReferencePhaseController(
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

    @Override
    public IDefinitionPhaseController getDefinitionPhaseController() {
        return definitionPhaseController;
    }

    @Override
    public IReferencePhaseController getReferencePhaseController() {
        return referencePhaseController;
    }

    @Override
    public IInferenceErrorReporter getInferenceErrorReporter() {
        return inferenceErrorReporter;
    }

    @Override
    public void reset() {
        inferenceErrorReporter.reset();
        createDefinitionPhaseController();
        createReferencePhaseController();
    }
}
