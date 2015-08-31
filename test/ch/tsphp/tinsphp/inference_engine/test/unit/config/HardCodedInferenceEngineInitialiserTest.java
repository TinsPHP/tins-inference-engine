/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.config;

import ch.tsphp.common.AstHelper;
import ch.tsphp.common.IAstHelper;
import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.IInferenceEngine;
import ch.tsphp.tinsphp.common.config.ICoreInitialiser;
import ch.tsphp.tinsphp.common.config.IInferenceEngineInitialiser;
import ch.tsphp.tinsphp.common.config.ISymbolsInitialiser;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.core.config.HardCodedCoreInitialiser;
import ch.tsphp.tinsphp.inference_engine.config.HardCodedInferenceEngineInitialiser;
import ch.tsphp.tinsphp.symbols.config.HardCodedSymbolsInitialiser;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

public class HardCodedInferenceEngineInitialiserTest
{
    @Test
    public void getEngine_SecondCall_ReturnsSameInstanceAsFirstCall() {
        IInferenceEngineInitialiser initialiser = createInitialiser();
        IInferenceEngine firstCall = initialiser.getEngine();

        IInferenceEngine result = initialiser.getEngine();

        assertThat(result, is(firstCall));
    }

    @Test
    public void getEngine_SecondCallAfterReset_ReturnsSameInstanceAsFirstCallBeforeReset() {
        IInferenceEngineInitialiser initialiser = createInitialiser();
        IInferenceEngine firstCall = initialiser.getEngine();
        initialiser.reset();

        IInferenceEngine result = initialiser.getEngine();

        assertThat(result, is(firstCall));
    }

    @Test
    public void getGlobalDefaultNamespace_SecondCall_ReturnsSameInstanceAsFirstCall() {
        IInferenceEngineInitialiser initialiser = createInitialiser();
        IGlobalNamespaceScope firstCall = initialiser.getGlobalDefaultNamespace();

        IGlobalNamespaceScope result = initialiser.getGlobalDefaultNamespace();

        assertThat(result, is(firstCall));
    }

    @Test
    public void getGlobalDefaultNamespace_SecondCallAfterReset_ReturnsADifferentInstance() {
        IInferenceEngineInitialiser initialiser = createInitialiser();
        IGlobalNamespaceScope firstCall = initialiser.getGlobalDefaultNamespace();
        initialiser.reset();

        IGlobalNamespaceScope result = initialiser.getGlobalDefaultNamespace();

        assertThat(result, is(not(firstCall)));
    }

    private IInferenceEngineInitialiser createInitialiser() {
        ITSPHPAstAdaptor astAdaptor = new TSPHPAstAdaptor();
        IAstHelper astHelper = new AstHelper(astAdaptor);
        ISymbolsInitialiser symbolsInitialiser = new HardCodedSymbolsInitialiser();
        ICoreInitialiser coreInitialiser = new HardCodedCoreInitialiser(astHelper, symbolsInitialiser);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        return new HardCodedInferenceEngineInitialiser(
                astAdaptor, astHelper, symbolsInitialiser, coreInitialiser, executorService);
    }

}
