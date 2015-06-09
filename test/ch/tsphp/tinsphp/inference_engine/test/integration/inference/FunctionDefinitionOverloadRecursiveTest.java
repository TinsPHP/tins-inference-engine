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
public class FunctionDefinitionOverloadRecursiveTest extends AInferenceOverloadTest
{

    public FunctionDefinitionOverloadRecursiveTest(String testString, OverloadTestStruct[] theTestStructs) {
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
        List<String> asBool = asList("{as (falseType | trueType)}");
        List<String> numUpper = asList("(float | int)");
        List<String> asNum = asList("{as (float | int)}");
        return asList(new Object[][]{
                //direct recursive functions
                {
                        "function fib($n){ return $n > 1 ? fib($n - 1) + fib($n - 2) : 1;}",
                        testStructs("fib()", "\\.\\.", functionDtos("fib()", 1, bindingDtos(
                                varBinding("fib()$n", "T2", asList("int"), asList("int"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T7", asList("int"), asList("int"), true)
                        )), 1, 0, 2)
                },
                {
                        "function spaces($n){ if($n > 0){ return ' '.spaces($n-1);} return '';}",
                        testStructs("spaces()", "\\.\\.", functionDtos("spaces()", 1, bindingDtos(
                                varBinding("spaces()$n", "T2", asList("int"), asList("int"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T7", asList("string"), asList("string"), true)
                        )), 1, 0, 2)
                },
                {
                        "function fac($n){ return $n > 0 ? $n * fac($n-1) : $n;}",
                        testStructs("fac()", "\\.\\.", functionDtos("fac()", 1, bindingDtos(
                                varBinding("fac()$n", "T2", asList("int"), asList("int"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T7", asList("int"), asList("int"), true)
                        )), 1, 0, 2)
                },
                {
                        "function endless(){ return endless();}",
                        testStructs("endless()", "\\.\\.", functionDtos("endless()", 0, bindingDtos(
                                varBinding(RETURN_VARIABLE_NAME, "T2", asList("mixed"), asList("mixed"), true)
                        )), 1, 0, 2)
                },
                {
                        "function endless2(){ $a = endless2(); return $a;}",
                        testStructs("endless2()", "\\.\\.", functionDtos("endless2()", 0, bindingDtos(
                                varBinding(RETURN_VARIABLE_NAME, "T2", asList("mixed"), asList("mixed"), true)
                        )), 1, 0, 2)
                },
                {
                        "function endless3($x){ $x = endless3(); return $x;}",
                        testStructs("endless3()", "\\.\\.", functionDtos("endless3()", 1, bindingDtos(
                                varBinding("endless3()$x", "T3", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T3", null, null, false)
                        )), 1, 0, 2)
                },
                {
                        "function endless4($x, $y){ $x = endless4(); $y = $x; return $y;}",
                        testStructs("endless4()", "\\.\\.", functionDtos("endless4()", 2, bindingDtos(
                                varBinding("endless4()$x", "T3", null, null, false),
                                varBinding("endless4()$y", "T3", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T3", null, null, false)
                        )), 1, 0, 2)
                },
                {
                        "function fac2($x, $y){ $y = $x > 0 ? $x * fac2($x-1, $y) : $x; return $y;}",
                        testStructs("fac2()", "\\.\\.", functionDtos("fac2()", 2, bindingDtos(
                                varBinding("fac2()$x", "T2", asList("int"), asList("int"), true),
                                varBinding("fac2()$y", "T10", asList("int"), asList("int"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T7", asList("int"), asList("int"), true)
                        )), 1, 0, 2)
                },
                //indirect recursive functions
                {
                        "function foo($x){ if($x > 0){return bar($x-1);} return $x;}"
                                + "function bar($x){ if($x > 0){return foo($x-1);} return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                        varBinding("foo()$x", "T2", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T6", asList("int"), asList("int"), true)
                                )), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos("bar()", 1, bindingDtos(
                                        varBinding("bar()$x", "T2", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T6", asList("int"), asList("int"), true)
                                )), 1, 1, 2)
                        }
                },
                // indirect recursive function with parameter which has no constraint other than recursive function,
                // hence will not have a binding during the first iteration
                {
                        "function foo($x){ return bar($x); }"
                                + "function bar($x){ if($x > 0){return foo($x-1);} return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                        varBinding("foo()$x", "T4", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T1", asList("int"), asList("int"), true)
                                )), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos("bar()", 1, bindingDtos(
                                        varBinding("bar()$x", "T2", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T6", asList("int"), asList("int"), true)
                                )), 1, 1, 2)
                        }
                },
                // indirect recursive function which does not change during first iteration,
                // dependent function changes though
                {
                        "function foo($x){ return foo2($x); }"
                                + "function foo2($x){ return bar($x);}"
                                + "function bar($x){ if($x > 0){return foo($x-1);} return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                        varBinding("foo()$x", "T4", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T1", asList("int"), asList("int"), true)
                                )), 1, 0, 2),
                                testStruct("foo2()", "\\.\\.", functionDtos("foo2()", 1, bindingDtos(
                                        varBinding("foo2()$x", "T4", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T1", asList("int"), asList("int"), true)
                                )), 1, 1, 2),
                                testStruct("bar()", "\\.\\.", functionDtos("bar()", 1, bindingDtos(
                                        varBinding("bar()$x", "T2", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T6", asList("int"), asList("int"), true)
                                )), 1, 2, 2)
                        }
                },
                //indirect recursive function with erroneous overloads (bool x bool -> int) is no longer valid if $y
                // is restricted to Ty <: (int|float) - functions have only one overload in the end
                {
                        "function foo($x, $y){ return $x > 0 ?  bar($x - $y, $y) : $x; }"
                                + "function bar($x, $y){ return $x > 10 ? foo($x - $y, $y) : $y;}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos(
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T2", asList("int"), asList("int"), true),
                                                varBinding("foo()$y", "T5", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("int"), asList("int"), true)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T2", asList("float"), asList("float"), true),
                                                varBinding("foo()$y", "T5", asList("float"), asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("float"), asList("float"), true)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T2", null, asList("{as int}"), false),
                                                varBinding("foo()$y", "T5", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("int", "@T2"), null, false)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T2", null, asList("{as (float | int)}"), false),
                                                varBinding("foo()$y", "T5", asList("float"), asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("float", "@T2"), null, false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos(
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "T2", asList("int"), asList("int"), true),
                                                varBinding("bar()$y", "T5", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("int"), asList("int"), true)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "T2", asList("float"), asList("float"), true),
                                                varBinding("bar()$y", "T5", asList("float"), asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("float"), asList("float"), true)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "T2",
                                                        asList("{as int}"), asList("{as int}"), true),
                                                varBinding("bar()$y", "T5", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("int"), asList("int"), true)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "T2", asNum, asNum, true),
                                                varBinding("bar()$y", "T5", asList("float"), asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("float"), asList("float"), true)
                                        ))
                                ), 1, 1, 2)
                        }
                },
                //TODO TINS-494 ambiguous overloads calculated
                //indirect recursive function which produces more overloads once the dependent function is known but
                // erroneous ones (in the end the functions still have only one overload)
