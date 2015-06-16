/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AReferenceErrorTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference;

import ch.tsphp.common.exceptions.ReferenceException;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.issues.ReferenceIssueDto;
import org.junit.Ignore;

import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public abstract class AReferenceErrorTest extends AReferenceTest
{

    protected ReferenceIssueDto[] errorDtos;

    public AReferenceErrorTest(String testString, ReferenceIssueDto[] theErrorDtos) {
        super(testString);
        errorDtos = theErrorDtos;
    }

    @Override
    protected void checkNoErrorsInReferencePhase() {
        assertsInReferencePhase();
    }

    @Override
    public void assertsInReferencePhase() {
        verifyReferences(errorMessagePrefix, exceptions, errorDtos, inferenceIssueReporter);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public static void verifyReferences(String errorMessagePrefix, List<Exception> exceptions,
            ReferenceIssueDto[] errorDtos, IInferenceIssueReporter theInferenceErrorReporter) {
        assertTrue(errorMessagePrefix + " failed. No exception occurred.",
                theInferenceErrorReporter.hasFound(EnumSet.allOf(EIssueSeverity.class)));

        assertEquals(errorMessagePrefix + " failed. More or less exceptions occurred." + exceptions.toString(),
                errorDtos.length, exceptions.size());

        for (int i = 0; i < errorDtos.length; ++i) {
            ReferenceException exception = (ReferenceException) exceptions.get(i);

            assertEquals(errorMessagePrefix + " failed. wrong identifier.",
                    errorDtos[i].identifier, exception.getDefinition().getText());

            assertEquals(errorMessagePrefix + " failed. wrong line.",
                    errorDtos[i].line, exception.getDefinition().getLine());

            assertEquals(errorMessagePrefix + " failed. wrong position.",
                    errorDtos[i].position, exception.getDefinition().getCharPositionInLine());
        }

    }
}
