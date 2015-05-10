/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceOverloadTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.OverloadTestStruct;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;

import static ch.tsphp.tinsphp.common.TinsPHPConstants.RETURN_VARIABLE_NAME;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.OverloadBindingsMatcher.varBinding;
import static java.util.Arrays.asList;


@RunWith(Parameterized.class)
public class FunctionDefinitionMultipleOverloadTest extends AInferenceOverloadTest
{

    public FunctionDefinitionMultipleOverloadTest(String testString, OverloadTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<String> boolLower = asList("falseType", "trueType");
        List<String> boolUpper = asList("(falseType | trueType)");
        List<String> numUpper = asList("(float | int)");
        return asList(new Object[][]{
                {
                        "function foo($x, $y){return $x + $y;}",
                        testStructs("foo()", "\\.\\.", functionDtos(
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T1", null, numUpper, false),
                                        varBinding("foo()$y", "T1", null, numUpper, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T1", null, numUpper, false))),
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", boolLower, boolUpper, true),
                                        varBinding("foo()$y", "T3", boolLower, boolUpper, true),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("int"), asList("int"), true))),
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", asList("array"), asList("array"), true),
                                        varBinding("foo()$y", "T3", asList("array"), asList("array"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("array"), asList("array"), true)))
                        ), 1, 0, 2)
                },
                {
                        "function foo($x, $y, $z){return $x + $y + $z;}",
                        testStructs("foo()", "\\.\\.", functionDtos(
                                functionDto("foo()", 3, bindingDtos(
                                        varBinding("foo()$x", "T4", null, numUpper, false),
                                        varBinding("foo()$y", "T4", null, numUpper, false),
                                        varBinding("foo()$z", "T4", null, numUpper, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", null, numUpper, false))),
                                functionDto("foo()", 3, bindingDtos(
                                        varBinding("foo()$x", "T2", boolLower, boolUpper, true),
                                        varBinding("foo()$y", "T3", boolLower, boolUpper, true),
                                        varBinding("foo()$z", "T4", asList("int"), numUpper, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("int"), numUpper, false))),
                                functionDto("foo()", 3, bindingDtos(
                                        varBinding("foo()$x", "T2", asList("array"), asList("array"), true),
                                        varBinding("foo()$y", "T3", asList("array"), asList("array"), true),
                                        varBinding("foo()$z", "T5", asList("array"), asList("array"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T6", asList("array"), asList("array"), true)))
                        ), 1, 0, 2)
                },
                {
                        "function foo($x, $y, $a, $b){return $a * ($x + $y) - $a * $b;}",
                        testStructs("foo()", "\\.\\.", functionDtos(
                                functionDto("foo()", 4, bindingDtos(
                                        varBinding("foo()$x", "T8", null, numUpper, false),
                                        varBinding("foo()$y", "T8", null, numUpper, false),
                                        varBinding("foo()$a", "T8", null, numUpper, false),
                                        varBinding("foo()$b", "T8", null, numUpper, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T8", null, numUpper, false))),
                                functionDto("foo()", 4, bindingDtos(
                                        varBinding("foo()$x", "T2", boolLower, boolUpper, true),
                                        varBinding("foo()$y", "T3", boolLower, boolUpper, true),
                                        varBinding("foo()$a", "T8", asList("int"), numUpper, false),
                                        varBinding("foo()$b", "T8", asList("int"), numUpper, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T8", asList("int"), numUpper, false)))
                        ), 1, 0, 2)
                },
                {
                        "function foo($x, $y){return $x / $y;}",
                        testStructs("foo()", "\\.\\.", functionDtos(
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", boolLower, boolUpper, true),
                                        varBinding("foo()$y", "T3", boolLower, boolUpper, true),
                                        varBinding(RETURN_VARIABLE_NAME, "T4",
                                                asList("int", "falseType"), asList("(falseType | int)"), true))),
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", asList("float"), numUpper, false),
                                        varBinding("foo()$y", "T2", asList("float"), numUpper, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4",
                                                asList("@T2", "float", "falseType"), null, false)))
                        ), 1, 0, 2)
                },
        });
    }
}