//                {
//                        "function foo($x, $y){ return bar($x, $y); }"
//                                + "function bar($x, $y){ return $x > 10 ? foo($x - $y, $y) : $y;}",
//                        new OverloadTestStruct[]{
//                                testStruct("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
//                                        varBinding("foo()$x", "T4", null, numUpper, false),
//                                        varBinding("foo()$y", "T4", null, numUpper, false),
//                                        varBinding(RETURN_VARIABLE_NAME, "T4", null, numUpper, false)
//                                )), 1, 0, 2),
//                                testStruct("bar()", "\\.\\.", functionDtos("bar()", 2, bindingDtos(
//                                        varBinding("bar()$x", "T4", null, numUpper, false),
//                                        varBinding("bar()$y", "T4", null, numUpper, false),
//                                        varBinding(RETURN_VARIABLE_NAME, "T4", null, numUpper, false)
//                                )), 1, 1, 2)
//                        }
//                },
                //indirect recursive function with erroneous overloads (bool x bool -> int) is no longer valid if $y
                // is restricted to Ty <: (int|float), Ty <: array respectively.
                {
                        "function foo($x, $y){ return $x > 0 ?  bar($x + $y, $y) : $x; }"
                                + "function bar($x, $y){ return $x > 10 ? foo($x + $y, $y) : $y;}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos(
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T2", asList("int"), asList("int"), true),
                                                varBinding("foo()$y", "T5", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("int"), asList("int"), true)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T2", asList("float"), asList("float"), true),
                                                varBinding("foo()$y", "T5", asList("float"), asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("float"), asList("float"), true)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T2", asList("array"), asList("array"), true),
                                                varBinding("foo()$y", "T5", asList("array"), asList("array"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("array"), asList("array"), true)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T2", null, asList("{as int}"), false),
                                                varBinding("foo()$y", "T5", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("int", "@T2"), null, false)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T2", null, asList("{as (float | int)}"), false),
                                                varBinding("foo()$y", "T5", asList("float"), asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("float", "@T2"), null, false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos(
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "T2", asList("int"), asList("int"), true),
                                                varBinding("bar()$y", "T5", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("int"), asList("int"), true)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "T2", asList("float"), asList("float"), true),
                                                varBinding("bar()$y", "T5", asList("float"), asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("float"), asList("float"), true)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "T2", asList("array"), asList("array"), true),
                                                varBinding("bar()$y", "T5", asList("array"), asList("array"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("array"), asList("array"), true)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "T2",
                                                        asList("{as int}"), asList("{as int}"), true),
                                                varBinding("bar()$y", "T5", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("int"), asList("int"), true)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "T2", asNum, asNum, true),
                                                varBinding("bar()$y", "T5", asList("float"), asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T8",
                                                        asList("float"), asList("float"), true)
                                        ))), 1, 1, 2)
                        }
                },
                //TODO TINS-494 ambiguous overloads calculated
