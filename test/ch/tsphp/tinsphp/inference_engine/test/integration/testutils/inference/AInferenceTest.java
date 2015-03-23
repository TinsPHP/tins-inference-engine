/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

import ch.tsphp.tinsphp.common.inference.IInferencePhaseController;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.inference_engine.InferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPInferenceWalker;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.WriteExceptionToConsole;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceTest;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.EnumSet;

import static org.junit.Assert.assertFalse;

@Ignore
public abstract class AInferenceTest extends AReferenceTest
{

    protected ErrorReportingTinsPHPInferenceWalker inference;
    protected IInferencePhaseController inferencePhaseController;

    public AInferenceTest(String testString) {
        super(testString);

        init();
    }

    private void init() {
        inferencePhaseController = createInferencePhaseController(
                symbolFactory,
                inferenceErrorReporter,
                constraintSolver,
                definitionPhaseController.getGlobalDefaultNamespace());
    }


    protected abstract void assertsInInferencePhase();

    protected void checkNoIssueInInferencePhase() {
        assertFalse(testString + " failed. Exceptions occurred." + exceptions,
                inferenceErrorReporter.hasFound(EnumSet.allOf(EIssueSeverity.class)));
        assertFalse(testString + " failed. inference walker exceptions occurred.",
                inference.hasFound(EnumSet.allOf(EIssueSeverity.class)));

        assertsInInferencePhase();
    }

    @Override
    protected void assertsInReferencePhase() {
        afterAssertsInReferencePhase();
        checkNoIssueInInferencePhase();
    }

    protected void afterAssertsInReferencePhase() {
        commonTreeNodeStream.reset();
        inference = createInferenceWalker(
                commonTreeNodeStream,
                inferencePhaseController,
                definitionPhaseController.getGlobalDefaultNamespace());
        registerInferenceErrorLogger();

        try {
            inference.compilationUnit();
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

        inferencePhaseController.solveAllConstraints();

        checkNoErrorsInReferencePhase();
    }

    protected void registerInferenceErrorLogger() {
        inference.registerIssueLogger(new WriteExceptionToConsole());
    }

    protected IInferencePhaseController createInferencePhaseController(
            ISymbolFactory theSymbolFactory,
            IInferenceIssueReporter theInferenceErrorReporter,
            IConstraintSolver theConstraintSolver,
            IGlobalNamespaceScope theGlobalDefaultNamespaceScope) {
        return new InferencePhaseController(
                theSymbolFactory,
                theInferenceErrorReporter,
                theConstraintSolver, theGlobalDefaultNamespaceScope);
    }

    protected ErrorReportingTinsPHPInferenceWalker createInferenceWalker(
            CommonTreeNodeStream theCommonTreeNodeStream,
            IInferencePhaseController theController,
            IGlobalNamespaceScope theGlobalDefaultNamespaceScope) {
        return new ErrorReportingTinsPHPInferenceWalker(
                theCommonTreeNodeStream, theController, theGlobalDefaultNamespaceScope);
    }
}
