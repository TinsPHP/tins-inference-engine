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
        return asList(new Object[][]{
                {
                        "function foo($x){return $x + 1;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T1", asList("int"), asList("num"), false),
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("int"), asList("num"), false)))
                                , 1, 0, 2)
                },
                {
                        "function foo($x){return $x + 1.5;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T1", asList("float"), asList("num"), false),
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("float"), asList("num"), false)))
                                , 1, 0, 2)
                },
                {
                        "function foo($x){return $x + (1 + 1.5);}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T4", asList("int", "float"), asList("(int | float)"), false),
                                varBinding(RETURN_VARIABLE_NAME,
                                        "T4", asList("int", "float"), asList("(int | float)"), false)))
                                , 1, 0, 2)
                },
                {
                        "function foo($x){return $x + [];}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T2", asList("array"), asList("array"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T4", asList("array"), asList("array"), true)))
                                , 1, 0, 2)
                },
                {
                        "function foo($x){return $x + true;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T2", asList("bool"), asList("bool"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T4", asList("int"), asList("int"), true)))
                                , 1, 0, 2)
                },
                {
                        "function foo($x){return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T2", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T2", null, null, false))), 1, 0, 2)
                },
                {
                        "function foo(){return null;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 0, bindingDtos(
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("null"), asList("null"), true))), 1, 0, 2)
                },
                //notice $x and $y are not used within the function body
                {
                        "function foo($x, $y){return null;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "T3", asList("mixed"), asList("mixed"), true),
                                varBinding("foo()$y", "T4", asList("mixed"), asList("mixed"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("null"), asList("null"), true))), 1, 0, 2)
                },
                {
                        "function foo($x, $y){ $x = $y; return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "T1", asList("@T3"), null, false),
                                varBinding("foo()$y", "T3", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("@T3"), null, false))), 1, 0, 2)
                },
                //additional indirection $y < $a < $x < rtn in contrast to previous
                {
                        "function foo($x, $y){ $a = $y; $x = $a; return $x;} /* $y < $a < $x < rtn */",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "T4", asList("@T3"), null, false),
                                varBinding("foo()$y", "T3", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T4", asList("@T3"), null, false))), 1, 0, 2)
                },
                //additional indirection $y < $a < $x < $b < rtn in contrast to previous
                {
                        "function foo($x, $y){ $a = $y; $x = $a; $b = $x; return $b;} /*$y < $a < $x < $b < rtn*/",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "T4", asList("@T3"), null, false),
                                varBinding("foo()$y", "T3", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T4", asList("@T3"), null, false))), 1, 0, 2)
                },
                //same as before but return is constant
                {
                        "function foo($x, $y){ $a = $y; $x = $a; $b = $x; return 1;} /* constant return */",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                //should have mixed as lower instead of bottom type
                                varBinding("foo()$x", "T4", asList("mixed"), asList("mixed"), true),
                                varBinding("foo()$y", "T3", asList("mixed"), asList("mixed"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T8", asList("int"), asList("int"), true))), 1, 0, 2)
                },
                //return is variable again but $x has additionally an upper bound
                {
                        "function foo($x, $y){ $x + 1; $a = $y; $x = $a; $b = $x; return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                varBinding("foo()$x", "T7", asList("@T6", "int"), asList("num"), false),
                                varBinding("foo()$y", "T6", null, asList("num"), false),
                                varBinding(RETURN_VARIABLE_NAME, "T7", asList("@T6", "int"), asList("num"), false)))
                                , 1, 0, 2)
                },
                //$x has no upper bound anymore but $a has an additional lower type bound
                {
                        "function foo($x, $y){ $a = 1; $a = $y; $x = $a; $b = $x; return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T6", asList("@T5", "int"), null, false),
                                        varBinding("foo()$y", "T5", null, null, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T6", asList("@T5", "int"), null, false))),
                                1, 0, 2)
                },
                //as before but $y has the lower type bound instead of $a
                {
                        "function foo($x, $y){ $y = 1; $a = $y; $x = $a; $b = $x; return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 2, bindingDtos(
                                        varBinding("foo()$x", "T6", asList("@T1", "int"), null, false),
                                        varBinding("foo()$y", "T1", asList("int"), null, false),
                                        varBinding(RETURN_VARIABLE_NAME, "T6", asList("@T1", "int"), null, false))),
                                1, 0, 2)
                },
                //$x is overwritten before it is used
                {
                        "function foo($x){ $x = null; return $x;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T1", asList("null"), null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T1", asList("null"), null, false))), 1, 0, 2)
                },
                //constant function but with indirection
                //see also TINS-386 - function with constant return via indirection
                {
                        "function foo(){ $a = null; return \n$a;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 0, bindingDtos(
                                varBinding(RETURN_VARIABLE_NAME, "T4", asList("null"), asList("null"), true))), 1, 0, 2)
                },
                //same as before but with unused parameter
                {
                        "function foo($x){ $a = null; return \n$a;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T5", asList("mixed"), asList("mixed"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T4", asList("null"), asList("null"), true))), 1, 0, 2)
                },
                //$x with useless statement. Second one is constant but $x has no lower bound
                //see also TINS-384 most specific overload and variable parameter
                {
                        "function foo($x){ $x + true; $x + true; return \n1;}",
                        testStructs("foo()", "\\.\\.", functionDtos("foo()", 1, bindingDtos(
                                varBinding("foo()$x", "T2", asList("bool"), asList("bool"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T6", asList("int"), asList("int"), true))), 1, 0, 2)
                }
        });
    }
}