//                //indirect recursive function which produces more overloads once the dependent function is known. An
//                // erroneous one (bool x bool -> int) and a valid one (array x array -> array)
//                {
//                        "function foo($x, $y){ return bar($x, $y); }"
//                                + "function bar($x, $y){ return $x > 10 ? foo($x + $y, $y) : $y;}",
//                        new OverloadTestStruct[]{
//                                testStruct("foo()", "\\.\\.", functionDtos(
//                                        functionDto("foo()", 2, bindingDtos(
//                                                varBinding("foo()$x", "T4", null, numUpper, false),
//                                                varBinding("foo()$y", "T4", null, numUpper, false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T4", null, numUpper, false)
//                                        )),
//                                        functionDto("foo()", 2, bindingDtos(
//                                                varBinding("foo()$x", "T4", asList("array"), asList("array"), true),
//                                                varBinding("foo()$y", "T5", null, asList("array"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T5",
//                                                        null, asList("array"), false)
//                                        ))), 1, 0, 2),
//                                testStruct("bar()", "\\.\\.", functionDtos(
//                                        functionDto("bar()", 2, bindingDtos(
//                                                varBinding("bar()$x", "T4", null, numUpper, false),
//                                                varBinding("bar()$y", "T4", null, numUpper, false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T4", null, numUpper, false)
//                                        )),
//                                        functionDto("bar()", 2, bindingDtos(
//                                                varBinding("bar()$x", "T2", asList("array"), asList("array"), true),
//                                                varBinding("bar()$y", "T5", null, asList("array"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T5",
//                                                        null, asList("array"), false)
//                                        ))), 1, 1, 2)
//                        }
//                },
                // call to an indirect recursive function
                {
                        "function foo($x, $y){ if($x){return $y;} return bar($y); }"
                                + "function bar($x){ if($x > 0){return foo(false, $x-1);} return $x;}"
                                + "function test(){return foo(true, 1);}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos(
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T5", boolLower, boolUpper, true),
                                                varBinding("foo()$y", "T2", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T1",
                                                        asList("int"), asList("int"), true)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T5", asBool, asBool, true),
                                                varBinding("foo()$y", "T2", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T1",
                                                        asList("int"), asList("int"), true)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos("bar()", 1, bindingDtos(
                                        varBinding("bar()$x", "T2", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T6", asList("int"), asList("int"), true)
                                )), 1, 1, 2),
                                testStruct("test()", "\\.\\.", functionDtos("test()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "T1", asList("int"), asList("int"), true)
                                )), 1, 2, 2)
                        }
                },
                //TODO TINS-465 - mixed or nothing as single type bound
                {
                        "function foo($x, $y){ if($x){return $y;} return bar($y); }"
                                + "function bar($x){ if($x > 0){return foo(false, $x);} return $x;}"
                                + "function test(){return foo(true, 'hello');}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos(
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T5", boolLower, boolUpper, true),
                                                varBinding("foo()$y", "T2", null, asList("mixed"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T2", null, asList("mixed"), false)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T5", asBool, asBool, true),
                                                varBinding("foo()$y", "T2", null, asList("mixed"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T2", null, asList("mixed"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos("bar()", 1, bindingDtos(
                                        varBinding("bar()$x", "T2", null, asList("mixed"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T2", null, asList("mixed"), false)
                                )), 1, 1, 2),
                                testStruct("test()", "\\.\\.", functionDtos("test()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "T1",
                                                asList("string"), asList("string"), true)
                                )), 1, 2, 2)
                        }
                },
                //TODO TINS-494 ambiguous overloads calculated
