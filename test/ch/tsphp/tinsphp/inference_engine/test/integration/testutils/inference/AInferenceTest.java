/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceTest;
import org.junit.Ignore;

import java.util.EnumSet;

import static org.junit.Assert.assertFalse;

@Ignore
public abstract class AInferenceTest extends AReferenceTest
{

    public AInferenceTest(String testString) {
        super(testString);
    }

    protected abstract void assertsInInferencePhase();

    protected void checkNoIssueInInferencePhase() {
        assertFalse(testString + " failed. Exceptions occurred." + exceptions,
                inferenceErrorReporter.hasFound(EnumSet.allOf(EIssueSeverity.class)));
        assertsInInferencePhase();
    }

    @Override
    protected void assertsInReferencePhase() {
        afterAssertsInReferencePhase();
        checkNoIssueInInferencePhase();
    }

    protected void afterAssertsInReferencePhase() {
        referencePhaseController.solveConstraints();

        checkNoIssueInInferencePhase();
    }
}
