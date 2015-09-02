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
        List<String> boolUpper = asList("(falseType | trueType)");
        List<String> asBool = asList("{as (falseType | trueType)}");
        List<String> numLower = asList("float", "int");
        List<String> asNum = asList("{as (float | int)}");
        return asList(new Object[][]{
                {
                        "function foo($x){return $x + [];}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "V2", null, asList("array"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("array"), null, true)
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
                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("nullType"), null, true)
                        )), 1, 0, 2)
                },
                //notice $x and $y are not used within the function body
                {
                        "function foo($x, $y){return null;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "V4", null, asList("mixed"), true),
                                varBinding("foo()$y", "V5", null, asList("mixed"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("nullType"), null, true)
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
                                varBinding("foo()$x", "V4", null, asList("mixed"), true),
                                varBinding("foo()$y", "V3", null, asList("mixed"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V10", asList("int"), null, true)
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
                                        varBinding("foo8()$y", "T2", null, asList("{as (float | int)}", "@T1"), false),
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
                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("nullType"), null, true)
                        )), 1, 0, 2)
                },
                //same as before but with unused parameter
                {
                        "function foo($x){ $a = null; return \n$a;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "V6", null, asList("mixed"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("nullType"), null, true)
                        )), 1, 0, 2)
                },
                //$x with useless statement. Second one is constant but $x has no lower bound
                //see also TINS-384 most specific overload and variable parameter
                {
                        "function foo14($x){ $x + true; $x + true; return \n1;}",
                        testStructs("foo14()", "\\.\\.", functionDtos(
                                functionDto("foo14()", 1, bindingDtos(
                                        varBinding("foo14()$x", "V2", null, asNum, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                ))
                        ), 1, 0, 2)
                },
                {
                        "function foo15($x, $y){ if($x){return $y;} return false; }"
                                + "function bar15($x){ if($x > 0){return foo15(true, $x-1);} return $x;}"
                                + "function test15A(){return foo15(true, 'hello');}"
                                + "function test15B(){return bar15(1.2);}",
                        new OverloadTestStruct[]{
                                testStruct("foo15()", "\\.\\.", functionDtos(
                                        functionDto("foo15()", 2, bindingDtos(
                                                varBinding("foo15()$x", "V5", null, boolUpper, true),
                                                varBinding("foo15()$y", "T", null, asList("@V3"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V3",
                                                        asList("falseType", "@T"), null, false)
                                        )),
                                        functionDto("foo15()", 2, bindingDtos(
                                                varBinding("foo15()$x", "V5", null, asBool, true),
                                                varBinding("foo15()$y", "T", null, asList("@V3"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V3",
                                                        asList("falseType", "@T"), null, false)
                                        ))), 1, 0, 2),
                                testStruct("bar15()", "\\.\\.", functionDtos(
                                        functionDto("bar15()", 1, bindingDtos(
                                                varBinding("bar15()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("falseType", "int"), null, true)
                                        )),
                                        functionDto("bar15()", 1, bindingDtos(
                                                varBinding("bar15()$x", "T1", null, asList("@V9", "{as T2}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V9",
                                                        asList("falseType", "int", "@T1", "@T2"), null, false),
                                                varBinding("cScope-@1|115", "T2",
                                                        asList("int"),
                                                        asList("(float | int)", "@V6", "@V8", "@V9"),
                                                        false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("test15A()", "\\.\\.", functionDtos("test15A()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V5",
                                                asList("string", "falseType"), null, true)
                                )), 1, 2, 2),
                                //TINS-600 function instantiation with convertibles too general
                                //should only be float and falseType
                                testStruct("test15B()", "\\.\\.", functionDtos("test15B()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V5",
                                                asList("float", "int", "falseType"), null, true)
                                )), 1, 3, 2)
                        }
                },
                //see TINS-466 - rename type variable does not promote type bounds
                {
                        "function test(){return foo(true, 'hello');}"
                                + "function foo($x, $y){ if($x){ } return $y;}",
                        new OverloadTestStruct[]{
                                testStruct("test()", "\\.\\.", functionDtos("test()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V5", asList("string"), null, true)
                                )), 1, 0, 2),
                                testStruct("foo()", "\\.\\.", functionDtos(
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V2", null, boolUpper, true),
                                                varBinding("foo()$y", "T", null, null, false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, null, false)
                                        )),
                                        functionDto("foo()", 2, bindingDtos(
                                                varBinding("foo()$x", "V2", null, asBool, true),
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
                                varBinding("foo7()$x", "V2", null, asList("(array | float)"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", asList("float"), null, true)
                        )), 1, 0, 2),
                },
                {
                        "function foo8(array $x){ $x = 'hello'; return $x + 1; }",
                        testStructs("foo8()", "\\.\\.", functionDtos("foo8()", 1, bindingDtos(
                                varBinding("foo8()$x", "V2", null, asList("(array | string)"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", numLower, null, true)
                        )), 1, 0, 2),
                },
                {
                        "function foo9(array $x){ $x = false; return $x + 1; }",
                        testStructs("foo9()", "\\.\\.", functionDtos("foo9()", 1, bindingDtos(
                                varBinding("foo9()$x", "V2", null, asList("(array | falseType)"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                        )), 1, 0, 2),
                },
                //soft typing and dependencies from soft typed function to another
                {
                        "function foo10(array $x){ $x = bar10(); return $x + 1; }"
                                + "function bar10(){return false;}",
                        testStructs("foo10()", "\\.\\.", functionDtos("foo10()", 1, bindingDtos(
                                varBinding("foo10()$x", "V3", null, asList("(array | falseType)"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                        )), 1, 0, 2),
                },
                //soft typing and dependencies to a soft typed function
                {
                        "function foo12(array $x){ $x = 1; return $x + 1; }"
                                + "function test12(){return foo12([1]);}",
                        new OverloadTestStruct[]{
                                testStruct("foo12()", "\\.\\.", functionDtos("foo12()", 1, bindingDtos(
                                        varBinding("foo12()$x", "V2", null, asList("(array | int)"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                                )), 1, 0, 2),
                                testStruct("test12()", "\\.\\.", functionDtos("test12()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V4", asList("int"), null, true)
                                )), 1, 1, 2),
                        }
                },
                //<strike>needs to fall back to soft typing during dependency resolving</strike>
                //not anymore, since we do no longer continue when we detect a dependency
                {
                        "function foo11($x){ $x = bar11(); return $x + 1; }"
                                + "function bar11(){return [1];}",
                        new OverloadTestStruct[]{
                                testStruct("foo11()", "\\.\\.", functionDtos("foo11()", 1, bindingDtos(
                                        varBinding("foo11()$x", "V3", null,
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", numLower, null, true)
                                )), 1, 0, 2),
                                testStruct("bar11()", "\\.\\.", functionDtos("bar11()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("array"), null, true)
                                )), 1, 1, 2),
                        }
                },
                //needs to fall back to soft typing during dependency resolving
                {
                        "function foo11B(){ $x = [1]; $x = 1; return bar11B($x);}"
                                + "function bar11B($x){return $x + 1;}",
                        new OverloadTestStruct[]{
                                testStruct("foo11B()", "\\.\\.", functionDtos("foo11B()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V8", numLower, null, true)
                                )), 1, 0, 2),
                                testStruct("bar11B()", "\\.\\.", functionDtos(
                                        functionDto("bar11B()", 1, bindingDtos(
                                                varBinding("bar11B()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("bar11B()", 1, bindingDtos(
                                                varBinding("bar11B()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 1, 2),
                        }
                },
                //soft typing and dependencies from a soft typed function to a soft typed function
                {
                        "function foo13(array $x){ $x = 1; return $x + 1; }"
                                + "function test13(){return foo13([1]) + 1;}",
                        new OverloadTestStruct[]{
                                testStruct("foo13()", "\\.\\.", functionDtos("foo13()", 1, bindingDtos(
                                        varBinding("foo13()$x", "V2", null, asList("(array | int)"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                                )), 1, 0, 2),
                                testStruct("test13()", "\\.\\.", functionDtos("test13()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int"), null, true)
                                )), 1, 1, 2),
                        }
                },
                //<strike>needs to fall back to soft typing during dependency resolving
                // and dependency was soft typed as well</strike>
                //not anymore, since we do no longer continue when we detect a dependency
                {
                        "function test13B(){return foo13B([1]) + 1;}"
                                + "function foo13B(array $x){ $x = 1; return $x + 1; }",
                        new OverloadTestStruct[]{
                                testStruct("test13B()", "\\.\\.", functionDtos("test13B()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int"), null, true)
                                )), 1, 0, 2),
                                testStruct("foo13B()", "\\.\\.", functionDtos("foo13B()", 1, bindingDtos(
                                        varBinding("foo13B()$x", "V2", null, asList("(array | int)"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                                )), 1, 1, 2),
                        }
                },
                //<strike>needs to fall back to soft typing during dependency resolving
                // and dependency was soft typed as well</strike>
                //not anymore, since we do no longer continue when we detect a dependency
                {
                        "function test13C(){$a = [1]; $a = 1; $a + 1; return foo13C([1]) + 1;}"
                                + "function foo13C(array $x){ $x = 1; return $x + 1; }",
                        new OverloadTestStruct[]{
                                testStruct("test13C()", "\\.\\.", functionDtos("test13C()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V13", asList("int"), null, true)
                                )), 1, 0, 2),
                                testStruct("foo13C()", "\\.\\.", functionDtos("foo13C()", 1, bindingDtos(
                                        varBinding("foo13C()$x", "V2", null, asList("(array | int)"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                                )), 1, 1, 2),
                        }
                },
                //needs to fall back to soft typing during dependency resolving and dependency was soft typed as well
                {
                        "function test13D(){$a = [1]; $a = 1; $a + 1; return foo13D([1]) + 1;}"
                                + "function foo13D(array $x){ bar13D($x); $x = 1; return $x + 1; }"
                                + "function bar13D($x){return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("test13D()", "\\.\\.", functionDtos("test13D()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V13", asList("int"), null, true)
                                )), 1, 0, 2),
                                testStruct("foo13D()", "\\.\\.", functionDtos("foo13D()", 1, bindingDtos(
                                        varBinding("foo13D()$x", "V3", null, asList("(array | int)"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                )), 1, 1, 2),
                                testStruct("bar13D()", "\\.\\.", functionDtos("bar13D()", 1, bindingDtos(
                                        varBinding("bar13D()$x", "T", null, null, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, null, false)
                                )), 1, 2, 2),
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
                                testStruct("plusWithEcho()", "\\.\\.", functionDtos(
                                        functionDto("plusWithEcho()", 2, bindingDtos(
                                                varBinding("plusWithEcho()$x", "V2", null, asList("string"), true),
                                                varBinding("plusWithEcho()$y", "V4", null, asList("string"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V7",
                                                        asList("int", "float"), null, true)
                                        )),
                                        functionDto("plusWithEcho()", 2, bindingDtos(
                                                varBinding("plusWithEcho()$x", "V2",
                                                        null, asList("{as string}", "{as T}"), true),
                                                varBinding("plusWithEcho()$y", "V4",
                                                        null, asList("{as string}", "{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        null, asList("(float | int)"), false)
                                        ))
                                ), 1, 0, 2),
                        }
                },
                {
                        "function foo16($x){$a = $x & 1; echo $x; return $a;}",
                        new OverloadTestStruct[]{
                                testStruct("foo16()", "\\.\\.", functionDtos(
                                        functionDto("foo16()", 1, bindingDtos(
                                                varBinding("foo16()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                        )),
                                        functionDto("foo16()", 1, bindingDtos(
                                                varBinding("foo16()$x", "V2",
                                                        null, asList("{as string}", "(array | {as int})"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                        ))
                                ), 1, 0, 2),
                        }
                },
                //see TINS-568 convertible types sometimes not generic
                {
                        "function foo28($x){return $x + 1;}\n"
                                + "function bar28($x, $y){return foo28($x) + $y;}"
                                + "function test1(){return bar28(2.2, 1);}"
                                + "function test2(){return bar28(2.2, '1');}"
                                + "function test3(){return bar28(1, false);}"
                                + "function test4(){return bar28(1, '1');}"
                                + "function test5(){return bar28(1, 2.2);}"
                                + "function test6(){return bar28('1', 2.2);}"
                                + "function test7(){return bar28(false, 1);}"
                                + "function test8(){return bar28('1', 1);}",
                        new OverloadTestStruct[]{
                                testStruct("foo28()", "\\.\\.", functionDtos(
                                        functionDto("foo28()", 1, bindingDtos(
                                                varBinding("foo28()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("foo28()", 1, bindingDtos(
                                                varBinding("foo28()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar28()", "\\.\\.", functionDtos(
                                        functionDto("bar28()", 2, bindingDtos(
                                                varBinding("bar28()$x", "V2", null, asList("int"), true),
                                                varBinding("bar28()$y", "V4", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V6", asList("int"), null, true)
                                        )),
                                        functionDto("bar28()", 2, bindingDtos(
                                                varBinding("bar28()$x", "V2",
                                                        null, asList("{as T2}"), true),
                                                varBinding("bar28()$y", "V4", null, asList("{as T1}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T1",
                                                        asList("int", "@T2"), asList("(float | int)"), false),
                                                varBinding("bar28()foo28()@2|30", "T2",
                                                        asList("int"), asList("(float | int)", "@T1"), false)
                                        ))
                                ), 1, 1, 2),
                                //TODO TINS-600 function instantiation with convertibles too general
                                testStruct("test1()", "\\.\\.", functionDtos("test1()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("float", "int"), null, true)
                                )), 1, 2, 2),
                                //TODO TINS-600 function instantiation with convertibles too general
                                testStruct("test2()", "\\.\\.", functionDtos("test2()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("float", "int"), null, true)
                                )), 1, 3, 2),
                                testStruct("test3()", "\\.\\.", functionDtos("test3()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int"), null, true)
                                )), 1, 4, 2),
                                testStruct("test4()", "\\.\\.", functionDtos("test4()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("float", "int"), null, true)
                                )), 1, 5, 2),
                                testStruct("test5()", "\\.\\.", functionDtos("test5()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("float"), null, true)
                                )), 1, 6, 2),
                                //TODO TINS-600 function instantiation with convertibles too general
                                testStruct("test6()", "\\.\\.", functionDtos("test6()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("float", "int"), null, true)
                                )), 1, 7, 2),
                                testStruct("test7()", "\\.\\.", functionDtos("test7()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("int"), null, true)
                                )), 1, 8, 2),
                                testStruct("test8()", "\\.\\.", functionDtos("test8()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("float", "int"), null, true)
                                )), 1, 9, 2),
                        }
                },
                //see TINS-549 convertible type with lower to same type variable
                {
                        "function foo3($x){$x = $x + 1; return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("foo3()", "\\.\\.", functionDtos(
                                        functionDto("foo3()", 1, bindingDtos(
                                                varBinding("foo3()$x", "V4", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V6", asList("int"), null, true)
                                        )),
                                        functionDto("foo3()", 1, bindingDtos(
                                                varBinding("foo3()$x", "T1",
                                                        asList("int", "@T2"), asList("{as T2}"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "T1",
                                                        asList("int", "@T2"), asList("{as T2}"), false),
                                                varBinding("foo3()+@1|31", "T2",
                                                        asList("int"), asList("(float | int)", "@T1"), false)
                                        ))
                                ), 1, 0, 2)
                        }
                },
                //dependency to ad-hoc polymorphic function
                {
                        "function foo4($x){return bar4($x); return baz4(1);} "
                                + "function bar4($x){return $x + 1;}"
                                + "function baz4($x){return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("foo4()", "\\.\\.", functionDtos(
                                        functionDto("foo4()", 1, bindingDtos(
                                                varBinding("foo4()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V4", asList("int"), null, true)
                                        )),
                                        functionDto("foo4()", 1, bindingDtos(
                                                varBinding("foo4()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar4()", "\\.\\.", functionDtos(
                                        functionDto("bar4()", 1, bindingDtos(
                                                varBinding("bar4()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("bar4()", 1, bindingDtos(
                                                varBinding("bar4()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("baz4()", "\\.\\.", functionDtos(
                                        functionDto("baz4()", 1, bindingDtos(
                                                varBinding("baz4()$x", "T", null, null, false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, null, false)
                                        ))
                                ), 1, 2, 2)
                        }
                },
                //dependency to ad-hoc polymorphic function (last dependency)
                {
                        "function foo4B($x){return bar4B($x); return baz4B(1);} "
                                + "function baz4B($x){return $x;}"
                                + "function bar4B($x){return $x + 1;}",
                        new OverloadTestStruct[]{
                                testStruct("foo4B()", "\\.\\.", functionDtos(
                                        functionDto("foo4B()", 1, bindingDtos(
                                                varBinding("foo4B()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V4", asList("int"), null, true)
                                        )),
                                        functionDto("foo4B()", 1, bindingDtos(
                                                varBinding("foo4B()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("baz4B()", "\\.\\.", functionDtos(
                                        functionDto("baz4B()", 1, bindingDtos(
                                                varBinding("baz4B()$x", "T", null, null, false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, null, false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("bar4B()", "\\.\\.", functionDtos(
                                        functionDto("bar4B()", 1, bindingDtos(
                                                varBinding("bar4B()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("bar4B()", 1, bindingDtos(
                                                varBinding("bar4B()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 2, 2)
                        }
                },
                //dependency to ad-hoc polymorphic function (last dependency) but with constant arguments
                {
                        "function foo4C($x){return bar4C(4); return baz4C($x);} "
                                + "function baz4C($x){return $x;}"
                                + "function bar4C($x){return $x + 1;}",
                        new OverloadTestStruct[]{
                                testStruct("foo4C()", "\\.\\.", functionDtos(
                                        functionDto("foo4C()", 1, bindingDtos(
                                                varBinding("foo4C()$x", "T", null, asList("@V4"), false),
                                                varBinding(RETURN_VARIABLE_NAME, "V4",
                                                        asList("int", "@T"), null, false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("baz4C()", "\\.\\.", functionDtos(
                                        functionDto("baz4C()", 1, bindingDtos(
                                                varBinding("baz4C()$x", "T", null, null, false),
                                                varBinding(RETURN_VARIABLE_NAME, "T", null, null, false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("bar4C()", "\\.\\.", functionDtos(
                                        functionDto("bar4C()", 1, bindingDtos(
                                                varBinding("bar4C()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("bar4C()", 1, bindingDtos(
                                                varBinding("bar4C()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 2, 2)
                        }
                },
                //dependency to two ad-hoc polymorphic functions (last dependency)
                {
                        "function foo4D($x){return bar4D($x); return baz4D($x);} "
                                + "function baz4D($x){return $x - 1;}"
                                + "function bar4D($x){return $x + 1;}",
                        new OverloadTestStruct[]{
                                testStruct("foo4D()", "\\.\\.", functionDtos(
                                        functionDto("foo4D()", 1, bindingDtos(
                                                varBinding("foo4D()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V4", asList("int"), null, true)
                                        )),
                                        functionDto("foo4D()", 1, bindingDtos(
                                                varBinding("foo4D()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 0, 2),
                                testStruct("baz4D()", "\\.\\.", functionDtos(
                                        functionDto("baz4D()", 1, bindingDtos(
                                                varBinding("baz4D()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("baz4D()", 1, bindingDtos(
                                                varBinding("baz4D()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 1, 2),
                                testStruct("bar4D()", "\\.\\.", functionDtos(
                                        functionDto("bar4D()", 1, bindingDtos(
                                                varBinding("bar4D()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("bar4D()", 1, bindingDtos(
                                                varBinding("bar4D()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 2, 2)
                        }
                },
                //see TINS-663 soft typing and increment operator
                {
                        "function foo5(){$a = [1]; $a = 1; ~$a; for($i = 0; $i < 10; ++$i){} return 1;}",
                        testStructs("foo5()", "\\.\\.", functionDtos(
                                functionDto("foo5()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V16", asList("int"), null, true)
                                ))
                        ), 1, 0, 2),
                },
                {
                        "function foo6(){foreach(['h','e','l','l','o'] as $v){echo $v;} return 1;}",
                        testStructs("foo6()", "\\.\\.", functionDtos(
                                functionDto("foo6()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), null, true)
                                ))
                        ), 1, 0, 2),
                },
                {
                        "function foo7(){$a = bar7(1); $b = bar7(2); $a + $b; return $a - 1; return $b * 2;}"
                                + "function bar7($x){return $x + 1;}",
                        new OverloadTestStruct[]{
                                testStruct("foo7()", "\\.\\.", functionDtos(
                                        functionDto("foo7()", 0, bindingDtos(
                                                varBinding(RETURN_VARIABLE_NAME, "V13", asList("int"), null, true)
                                        ))
                                ), 1, 0, 2),
                                testStruct("bar7()", "\\.\\.", functionDtos(
                                        functionDto("bar7()", 1, bindingDtos(
                                                varBinding("bar7()$x", "V2", null, asList("int"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "V5", asList("int"), null, true)
                                        )),
                                        functionDto("bar7()", 1, bindingDtos(
                                                varBinding("bar7()$x", "V2", null, asList("{as T}"), true),
                                                varBinding(RETURN_VARIABLE_NAME, "T",
                                                        asList("int"), asList("(float | int)"), false)
                                        ))
                                ), 1, 1, 2)
                        }
                },
                //see TINS-691 soft typing and dependency solving
                {
                        "function foo40(){ $a = 1; bar40($a); return bar40($a);}\n"
                                + "function bar40(array $x){return 1;}",
                        new OverloadTestStruct[]{
                                testStruct("foo40()", "\\.\\.", functionDtos("foo40()", 0, bindingDtos(
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), null, true)
                                )), 1, 0, 2),
                                testStruct("bar40()", "\\.\\.", functionDtos("bar40()", 1, bindingDtos(
                                        varBinding("bar40()$x", "V4", null, asList("array"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("int"), null, true)
                                )), 1, 1, 2)
                        }
                },
                //see TINS-668 AmbiguousOverloadException
                {
                        "function arithmetic1($x, $y, $z){ return ($x + $y) * $z / 1.2; }",
                        testStructs("arithmetic1()", "\\.\\.", functionDtos(
                                functionDto("arithmetic1()", 3, bindingDtos(
                                        varBinding("arithmetic1()$x", "V2", null, asList("int"), true),
                                        varBinding("arithmetic1()$y", "V3", null, asList("int"), true),
                                        varBinding("arithmetic1()$z", "V5", null, asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V9", asList("falseType", "float"), null, true)
                                )),
                                functionDto("arithmetic1()", 3, bindingDtos(
                                        varBinding("arithmetic1()$x", "V2", null, asList("float"), true),
                                        varBinding("arithmetic1()$y", "V3", null, asList("float"), true),
                                        varBinding("arithmetic1()$z", "V5", null, asList("float"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V9", asList("falseType", "float"), null, true)
                                )),
                                functionDto("arithmetic1()", 3, bindingDtos(
                                        varBinding("arithmetic1()$x", "V2", null, asList("{as (float | int)}"), true),
                                        varBinding("arithmetic1()$y", "V3", null, asList("{as (float | int)}"), true),
                                        varBinding("arithmetic1()$z", "V5", null, asList("{as (float | int)}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V9", asList("falseType", "float"), null, true)
                                ))
                        ), 1, 0, 2),
                },
                //TODO TINS-697 - division results in {as float} instead of {as num}
                {
                        "function arithmetic1B($x, $y, $z){ return ($x + $y) * $z / 2;}",
                        testStructs("arithmetic1B()", "\\.\\.", functionDtos(
                                functionDto("arithmetic1B()", 3, bindingDtos(
                                        varBinding("arithmetic1B()$x", "V2", null, asList("int"), true),
                                        varBinding("arithmetic1B()$y", "V3", null, asList("int"), true),
                                        varBinding("arithmetic1B()$z", "V5", null, asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V9",
                                                asList("falseType", "int", "float"), null, true)
                                )),
                                functionDto("arithmetic1B()", 3, bindingDtos(
                                        varBinding("arithmetic1B()$x", "V2", null, asList("float"), true),
                                        varBinding("arithmetic1B()$y", "V3", null, asList("float"), true),
                                        varBinding("arithmetic1B()$z", "V5", null, asList("float"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V9",
                                                asList("falseType", "float"), null, true)
                                )),
                                functionDto("arithmetic1B()", 3, bindingDtos(
                                        varBinding("arithmetic1B()$x", "V2", null, asList("{as float}"), true),
                                        varBinding("arithmetic1B()$y", "V3", null, asList("{as float}"), true),
                                        varBinding("arithmetic1B()$z", "V5", null, asList("{as float}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V9",
                                                asList("falseType", "float"), null, true)
                                ))
                        ), 1, 0, 2),
                }
        });
    }
}
