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

import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.OverloadBindingsMatcher.varBinding;
import static ch.tsphp.tinsphp.symbols.TypeVariableNames.RETURN_VARIABLE_NAME;
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
        return asList(new Object[][]{
                {
                        "function foo($x, $y){return $x + $y;}",
                        testStructs("foo()", "\\.\\.", functionDtos(
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T1", null, asList("num"), false),
                                        varBinding("foo()$y", "T1", null, asList("num"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T1", null, asList("num"), false))),
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", asList("bool"), asList("bool"), true),
                                        varBinding("foo()$y", "T3", asList("bool"), asList("bool"), true),
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
                                        varBinding("foo()$x", "T4", null, asList("num"), false),
                                        varBinding("foo()$y", "T4", null, asList("num"), false),
                                        varBinding("foo()$z", "T4", null, asList("num"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", null, asList("num"), false))),
                                functionDto("foo()", 3, bindingDtos(
                                        varBinding("foo()$x", "T2", asList("bool"), asList("bool"), true),
                                        varBinding("foo()$y", "T3", asList("bool"), asList("bool"), true),
                                        varBinding("foo()$z", "T4", asList("int"), asList("num"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("int"), asList("num"), false))),
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
                                        varBinding("foo()$x", "T8", null, asList("num"), false),
                                        varBinding("foo()$y", "T8", null, asList("num"), false),
                                        varBinding("foo()$a", "T8", null, asList("num"), false),
                                        varBinding("foo()$b", "T8", null, asList("num"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T8", null, asList("num"), false))),
                                functionDto("foo()", 4, bindingDtos(
                                        varBinding("foo()$x", "T2", asList("bool"), asList("bool"), true),
                                        varBinding("foo()$y", "T3", asList("bool"), asList("bool"), true),
                                        varBinding("foo()$a", "T8", asList("int"), asList("num"), false),
                                        varBinding("foo()$b", "T8", asList("int"), asList("num"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T8", asList("int"), asList("num"), false)))
                        ), 1, 0, 2)
                },
                {
                        "function foo($x, $y){return $x / $y;}",
                        testStructs("foo()", "\\.\\.", functionDtos(
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", asList("bool"), asList("bool"), true),
                                        varBinding("foo()$y", "T3", asList("bool"), asList("bool"), true),
                                        varBinding(RETURN_VARIABLE_NAME,
                                                "T4", asList("int", "false"), asList("(int | false)"), true))),
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", asList("float"), asList("num"), false),
                                        varBinding("foo()$y", "T2", asList("float"), asList("num"), false),
                                        varBinding(RETURN_VARIABLE_NAME,
                                                "T4", asList("@T2", "float", "false"), null, false)))
                        ), 1, 0, 2)
                },
        });
    }
}