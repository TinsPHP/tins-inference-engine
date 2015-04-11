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

import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingMatcher.varBinding;
import static java.util.Arrays.asList;


@RunWith(Parameterized.class)
public class FunctionDeclarationTest extends AInferenceBindingTest
{

    public FunctionDeclarationTest(String testString, BindingTestStruct[] theTestStructs) {
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
                                varBinding("foo()$x", "T1", null, null),
                                varBinding("rtn", "T1", null, null))), 1, 0, 2)
                },
                {
                        "function foo(){return \nnull;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding("rtn", "T1", asList("null"), null),
                                varBinding("null@2|0", "T1", asList("null"), null))), 1, 0, 2)
                },
                {
                        "function foo($name){return \n'hello '\n.$name;}",
                        testStructs("foo()", "\\.\\.", matcherDtos(matcherDto(
                                varBinding("rtn", "T4", asList("string"), null),
                                varBinding("foo().@3|0", "T4", asList("string"), null),
                                varBinding("foo()$name", "T3", null, asList("string")),
                                varBinding("'hello '@2|0", "T2", asList("string"), asList("string")))), 1, 0, 2)
                },
        });
    }
}
