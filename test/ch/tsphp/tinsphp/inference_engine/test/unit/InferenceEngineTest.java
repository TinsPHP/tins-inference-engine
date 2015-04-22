/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit;

import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.IInferenceEngine;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.inference_engine.InferenceEngine;
import org.junit.Test;

import java.util.EnumSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class InferenceEngineTest
{

    @Test
    public void hasFoundError_NoIssueOccurred_ReturnsFalse() {
        //no arrange necessary

        IInferenceEngine inferenceEngine = createInferenceEngine();
        boolean result = inferenceEngine.hasFound(EnumSet.allOf(EIssueSeverity.class));

        assertThat(result, is(false));
    }

    private IInferenceEngine createInferenceEngine() {
        return createInferenceEngine(mock(ITSPHPAstAdaptor.class),
                mock(IInferenceIssueReporter.class),
                mock(IDefinitionPhaseController.class),
                mock(IReferencePhaseController.class));
    }

    protected IInferenceEngine createInferenceEngine(ITSPHPAstAdaptor theAstAdaptor,
            IInferenceIssueReporter theInferenceIssueReporter,
            IDefinitionPhaseController theDefinitionPhaseController,
            IReferencePhaseController theReferencePhaseController) {
        return new InferenceEngine(
                theAstAdaptor,
                theInferenceIssueReporter,
                theDefinitionPhaseController,
                theReferencePhaseController
        );
    }
}
