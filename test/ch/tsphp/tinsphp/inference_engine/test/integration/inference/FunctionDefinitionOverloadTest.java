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
public class FunctionDefinitionOverloadTest extends AInferenceOverloadTest
{

    public FunctionDefinitionOverloadTest(String testString, OverloadTestStruct[] theTestStructs) {
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
        List<String> numLower = asList("float", "int");
        List<String> numUpper = asList("(float | int)");
        List<String> asNum = asList("{as (float | int)}");
        return asList(new Object[][]{
                {
                        "function foo($x){return $x + [];}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "V2", asList("array"), asList("array"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("array"), asList("array"), true)
                        )), 1, 0, 2)
                },
                {
                        "function foo($x){return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T", null, null, false)
                        )), 1, 0, 2)
                },
                {
                        "function foo(){return null;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 0, bindingDtos(
                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("nullType"), asList("nullType"), true)
                        )), 1, 0, 2)
                },
                //notice $x and $y are not used within the function body
                {
                        "function foo($x, $y){return null;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "V4", asList("mixed"), asList("mixed"), true),
                                varBinding("foo()$y", "V5", asList("mixed"), asList("mixed"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("nullType"), asList("nullType"), true)
                        )), 1, 0, 2)
                },
                {
                        "function foo($x, $y){ $x = $y; return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "T1", asList("@T2"), null, false),
                                varBinding("foo()$y", "T2", null, asList("@T1"), false),
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("@T2"), null, false)
                        )), 1, 0, 2)
                },
                //additional indirection $y < $a < $x < rtn in contrast to previous
                {
                        "function foo($x, $y){ $a = $y; $x = $a; return $x;} /* $y < $a < $x < rtn */",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "T1", asList("@T2"), null, false),
                                varBinding("foo()$y", "T2", null, asList("@T1"), false),
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("@T2"), null, false)
                        )), 1, 0, 2)
                },
                //additional indirection $y < $a < $x < $b < rtn in contrast to previous
                {
                        "function foo($x, $y){ $a = $y; $x = $a; $b = $x; return $b;} /*$y < $a < $x < $b < rtn*/",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "T1", asList("@T2"), null, false),
                                varBinding("foo()$y", "T2", null, asList("@T1"), false),
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("@T2"), null, false)
                        )), 1, 0, 2)
                },
                //same as before but return is constant
                {
                        "function foo($x, $y){ $a = $y; $x = $a; $b = $x; return 1;} /* constant return */",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                //should have mixed as lower instead of bottom type
                                varBinding("foo()$x", "V4", asList("mixed"), asList("mixed"), true),
                                varBinding("foo()$y", "V3", asList("mixed"), asList("mixed"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V10", asList("int"), asList("int"), true)
                        )), 1, 0, 2)
                },
                //return is variable again but $x has additionally an upper bound
                {
                        "function foo8($x, $y){ $x + 1; $a = $y; $x = $a; $b = $x; return $x;}",
                        testStructs("foo8()", "\\.\\.", functionDtos(
                                //int x int -> int
                                functionDto("foo8()", 2, bindingDtos(
                                        varBinding("foo8()$x", "T1", asList("@T2"), asList("int"), false),
                                        varBinding("foo8()$y", "T2", null, asList("int", "@T1"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T1", asList("@T2"), asList("int"), false)
                                )),
                                functionDto("foo8()", 2, bindingDtos(
                                        varBinding("foo8()$x", "T1", asList("@T2"), asNum, false),
                                        varBinding("foo8()$y", "T2", null, asList("(float | int)", "@T1"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T1", asList("@T2"), asNum, false)
                                ))
                        ), 1, 0, 2)
                },
                //$x has no upper bound anymore but $a has an additional lower type bound
                {
                        "function foo($x, $y){ $a = 1; $a = $y; $x = $a; $b = $x; return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "T1", asList("@T2", "int"), null, false),
                                varBinding("foo()$y", "T2", null, asList("@T1", "@V4"), false),
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("@T2", "int"), null, false)
                        )), 1, 0, 2)
                },
                //as before but $y has the lower type bound instead of $a
                {
                        "function foo($x, $y){ $y = 1; $a = $y; $x = $a; $b = $x; return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "T1", asList("@T2", "int"), null, false),
                                varBinding("foo()$y", "T2", asList("int"), asList("@T1"), false),
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("@T2", "int"), null, false)
                        )), 1, 0, 2)
                },
                //$x is overwritten before it is used
                {
                        "function foo($x){ $x = null; return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T", asList("nullType"), null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T", asList("nullType"), null, false)
                        )), 1, 0, 2)
                },
                //constant function but with indirection
                //see also TINS-386 - function with constant return via indirection
                {
                        "function foo(){ $a = null; return \n$a;} /* constant function but with indirection */",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 0, bindingDtos(
                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("nullType"), asList("nullType"), true)
                        )), 1, 0, 2)
                },
                //same as before but with unused parameter
                {
                        "function foo($x){ $a = null; return \n$a;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "V6", asList("mixed"), asList("mixed"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("nullType"), asList("nullType"), true)
                        )), 1, 0, 2)
                },
                //$x with useless statement. Second one is constant but $x has no lower bound
                //see also TINS-384 most specific overload and variable parameter
                {
                        "function foo14($x){ $x + true; $x + true; return \n1;}",
                        testStructs("foo14()", "\\.\\.", functionDtos(
                                functionDto("foo14()", 1, bindingDtos(
                                        varBinding("foo14()$x", "V2", asNum, asNum, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), asList("int"), true)
                                ))
                        ), 1, 0, 2)
                },
                {
                        "function foo($x, $y){ if($x){return $y;} return false; }"
                                + "function bar($x){ if($x > 0){return foo(true, $x-1);} return $x;}"
                                + "function test(){return foo(true, 'hello');}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos(
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V5", boolLower, boolUpper, true),
                                                varBinding("foo()$y", "T", null, asList("@V3"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V3",
                                                        asList("falseType", "@T"), null, false)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V5", asBool, asBool, true),
                                                varBinding("foo()$y", "T", null, asList("@V3"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V3",
                                                        asList("falseType", "@T"), null, false)
                                        ))), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos(
                                        functionDto("bar()", 1, bindingDtos(
                                                varBinding("bar()$x", "V2", asList("int"), asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("falseType", "int"), asList("(falseType | int)"), true)
                                        )),
                                        functionDto("bar()", 1, bindingDtos(
                                                varBinding("bar()$x", "T1", null, asList("@V9", "{as T2}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("falseType", "int", "@T1", "@T2"), null, false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("test()", "\\.\\.", functionDtos("test()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V5",
                                                asList("string", "falseType"), asList("(falseType | string)"), true)
                                )), 1, 2, 2)
                        }
                },
                //see TINS-466 - rename type variable does not promote type bounds
                {
                        "function test(){return foo(true, 'hello');}"
                                + "function foo($x, $y){ if($x){ } return $y;}",
                        new OverloadTestStruct[]{
                                testStruct("test()", "\\.\\.", functionDtos("test()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V3",
                                                asList("string"), asList("string"), true)
                                )), 1, 0, 2),
                                testStruct("foo()", "\\.\\.", functionDtos(
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V2", boolLower, boolUpper, true),
                                                varBinding("foo()$y", "T", null, null, false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, null, false)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V2", asBool, asBool, true),
                                                varBinding("foo()$y", "T", null, null, false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, null, false)
                                        ))
                                ), 1, 1, 2)
                        }
                },
                //TINS-420 constraint solving fall back to soft typing
                {
                        "function foo7(array $x){ $x = 1.2; return $x + 1; }",
                        testStructs("foo7()", "\\.\\.", functionDtos("foo7()", 1, bindingDtos(
                                varBinding("foo7()$x", "V2", asList("array", "float"), asList("(array | float)"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", asList("float"), asList("float"), true)
                        )), 1, 0, 2),
                },
                {
                        "function foo8(array $x){ $x = 'hello'; return $x + 1; }",
                        testStructs("foo8()", "\\.\\.", functionDtos("foo8()", 1, bindingDtos(
                                varBinding("foo8()$x", "V2",
                                        asList("array", "string"), asList("(array | string)"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", numLower, numUpper, true)
                        )), 1, 0, 2),
                },
                {
                        "function foo9(array $x){ $x = false; return $x + 1; }",
                        testStructs("foo9()", "\\.\\.", functionDtos("foo9()", 1, bindingDtos(
                                varBinding("foo9()$x", "V2",
                                        asList("array", "falseType"),
                                        asList("(array | falseType)"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), asList("int"), true)
                        )), 1, 0, 2),
                },
                //soft typing and dependencies from soft typed function to another
                {
                        "function foo10(array $x){ $x = bar10(); return $x + 1; }"
                                + "function bar10(){return false;}",
                        testStructs("foo10()", "\\.\\.", functionDtos("foo10()", 1, bindingDtos(
                                varBinding("foo10()$x", "V3",
                                        asList("array", "falseType"),
                                        asList("(array | falseType)"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), asList("int"), true)
                        )), 1, 0, 2),
                },
                //soft typing and dependencies to a soft typed function
                {
                        "function foo12(array $x){ $x = 1; return $x + 1; }"
                                + "function test12(){return foo12([1]);}",
                        new OverloadTestStruct[]{
                                testStruct("foo12()", "\\.\\.", functionDtos("foo12()", 1, bindingDtos(
                                        varBinding("foo12()$x", "V2",
                                                asList("array", "int"), asList("(array | int)"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), asList("int"), true)
                                )), 1, 0, 2),
                                testStruct("test12()", "\\.\\.", functionDtos("test12()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V4", asList("int"), asList("int"), true)
                                )), 1, 1, 2),
                        }
                },
                //needs to fall back to soft typing during dependency resolving
                {
                        "function foo11($x){ $x = bar11(); return $x + 1; }"
                                + "function bar11(){return [1];}",
                        testStructs("foo11()", "\\.\\.", functionDtos("foo11()", 1, bindingDtos(
                                varBinding("foo11()$x", "V3",
                                        asList("array", "{as (float | int)}"),
                                        asList("(array | {as (float | int)})"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", numLower, numUpper, true)
                        )), 1, 0, 2),
                },
                //soft typing and dependencies from a soft typed function to a soft typed function
                {
                        "function foo13(array $x){ $x = 1; return $x + 1; }"
                                + "function test13(){return foo13([1]) + 1;}",
                        new OverloadTestStruct[]{
                                testStruct("foo13()", "\\.\\.", functionDtos("foo13()", 1, bindingDtos(
                                        varBinding("foo13()$x", "V2",
                                                asList("array", "int"), asList("(array | int)"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), asList("int"), true)
                                )), 1, 0, 2),
                                testStruct("test13()", "\\.\\.", functionDtos("test13()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int"), asList("int"), true)
                                )), 1, 1, 2),
                        }
                },
                //needs to fall back to soft typing during dependency resolving and dependency was soft typed as well
                {
                        "function test13(){return foo13([1]) + 1;}"
                                + "function foo13(array $x){ $x = 1; return $x + 1; }",
                        new OverloadTestStruct[]{
                                testStruct("test13()", "\\.\\.", functionDtos("test13()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), asList("int"), true)
                                )), 1, 0, 2),
                                testStruct("foo13()", "\\.\\.", functionDtos("foo13()", 1, bindingDtos(
                                        varBinding("foo13()$x", "V2",
                                                asList("array", "int"), asList("(array | int)"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), asList("int"), true)
                                )), 1, 1, 2),
                        }
                },
                {
                        "function test14(array $x){return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("test14()", "\\.\\.", functionDtos("test14()", 1, bindingDtos(
                                        varBinding("test14()$x", "T", null, asList("array"), false),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("array"), false)
                                )), 1, 0, 2),
                        }
                },
                {
                        "function plusWithEcho($x, $y){echo $x; echo $y; return $x + $y;}",
                        new OverloadTestStruct[]{
                                testStruct("plusWithEcho()", "\\.\\.", functionDtos("plusWithEcho()", 2, bindingDtos(
                                        varBinding("plusWithEcho()$x", "V2",
                                                asList("({as T} & {as string})"), asList("{as string}", "{as T}"),
                                                true),
                                        varBinding("plusWithEcho()$y", "V4",
                                                asList("({as T} & {as string})"), asList("{as string}", "{as T}"),
                                                true),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("(float | int)"), false)
                                )), 1, 0, 2),
                        }
                },
        });
    }
}
