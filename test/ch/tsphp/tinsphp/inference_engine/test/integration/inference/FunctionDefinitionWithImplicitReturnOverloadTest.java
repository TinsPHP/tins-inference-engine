/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceOverloadTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.OverloadTestStruct;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import static ch.tsphp.tinsphp.common.TinsPHPConstants.RETURN_VARIABLE_NAME;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingCollectionMatcher.varBinding;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(Parameterized.class)
public class FunctionDefinitionWithImplicitReturnOverloadTest extends AInferenceOverloadTest
{

    public FunctionDefinitionWithImplicitReturnOverloadTest(String testString, OverloadTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Override
    protected void checkNoErrorsInReferencePhase() {
        assertTrue(testString + " failed. no return / partial return from function expected but not reported",
                inferenceIssueReporter.hasFound(EnumSet.allOf(EIssueSeverity.class)));
        assertFalse(testString + " failed. reference walker exceptions occurred.",
                reference.hasFound(EnumSet.allOf(EIssueSeverity.class)));
        inferenceIssueReporter.reset();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<String> asBool = asList("{as (falseType | trueType)}");
        return asList(new Object[][]{
                {
                        "function foo(){}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 0, bindingDtos(
                                varBinding(RETURN_VARIABLE_NAME, "V1", asList("nullType"), null, true)
                        )), 1, 0, 2)
                },
                {
                        "function foo($x){}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "V3", null, asList("mixed"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V1", asList("nullType"), null, true)
                        )), 1, 0, 2)
                },
                //partial return
                {
                        "function foo($x){if($x){ return 1;}}",
                        testStructs("foo()", "\\.\\.", functionDtos(
                                functionDto("foo()", 1, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("int", "nullType"), null, true),
                                        varBinding("foo()$x", "V5", null, asList("(falseType | trueType)"), true)
                                )),
                                functionDto("foo()", 1, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("int", "nullType"), null, true),
                                        varBinding("foo()$x", "V5", null, asBool, true)
                                ))), 1, 0, 2)
                },
        });
    }
}