//                //call to an indirect recursive function which produces more overloads once the dependent function is
//                // known. An erroneous one (bool x bool -> int) and a valid one (array x array -> array)
//                {
//                        "function foo($x, $y){ return bar($x, $y); }"
//                                + "function bar($x, $y){ return $x > 10 ? foo($x + $y, $y) : $y;}"
//                                + "function test(){return foo(1, 2);}",
//                        new OverloadTestStruct[]{
//                                testStruct("foo()", "\\.\\.", functionDtos(
//                                        functionDto("foo()", 2, bindingDtos(
//                                                varBinding("foo()$x", "T4", null, numUpper, false),
//                                                varBinding("foo()$y", "T4", null, numUpper, false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T4", null, numUpper, false)
//                                        )),
//                                        functionDto("foo()", 2, bindingDtos(
//                                                varBinding("foo()$x", "T4", asList("array"), asList("array"), true),
//                                                varBinding("foo()$y", "T5", null, asList("array"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T5",
//                                                        null, asList("array"), false)
//                                        ))), 1, 0, 2),
//                                testStruct("bar()", "\\.\\.", functionDtos(
//                                        functionDto("bar()", 2, bindingDtos(
//                                                varBinding("bar()$x", "T4", null, numUpper, false),
//                                                varBinding("bar()$y", "T4", null, numUpper, false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T4", null, numUpper, false)
//                                        )),
//                                        functionDto("bar()", 2, bindingDtos(
//                                                varBinding("bar()$x", "T2", asList("array"), asList("array"), true),
//                                                varBinding("bar()$y", "T5", null, asList("array"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T5",
//                                                        null, asList("array"), false)
//                                        ))), 1, 1, 2),
//                                testStruct("test()", "\\.\\.", functionDtos("test()", 0, bindingDtos(
//                                        varBinding(RETURN_VARIABLE_NAME, "T1", asList("int"), asList("int"), true)
//                                )), 1, 2, 2)
//                        }
//                },
        });
    }
}
