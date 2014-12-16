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
import ch.tsphp.tinsphp.common.inference.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.error.ReferenceErrorDto;
import org.junit.Ignore;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public abstract class AReferenceErrorTest extends AReferenceTest
{

    protected ReferenceErrorDto[] errorDtos;

    public AReferenceErrorTest(String testString, ReferenceErrorDto[] theErrorDtos) {
        super(testString);
        errorDtos = theErrorDtos;
    }

    @Override
    protected void checkReferences() {
        verifyReferences();
    }

    @Override
    public void verifyReferences() {
        verifyReferences(errorMessagePrefix, exceptions, errorDtos, inferenceErrorReporter);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public static void verifyReferences(String errorMessagePrefix, List<Exception> exceptions,
            ReferenceErrorDto[] errorDtos, IInferenceErrorReporter theInferenceErrorReporter) {
        assertTrue(errorMessagePrefix + " failed. No exception occurred.", theInferenceErrorReporter.hasFoundError());

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
