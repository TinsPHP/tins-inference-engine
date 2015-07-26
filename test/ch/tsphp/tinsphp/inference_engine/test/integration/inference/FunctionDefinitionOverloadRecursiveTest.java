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
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingCollectionMatcher.varBinding;
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
        List<String> asNum = asList("{as (float | int)}");
        return asList(new Object[][]{
                //direct recursive functions
                {
                        "function fib($n){ return $n > 1 ? fib($n - 1) + fib($n - 2) : 1;}",
                        testStructs("fib()", "\\.\\.", functionDtos(
                                functionDto("fib()", 1, bindingDtos(
                                        varBinding("fib()$n", "V2", null, asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                                )),
                                functionDto("fib()", 1, bindingDtos(
                                        varBinding("fib()$n", "V2", null, asNum, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                                ))
                        ), 1, 0, 2)
                },
                {
                        "function spaces($n){ if($n > 0){ return ' '.spaces($n-1);} return '';}",
                        testStructs("spaces()", "\\.\\.", functionDtos(
                                functionDto("spaces()", 1, bindingDtos(
                                        varBinding("spaces()$n", "V2", null, asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("string"), null, true)
                                )),
                                functionDto("spaces()", 1, bindingDtos(
                                        varBinding("spaces()$n", "V2", null, asNum, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("string"), null, true)
                                ))
                        ), 1, 0, 2)
                },
                {
                        "function fac($n){ return $n > 0 ? $n * fac($n-1) : $n;}",
                        testStructs("fac()", "\\.\\.", functionDtos(
                                functionDto("fac()", 1, bindingDtos(
                                        varBinding("fac()$n", "V2", null, asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                                )),
                                functionDto("fac()", 1, bindingDtos(
                                        varBinding("fac()$n", "T1",
                                                null, asList("{as T2}", "@V6", "@V7", "@V9", "@V10"), false),
                                        varBinding("cScope*@1|42", "T2",
                                                null, asList("(float | int)", "@V6", "@V7", "@V9", "@V10"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "V7",
                                                asList("@T1", "@T2"), asList("{as T2}"), false)
                                ))
                        ), 1, 0, 2)
                },
                {
                        "function endless(){ return endless();}",
                        testStructs("endless()", "\\.\\.", functionDtos("endless()", 0, bindingDtos(
                                varBinding(RETURN_VARIABLE_NAME, "V2", asList("mixed"), null, true)
                        )), 1, 0, 2)
                },
                {
                        "function endless2(){ $a = endless2(); return $a;}",
                        testStructs("endless2()", "\\.\\.", functionDtos("endless2()", 0, bindingDtos(
                                varBinding(RETURN_VARIABLE_NAME, "V2", asList("mixed"), null, true)
                        )), 1, 0, 2)
                },
                {
                        "function endless3($x){ $x = endless3(); return $x;}",
                        testStructs("endless3()", "\\.\\.", functionDtos("endless3()", 1, bindingDtos(
                                varBinding("endless3()$x", "T", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T", null, null, false)
                        )), 1, 0, 2)
                },
                {
                        "function endless4($x, $y){ $x = endless4(); $y = $x; return $y;}",
                        testStructs("endless4()", "\\.\\.", functionDtos("endless4()", 2, bindingDtos(
                                varBinding("endless4()$x", "T", null, null, false),
                                varBinding("endless4()$y", "T", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T", null, null, false)
                        )), 1, 0, 2)
                },
                {
                        "function fac2($x, $y){ $y = $x > 0 ? $x \n * fac2($x-1, $y) : $x; return $y;}",
                        testStructs("fac2()", "\\.\\.", functionDtos(
                                functionDto("fac2()", 2, bindingDtos(
                                        varBinding("fac2()$x", "V2", null, asList("int"), true),
                                        varBinding("fac2()$y", "V10", null, asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                                )),
                                functionDto("fac2()", 2, bindingDtos(
                                        varBinding("fac2()$x", "T1",
                                                null, asList("{as T3}", "@V6", "@V9", "@T2"), false),
                                        varBinding("fac2()$y", "T2",
                                                asList("@T1", "@T3"), asList("{as T3}", "@V6"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T2",
                                                asList("@T1", "@T3"), asList("{as T3}", "@V6"), false),
                                        varBinding("cScope*@2|1", "T3",
                                                null, asList("(float | int)", "@V6", "@V9", "@T2"), false)
                                ))
                        ), 1, 0, 2)
                },
                //indirect recursive functions
                {
                        "function foo8($x){ if($x > 0){return bar8($x-1);} return $x;}"
                                + "function bar8($x){ if($x > 0){return foo8($x-1);} return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("foo8()", "\\.\\.", functionDtos(
                                        functionDto("foo8()", 1, bindingDtos(
                                                varBinding("foo8()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                        )),
                                        functionDto("foo8()", 1, bindingDtos(
                                                varBinding("foo8()$x", "T1", null, asList("{as T2}", "@V8"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V8",
                                                        asList("@T1", "@T2", "int"), null, false),
                                                varBinding("cScope-@1|49", "T2",
                                                        asList("int"), asList("@V8", "(float | int)"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar8()", "\\.\\.", functionDtos(
                                        functionDto("bar8()", 1, bindingDtos(
                                                varBinding("bar8()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                        )),
                                        functionDto("bar8()", 1, bindingDtos(
                                                varBinding("bar8()$x", "T1", null, asList("{as T2}", "@V8"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V8",
                                                        asList("@T1", "@T2", "int"), null, false),
                                                varBinding("cScope-@1|110", "T2",
                                                        asList("int"), asList("@V8", "(float | int)"), false)
                                        ))
                                ), 1, 1, 2),
                        }
                },
                // indirect recursive function with parameter which has no constraint other than recursive function,
                // hence will not have a binding during the first iteration
                {
                        "function foo9($x){ return bar9($x); }"
                                + "function bar9($x){ if($x > 0){return foo9($x-1);} return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("foo9()", "\\.\\.", functionDtos(
                                        functionDto("foo9()", 1, bindingDtos(
                                                varBinding("foo9()$x", "V4", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("int"), null, true)
                                        )),
                                        functionDto("foo9()", 1, bindingDtos(
                                                varBinding("foo9()$x", "T",
                                                        null, asList("{as (float | int)}", "@V1", "@V2", "@V3"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("int", "@T"), null, false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar9()", "\\.\\.", functionDtos(
                                        functionDto("bar9()", 1, bindingDtos(
                                                varBinding("bar9()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                        )),
                                        functionDto("bar9()", 1, bindingDtos(
                                                varBinding("bar9()$x", "T1", null, asList("@V8", "{as T2}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V8",
                                                        asList("@T1", "@T2", "int"), null, false),
                                                varBinding("cScope-@1|86", "T2",
                                                        asList("int"), asList("(float | int)", "@V8"), false)
                                        ))
                                ), 1, 1, 2)
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
                                        varBinding("foo()$x", "V4", null, asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("int"), null, true)
                                )), 1, 0, 2),
                                testStruct("foo2()", "\\.\\.", functionDtos(
                                        functionDto("foo2()", 1, bindingDtos(
                                                varBinding("foo2()$x", "V4", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("int"), null, true)
                                        )),
                                        functionDto("foo2()", 1, bindingDtos(
                                                varBinding("foo2()$x", "T",
                                                        null, asList("{as int}", "@V1", "@V2", "@V3"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("int", "@T"), null, false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("bar()", "\\.\\.", functionDtos(
                                        functionDto("bar()", 1, bindingDtos(
                                                varBinding("bar()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                        )),
                                        functionDto("bar()", 1, bindingDtos(
                                                varBinding("bar()$x", "T", null, asList("{as int}", "@V8"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("@T", "int"), null, false)
                                        ))
                                ), 1, 2, 2)
                        }
                },
                //indirect recursive function with erroneous overloads ((array | {as int}) x (array | {as int}) -> int)
                // is no longer valid if $y is restricted to Ty <: {as num}
                {
                        "function foo($x, $y){ return $x > 0 ?  bar($x & $y, $y) : $x; }"
                                + "function bar($x, $y){ return $x > 10 ? foo($x - $y, $y) : $y;}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos(
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V2", null, asList("int"), true),
                                                varBinding("foo()$y", "V5", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("int"), null, true)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T1",
                                                        null, asList("string", "@V6", "@V8", "@V9"), false),
                                                varBinding("foo()$y", "T2",
                                                        null, asList("string", "@V6", "@V8", "@V9"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("@T1", "@T2"), null, false)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "T1",
                                                        null, asList("(array | {as int})", "@V6", "@V8", "@V9"), false),
                                                varBinding("foo()$y", "T2",
                                                        null, asList("(array | {as int})", "{as (float | int)}",
                                                                "@V6", "@V8", "@V9"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("@T1", "@T2"), null, false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos(
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "V2", null, asList("int"), true),
                                                varBinding("bar()$y", "V5", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("int"), null, true)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "V2", null, asList("float"), true),
                                                varBinding("bar()$y", "V5", null, asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("float"), null, true)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "V2", null, asList("{as T2}"), true),
                                                varBinding("bar()$y", "T1",
                                                        null, asList("(array | {as int})", "{as (float | int)}",
                                                                "@V6", "@V7", "@V8", "@V9"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("@T1", "@T2"), null, false),
                                                varBinding("cScope-@1|114", "T2",
                                                        null,
                                                        asList("(float | int)", "@V6", "@V7", "@V8", "@V9"), false)
                                        ))
                                ), 1, 1, 2)
                        }
                },
                //indirect recursive function which produces more overloads once the dependent function is known
                {
                        "function foo($x, $y){ return bar($x, $y); }"
                                + "function bar($x, $y){ return $x > 10 ? foo($x - $y, $y) : $y;}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos(
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V4", null, asList("int"), true),
                                                varBinding("foo()$y", "T", null, asList("int"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V4", null, asList("float"), true),
                                                varBinding("foo()$y", "T", null, asList("float"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V4", null, asList("{as (float | int)}"), true),
                                                varBinding("foo()$y", "T", null, asList("{as (float | int)}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        null, asList("{as (float | int)}"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos(
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "V2", null, asList("int"), true),
                                                varBinding("bar()$y", "T", null, asList("int"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "V2", null, asList("float"), true),
                                                varBinding("bar()$y", "T", null, asList("float"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
                                        )),
                                        functionDto("bar()", 2, bindingDtos(
                                                varBinding("bar()$x", "V2", null, asList("{as (float | int)}"), true),
                                                varBinding("bar()$y", "T", null, asList("{as (float | int)}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        null, asList("{as (float | int)}"), false)
                                        ))
                                ), 1, 1, 2)
                        }
                },
                //TODO TINS-524 erroneous overload
                //indirect recursive function with erroneous overloads (bool x bool -> int) is no longer valid if $y
                // is restricted to Ty <: (int|float), Ty <: array respectively.
//                {
//                        "function foo14($x, $y){ return $x > 0 ?  bar14($x + $y, $y) : $x; }"
//                                + "function bar14($x, $y){ return $x > 10 ? foo14($x + $y, $y) : $y;}",
//                        new OverloadTestStruct[]{
//                                testStruct("foo14()", "\\.\\.", functionDtos(
//                                        functionDto("foo14()", 2, bindingDtos(
//                                                varBinding("foo14()$x", "V2", asList("int"), asList("int"), true),
//                                                varBinding("foo14()$y", "V5", asList("int"), asList("int"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V9",
//                                                        asList("int"), asList("int"), true)
//                                        )),
//                                        functionDto("foo14()", 2, bindingDtos(
//                                                varBinding("foo14()$x", "V2", asList("float"), asList("float"), true),
//                                                varBinding("foo14()$y", "V5", asList("float"), asList("float"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V9",
//                                                        asList("float"), asList("float"), true)
//                                        )),
//                                        functionDto("foo14()", 2, bindingDtos(
//                                                varBinding("foo14()$x", "V2", asList("array"), asList("array"), true),
//                                                varBinding("foo14()$y", "V5", asList("array"), asList("array"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V9",
//                                                        asList("array"), asList("array"), true)
//                                        )),
//                                        functionDto("foo14()", 2, bindingDtos(
//                                                varBinding("foo14()$x", "T",
//                                                        null, asList("{as int}", "@V8", "@V6", "@V9"), false),
//                                                varBinding("foo14()$y", "V5", asList("int"), asList("int"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V9",
//                                                        asList("int", "@T"), null, false)
//                                        )),
//                                        functionDto("foo14()", 2, bindingDtos(
//                                                varBinding("foo14()$x", "T",
//                                                        null, asList("{as (float | int)}", "@V8", "@V6", "@V9"),
// false),
//                                                varBinding("foo14()$y", "V5", asList("float"), asList("float"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V9",
//                                                        asList("float", "@T"), null, false)
//                                        ))
//                                ), 1, 0, 2),
//                                testStruct("bar14()", "\\.\\.", functionDtos(
//                                        functionDto("bar14()", 2, bindingDtos(
//                                                varBinding("bar14()$x", "V2", asList("int"), asList("int"), true),
//                                                varBinding("bar14()$y", "V5", asList("int"), asList("int"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V9",
//                                                        asList("int"), asList("int"), true)
//                                        )),
//                                        functionDto("bar14()", 2, bindingDtos(
//                                                varBinding("bar14()$x", "V2", asList("float"), asList("float"), true),
//                                                varBinding("bar14()$y", "V5", asList("float"), asList("float"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V9",
//                                                        asList("float"), asList("float"), true)
//                                        )),
//                                        functionDto("bar14()", 2, bindingDtos(
//                                                varBinding("bar14()$x", "V2", asList("array"), asList("array"), true),
//                                                varBinding("bar14()$y", "V5", asList("array"), asList("array"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V9",
//                                                        asList("array"), asList("array"), true)
//                                        )),
//                                        functionDto("bar14()", 2, bindingDtos(
//                                                varBinding("bar14()$x", "V2",
//                                                        asList("{as int}"), asList("{as int}"), true),
//                                                varBinding("bar14()$y", "V5", asList("int"), asList("int"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V9",
//                                                        asList("int"), asList("int"), true)
//                                        )),
//                                        functionDto("bar14()", 2, bindingDtos(
//                                                varBinding("bar14()$x", "V2", asNum, asNum, true),
//                                                varBinding("bar14()$y", "V5", asList("float"), asList("float"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V9",
//                                                        asList("float"), asList("float"), true)
//                                        ))), 1, 1, 2)
//                        }
//                },
//                //indirect recursive function which produces more overloads once the dependent function is known. An
//                // erroneous one (bool x bool -> int) and a valid one (array x array -> array)
//                {
//                        "function foo($x, $y){ return bar($x, $y); }"
//                                + "function bar($x, $y){ return $x > 10 ? foo($x + $y, $y) : $y;}",
//                        new OverloadTestStruct[]{
//                                testStruct("foo()", "\\.\\.", functionDtos(
//                                        functionDto("foo()", 2, bindingDtos(
//                                                varBinding("foo()$x", "V4", asList("int"), asList("int"), true),
//                                                varBinding("foo()$y", "T", null, asList("int"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
//                                        )),
//                                        functionDto("foo()", 2, bindingDtos(
//                                                varBinding("foo()$x", "V4", asList("float"), asList("float"), true),
//                                                varBinding("foo()$y", "T", null, asList("float"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
//                                        )),
//                                        functionDto("foo()", 2, bindingDtos(
//                                                varBinding("foo()$x", "V4", asNum, asNum, true),
//                                                varBinding("foo()$y", "T", null, asList("float"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
//                                        )),
//                                        functionDto("foo()", 2, bindingDtos(
//                                                varBinding("foo()$x", "V4", asList("array"), asList("array"), true),
//                                                varBinding("foo()$y", "T", null, asList("array"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("array"), false)
//                                        ))
//                                ), 1, 0, 2),
//                                testStruct("bar()", "\\.\\.", functionDtos(
//                                        functionDto("bar()", 2, bindingDtos(
//                                                varBinding("bar()$x", "V2", asList("int"), asList("int"), true),
//                                                varBinding("bar()$y", "T", null, asList("int"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
//                                        )),
//                                        functionDto("bar()", 2, bindingDtos(
//                                                varBinding("bar()$x", "V2", asList("float"), asList("float"), true),
//                                                varBinding("bar()$y", "T", null, asList("float"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
//                                        )),
//                                        functionDto("bar()", 2, bindingDtos(
//                                                varBinding("bar()$x", "V2",
//                                                        asList("{as int}"), asList("{as int}"), true),
//                                                varBinding("bar()$y", "T", null, asList("int"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
//                                        )),
//                                        functionDto("bar()", 2, bindingDtos(
//                                                varBinding("bar()$x", "V2", asNum, asNum, true),
//                                                varBinding("bar()$y", "T", null, asList("float"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
//                                        )),
//                                        functionDto("bar()", 2, bindingDtos(
//                                                varBinding("bar()$x", "V2", asList("array"), asList("array"), true),
//                                                varBinding("bar()$y", "T", null, asList("array"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("array"), false)
//                                        ))
//                                ), 1, 1, 2)
//                        }
//                },
                // call to an indirect recursive function
                {
                        "function foo13($x, $y){ if($x){return $y;} return bar13($y); }"
                                + "function bar13($x){ if($x > 0){return foo13(false, $x-1);} return $x;}"
                                + "function test13(){return foo13(true, 1);}",
                        new OverloadTestStruct[]{
                                testStruct("foo13()", "\\.\\.", functionDtos(
                                        functionDto("foo13()", 2, bindingDtos(
                                                varBinding("foo13()$x", "V5", null, boolUpper, true),
                                                varBinding("foo13()$y", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("int"), null, true)
                                        )),
                                        functionDto("foo13()", 2, bindingDtos(
                                                varBinding("foo13()$x", "V5", null, asBool, true),
                                                varBinding("foo13()$y", "T",
                                                        null, asList("{as (float | int)}", "@V3", "@V6", "@V7"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("@T", "int"), null, false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar13()", "\\.\\.", functionDtos(
                                        functionDto("bar13()", 1, bindingDtos(
                                                varBinding("bar13()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                        )),
                                        functionDto("bar13()", 1, bindingDtos(
                                                varBinding("bar13()$x", "T1", null, asList("{as T2}", "@V8"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V8",
                                                        asList("@T1", "@T2", "int"), null, false),
                                                varBinding("cScope-@1|120", "T2",
                                                        asList("int"), asList("(float | int)", "@V8"), false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("test13()", "\\.\\.", functionDtos("test13()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("int"), null, true)
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
                                                varBinding("foo()$x", "V5", null, boolUpper, true),
                                                varBinding("foo()$y", "T", null, asList("mixed"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("mixed"), false)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V5", null, asBool, true),
                                                varBinding("foo()$y", "T", null, asList("mixed"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("mixed"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos("bar()", 1, bindingDtos(
                                        varBinding("bar()$x", "T", null, asList("mixed"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("mixed"), false)
                                )), 1, 1, 2),
                                testStruct("test()", "\\.\\.", functionDtos("test()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("string"), null, true)
                                )), 1, 2, 2)
                        }
                },
                //TODO TINS-524 erroneous overload
//                //call to an indirect recursive function which produces more overloads once the dependent function is
//                // known. An erroneous one (bool x bool -> int) and a valid one (array x array -> array)
//                {
//                        "function foo17($x, $y){ return bar17($x, $y); }"
//                                + "function bar17($x, $y){ return $x > 10 ? foo17($x + $y, $y) : $y;}"
//                                + "function test(){return foo17(1, 2);}",
//                        new OverloadTestStruct[]{
//                                testStruct("foo17()", "\\.\\.", functionDtos(
//                                        functionDto("foo17()", 2, bindingDtos(
//                                                varBinding("foo17()$x", "V4", asList("int"), asList("int"), true),
//                                                varBinding("foo17()$y", "T", null, asList("int"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
//                                        )),
//                                        functionDto("foo17()", 2, bindingDtos(
//                                                varBinding("foo17()$x", "V4", asList("float"), asList("float"), true),
//                                                varBinding("foo17()$y", "T", null, asList("float"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
//                                        )),
//                                        functionDto("foo17()", 2, bindingDtos(
//                                                varBinding("foo17()$x", "V4", asNum, asNum, true),
//                                                varBinding("foo17()$y", "T", null, asList("float"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
//                                        )),
//                                        functionDto("foo17()", 2, bindingDtos(
//                                                varBinding("foo17()$x", "V4", asList("array"), asList("array"), true),
//                                                varBinding("foo17()$y", "T", null, asList("array"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("array"), false)
//                                        ))
//                                ), 1, 0, 2),
//                                testStruct("bar17()", "\\.\\.", functionDtos(
//                                        functionDto("bar17()", 2, bindingDtos(
//                                                varBinding("bar17()$x", "V2", asList("int"), asList("int"), true),
//                                                varBinding("bar17()$y", "T", null, asList("int"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
//                                        )),
//                                        functionDto("bar17()", 2, bindingDtos(
//                                                varBinding("bar17()$x", "V2", asList("float"), asList("float"), true),
//                                                varBinding("bar17()$y", "T", null, asList("float"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
//                                        )),
//                                        functionDto("bar17()", 2, bindingDtos(
//                                                varBinding("bar17()$x", "V2",
//                                                        asList("{as int}"),asList("{as int}"), true),
//                                                varBinding("bar17()$y", "T", null, asList("int"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
//                                        )),
//                                        functionDto("bar17()", 2, bindingDtos(
//                                                varBinding("bar17()$x", "V2", asNum, asNum, true),
//                                                varBinding("bar17()$y", "T", null, asList("float"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
//                                        )),
//                                        functionDto("bar17()", 2, bindingDtos(
//                                                varBinding("bar17()$x", "V2", asList("array"), asList("array"), true),
//                                                varBinding("bar17()$y", "T", null, asList("array"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("array"), false)
//                                        ))
//                                ), 1, 1, 2),
//                                testStruct("test()", "\\.\\.", functionDtos("test()", 0, bindingDtos(
//                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("int"), asList("int"), true)
//                                )), 1, 2, 2)
//                        }
//                },
                //direct recursive functions and soft typing
                {
                        "function fib2(array $a){ $a = $a > 1 ? fib2($a - 1) : 0; return $a;}",
                        testStructs("fib2()", "\\.\\.", functionDtos("fib2()", 1, bindingDtos(
                                varBinding("fib2()$a", "V10", null, asList("(array | int)"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", asList("array", "int"), null, true)
                        )), 1, 0, 2)
                },
        });
    }
}
