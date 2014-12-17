/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit;

import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.tinsphp.common.IInferenceEngine;
import ch.tsphp.tinsphp.common.inference.IInferenceEngineInitialiser;
import ch.tsphp.tinsphp.inference_engine.InferenceEngine;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InferenceEngineTest
{

    class DummyInferenceEngine extends InferenceEngine
    {

        public DummyInferenceEngine(IInferenceEngineInitialiser initialiser) {
            inferenceEngineInitialiser = initialiser;
        }
    }

    @Test
    public void hasFoundError_NoIssueOccurred_ReturnsFalse() {
        //no arrange necessary

        IInferenceEngine inferenceEngine = createInferenceEngine();
        boolean result = inferenceEngine.hasFoundError();

        assertThat(result, is(false));
    }

    @Test
    public void hasFoundError_LoggedOne_ReturnsTrue() {
        //no arrange necessary

        InferenceEngine inferenceEngine = createInferenceEngineImpl();
        inferenceEngine.log(new TSPHPException());
        boolean result = inferenceEngine.hasFoundError();

        assertThat(result, is(true));
    }

    @Test
    public void reset_Standard_CallsResetOnInitialiser() {
        IInferenceEngineInitialiser initialiser = mock(IInferenceEngineInitialiser.class);

        DummyInferenceEngine inferenceEngine = new DummyInferenceEngine(initialiser);
        inferenceEngine.reset();

        verify(initialiser).reset();
    }

    private IInferenceEngine createInferenceEngine() {
        return createInferenceEngineImpl();
    }

    protected InferenceEngine createInferenceEngineImpl() {
        return new InferenceEngine();
    }
}
