/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AReferenceDefinitionErrorTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference;

import ch.tsphp.common.exceptions.DefinitionException;
import ch.tsphp.tinsphp.common.issues.DefinitionIssueDto;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.EnumSet;
import java.util.List;

@Ignore
public abstract class AReferenceDefinitionErrorTest extends AReferenceTest
{

    protected DefinitionIssueDto[] errorDtos;

    public AReferenceDefinitionErrorTest(String testString, DefinitionIssueDto[] theErrorDtos) {
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
            DefinitionIssueDto[] errorDtos, IInferenceIssueReporter theInferenceErrorReporter) {

        Assert.assertTrue(errorMessagePrefix + " failed. No reference exception occurred.",
                theInferenceErrorReporter.hasFound(EnumSet.allOf(EIssueSeverity.class)));

        Assert.assertEquals(errorMessagePrefix + " failed. More or less reference exceptions occurred." + exceptions
                        .toString(),
                errorDtos.length, exceptions.size());

        for (int i = 0; i < errorDtos.length; ++i) {
            DefinitionException exception = (DefinitionException) exceptions.get(i);

            Assert.assertEquals(errorMessagePrefix + " failed. wrong existing identifier.",
                    errorDtos[i].identifier, exception.getExistingDefinition().getText());
            Assert.assertEquals(errorMessagePrefix + " failed. wrong existing line.",
                    errorDtos[i].line, exception.getExistingDefinition().getLine());
            Assert.assertEquals(errorMessagePrefix + " failed. wrong existing position.",
                    errorDtos[i].position, exception.getExistingDefinition().getCharPositionInLine());

            Assert.assertEquals(errorMessagePrefix + " failed. wrong new identifier.",
                    errorDtos[i].identifierNewDefinition, exception.getNewDefinition().getText());
            Assert.assertEquals(errorMessagePrefix + " failed. wrong new line. ",
                    errorDtos[i].lineNewDefinition, exception.getNewDefinition().getLine());
            Assert.assertEquals(errorMessagePrefix + " failed. wrong new position. ",
                    errorDtos[i].positionNewDefinition, exception.getNewDefinition().getCharPositionInLine());
        }

    }
}
