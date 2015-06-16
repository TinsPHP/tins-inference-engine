/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IIssueMessageProvider;
import ch.tsphp.tinsphp.common.issues.WrongArgumentTypeIssueDto;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.ConstraintErrorDto;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.EnumSet;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.DescribedAs.describedAs;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(Parameterized.class)
public class ConstraintErrorTest extends AInferenceTest
{
    private final ConstraintErrorDto[] dtos;

    public ConstraintErrorTest(String testString, ConstraintErrorDto[] theDtos) {
        super(testString);
        dtos = theDtos;
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Override
    protected void assertsInInferencePhase() {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<WrongArgumentTypeIssueDto> dtoCaptor = ArgumentCaptor.forClass(WrongArgumentTypeIssueDto.class);

        try {
            verify(issueMessageProvider, times(dtos.length))
                    .getWrongArgumentTypeIssueMessage(keyCaptor.capture(), dtoCaptor.capture());
        } catch (AssertionError ex) {
            System.out.println(testString + " - failed. provider for " + dtos[0].key + " was not called (enough).");
            throw ex;
        }
        for (int i = 0; i < dtos.length; ++i) {
            assertThat(keyCaptor.getAllValues().get(i),
                    describedAs(testString + " - failed. key expected " + dtos[i].key, is(dtos[i].key)));
            WrongArgumentTypeIssueDto dto = dtoCaptor.getAllValues().get(i);
            assertEquals(errorMessagePrefix + " failed. wrong identifier.",
                    dtos[i].identifier, dto.identifier);

            assertEquals(errorMessagePrefix + " failed. wrong line.",
                    dtos[i].line, dto.line);

            assertEquals(errorMessagePrefix + " failed. wrong position.",
                    dtos[i].position, dto.position);
        }
    }

    @Override
    protected void checkNoIssueInInferencePhase() {
        assertTrue(testString + " failed. Exceptions did not occur but we expected a wrongOperatorUsage." + exceptions,
                inferenceErrorReporter.hasFound(EnumSet.of(EIssueSeverity.Error)));
        assertsInInferencePhase();
    }

    protected IIssueMessageProvider createIssueMessageProvider() {
        return spy(super.createIssueMessageProvider());
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return asList(new Object[][]{
                {"$a = \n strpos('hello', 1);", dtos("wrongFunctionCall", "strpos()", 2, 1)},
                {"$a = 1; \n strpos('hello', $a);", dtos("wrongFunctionCall", "strpos()", 2, 1)},
                {"$a = \n strpos('hello');", dtos("wrongFunctionCall", "strpos()", 2, 1)},
                {"[0] \n+ 1;", dtos("wrongOperatorUsage", "+", 2, 0)},
                {"[0] \n- 1;", dtos("wrongOperatorUsage", "-", 2, 0)},
                {"[0] \n* 1;", dtos("wrongOperatorUsage", "*", 2, 0)},
                {"function foo1(array $x){ return $x \n+ 1; }", dtos("wrongOperatorUsage", "+", 2, 0)},
                {
                        "function foo2(array $x){ $x = bar(); return $x \n+ 1;} function bar(){return [1];}",
                        dtos("wrongOperatorUsage", "+", 2, 0)
                },
                //$a does not change due to the recursive call
                {
                        "function endless5(array $a){ $a = endless5(); return $a \n+ 1;}",
                        dtos("wrongOperatorUsage", "+", 2, 0)
                }
        });
    }

    private static ConstraintErrorDto[] dtos(String key, String name, int line, int pos) {
        return new ConstraintErrorDto[]{new ConstraintErrorDto(key, name, line, pos)};
    }
}
