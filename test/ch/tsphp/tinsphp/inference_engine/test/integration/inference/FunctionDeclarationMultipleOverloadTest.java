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
public class FunctionDeclarationMultipleOverloadTest extends AInferenceOverloadTest
{

    public FunctionDeclarationMultipleOverloadTest(String testString, OverloadTestStruct[] theTestStructs) {
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
                                        varBinding("foo()$x", "T4", asList("@T4"), asList("num"), false),
                                        varBinding("foo()$y", "T4", asList("@T4"), asList("num"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("@T4"), asList("num"), false))),
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", null, asList("bool"), true),
                                        varBinding("foo()$y", "T3", null, asList("bool"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("int"), null, true))),
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", null, asList("array"), true),
                                        varBinding("foo()$y", "T3", null, asList("array"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("array"), null, true)))
                        ), 1, 0, 2)
                },
                //TODO TINS-385 propagate lower bounds to dependencies - should not be constant
//            {
//                    "function foo($x, $y, $z){return $x + $y + $z;}",
//                    testStructs("foo()", "\\.\\.", functionDtos(
//                            functionDto("foo()", 3, bindingDtos(
//                                    varBinding("foo()$x", "T6", asList("@T6"), asList("num"), false),
//                                    varBinding("foo()$y", "T6", asList("@T6"), asList("num"), false),
//                                    varBinding("foo()$z", "T6", asList("@T6"), asList("num"), false),
//                                    varBinding(RETURN_VARIABLE_NAME, "T6", asList("@T6"), asList("num"), false))),
//                            functionDto("foo()", 3, bindingDtos(
//                                    varBinding("foo()$x", "T2", null, asList("bool"), true),
//                                    varBinding("foo()$y", "T3", null, asList("bool"), true),
//                                    varBinding("foo()$z", "T6", asList("T6","int"), asList("num"), true),
//                                    varBinding(RETURN_VARIABLE_NAME, "T6", asList("T6","int"), asList("num"), true))),
//                            functionDto("foo()", 3, bindingDtos(
//                                    varBinding("foo()$x", "T2", null, asList("array"), true),
//                                    varBinding("foo()$y", "T3", null, asList("array"), true),
//                                    varBinding("foo()$z", "T5", null, asList("array"), true),
//                                    varBinding(RETURN_VARIABLE_NAME, "T6", asList("array"), null, true)))
//                    ), 1, 0, 2)
//            },
                {
                        "function foo($x, $y, $a, $b){return $a * ($x + $y) - $a * $b;}",
                        testStructs("foo()", "\\.\\.", functionDtos(
                                functionDto("foo()", 4, bindingDtos(
                                        varBinding("foo()$x", "T9", asList("@T9"), asList("num"), false),
                                        varBinding("foo()$y", "T9", asList("@T9"), asList("num"), false),
                                        varBinding("foo()$a", "T9", asList("@T9"), asList("num"), false),
                                        varBinding("foo()$b", "T9", asList("@T9"), asList("num"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T9", asList("@T9"), asList("num"), false))),
                                functionDto("foo()", 4, bindingDtos(
                                        varBinding("foo()$x", "T2", null, asList("bool"), true),
                                        varBinding("foo()$y", "T3", null, asList("bool"), true),
                                        varBinding("foo()$a", "T9", asList("@T9", "int"), asList("num"), false),
                                        varBinding("foo()$a", "T9", asList("@T9", "int"), asList("num"), false),
                                        varBinding(RETURN_VARIABLE_NAME,
                                                "T9", asList("@T9", "int"), asList("num"), false)))
                        ), 1, 0, 2)
                },
                {
                        "function foo($x, $y){return $x / $y;}",
                        testStructs("foo()", "\\.\\.", functionDtos(
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", null, asList("bool"), true),
                                        varBinding("foo()$y", "T3", null, asList("bool"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("(int | false)"), null, true))),
                                functionDto("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T2", asList("float"), asList("num"), false),
                                        varBinding("foo()$y", "T2", asList("float"), asList("num"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("@T4", "@T2", "false"), null,
                                                false)))
                        ), 1, 0, 2)
                },
        });
    }
}
