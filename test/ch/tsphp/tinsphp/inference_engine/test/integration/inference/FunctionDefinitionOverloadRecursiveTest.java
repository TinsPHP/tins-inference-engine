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
                                        varBinding(RETURN_VARIABLE_NAME, "V7",
                                                asList("int", "@T1", "@T2"), asList("{as T2}"), false),
                                        varBinding("cScope*@1|42", "T2",
                                                asList("int"),
                                                asList("(float | int)", "@V6", "@V7", "@V9", "@V10"),
                                                false)
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
                                                asList("int", "@T1", "@T3"), asList("{as T3}", "@V6"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T2",
                                                asList("int", "@T1", "@T3"), asList("{as T3}", "@V6"), false),
                                        varBinding("cScope*@2|1", "T3",
                                                asList("int"), asList("(float | int)", "@V6", "@V9", "@T2"), false)
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
                                                varBinding("foo8()$x", "T1", null, asList("{as T2}", "@V9"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("@T1", "@T2", "int"), null, false),
                                                varBinding("cScope-@1|49", "T2",
                                                        asList("int"), asList("@V9", "(float | int)"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar8()", "\\.\\.", functionDtos(
                                        functionDto("bar8()", 1, bindingDtos(
                                                varBinding("bar8()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                        )),
                                        functionDto("bar8()", 1, bindingDtos(
                                                varBinding("bar8()$x", "T1", null, asList("{as T2}", "@V9"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("@T1", "@T2", "int"), null, false),
                                                varBinding("cScope-@1|110", "T2",
                                                        asList("int"), asList("@V9", "(float | int)"), false)
                                        ))
                                ), 1, 1, 2),
                        }
                },
                // indirect recursive function with parameter which has no constraint other than recursive function,
                // hence will not have a binding during the first iteration
                {
                        "function foo9($x){ return bar9($x); }"
                                + "function bar9($x){ if($x > 0){return foo9($x-1);} return $x;}"
                                + "function test1(){return foo9(false);}"
                                + "function test2(){return foo9(1.2);}"
                                + "function test3(){return foo9('1');}",
                        new OverloadTestStruct[]{
                                testStruct("foo9()", "\\.\\.", functionDtos(
                                        functionDto("foo9()", 1, bindingDtos(
                                                varBinding("foo9()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V4", asList("int"), null, true)
                                        )),
                                        functionDto("foo9()", 1, bindingDtos(
                                                varBinding("foo9()$x", "T1",
                                                        null, asList("{as T2}", "@V1", "@V4", "@V5"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V5",
                                                        asList("int", "@T1", "@T2"), null, false),
                                                varBinding("!help0", "T2",
                                                        asList("int"), asList("(float | int)", "@V1", "@V4", "@V5"),
                                                        false)
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
                                ), 1, 1, 2),
                                testStruct("test1()", "\\.\\.", functionDtos("test1()", 0, bindingDtos(
                                                varBinding(RETURN_VARIABLE_NAME, "V5",
                                                        asList("falseType", "int"), null, true))
                                ), 1, 2, 2),
                                //TODO TINS-600 function instantiation with convertibles too general
                                //should only be float
                                testStruct("test2()", "\\.\\.", functionDtos("test2()", 0, bindingDtos(
                                                varBinding(RETURN_VARIABLE_NAME, "V5",
                                                        asList("float", "int"), null, true))
                                ), 1, 3, 2),
                                testStruct("test3()", "\\.\\.", functionDtos("test3()", 0, bindingDtos(
                                                varBinding(RETURN_VARIABLE_NAME, "V5",
                                                        asList("int", "float", "string"), null, true))
                                ), 1, 4, 2)
                        }
                },

                // indirect recursive function which does not change during first iteration,
                // dependent function changes though
                {
                        "function foo10($x){ return foo10B($x); }"
                                + "function foo10B($x){ return bar10($x);}"
                                + "function bar10($x){ if($x > 0){return foo10($x-1);} return $x;}"
                                + "function test1(){return foo10(1);}"
                                + "function test2(){return foo10(false);}"
                                + "function test3(){return foo10(1.2);}"
                                + "function test4(){return foo10('1');}",
                        new OverloadTestStruct[]{
                                testStruct("foo10()", "\\.\\.", functionDtos(
                                        functionDto("foo10()", 1, bindingDtos(
                                                varBinding("foo10()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V4", asList("int"), null, true)
                                        )),
                                        functionDto("foo10()", 1, bindingDtos(
                                                varBinding("foo10()$x", "T1",
                                                        null, asList("{as T2}", "@V1", "@V3", "@V4"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V4",
                                                        asList("int", "@T1", "@T2"), null, false),
                                                varBinding("!help0", "T2",
                                                        asList("int"), asList("(float | int)", "@V1", "@V3", "@V4"),
                                                        false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("foo10B()", "\\.\\.", functionDtos(
                                        functionDto("foo10B()", 1, bindingDtos(
                                                varBinding("foo10B()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V4", asList("int"), null, true)
                                        )),
                                        functionDto("foo10B()", 1, bindingDtos(
                                                varBinding("foo10B()$x", "T1",
                                                        null, asList("{as T2}", "@V1", "@V4", "@V5"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V5",
                                                        asList("int", "@T1", "@T2"), null, false),
                                                varBinding("!help0", "T2",
                                                        asList("int"), asList("(float | int)", "@V1", "@V4", "@V5"),
                                                        false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("bar10()", "\\.\\.", functionDtos(
                                        functionDto("bar10()", 1, bindingDtos(
                                                varBinding("bar10()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                        )),
                                        functionDto("bar10()", 1, bindingDtos(
                                                varBinding("bar10()$x", "T1", null, asList("{as T2}", "@V8"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V8",
                                                        asList("int", "@T1", "@T2"), null, false),
                                                varBinding("cScope-@1|130", "T2",
                                                        asList("int"), asList("(float | int)", "@V8"), false)
                                        ))
                                ), 1, 2, 2),
                                testStruct("test1()", "\\.\\.", functionDtos("test1()", 0, bindingDtos(
                                                varBinding(RETURN_VARIABLE_NAME, "V4",
                                                        asList("int"), null, true))
                                ), 1, 3, 2),
                                testStruct("test2()", "\\.\\.", functionDtos("test2()", 0, bindingDtos(
                                                varBinding(RETURN_VARIABLE_NAME, "V5",
                                                        asList("falseType", "int"), null, true))
                                ), 1, 4, 2),
                                //TODO TINS-600 function instantiation with convertibles too general
                                //should only be float
                                testStruct("test3()", "\\.\\.", functionDtos("test3()", 0, bindingDtos(
                                                varBinding(RETURN_VARIABLE_NAME, "V5",
                                                        asList("float", "int"), null, true))
                                ), 1, 5, 2),
                                testStruct("test4()", "\\.\\.", functionDtos("test4()", 0, bindingDtos(
                                                varBinding(RETURN_VARIABLE_NAME, "V5",
                                                        asList("int", "float", "string"), null, true))
                                ), 1, 6, 2)
                        }
                },
                //indirect recursive function with erroneous overloads ((array | {as int}) x (array | {as int}) -> int)
                // is no longer valid if $y is restricted to Ty <: {as num}
                {
                        "function foo11($x, $y){ return $x > 0 ?  bar11($x & $y, $y) : $x; }"
                                + "function bar11($x, $y){ return $x > 10 ? foo11($x - $y, $y) : $y;}"
                                + "function test1(){return foo11(2.2, 1);}"
                                + "function test2(){return foo11(2.2, '1');}"
                                + "function test3(){return foo11(1, false);}"
                                + "function test4(){return foo11(1, '1');}"
                                + "function test5(){return bar11(1, '1');}"
                                + "function test6(){return bar11('a', '1');}"
                                + "function test7(){return bar11(1.2, 3.4);}",
                        new OverloadTestStruct[]{
                                testStruct("foo11()", "\\.\\.", functionDtos(
                                        functionDto("foo11()", 2, bindingDtos(
                                                varBinding("foo11()$x", "V2", null, asList("int"), true),
                                                varBinding("foo11()$y", "V5", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("int"), null, true)
                                        )),
                                        functionDto("foo11()", 2, bindingDtos(
                                                varBinding("foo11()$x", "T1",
                                                        null, asList("string", "@V8", "@V9", "@V10"), false),
                                                varBinding("foo11()$y", "T2",
                                                        null, asList("string", "@V6", "@V8", "@V9", "@V10"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V10",
                                                        asList("int", "float", "@T1", "@T2"), null, false)
                                        )),
                                        functionDto("foo11()", 2, bindingDtos(
                                                varBinding("foo11()$x", "T1",
                                                        null, asList("(array | {as int})", "@V8", "@V9", "@V10"),
                                                        false),
                                                varBinding("foo11()$y", "T2",
                                                        null, asList("(array | {as int})", "{as T3}",
                                                                "@V6", "@V8", "@V9", "@V10"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V10",
                                                        asList("int", "@T1", "@T2", "@T3"), null, false),
                                                varBinding("!help1", "T3",
                                                        asList("int"),
                                                        asList("(float | int)", "@V6", "@V8", "@V9", "@V10"),
                                                        false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar11()", "\\.\\.", functionDtos(
                                        functionDto("bar11()", 2, bindingDtos(
                                                varBinding("bar11()$x", "V2", null, asList("int"), true),
                                                varBinding("bar11()$y", "V5", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("int"), null, true)
                                        )),
                                        functionDto("bar11()", 2, bindingDtos(
                                                varBinding("bar11()$x", "V2", null, asList("float"), true),
                                                varBinding("bar11()$y", "V5", null, asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("int", "float"), null, true)
                                        )),
                                        functionDto("bar11()", 2, bindingDtos(
                                                varBinding("bar11()$x", "V2", null, asList("{as T2}"), true),
                                                varBinding("bar11()$y", "T1",
                                                        null, asList("(array | {as int})", "{as T3}",
                                                                "@V6", "@V7", "@V8", "@V9"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("int", "@T1", "@T2", "@T3"), null, false),
                                                varBinding("cScope-@1|122", "T2",
                                                        null,
                                                        asList("(float | int)", "@V6", "@V7", "@V8", "@V9"), false),
                                                varBinding("!help0", "T3",
                                                        asList("int"),
                                                        asList("(float | int)", "@V6", "@V7", "@V8", "@V9"), false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("test1()", "\\.\\.", functionDtos("test1()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int", "float"), null, true)
                                )), 1, 2, 2),
                                testStruct("test2()", "\\.\\.", functionDtos("test2()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6",
                                                asList("string", "int", "float"), null, true)
                                )), 1, 3, 2),
                                testStruct("test3()", "\\.\\.", functionDtos("test3()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("falseType", "int"), null, true)
                                )), 1, 4, 2),
                                testStruct("test4()", "\\.\\.", functionDtos("test4()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6",
                                                asList("float", "int", "string"), null, true)
                                )), 1, 5, 2),
                                testStruct("test5()", "\\.\\.", functionDtos("test5()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V7",
                                                asList("float", "int", "string"), null, true)
                                )), 1, 6, 2),
                                testStruct("test6()", "\\.\\.", functionDtos("test6()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V7",
                                                asList("float", "int", "string"), null, true)
                                )), 1, 7, 2),
                                //TODO TINS-600 function instantiation with convertibles too general
                                //should only be float
                                testStruct("test7()", "\\.\\.", functionDtos("test7()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V5",
                                                asList("int", "float"), null, true)
                                )), 1, 8, 2),
                        }
                },
                //indirect recursive function which produces more overloads once the dependent function is known
                {
                        "function foo1($x, $y){ return bar1($x, $y); }"
                                + "function bar1($x, $y){ return $x > 10 ? foo1($x - $y, $y) : $y;}",
                        new OverloadTestStruct[]{
                                testStruct("foo1()", "\\.\\.", functionDtos(
                                        functionDto("foo1()", 2, bindingDtos(
                                                varBinding("foo1()$x", "V2", null, asList("int"), true),
                                                varBinding("foo1()$y", "T", null, asList("int"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
                                        )),
                                        functionDto("foo1()", 2, bindingDtos(
                                                varBinding("foo1()$x", "V2", null, asList("float"), true),
                                                varBinding("foo1()$y", "T", null, asList("float"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
                                        )),
                                        functionDto("foo1()", 2, bindingDtos(
                                                varBinding("foo1()$x", "V2", null, asList("{as (float | int)}"), true),
                                                varBinding("foo1()$y", "T", null, asList("{as (float | int)}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        null, asList("{as (float | int)}"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar1()", "\\.\\.", functionDtos(
                                        functionDto("bar1()", 2, bindingDtos(
                                                varBinding("bar1()$x", "V2", null, asList("int"), true),
                                                varBinding("bar1()$y", "T", null, asList("int"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
                                        )),
                                        functionDto("bar1()", 2, bindingDtos(
                                                varBinding("bar1()$x", "V2", null, asList("float"), true),
                                                varBinding("bar1()$y", "T", null, asList("float"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
                                        )),
                                        functionDto("bar1()", 2, bindingDtos(
                                                varBinding("bar1()$x", "V2", null, asList("{as (float | int)}"), true),
                                                varBinding("bar1()$y", "T", null, asList("{as (float | int)}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        null, asList("{as (float | int)}"), false)
                                        ))
                                ), 1, 1, 2)
                        }
                },
                //indirect recursive function with helper variables
                {
                        "function foo14($x, $y){ return $x > 0 ?  bar14($x + $y, $y) : $x; }"
                                + "function bar14($x, $y){ return $x > 10 ? foo14($x + $y, $y) : $y;}"
                                + "function test1(){return foo14(2.2, 1);}"
                                + "function test2(){return foo14(2.2, '1');}"
                                + "function test3(){return foo14(1, false);}"
                                + "function test4(){return foo14(1, '1');}",
                        new OverloadTestStruct[]{
                                testStruct("foo14()", "\\.\\.", functionDtos(
                                        functionDto("foo14()", 2, bindingDtos(
                                                varBinding("foo14()$x", "V2", null, asList("int"), true),
                                                varBinding("foo14()$y", "V5", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("int"), null, true)
                                        )),
                                        functionDto("foo14()", 2, bindingDtos(
                                                varBinding("foo14()$x", "V2", null, asList("float"), true),
                                                varBinding("foo14()$y", "V5", null, asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("float"), null, true)
                                        )),
                                        functionDto("foo14()", 2, bindingDtos(
                                                varBinding("foo14()$x", "V2", null, asList("array"), true),
                                                varBinding("foo14()$y", "V5", null, asList("array"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("array"), null, true)
                                        )),
                                        functionDto("foo14()", 2, bindingDtos(
                                                varBinding("foo14()$x", "T1",
                                                        null, asList("{as T3}", "@V8", "@V9", "@V10"), false),
                                                varBinding("foo14()$y", "T2",
                                                        null, asList("{as T4}", "@V6", "@V8", "@V9", "@V10"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V10",
                                                        asList("@T1", "@T2", "@T4"), null, false),
                                                varBinding("cScope+@1|55", "T3",
                                                        null, asList("(float | int)", "@T4"), false),
                                                varBinding("!help1", "T4",
                                                        asList("@T3"),
                                                        asList("(float | int)", "@V6", "@V8", "@V9", "@V10"),
                                                        false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar14()", "\\.\\.", functionDtos(
                                        functionDto("bar14()", 2, bindingDtos(
                                                varBinding("bar14()$x", "V2", null, asList("int"), true),
                                                varBinding("bar14()$y", "V5", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("int"), null, true)
                                        )),
                                        functionDto("bar14()", 2, bindingDtos(
                                                varBinding("bar14()$x", "V2", null, asList("float"), true),
                                                varBinding("bar14()$y", "V5", null, asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("float"), null, true)
                                        )),
                                        functionDto("bar14()", 2, bindingDtos(
                                                varBinding("bar14()$x", "V2", null, asList("array"), true),
                                                varBinding("bar14()$y", "V5", null, asList("array"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("array"), null, true)
                                        )),
                                        functionDto("bar14()", 2, bindingDtos(
                                                varBinding("bar14()$x", "V2", null, asList("{as T2}"), true),
                                                varBinding("bar14()$y", "T1",
                                                        null, asList("{as T3}", "@V6", "@V8", "@V9", "@V10"),
                                                        false),
                                                varBinding(RETURN_VARIABLE_NAME, "V10",
                                                        asList("@T1", "@T2", "@T3"), null, false),
                                                varBinding("cScope+@1|122", "T2",
                                                        null,
                                                        asList("(float | int)", "@T3", "@V6", "@V8", "@V9", "@V10"),
                                                        false),
                                                varBinding("!help1", "T3",
                                                        asList("@T2"),
                                                        asList("(float | int)", "@V6", "@V8", "@V9", "@V10"),
                                                        false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("test1()", "\\.\\.", functionDtos("test1()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int", "float"), null, true)
                                )), 1, 2, 2),
                                //TODO TINS-600 function instantiation with convertibles too general
                                //should only be string, float
                                testStruct("test2()", "\\.\\.", functionDtos("test2()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int", "string", "float"),
                                                null, true)
                                )), 1, 3, 2),
                                testStruct("test3()", "\\.\\.", functionDtos("test3()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("falseType", "int"), null, true)
                                )), 1, 4, 2),
                                testStruct("test4()", "\\.\\.", functionDtos("test4()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V7",
                                                asList("float", "int", "string"), null, true)
                                )), 1, 5, 2),
                        }
                },
                //indirect recursive function which produces more overloads once the dependent function is known.
                {
                        "function foo3($x, $y){ return bar3($x, $y); }"
                                + "function bar3($x, $y){ return $x > 10 ? foo3($x + $y, $y) : $y;}",
                        new OverloadTestStruct[]{
                                testStruct("foo3()", "\\.\\.", functionDtos(
                                        functionDto("foo3()", 2, bindingDtos(
                                                varBinding("foo3()$x", "V2", null, asList("int"), true),
                                                varBinding("foo3()$y", "T", null, asList("int"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
                                        )),
                                        functionDto("foo3()", 2, bindingDtos(
                                                varBinding("foo3()$x", "V2", null, asList("float"), true),
                                                varBinding("foo3()$y", "T", null, asList("float"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
                                        )),
                                        functionDto("foo3()", 2, bindingDtos(
                                                varBinding("foo3()$x", "V2", null, asNum, true),
                                                varBinding("foo3()$y", "T", null, asNum, false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asNum, false)
                                        )),
                                        functionDto("foo3()", 2, bindingDtos(
                                                varBinding("foo3()$x", "V2", null, asList("array"), true),
                                                varBinding("foo3()$y", "T", null, asList("array"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("array"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar3()", "\\.\\.", functionDtos(
                                        functionDto("bar3()", 2, bindingDtos(
                                                varBinding("bar3()$x", "V2", null, asList("int"), true),
                                                varBinding("bar3()$y", "T", null, asList("int"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
                                        )),
                                        functionDto("bar3()", 2, bindingDtos(
                                                varBinding("bar3()$x", "V2", null, asList("float"), true),
                                                varBinding("bar3()$y", "T", null, asList("float"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
                                        )),
                                        functionDto("bar3()", 2, bindingDtos(
                                                varBinding("bar3()$x", "V2", null, asNum, true),
                                                varBinding("bar3()$y", "T", null, asNum, false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asNum, false)
                                        )),
                                        functionDto("bar3()", 2, bindingDtos(
                                                varBinding("bar3()$x", "V2", null, asList("array"), true),
                                                varBinding("bar3()$y", "T", null, asList("array"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("array"), false)
                                        ))
                                ), 1, 1, 2)
                        }
                },
                //TODO TINS-629 indirect recursive functions and application filtering
                // call to an indirect recursive function
//                {
//                        "function foo13($x, $y){ if($x){return $y;} return bar13($y); }"
//                                + "function bar13($x){ if($x > 0){return foo13(false, $x-1);} return $x;}"
//                                + "function test13(){return foo13(true, 1);}",
//                        new OverloadTestStruct[]{
//                                testStruct("foo13()", "\\.\\.", functionDtos(
//                                        functionDto("foo13()", 2, bindingDtos(
//                                                varBinding("foo13()$x", "V5", null, boolUpper, true),
//                                                varBinding("foo13()$y", "V2", null, asList("int"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("int"), null, true)
//                                        )),
//                                        functionDto("foo13()", 2, bindingDtos(
//                                                varBinding("foo13()$x", "V5", null, boolUpper, true),
//                                                varBinding("foo13()$y", "T1",
//                                                        null, asList("{as T2}", "@V3", "@V6", "@V7"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "V3",
//                                                        asList("int", "@T1", "@T2"), null, false),
//                                                varBinding("!help0", "T2",
//                                                        asList("int"),
//                                                        asList("(float | int)", "@V3", "@V6", "@V7"),
//                                                        false)
//                                        )),
//                                        functionDto("foo13()", 2, bindingDtos(
//                                                varBinding("foo13()$x", "V5", null, asBool, true),
//                                                varBinding("foo13()$y", "V2", null, asList("int"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("int"), null, true)
//                                        )),
//                                        functionDto("foo13()", 2, bindingDtos(
//                                                varBinding("foo13()$x", "V5", null, asBool, true),
//                                                varBinding("foo13()$y", "T1",
//                                                        null, asList("{as T2}", "@V3", "@V6", "@V7"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "V3",
//                                                        asList("int", "@T1", "@T2"), null, false),
//                                                varBinding("!help0", "T2",
//                                                        asList("int"),
//                                                        asList("(float | int)", "@V3", "@V6", "@V7"),
//                                                        false)
//                                        ))
//                                ), 1, 0, 2),
//                                testStruct("bar13()", "\\.\\.", functionDtos(
//                                        functionDto("bar13()", 1, bindingDtos(
//                                                varBinding("bar13()$x", "V2", null, asList("int"), true),
//                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
//                                        )),
//                                        functionDto("bar13()", 1, bindingDtos(
//                                                varBinding("bar13()$x", "T1", null, asList("{as T2}", "@V8"), false),
//                                                varBinding(RETURN_VARIABLE_NAME, "V8",
//                                                        asList("@T1", "@T2", "int"), null, false),
//                                                varBinding("cScope-@1|120", "T2",
//                                                        asList("int"), asList("(float | int)", "@V8"), false)
//                                        ))
//                                ), 1, 1, 2),
//                                testStruct("test13()", "\\.\\.", functionDtos("test13()", 0, bindingDtos(
//                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("int"), null, true)
//                                )), 1, 2, 2)
//                        }
//                },
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
                                        varBinding(RETURN_VARIABLE_NAME, "V5", asList("string"), null, true)
                                )), 1, 2, 2)
                        }
                },
                //call to an indirect recursive function which produces more overloads once the dependent function is
                // known.
                {
                        "function foo17($x, $y){ return bar17($x, $y); }"
                                + "function bar17($x, $y){ return $x > 10 ? foo17($x + $y, $y) : $y;}"
                                + "function test1(){return foo17(1, 2.2);}"
                                + "function test2(){return foo17(1.2, '1');}",
                        new OverloadTestStruct[]{
                                testStruct("foo17()", "\\.\\.", functionDtos(
                                        functionDto("foo17()", 2, bindingDtos(
                                                varBinding("foo17()$x", "V2", null, asList("int"), true),
                                                varBinding("foo17()$y", "T", null, asList("int"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
                                        )),
                                        functionDto("foo17()", 2, bindingDtos(
                                                varBinding("foo17()$x", "V2", null, asList("float"), true),
                                                varBinding("foo17()$y", "T", null, asList("float"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
                                        )),
                                        functionDto("foo17()", 2, bindingDtos(
                                                varBinding("foo17()$x", "V2", null, asNum, true),
                                                varBinding("foo17()$y", "T", null, asNum, false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asNum, false)
                                        )),
                                        functionDto("foo17()", 2, bindingDtos(
                                                varBinding("foo17()$x", "V2", null, asList("array"), true),
                                                varBinding("foo17()$y", "T", null, asList("array"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("array"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar17()", "\\.\\.", functionDtos(
                                        functionDto("bar17()", 2, bindingDtos(
                                                varBinding("bar17()$x", "V2", null, asList("int"), true),
                                                varBinding("bar17()$y", "T", null, asList("int"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("int"), false)
                                        )),
                                        functionDto("bar17()", 2, bindingDtos(
                                                varBinding("bar17()$x", "V2", null, asList("float"), true),
                                                varBinding("bar17()$y", "T", null, asList("float"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("float"), false)
                                        )),
                                        functionDto("bar17()", 2, bindingDtos(
                                                varBinding("bar17()$x", "V2", null, asNum, true),
                                                varBinding("bar17()$y", "T", null, asNum, false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asNum, false)
                                        )),
                                        functionDto("bar17()", 2, bindingDtos(
                                                varBinding("bar17()$x", "V2", null, asList("array"), true),
                                                varBinding("bar17()$y", "T", null, asList("array"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, asList("array"), false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("test1()", "\\.\\.", functionDtos("test1()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V5", asList("float"), null, true)
                                )), 1, 2, 2),
                                testStruct("test2()", "\\.\\.", functionDtos("test2()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V5", asList("string"), null, true)
                                )), 1, 3, 2)
                        }
                },
                {
                        "function foo17B($x, $y){ return bar17B($x, $y); }"
                                + "function bar17B($x, $y){ return $x > 10 ? foo17B($x, $x + $y) : $y;}"
                                + "function test1(){return foo17B(2.2, 1);}"
                                + "function test2(){return foo17B(2.2, '1');}"
                                + "function test3(){return foo17B(1, false);}"
                                + "function test4(){return foo17B(1, '1');}"
                                + "function test5(){return foo17B(1, 2.2);}"
                                + "function test6(){return foo17B('1', 2.2);}"
                                + "function test7(){return foo17B(false, 1);}"
                                + "function test8(){return foo17B('1', 1);}",
                        new OverloadTestStruct[]{
                                testStruct("foo17B()", "\\.\\.", functionDtos(
                                        functionDto("foo17B()", 2, bindingDtos(
                                                varBinding("foo17B()$x", "V2", null, asList("int"), true),
                                                varBinding("foo17B()$y", "V3", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("foo17B()", 2, bindingDtos(
                                                varBinding("foo17B()$x", "V2", null, asList("float"), true),
                                                varBinding("foo17B()$y", "V3", null, asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("float"), null, true)
                                        )),
                                        functionDto("foo17B()", 2, bindingDtos(
                                                varBinding("foo17B()$x", "V2", null, asList("{as T2}"), true),
                                                varBinding("foo17B()$y", "T1",
                                                        null, asList("{as T2}", "@V1", "@V5", "@V6"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V6",
                                                        asList("@T1", "@T2"), null, false),
                                                varBinding("!help0", "T2",
                                                        null, asList("(float | int)", "@V1", "@V5", "@V6"), false)
                                        )),
                                        functionDto("foo17B()", 2, bindingDtos(
                                                varBinding("foo17B()$x", "V2", null, asList("array"), true),
                                                varBinding("foo17B()$y", "V3", null, asList("array"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("array"), null, true)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar17B()", "\\.\\.", functionDtos(
                                        functionDto("bar17B()", 2, bindingDtos(
                                                varBinding("bar17B()$x", "V2", null, asList("int"), true),
                                                varBinding("bar17B()$y", "V5", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("int"), null, true)
                                        )),
                                        functionDto("bar17B()", 2, bindingDtos(
                                                varBinding("bar17B()$x", "V2", null, asList("float"), true),
                                                varBinding("bar17B()$y", "V5", null, asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("float"), null, true)
                                        )),
                                        functionDto("bar17B()", 2, bindingDtos(
                                                varBinding("bar17B()$x", "V2", null, asList("{as T3}"), true),
                                                varBinding("bar17B()$y", "T1",
                                                        null, asList("{as T2}", "@V7", "@V8", "@V9"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("@T1", "@T2", "@T3"), null, false),
                                                varBinding("cScope+@1|110", "T2",
                                                        null,
                                                        asList("(float | int)", "@T3", "@V6", "@V7", "@V8", "@V9"),
                                                        false),
                                                varBinding("!help0", "T3",
                                                        asList("@T2"),
                                                        asList("(float | int)", "@V6", "@V7", "@V8", "@V9"),
                                                        false)
                                        )),
                                        functionDto("bar17B()", 2, bindingDtos(
                                                varBinding("bar17B()$x", "V2", null, asList("array"), true),
                                                varBinding("bar17B()$y", "V5", null, asList("array"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("array"), null, true)
                                        ))
                                ), 1, 1, 2),
                                testStruct("test1()", "\\.\\.", functionDtos("test1()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int", "float"), null, true)
                                )), 1, 2, 2),
                                testStruct("test2()", "\\.\\.", functionDtos("test2()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("string", "float"), null, true)
                                )), 1, 3, 2),
                                testStruct("test3()", "\\.\\.", functionDtos("test3()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("falseType", "int"), null, true)
                                )), 1, 4, 2),
                                testStruct("test4()", "\\.\\.", functionDtos("test4()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6",
                                                asList("float", "int", "string"), null, true)
                                )), 1, 5, 2),
                                //TODO TINS-600 function instantiation with convertibles too general
                                //should only be float
                                testStruct("test5()", "\\.\\.", functionDtos("test5()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int", "float"), null, true)
                                )), 1, 6, 2),
                                testStruct("test6()", "\\.\\.", functionDtos("test6()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("float"), null, true)
                                )), 1, 7, 2),
                                testStruct("test7()", "\\.\\.", functionDtos("test7()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int"), null, true)
                                )), 1, 8, 2),
                                testStruct("test8()", "\\.\\.", functionDtos("test8()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("float", "int"), null, true)
                                )), 1, 9, 2),
                        }
                },
                //direct recursive functions and soft typing
                {
                        "function test(array $a){ $a = $a > 1 ? test($a - 1) : 0; return $a;}",
                        testStructs("test()", "\\.\\.", functionDtos("test()", 1, bindingDtos(
                                varBinding("test()$a", "V10", null, asList("(array | int)"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", asList("array", "int"), null, true)
                        )), 1, 0, 2)
                },
                //parametric polymorphic direct recursive function
                {
                        "function fibGen($n){return $n > 0 ? fibGen($n-1) + fibGen($n-2) : $n;}",
                        testStructs("fibGen()", "\\.\\.", functionDtos(
                                functionDto("fibGen()", 1, bindingDtos(
                                        varBinding("fibGen()$n", "V2", null, asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                                )),
                                functionDto("fibGen()", 1, bindingDtos(
                                        varBinding("fibGen()$n", "T1",
                                                null, asList("{as T2}", "@V6", "@V7", "@V10", "@V12", "@V13"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "V7",
                                                asList("int", "@T1", "@T2"), asList("{as T2}"), false),
                                        varBinding("cScope+@1|54", "T2",
                                                asList("int"),
                                                asList("(float | int)", "@V6", "@V7", "@V10", "@V12", "@V13"), false)
                                ))
                        ), 1, 0, 2)
                },
                //parametric polymorphic indirect recursive function
                {
                        "function fibGenA($n){return $n > 0 ? fibGenB($n-1) + fibGenB($n-2) : $n;}\n"
                                + "function fibGenB($n){return $n > 0 ? fibGenA($n-1) + fibGenA($n-2) : $n;}",
                        new OverloadTestStruct[]{
                                testStruct("fibGenA()", "\\.\\.", functionDtos(
                                        functionDto("fibGenA()", 1, bindingDtos(
                                                varBinding("fibGenA()$n", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V13", asList("int"), null, true)
                                        )),
                                        functionDto("fibGenA()", 1, bindingDtos(
                                                varBinding("fibGenA()$n", "T1",
                                                        null, asList("{as T2}", "@V13", "@V14", "@V15"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V15",
                                                        asList("int", "@T1", "@T2"), null, false),
                                                varBinding("cScope+@1|56", "T2",
                                                        asList("int"),
                                                        asList("(float | int)", "@V13", "@V14", "@V15"),
                                                        false)

                                        ))
                                ), 1, 0, 2),
                                testStruct("fibGenB()", "\\.\\.", functionDtos(
                                        functionDto("fibGenB()", 1, bindingDtos(
                                                varBinding("fibGenB()$n", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V13", asList("int"), null, true)
                                        )),
                                        functionDto("fibGenB()", 1, bindingDtos(
                                                varBinding("fibGenB()$n", "T1",
                                                        null, asList("{as T2}", "@V13", "@V14", "@V15"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V15",
                                                        asList("int", "@T1", "@T2"), null, false),
                                                varBinding("cScope+@2|51", "T2",
                                                        asList("int"),
                                                        asList("(float | int)", "@V13", "@V14", "@V15"),
                                                        false)

                                        ))
                                ), 1, 1, 2)
                        }
                },
                //see TINS-559 NoSuchElement for indirect recursive function
                {
                        "function foo15($x){$x . 1; return bar15($x);}\n"
                                + "function bar15($x){asString($x); return $x > 10 ? foo15($x) : $x;}\n"
                                + "function asString($x){$x . 1; return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("foo15()", "\\.\\.", functionDtos(
                                        functionDto("foo15()", 1, bindingDtos(
                                                varBinding("foo15()$x", "T", null, asList("{as string}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        null, asList("{as string}"), false)

                                        ))
                                ), 1, 0, 2),
                                testStruct("bar15()", "\\.\\.", functionDtos(
                                        functionDto("bar15()", 1, bindingDtos(
                                                varBinding("bar15()$x", "T", null, asList("{as string}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        null, asList("{as string}"), false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("asString()", "\\.\\.", functionDtos(
                                        functionDto("asString()", 1, bindingDtos(
                                                varBinding("asString()$x", "T", null, asList("{as string}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        null, asList("{as string}"), false)
                                        ))
                                ), 1, 2, 2)
                        }
                },
                {
                        "function foo16($x, $y){ return bar16($x, $y); return $x; return $y; }"
                                + "function bar16($x, $y){ return foo16($x, $y); return $x + $y;}"
                                + "function test1(){return foo16(2.2, 1);}"
                                + "function test2(){return foo16(2.2, '1');}"
                                + "function test3(){return foo16(1, false);}"
                                + "function test4(){return foo16(1, '1');}"
                                + "function test5(){return foo16(1, 2.2);}"
                                + "function test6(){return foo16('1', 2.2);}"
                                + "function test7(){return foo16(false, 1);}"
                                + "function test8(){return foo16('1', 1);}",
                        new OverloadTestStruct[]{
                                testStruct("foo16()", "\\.\\.", functionDtos(
                                        functionDto("foo16()", 2, bindingDtos(
                                                varBinding("foo16()$x", "V2", null, asList("int"), true),
                                                varBinding("foo16()$y", "V3", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("foo16()", 2, bindingDtos(
                                                varBinding("foo16()$x", "V2", null, asList("float"), true),
                                                varBinding("foo16()$y", "V3", null, asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("float"), null, true)
                                        )),
                                        functionDto("foo16()", 2, bindingDtos(
                                                varBinding("foo16()$x", "T1",
                                                        null, asList("{as T3}", "@V1", "@V4", "@V5"), false),
                                                varBinding("foo16()$y", "T2",
                                                        null, asList("{as T3}", "@V1", "@V4", "@V5"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V5",
                                                        asList("@T1", "@T2", "@T3"), null, false),
                                                varBinding("!help0", "T3",
                                                        null, asList("(float | int)", "@V1", "@V4", "@V5"), false)
                                        )),
                                        functionDto("foo16()", 2, bindingDtos(
                                                varBinding("foo16()$x", "V2", null, asList("array"), true),
                                                varBinding("foo16()$y", "V3", null, asList("array"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("array"), null, true)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar16()", "\\.\\.", functionDtos(
                                        functionDto("bar16()", 2, bindingDtos(
                                                varBinding("bar16()$x", "V2", null, asList("int"), true),
                                                varBinding("bar16()$y", "V3", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("bar16()", 2, bindingDtos(
                                                varBinding("bar16()$x", "V2", null, asList("float"), true),
                                                varBinding("bar16()$y", "V3", null, asList("float"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("float"), null, true)
                                        )),
                                        functionDto("bar16()", 2, bindingDtos(
                                                varBinding("bar16()$x", "T1",
                                                        null, asList("{as T3}", "@V1", "@V4", "@V5"), false),
                                                varBinding("bar16()$y", "T2",
                                                        null, asList("{as T3}", "@V1", "@V4", "@V5"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V5",
                                                        asList("@T1", "@T2", "@T3"), null, false),
                                                varBinding("!help0", "T3",
                                                        null, asList("(float | int)", "@V1", "@V4", "@V5"), false)
                                        )),
                                        functionDto("bar16()", 2, bindingDtos(
                                                varBinding("bar16()$x", "V2", null, asList("array"), true),
                                                varBinding("bar16()$y", "V3", null, asList("array"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("array"), null, true)
                                        ))
                                ), 1, 1, 2),
                                testStruct("test1()", "\\.\\.", functionDtos("test1()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int", "float"), null, true)
                                )), 1, 2, 2),
                                testStruct("test2()", "\\.\\.", functionDtos("test2()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("string", "float"), null, true)
                                )), 1, 3, 2),
                                testStruct("test3()", "\\.\\.", functionDtos("test3()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("falseType", "int"), null, true)
                                )), 1, 4, 2),
                                testStruct("test4()", "\\.\\.", functionDtos("test4()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6",
                                                asList("float", "int", "string"), null, true)
                                )), 1, 5, 2),
                                testStruct("test5()", "\\.\\.", functionDtos("test5()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int", "float"), null, true)
                                )), 1, 6, 2),
                                testStruct("test6()", "\\.\\.", functionDtos("test6()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("string", "float"), null, true)
                                )), 1, 7, 2),
                                testStruct("test7()", "\\.\\.", functionDtos("test7()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("falseType", "int"), null, true)
                                )), 1, 8, 2),
                                testStruct("test8()", "\\.\\.", functionDtos("test8()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6",
                                                asList("string", "float", "int"), null, true)
                                )), 1, 9, 2),
                        }
                },
                //see TINS-559 NoSuchElement for indirect recursive function
                {
                        "function foo23($x, $y, $z){return bar23($x, $y, $z);}\n"
                                + "function bar23($x, $y, $z){return foo23($x-1, $y+1, $z*2);}",
                        new OverloadTestStruct[]{
                                testStruct("foo23()", "\\.\\.", functionDtos(
                                        functionDto("foo23()", 3, bindingDtos(
                                                varBinding("foo23()$x", "V2", null, asList("int"), true),
                                                varBinding("foo23()$y", "V3", null, asList("int"), true),
                                                varBinding("foo23()$z", "V4", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V6", asList("mixed"), null, true)
                                        )),
                                        functionDto("foo23()", 3, bindingDtos(
                                                varBinding("foo23()$x", "V2", null, asList("{as (float | int)}"), true),
                                                varBinding("foo23()$y", "V3", null, asList("{as (float | int)}"), true),
                                                varBinding("foo23()$z", "V4", null, asList("{as (float | int)}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V7", asList("mixed"), null, true)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar23()", "\\.\\.", functionDtos(
                                        functionDto("bar23()", 3, bindingDtos(
                                                varBinding("bar23()$x", "V2", null, asList("int"), true),
                                                varBinding("bar23()$y", "V5", null, asList("int"), true),
                                                varBinding("bar23()$z", "V8", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V12", asList("mixed"), null, true)
                                        )),
                                        functionDto("bar23()", 3, bindingDtos(
                                                varBinding("bar23()$x", "V2", null, asList("{as (float | int)}"), true),
                                                varBinding("bar23()$y", "V5", null, asList("{as (float | int)}"), true),
                                                varBinding("bar23()$z", "V8", null, asList("{as (float | int)}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V12", asList("mixed"), null, true)
                                        ))
                                ), 1, 1, 2),
                        }
                }
        });
    }
}
