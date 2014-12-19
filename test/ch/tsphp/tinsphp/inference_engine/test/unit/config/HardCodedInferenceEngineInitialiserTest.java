/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.config;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.inference.IInferenceEngineInitialiser;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.inference_engine.config.HardCodedInferenceEngineInitialiser;
import org.junit.Test;

import java.util.EnumSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class HardCodedInferenceEngineInitialiserTest
{
    @Test
    public void getDefinitionPhaseController_CallTwice_IsTheSame() {
        //no arrange necessary

        IInferenceEngineInitialiser initialiser = createInferenceEngineInitialiser();
        IDefinitionPhaseController result1 = initialiser.getDefinitionPhaseController();
        IDefinitionPhaseController result2 = initialiser.getDefinitionPhaseController();

        assertThat(result1, is(result2));
    }

    @Test
    public void getReferencePhaseController_CallTwice_IsTheSame() {
        //no arrange necessary

        IInferenceEngineInitialiser initialiser = createInferenceEngineInitialiser();
        IReferencePhaseController result1 = initialiser.getReferencePhaseController();
        IReferencePhaseController result2 = initialiser.getReferencePhaseController();

        assertThat(result1, is(result2));
    }

    @Test
    public void reset_Standard_DefinitionAndReferencePhaseControllerIsNewAndErrorReporterIsTheSame() {
        //no arrange necessary

        IInferenceEngineInitialiser initialiser = createInferenceEngineInitialiser();
        IDefinitionPhaseController definitionPhaseController1 = initialiser.getDefinitionPhaseController();
        IReferencePhaseController referencePhaseController1 = initialiser.getReferencePhaseController();
        IInferenceIssueReporter inferenceErrorReporter1 = initialiser.getInferenceErrorReporter();
        initialiser.reset();
        IDefinitionPhaseController definitionPhaseController2 = initialiser.getDefinitionPhaseController();
        IReferencePhaseController referencePhaseController2 = initialiser.getReferencePhaseController();
        IInferenceIssueReporter inferenceErrorReporter2 = initialiser.getInferenceErrorReporter();

        assertThat(definitionPhaseController1, is(not(definitionPhaseController2)));
        assertThat(referencePhaseController1, is(not(referencePhaseController2)));
        assertThat(inferenceErrorReporter1, is(inferenceErrorReporter2));
    }

    @Test
    public void reset_Standard_InferenceEngineGetsResetAsWell() {
        //no arrange necessary

        IInferenceEngineInitialiser initialiser = createInferenceEngineInitialiser();
        IInferenceIssueReporter inferenceErrorReporter = initialiser.getInferenceErrorReporter();
        assertThat(inferenceErrorReporter.hasFound(EnumSet.allOf(EIssueSeverity.class)), is(false));
        inferenceErrorReporter.noReturnFromFunction(mock(ITSPHPAst.class));
        assertThat(inferenceErrorReporter.hasFound(EnumSet.allOf(EIssueSeverity.class)), is(true));
        initialiser.reset();

        assertThat(inferenceErrorReporter.hasFound(EnumSet.allOf(EIssueSeverity.class)), is(false));
    }

    protected IInferenceEngineInitialiser createInferenceEngineInitialiser() {
        return new HardCodedInferenceEngineInitialiser();
    }
}
