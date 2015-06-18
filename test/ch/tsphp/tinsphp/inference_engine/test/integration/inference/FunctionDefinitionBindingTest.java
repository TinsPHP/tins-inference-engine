/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceBindingTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.BindingTestStruct;
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
public class FunctionDefinitionBindingTest extends AInferenceBindingTest
{

    public FunctionDefinitionBindingTest(String testString, BindingTestStruct[] theTestStructs) {
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
        List<String> numLower = asList("float", "int");
        List<String> numUpper = asList("(float | int)");
        List<String> asBool = asList("{as (falseType | trueType)}");
        List<String> asNum = asList("{as (float | int)}");
        return asList(new Object[][]{
                {
                        "function foo($x){\nreturn $x;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding("foo()$x", "T", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T", null, null, false),
                                varBinding("return@2|0", "T", null, null, false)
                        )), 1, 0, 2)
                },
                {
                        "function greet($name){\nreturn \n'hello '\n.$name;}",
                        testStructs("greet()", "\\.\\.", matcherDtos(
                                matcherDto(
                                        varBinding("greet().@4|0", "V1", asList("string"), asList("string"), true),
                                        //is constant as well since there is no parametric overload of . operator
                                        varBinding("'hello '@3|0", "V2", asList("string"), asList("string"), true),
                                        varBinding("greet()$name", "V3", asList("string"), asList("string"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V5",
                                                asList("string"), asList("string"), true),
                                        varBinding("return@2|0", "V4", asList("string"), asList("string"), true)
                                ),
                                matcherDto(
                                        varBinding("greet().@4|0", "V1", asList("string"), asList("string"), true),
                                        //is constant as well since there is no parametric overload of . operator
                                        varBinding("'hello '@3|0", "V2", asList("string"), asList("{as string}"), true),
                                        varBinding("greet()$name", "V3",
                                                asList("{as string}"), asList("{as string}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V5",
                                                asList("string"), asList("string"), true),
                                        varBinding("return@2|0", "V4", asList("string"), asList("string"), true)
                                )), 1, 0, 2)
                },
                {
                        "function foo(){\nreturn \n1 \n+ 1 \n+ 1 \n+ 1 \n+ 1 \n+ 1;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding("foo()+@4|0", "V1", asList("int"), asList("int"), true),
                                varBinding("1@3|0", "V2", asList("int"), asList("int"), true),
                                varBinding("1@4|2", "V3", asList("int"), asList("int"), true),
                                varBinding("foo()+@5|0", "V4", asList("int"), asList("int"), true),
                                varBinding("1@5|2", "V5", asList("int"), asList("int"), true),
                                varBinding("foo()+@6|0", "V6", asList("int"), asList("int"), true),
                                varBinding("1@6|2", "V7", asList("int"), asList("int"), true),
                                varBinding("foo()+@7|0", "V8", asList("int"), asList("int"), true),
                                varBinding("1@7|2", "V9", asList("int"), asList("int"), true),
                                varBinding("foo()+@8|0", "V10", asList("int"), asList("int"), true),
                                varBinding("1@8|2", "V11", asList("int"), asList("int"), true),
                                varBinding("return@2|0", "V12", asList("int"), asList("int"), true),
                                varBinding(RETURN_VARIABLE_NAME, "V13", asList("int"), asList("int"), true)
                        )), 1, 0, 2)
                },
                {
                        "function foo(){ $a \n= \n1; $b \n= \n2; \nreturn \n3;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding("foo()=@2|0", "V1", asList("int"), asList("int"), true),
                                varBinding("foo()$a", "V1", asList("int"), asList("int"), true),
                                varBinding("1@3|0", "V3", asList("int"), null, true),
                                varBinding("foo()=@4|0", "V4", asList("int"), asList("int"), true),
                                varBinding("foo()$b", "V4", asList("int"), asList("int"), true),
                                varBinding("2@5|0", "V6", asList("int"), null, true),
                                varBinding("return@6|0", "V7", asList("int"), asList("int"), true),
                                varBinding("3@7|0", "V8", asList("int"), null, true),
                                varBinding(RETURN_VARIABLE_NAME, "V9", asList("int"), asList("int"), true)
                        )), 1, 0, 2)
                },
                //see TINS-405 multiple return cause NullPointerException
                {
                        "function foo(){\nreturn \n1; \nreturn \n1.2;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding("return@2|0", "V1", asList("int"), asList("int"), true),
                                varBinding("1@3|0", "V2", asList("int"), null, true),
                                varBinding(RETURN_VARIABLE_NAME, "V3", asList("int", "float"), numUpper, true),
                                varBinding("return@4|0", "V4", asList("float"), asList("float"), true),
                                varBinding("1.2@5|0", "V5", asList("float"), null, true)
                        )), 1, 0, 2)
                },
                {
                        "function foo5($x){ \nif($x){ \nreturn \n1;} \nreturn \n1.2;}",
                        testStructs("foo5()", "\\.\\.", matcherDtos(
                                matcherDto(
                                        varBinding("return@3|0", "V1", asList("int"), asList("int"), true),
                                        varBinding("1@4|0", "V2", asList("int"), null, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("int", "float"), numUpper, true),
                                        varBinding("if@2|0", "V4", asList("mixed"), asList("mixed"), true),
                                        varBinding("foo5()$x", "V5", boolLower, boolUpper, true),
                                        varBinding("return@5|0", "V6", asList("float"), asList("float"), true),
                                        varBinding("1.2@6|0", "V7", asList("float"), null, true)
                                ),
                                matcherDto(
                                        varBinding("return@3|0", "V1", asList("int"), asList("int"), true),
                                        varBinding("1@4|0", "V2", asList("int"), null, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V3", asList("int", "float"), numUpper, true),
                                        varBinding("if@2|0", "V4", asList("mixed"), asList("mixed"), true),
                                        varBinding("foo5()$x", "V5", asBool, asBool, true),
                                        varBinding("return@5|0", "V6", asList("float"), asList("float"), true),
                                        varBinding("1.2@6|0", "V7", asList("float"), null, true)
                                )), 1, 0, 2)
                },
                //see TINS-449 unused ad-hoc polymorphic parameters
                {
                        "function foo6($x, $y){$a \n= $x \n+ $y;\nreturn \n1;}",
                        testStructs("foo6()", "\\.\\.", matcherDtos(
                                //int x int -> int
                                matcherDto(
                                        varBinding("foo6()+@3|0", "V1", asList("int"), asList("int"), true),
                                        varBinding("foo6()$x", "V2", asList("int"), asList("int"), true),
                                        varBinding("foo6()$y", "V3", asList("int"), asList("int"), true),
                                        varBinding("foo6()=@2|0", "V4", asList("int"), asList("int"), true),
                                        varBinding("foo6()$a", "V4", asList("int"), asList("int"), true),
                                        varBinding("return@4|0", "V6", asList("int"), asList("int"), true),
                                        varBinding("1@5|0", "V7", asList("int"), null, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), asList("int"), true)
                                ),
                                //float x float -> int
                                matcherDto(
                                        varBinding("foo6()+@3|0", "V1", asList("float"), asList("float"), true),
                                        varBinding("foo6()$x", "V2", asList("float"), asList("float"), true),
                                        varBinding("foo6()$y", "V3", asList("float"), asList("float"), true),
                                        varBinding("foo6()=@2|0", "V4", asList("float"), asList("float"), true),
                                        varBinding("foo6()$a", "V4", asList("float"), asList("float"), true),
                                        varBinding("return@4|0", "V6", asList("int"), asList("int"), true),
                                        varBinding("1@5|0", "V7", asList("int"), null, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), asList("int"), true)
                                ),
                                //float x {as num} -> int
                                matcherDto(
                                        varBinding("foo6()+@3|0", "V1", asList("float"), asList("float"), true),
                                        varBinding("foo6()$x", "V2", asList("float"), asList("float"), true),
                                        varBinding("foo6()$y", "V3", asNum, asNum, true),
                                        varBinding("foo6()=@2|0", "V4", asList("float"), asList("float"), true),
                                        varBinding("foo6()$a", "V4", asList("float"), asList("float"), true),
                                        varBinding("return@4|0", "V6", asList("int"), asList("int"), true),
                                        varBinding("1@5|0", "V7", asList("int"), null, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), asList("int"), true)
                                ),
                                //{as num} x float -> int
                                matcherDto(
                                        varBinding("foo6()+@3|0", "V1", asList("float"), asList("float"), true),
                                        varBinding("foo6()$x", "V2", asNum, asNum, true),
                                        varBinding("foo6()$y", "V3", asList("float"), asList("float"), true),
                                        varBinding("foo6()=@2|0", "V4", asList("float"), asList("float"), true),
                                        varBinding("foo6()$a", "V4", asList("float"), asList("float"), true),
                                        varBinding("return@4|0", "V6", asList("int"), asList("int"), true),
                                        varBinding("1@5|0", "V7", asList("int"), null, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), asList("int"), true)
                                ),
                                //{as num} x {as num} -> int
                                matcherDto(
                                        varBinding("foo6()+@3|0", "V1", numLower, numUpper, true),
                                        varBinding("foo6()$x", "V2", asNum, asNum, true),
                                        varBinding("foo6()$y", "V3", asNum, asNum, true),
                                        varBinding("foo6()=@2|0", "V4", numLower, numUpper, true),
                                        varBinding("foo6()$a", "V4", numLower, numUpper, true),
                                        varBinding("return@4|0", "V6", asList("int"), asList("int"), true),
                                        varBinding("1@5|0", "V7", asList("int"), null, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), asList("int"), true)
                                ),
                                //array x array -> int
                                matcherDto(
                                        varBinding("foo6()+@3|0", "V1", asList("array"), asList("array"), true),
                                        varBinding("foo6()$x", "V2", asList("array"), asList("array"), true),
                                        varBinding("foo6()$y", "V3", asList("array"), asList("array"), true),
                                        varBinding("foo6()=@2|0", "V4", asList("array"), asList("array"), true),
                                        varBinding("foo6()$a", "V4", asList("array"), asList("array"), true),
                                        varBinding("return@4|0", "V6", asList("int"), asList("int"), true),
                                        varBinding("1@5|0", "V7", asList("int"), null, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), asList("int"), true)
                                )
                        ), 1, 0, 2)
                },
                //TINS-420 constraint solving fall back to soft typing
                {
                        "function foo7(array $x){ $x \n= \n1; \nreturn $x \n+ \n1; }",
                        testStructs("foo7()", "\\.\\.", matcherDtos(
                                matcherDto(
                                        varBinding("foo7()$x", "V2",
                                                asList("int", "array"), asList("(array | int)"), true),
                                        varBinding("foo7()=@2|0", "V2",
                                                asList("int", "array"), asList("(array | int)"), true),
                                        varBinding("1@3|0", "V3", asList("int"), null, true),
                                        varBinding("foo7()+@5|0", "V4", asList("int"), asList("int"), true),
                                        //TODO TINS-537 - most specific overload in soft typing
                                        //varBinding("1@6|0", "V5", asList("int"), asList("int"), true),
                                        varBinding("1@6|0", "V5", asList("int"), asList("{as int}"), true),
                                        varBinding("return@4|0", "V6", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", asList("int"), asList("int"), true)
                                )
                        ), 1, 0, 2)
                },
                //TINS-420 constraint solving fall back to soft typing
                {
                        "function foo8(array $x){ $x \n= \n1; \nreturn $x \n+ \n1.2; }",
                        testStructs("foo8()", "\\.\\.", matcherDtos(
                                matcherDto(
                                        varBinding("foo8()$x", "V2",
                                                asList("int", "array"), asList("(array | int)"), true),
                                        varBinding("foo8()=@2|0", "V2",
                                                asList("int", "array"), asList("(array | int)"), true),
                                        varBinding("1@3|0", "V3", asList("int"), null, true),
                                        varBinding("foo8()+@5|0", "V4", numLower, numUpper, true),
                                        varBinding("1.2@6|0", "V5",
                                                asList("float"), asList("{as (float | int)}"), true),
                                        varBinding("return@4|0", "V6", numLower, numUpper, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V7", numLower, numUpper, true)
                                )
                        ), 1, 0, 2)
                },
                //fall back to soft typing - param has lower refs
                {
                        "function foo13(array $x){ $a \n= \n1; $x \n= $a; \nreturn $x \n+ \n1; }",
                        testStructs("foo13()", "\\.\\.", matcherDtos(
                                matcherDto(
                                        varBinding("foo13()$a", "V2", asList("int"), asList("int"), true),
                                        varBinding("foo13()=@2|0", "V2", asList("int"), asList("int"), true),
                                        varBinding("1@3|0", "V4", asList("int"), null, true),
                                        varBinding("foo13()$x", "V5",
                                                asList("int", "array"), asList("(array | int)"), true),
                                        varBinding("foo13()=@4|0", "V5",
                                                asList("int", "array"), asList("(array | int)"), true),
                                        varBinding("foo13()+@6|0", "V6", asList("int"), asList("int"), true),
                                        //TODO TINS-537 - most specific overload in soft typing
                                        //varBinding("1@7|0", "V7", asList("int"), asList("int"), true),
                                        varBinding("1@7|0", "V7", asList("int"), asList("{as int}"), true),
                                        varBinding("return@5|0", "V8", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V9", asList("int"), asList("int"), true)
                                )
                        ), 1, 0, 2)
                },
                //fall back to soft typing - param has lower refs with different upper bound
                {
                        "function foo14(array $x){ $a \n= \n1; $a \n+ \n1.2; $x \n= $a; \nreturn $x \n+ \n1; }",
                        testStructs("foo14()", "\\.\\.", matcherDtos(
                                matcherDto(
                                        varBinding("foo14()$a", "V2", asList("int"), asList("int"), true),
                                        varBinding("foo14()=@2|0", "V2", asList("int"), asList("int"), true),
                                        varBinding("1@3|0", "V4", asList("int"), null, true),
                                        varBinding("foo14()+@4|0", "V5", asList("float"), asList("float"), true),
                                        varBinding("1.2@5|0", "V6", asList("float"), asList("float"), true),
                                        varBinding("foo14()$x", "V7",
                                                asList("{as (float | int)}", "array"),
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding("foo14()=@6|0", "V7",
                                                asList("{as (float | int)}", "array"),
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding("foo14()+@8|0", "V8",
                                                asList("int", "float"), asList("(float | int)"), true),
                                        //TODO TINS-537 - most specific overload in soft typing
                                        //varBinding("1@6|0", "V5", asList("int"), asList("int"), true),
                                        varBinding("1@9|0", "V9", asList("int"), asList("{as (float | int)}"), true),
                                        varBinding("return@7|0", "V10",
                                                asList("int", "float"), asList("(float | int)"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V11",
                                                asList("int", "float"), asList("(float | int)"), true)
                                )
                        ), 1, 0, 2)
                },
                //fall back to soft typing - param cyclic ref
                {
                        "function foo15(array $x){ "
                                + "$a \n= \n1; $a \n+ \n1.2; $a \n= $x; $x \n= $a; \nreturn $x \n+ \n1; }",
                        testStructs("foo15()", "\\.\\.", matcherDtos(
                                matcherDto(
                                        varBinding("foo15()$a", "V7",
                                                asList("{as (float | int)}", "array"),
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding("foo15()=@2|0", "V7",
                                                asList("{as (float | int)}", "array"),
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding("1@3|0", "V4", asList("int"), null, true),
                                        varBinding("foo15()+@4|0", "V5", asList("float"), asList("float"), true),
                                        //TODO TINS-537 - most specific overload in soft typing
                                        //varBinding("1.2@5|0", "V6", asList("float"), asList("float"), true),
                                        varBinding("1.2@5|0", "V6",
                                                asList("float"), asList("{as (float | int)}"), true),
                                        varBinding("foo15()=@6|0", "V7",
                                                asList("{as (float | int)}", "array"),
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding("foo15()$x", "V8",
                                                asList("{as (float | int)}", "array"),
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding("foo15()=@7|0", "V8",
                                                asList("{as (float | int)}", "array"),
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding("foo15()+@9|0", "V9",
                                                asList("int", "float"), asList("(float | int)"), true),
                                        //TODO TINS-537 - most specific overload in soft typing
                                        //varBinding("1@6|0", "V5", asList("int"), asList("int"), true),
                                        varBinding("1@10|0", "V10", asList("int"), asList("{as (float | int)}"), true),
                                        varBinding("return@8|0", "V11",
                                                asList("int", "float"), asList("(float | int)"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V12",
                                                asList("int", "float"), asList("(float | int)"), true)
                                )
                        ), 1, 0, 2)
                },
                //TODO TINS-535 improve precision in soft typing for unconstrained parameters
//                {
//                        "function foo9(array $x, $y){ $x = $y; return $x \n+ 1; }",
//                        testStructs("foo9()", "\\.\\.", matcherDtos(
//                                matcherDto(
//                                        varBinding("foo9()+@2|0", "V1", asList("int"), asList("int"), true),
//                                        varBinding("foo9()$x", "V2", asList("int"), asList("int"), true),
//                                        varBinding("foo9()1@2|0", "V4", asList("int"), asList("int"), true),
//                                        varBinding("return@4|0", "V6", asList("int"), asList("int"), true),
//                                        varBinding(RETURN_VARIABLE_NAME, "V8", asList("int"), asList("int"), true)
//                                )
//                        ), 1, 0, 2)
//                },
                //TINS-420 constraint solving fall back to soft typing
                {
                        "function foo10(array $x, $y){ $x \n= $y; \nreturn $x \n+ $y \n+ \n1; }",
                        testStructs("foo10()", "\\.\\.", matcherDtos(
                                matcherDto(
                                        varBinding("foo10()$x", "V3",
                                                asList("{as (float | int)}", "array"),
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding("foo10()=@2|0", "V3",
                                                asList("{as (float | int)}", "array"),
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding("foo10()$y", "V2",
                                                asList("{as (float | int)}", "array"),
                                                asList("(array | {as (float | int)})"), true),
                                        varBinding("foo10()+@4|0", "V4",
                                                asList("float", "int", "array"),
                                                asList("(array | float | int)"), true),
                                        varBinding("foo10()+@5|0", "V5", numLower, numUpper, true),
                                        varBinding("1@6|0", "V6", asList("int"), asList("{as (float | int)}"), true),
                                        varBinding("return@3|0", "V7", numLower, numUpper, true),
                                        varBinding(RETURN_VARIABLE_NAME, "V8", numLower, numUpper, true)
                                )
                        ), 1, 0, 2)
                },
                //TINS-420 constraint solving fall back to soft typing
                {
                        "function foo11(array $x){ $x \n= \n1;  $a \n= 1; \nreturn $x \n+ $a; }",
                        testStructs("foo11()", "\\.\\.", matcherDtos(
                                matcherDto(
                                        varBinding("foo11()$x", "V2",
                                                asList("int", "array"), asList("(array | int)"), true),
                                        varBinding("foo11()=@2|0", "V2",
                                                asList("int", "array"), asList("(array | int)"), true),
                                        varBinding("foo11()$a", "V4", asList("int"), asList("int"), true),
                                        varBinding("1@3|0", "V3", asList("int"), null, true),
                                        varBinding("foo11()=@4|0", "V4", asList("int"), asList("int"), true),
                                        varBinding("1@4|2", "V6", asList("int"), null, true),
                                        varBinding("foo11()+@6|0", "V7", asList("int"), asList("int"), true),
                                        varBinding("return@5|0", "V8", asList("int"), asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "V9", asList("int"), asList("int"), true)
                                )
                        ), 1, 0, 2)
                },
        });
    }
}
