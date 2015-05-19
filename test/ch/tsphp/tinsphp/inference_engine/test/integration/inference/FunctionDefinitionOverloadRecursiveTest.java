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
        List<String> numLower = asList("float", "int");
        List<String> numUpper = asList("(float | int)");
        return asList(new Object[][]{
                //direct recursive functions
                {
                        "function fib($n){ return $n > 1 ? fib($n - 1) + fib($n - 2) : 1;}",
                        testStructs("fib()", "\\.\\.", functionDtos("fib()", 1, bindingDtos(
                                varBinding("fib()$n", "T8", numLower, numUpper, true),
                                varBinding(RETURN_VARIABLE_NAME, "T7", asList("int"), asList("int"), true)
                        )), 1, 0, 2)
                },
                {
                        "function spaces($n){ if($n > 0){ return ' '.spaces($n-1);} return '';}",
                        testStructs("spaces()", "\\.\\.", functionDtos("spaces()", 1, bindingDtos(
                                varBinding("spaces()$n", "T4", numLower, numUpper, true),
                                varBinding(RETURN_VARIABLE_NAME, "T7", asList("string"), asList("string"), true)
                        )), 1, 0, 2)
                },
                {
                        "function fac($n){ return $n > 0 ? $n * fac($n-1) : $n;}",
                        testStructs("fac()", "\\.\\.", functionDtos("fac()", 1, bindingDtos(
                                varBinding("fac()$n", "T8", asList("int"), numUpper, false),
                                varBinding(RETURN_VARIABLE_NAME, "T8", asList("int"), numUpper, false)
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
                                varBinding("fac2()$x", "T8", asList("int"), numUpper, false),
                                varBinding("fac2()$y", "T8", asList("int"), numUpper, false),
                                varBinding(RETURN_VARIABLE_NAME, "T8", asList("int"), numUpper, false)
                        )), 1, 0, 2)
                },
                //indirect recursive functions
                {
                        "function foo($x){ if($x > 0){return bar($x-1);} return $x;}"
                                + "function bar($x){ if($x > 0){return foo($x-1);} return $x;}",
                        new OverloadTestStruct[]{
                                testStruct("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                        varBinding("foo()$x", "T4", asList("int"), numUpper, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("int"), numUpper, false)
                                )), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos("bar()", 1, bindingDtos(
                                        varBinding("bar()$x", "T4", asList("int"), numUpper, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("int"), numUpper, false)
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
                                        varBinding("foo()$x", "T4", asList("int"), numUpper, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("int"), numUpper, false)
                                )), 1, 0, 2),
                                testStruct("bar()", "\\.\\.", functionDtos("bar()", 1, bindingDtos(
                                        varBinding("bar()$x", "T4", asList("int"), numUpper, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("int"), numUpper, false)
                                )), 1, 1, 2)
                        }
                },

        });
    }
}
