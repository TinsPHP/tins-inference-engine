/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceEvalTypeScopeTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.TypeScopeTestStruct;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;


@RunWith(Parameterized.class)
public class ResolvePrimitiveLiteralTest extends AReferenceEvalTypeScopeTest
{


    public ResolvePrimitiveLiteralTest(String testString, TypeScopeTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                {"null;", typeStruct("null", "null", "", 1, 0, 0)},
                {"true;", typeStruct("true", "true", "", 1, 0, 0)},
                {"false;", typeStruct("false", "false", "", 1, 0, 0)},
                {"1;", typeStruct("1", "int", "", 1, 0, 0)},
                {"1.2;", typeStruct("1.2", "float", "", 1, 0, 0)},
                {"\"hello\";", typeStruct("\"hello\"", "string", "", 1, 0, 0)},
                {"'hi';", typeStruct("'hi'", "string", "", 1, 0, 0)},
                {"array(1,2);", typeStruct("(array 1 2)", "array", "", 1, 0, 0)},
                {"[1,2];", typeStruct("(array 1 2)", "array", "", 1, 0, 0)},
        });
    }
}
