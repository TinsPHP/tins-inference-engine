/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceNamespaceTypeTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AbsoluteTypeNameTestStruct;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static java.util.Arrays.asList;


@RunWith(Parameterized.class)
public class ConstantDefinitionTest extends AInferenceNamespaceTypeTest
{

    public ConstantDefinitionTest(String testString, AbsoluteTypeNameTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return asList(new Object[][]{
                {"const a = null;", testStructs("(a# null)", "\\.\\.", asList("nullType"), 1, 0, 1)},
                {"const a = false;", testStructs("(a# false)", "\\.\\.", asList("falseType"), 1, 0, 1)},
                {"const a = true;", testStructs("(a# true)", "\\.\\.", asList("trueType"), 1, 0, 1)},
                {"const a = 1;", testStructs("(a# 1)", "\\.\\.", asList("int"), 1, 0, 1)},
                {"const a = 1.4;", testStructs("(a# 1.4)", "\\.\\.", asList("float"), 1, 0, 1)},
                {"const a = 'h';", testStructs("(a# 'h')", "\\.\\.", asList("string"), 1, 0, 1)},
                {
                        "const a = 1; const b = a;", new AbsoluteTypeNameTestStruct[]{
                        testStruct("(a# 1)", "\\.\\.", asList("int"), 1, 0, 1),
                        testStruct("(b# a#)", "\\.\\.", asList("int"), 1, 1, 1)}
                },
                {"const a = +false;", testStructs("(a# (uPlus false))", "\\.\\.", asList("falseType"), 1, 0, 1)},
                {"const a = +true;", testStructs("(a# (uPlus true))", "\\.\\.", asList("trueType"), 1, 0, 1)},
                {"const a = +1;", testStructs("(a# (uPlus 1))", "\\.\\.", asList("int"), 1, 0, 1)},
                {"const a = +1.5;", testStructs("(a# (uPlus 1.5))", "\\.\\.", asList("float"), 1, 0, 1)},
                {"const a = -false;", testStructs("(a# (uMinus false))", "\\.\\.", asList("falseType"), 1, 0, 1)},
                {"const a = -true;", testStructs("(a# (uMinus true))", "\\.\\.", asList("trueType"), 1, 0, 1)},
                {"const a = -1;", testStructs("(a# (uMinus 1))", "\\.\\.", asList("int"), 1, 0, 1)},
                {"const a = -1.5;", testStructs("(a# (uMinus 1.5))", "\\.\\.", asList("float"), 1, 0, 1)},
                {
                        "const a = 1, b = 'hi';", new AbsoluteTypeNameTestStruct[]{
                        testStruct("(a# 1)", "\\.\\.", asList("int"), 1, 0, 1),
                        testStruct("(b# 'hi')", "\\.\\.", asList("string"), 1, 0, 2)}
                },
                {
                        "const a = 1, b = a;", new AbsoluteTypeNameTestStruct[]{
                        testStruct("(a# 1)", "\\.\\.", asList("int"), 1, 0, 1),
                        testStruct("(b# a#)", "\\.\\.", asList("int"), 1, 0, 2)}
                },
        });
    }
}
