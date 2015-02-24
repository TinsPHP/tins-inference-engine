/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit;

import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.IInferenceEngine;
import ch.tsphp.tinsphp.common.inference.IInferenceEngineInitialiser;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.inference_engine.InferenceEngine;
import org.junit.Test;

import java.util.EnumSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InferenceEngineTest
{

    class DummyInferenceEngine extends InferenceEngine
    {

        public DummyInferenceEngine(ITSPHPAstAdaptor astAdaptor, IInferenceEngineInitialiser initialiser) {
            super(astAdaptor);
            inferenceEngineInitialiser = initialiser;
        }
    }

    @Test
    public void hasFoundError_NoIssueOccurred_ReturnsFalse() {
        //no arrange necessary

        IInferenceEngine inferenceEngine = createInferenceEngine();
        boolean result = inferenceEngine.hasFound(EnumSet.allOf(EIssueSeverity.class));

        assertThat(result, is(false));
    }

    @Test
    public void reset_Standard_CallsResetOnInitialiser() {
        IInferenceEngineInitialiser initialiser = mock(IInferenceEngineInitialiser.class);

        DummyInferenceEngine inferenceEngine = new DummyInferenceEngine(new TSPHPAstAdaptor(), initialiser);
        inferenceEngine.reset();

        verify(initialiser).reset();
    }

    private IInferenceEngine createInferenceEngine() {
        return createInferenceEngineImpl(new TSPHPAstAdaptor());
    }

    protected InferenceEngine createInferenceEngineImpl(ITSPHPAstAdaptor astAdaptor) {
        return new InferenceEngine(astAdaptor);
    }
}
