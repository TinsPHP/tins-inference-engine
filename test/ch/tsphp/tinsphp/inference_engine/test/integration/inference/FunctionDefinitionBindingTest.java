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

import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.OverloadBindingsMatcher.varBinding;
import static ch.tsphp.tinsphp.symbols.TypeVariableNames.RETURN_VARIABLE_NAME;
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
        return asList(new Object[][]{
                {
                        "function foo($x){return $x;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding("foo()$x", "T2", null, null, false),
                                varBinding(RETURN_VARIABLE_NAME, "T2", null, null, false))), 1, 0, 2)
                },
                {
                        "function foo($name){return \n'hello '\n.$name;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                        varBinding("foo().@3|0", "T1", asList("string"), asList("string"), true),
                                        //is constant as well since there is no parametric overload of . operator
                                        varBinding("'hello '@2|0", "T2", asList("string"), asList("string"), true),
                                        varBinding("foo()$name", "T3", asList("string"), asList("string"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T4", asList("string"), asList("string"),
                                                true))),
                                1, 0, 2)
                },
                {
                        "function foo(){return \n1 \n+ 1 \n+ 1 \n+ 1 \n+ 1 \n+ 1;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding("1@2|0", "T10", asList("int"), asList("int"), true),
                                varBinding("foo()+@3|0", "T10", asList("int"), asList("int"), true),
                                varBinding("1@3|2", "T10", asList("int"), asList("int"), true),
                                varBinding("foo()+@4|0", "T10", asList("int"), asList("int"), true),
                                varBinding("1@4|2", "T10", asList("int"), asList("int"), true),
                                varBinding("foo()+@5|0", "T10", asList("int"), asList("int"), true),
                                varBinding("1@5|2", "T10", asList("int"), asList("int"), true),
                                varBinding("foo()+@6|0", "T10", asList("int"), asList("int"), true),
                                varBinding("1@6|2", "T10", asList("int"), asList("int"), true),
                                varBinding("foo()+@7|0", "T10", asList("int"), asList("int"), true),
                                varBinding("1@7|2", "T10", asList("int"), asList("int"), true),
                                varBinding(RETURN_VARIABLE_NAME, "T12", asList("int"), asList("int"), true))), 1, 0, 2)
                },
                {
                        "function foo(){ $a \n= \n1; $b \n= \n2; return \n3;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding("foo()=@2|0", "T1", asList("int"), asList("int"), true),
                                varBinding("foo()$a", "T1", asList("int"), asList("int"), true),
                                varBinding("1@3|0", "T3", asList("int"), null, true),
                                varBinding("foo()=@4|0", "T4", asList("int"), asList("int"), true),
                                varBinding("foo()$b", "T4", asList("int"), asList("int"), true),
                                varBinding("2@5|0", "T6", asList("int"), null, true),
                                varBinding(RETURN_VARIABLE_NAME, "T7", asList("int"), asList("int"), true),
                                varBinding("3@6|0", "T8", asList("int"), null, true))), 1, 0, 2)
                },
                //see TINS-405 multiple return cause NullPointerException
                {
                        "function foo(){return \n1; return \n1.2;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding(RETURN_VARIABLE_NAME,
                                        "T1", asList("int", "float"), asList("(int | float)"), true),
                                varBinding("1@2|0", "T2", asList("int"), null, true),
                                varBinding("1.2@3|0", "T3", asList("float"), null, true))), 1, 0, 2)
                },
                {
                        "function foo($x){ \nif($x){ return \n1;} return \n1.2;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding(RETURN_VARIABLE_NAME,
                                        "T1", asList("int", "float"), asList("(int | float)"), true),
                                varBinding("1@3|0", "T2", asList("int"), null, true),
                                varBinding("if@2|0", "T3", asList("mixed"), asList("mixed"), true),
                                varBinding("foo()$x", "T4", asList("bool"), asList("bool"), true),
                                varBinding("1.2@4|0", "T5", asList("float"), null, true))), 1, 0, 2)
                },
                {
                        "function foo($x){ \nif($x){ return \n1;} else { return \n1.2;} }",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding(RETURN_VARIABLE_NAME,
                                        "T1", asList("int", "float"), asList("(int | float)"), true),
                                varBinding("1@3|0", "T2", asList("int"), null, true),
                                varBinding("1.2@4|0", "T3", asList("float"), null, true),
                                varBinding("if@2|0", "T4", asList("mixed"), asList("mixed"), true),
                                varBinding("foo()$x", "T5", asList("bool"), asList("bool"), true))), 1, 0, 2)
                },
        });
    }
}
