/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceTypeTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AbsoluteTypeNameTestStruct;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;


@RunWith(Parameterized.class)
public class ConstantDeclarationTest extends AInferenceTypeTest
{

    public ConstantDeclarationTest(String testString, AbsoluteTypeNameTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
//                {"const a = null;", testStructs("(a# null)", "\\.\\.", "null", 1, 0, 1)},
//                {"const a = false;", testStructs("(a# false)", "\\.\\.", "false", 1, 0, 1)},
//                {"const a = true;", testStructs("(a# true)", "\\.\\.", "true", 1, 0, 1)},
//                {"const a = 1;", testStructs("(a# 1)", "\\.\\.", "int", 1, 0, 1)},
//                {"const a = 1.4;", testStructs("(a# 1.4)", "\\.\\.", "float", 1, 0, 1)},
//                {"const a = 'h';", testStructs("(a# 'h')", "\\.\\.", "string", 1, 0, 1)},
                {
                        "const a = 1; const b = a;", new AbsoluteTypeNameTestStruct[]{
                        testStruct("(a# 1)", "\\.\\.", "int", 1, 0, 1),
                        testStruct("(b# a#)", "\\.\\.", "int", 1, 1, 1)}
                },
                //TODO rstoll TINS-344 seeding and constants
//                {
//                        "const a = 1, b = 'h';", new AbsoluteTypeNameTestStruct[]{
//                        testStruct("(a# 1)", "\\.\\.", "int", 1, 0, 1),
//                        testStruct("(b# a#)", "\\.\\.", "int", 1, 0, 2)}
//                },
//                {
//                        "const a = 1, b = a;", new AbsoluteTypeNameTestStruct[]{
//                        testStruct("(a# 1)", "\\.\\.", "int", 1, 0, 1),
//                        testStruct("(b# a#)", "\\.\\.", "int", 1, 0, 2)}
//                },
        });
    }
}
