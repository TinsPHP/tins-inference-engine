/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.inference_engine.resolver.PutAtTopVariableDeclarationCreator;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceNamespaceTypeTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AbsoluteTypeNameTestStruct;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;

@RunWith(Parameterized.class)
public class OperatorUpToGreaterThanTest extends AInferenceNamespaceTypeTest
{

    public OperatorUpToGreaterThanTest(String testString, AbsoluteTypeNameTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    protected IVariableDeclarationCreator createVariableDeclarationCreator(
            ISymbolFactory theSymbolFactory,
            IAstModificationHelper theAstModificationHelper,
            IDefinitionPhaseController theDefinitionPhaseController) {
        return new PutAtTopVariableDeclarationCreator(
                theSymbolFactory, theAstModificationHelper, theDefinitionPhaseController);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<String> bool = asList("falseType", "trueType");
        List<String> num = asList("int", "float");
        return asList(new Object[][]{
                // or
                {"false or false;", testStructs("(or false false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"true or true;", testStructs("(or true true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"$x = (bool) true; true or $x;", testStructs("(or true $x)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"true or 0;", testStructs("(or true 0)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"$x = (bool) true; $x or true;", testStructs("(or $x true)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"0 or true;", testStructs("(or 0 true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"$x = (bool) true; $x or $x;", testStructs("(or $x $x)", "\\.\\.", bool, 1, 2, 0)},
                {"0 or 0;", testStructs("(or 0 0)", "\\.\\.", bool, 1, 0, 0)},
                // xor
                {"false xor true;", testStructs("(xor false true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"true xor false;", testStructs("(xor true false)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"false xor false;", testStructs("(xor false false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"true xor true;", testStructs("(xor true true)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"$x = (bool) true; $x xor $x;", testStructs("(xor $x $x)", "\\.\\.", bool, 1, 2, 0)},
                {"0 xor 0;", testStructs("(xor 0 0)", "\\.\\.", bool, 1, 0, 0)},
                // and
                {"false && false;", testStructs("(&& false false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {
                        "$x = (bool) true; false and $x;",
                        testStructs("(and false $x)", "\\.\\.", asList("falseType"), 1, 2, 0)
                },
                {"false and 0;", testStructs("(and false 0)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {
                        "$x = (bool) true; $x and false;",
                        testStructs("(and $x false)", "\\.\\.", asList("falseType"), 1, 2, 0)
                },
                {"0 and false;", testStructs("(and 0 false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"true and true;", testStructs("(and true true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"$x = (bool) true; $x and $x;", testStructs("(and $x $x)", "\\.\\.", bool, 1, 2, 0)},
                {"0 and 0;", testStructs("(and 0 0)", "\\.\\.", bool, 1, 0, 0)},
                // +=
                {"$x = 1; $x += 1;", testStructs("(+= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1.3; $x += 1.3;", testStructs("(+= $x 1.3)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = 1.2; $x += 1;", testStructs("(+= $x 1)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = 1; $x += 1.2;", testStructs("(+= $x 1.2)", "\\.\\.", num, 1, 2, 0)},
                {"$x = 1.2; $x += '1';", testStructs("(+= $x '1')", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = '1'; $x += 1.2;", testStructs("(+= $x 1.2)", "\\.\\.", asList("float", "string"), 1, 2, 0)},
                {
                        "$x = '1'; $x += '1.2';",
                        testStructs("(+= $x '1.2')", "\\.\\.", asList("float", "int", "string"), 1, 2, 0)
                },
                {"$x = (bool) 1 ? 1 : 1.5; $x += $x;", testStructs("(+= $x $x)", "\\.\\.", num, 1, 2, 0)},
                {"$x = []; $x += [1];", testStructs("(+= $x (array 1))", "\\.\\.", asList("array"), 1, 2, 0)},
                // -=
                {"$x = 1; $x -= 1;", testStructs("(-= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1.3; $x -= 1.3;", testStructs("(-= $x 1.3)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = 1.2; $x -= 1;", testStructs("(-= $x 1)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = 1; $x -= 1.2;", testStructs("(-= $x 1.2)", "\\.\\.", num, 1, 2, 0)},
                {"$x = 1.2; $x -= '1';", testStructs("(-= $x '1')", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = '1'; $x -= 1.2;", testStructs("(-= $x 1.2)", "\\.\\.", asList("float", "string"), 1, 2, 0)},
                {
                        "$x = '1'; $x -= '1.2';",
                        testStructs("(-= $x '1.2')", "\\.\\.", asList("float", "int", "string"), 1, 2, 0)
                },
                {"$x = (bool) 1 ? 1 : 1.5; $x -= $x;", testStructs("(-= $x $x)", "\\.\\.", num, 1, 2, 0)},
                // *=
                {"$x = 1; $x *= 1;", testStructs("(*= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1.3; $x *= 1.3;", testStructs("(*= $x 1.3)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = 1.2; $x *= 1;", testStructs("(*= $x 1)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = 1; $x *= 1.2;", testStructs("(*= $x 1.2)", "\\.\\.", num, 1, 2, 0)},
                {"$x = 1.2; $x *= '1';", testStructs("(*= $x '1')", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = '1'; $x *= 1.2;", testStructs("(*= $x 1.2)", "\\.\\.", asList("float", "string"), 1, 2, 0)},
                {
                        "$x = '1'; $x *= '1.2';",
                        testStructs("(*= $x '1.2')", "\\.\\.", asList("float", "int", "string"), 1, 2, 0)
                },
                {"$x = (bool) 1 ? 1 : 1.5; $x *= $x;", testStructs("(*= $x $x)", "\\.\\.", num, 1, 2, 0)},
                // /=
                {"$x = 1.3; $x /= 1.3;", testStructs("(/= $x 1.3)", "\\.\\.", asList("falseType", "float"), 1, 2, 0)},
                {
                        "$x = 1 ? 1.5 : false; $x /= 1.3;",
                        testStructs("(/= $x 1.3)", "\\.\\.", asList("falseType", "float"), 1, 2, 0)
                },
                {
                        "$x = 1; $x /= 1.2;",
                        testStructs("(/= $x 1.2)", "\\.\\.", asList("falseType", "float", "int"), 1, 2, 0)
                },
                {
                        "$x = '1'; $x /= 1.2;",
                        testStructs("(/= $x 1.2)", "\\.\\.", asList("falseType", "float", "string"), 1, 2, 0)
                },
                {
                        "$x = true; $x /= 1.2;",
                        testStructs("(/= $x 1.2)", "\\.\\.", asList("falseType", "float", "trueType"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x /= true;",
                        testStructs("(/= $x true)", "\\.\\.", asList("falseType", "float", "int"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x /= 1;",
                        testStructs("(/= $x 1)", "\\.\\.", asList("falseType", "float", "int"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x /= '1';",
                        testStructs("(/= $x '1')", "\\.\\.", asList("falseType", "float", "int"), 1, 2, 0)
                },
                {
                        "$x = 1; $x /= 1;",
                        testStructs("(/= $x 1)", "\\.\\.", asList("falseType", "float", "int"), 1, 2, 0)
                },
                {
                        "$x = '1'; $x /= '1.2';",
                        testStructs("(/= $x '1.2')", "\\.\\.", asList("falseType", "float", "int", "string"), 1, 2, 0)
                },
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x /= $x;",
                        testStructs("(/= $x $x)", "\\.\\.", asList("falseType", "float", "int"), 1, 2, 0)
                },
                // %=
                {"$x = 1; $x %= 1;", testStructs("(%= $x 1)", "\\.\\.", asList("int", "falseType"), 1, 2, 0)},
                {"$x = false; $x %= 1;", testStructs("(%= $x 1)", "\\.\\.", asList("int", "falseType"), 1, 2, 0)},
                {
                        "$x = 1 ? 1 : false; $x %= 1;",
                        testStructs("(%= $x 1)", "\\.\\.", asList("int", "falseType"), 1, 2, 0)
                },
                {"$x = 1; $x %= '1';", testStructs("(%= $x '1')", "\\.\\.", asList("int", "falseType"), 1, 2, 0)},
                {"$x = 1; $x %= true;", testStructs("(%= $x true)", "\\.\\.", asList("int", "falseType"), 1, 2, 0)},
                {"$x = 1; $x %= 1.2;", testStructs("(%= $x 1.2)", "\\.\\.", asList("int", "falseType"), 1, 2, 0)},
                {
                        "$x = true; $x %= 1;",
                        testStructs("(%= $x 1)", "\\.\\.", asList("int", "falseType", "trueType"), 1, 2, 0)
                },
                {
                        "$x = true; $x %= true;",
                        testStructs("(%= $x true)", "\\.\\.", asList("int", "falseType", "trueType"), 1, 2, 0)
                },
                {
                        "$x = '1'; $x %= 1;",
                        testStructs("(%= $x 1)", "\\.\\.", asList("int", "falseType", "string"), 1, 2, 0)
                },
                {
                        "$x = '1'; $x %= '1';",
                        testStructs("(%= $x '1')", "\\.\\.", asList("int", "falseType", "string"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x %= 1;",
                        testStructs("(%= $x 1)", "\\.\\.", asList("int", "falseType", "float"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x %= 1.5;",
                        testStructs("(%= $x 1.5)", "\\.\\.", asList("int", "falseType", "float"), 1, 2, 0)
                },
                {
                        "$x = 1 ? 1: 1.5; $x %= $x;",
                        testStructs("(%= $x $x)", "\\.\\.", asList("int", "falseType", "float"), 1, 2, 0)
                },
                // |=
                {"$x = 1; $x |= 1;", testStructs("(|= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 'a'; $x |= 'b';", testStructs("(|= $x 'b')", "\\.\\.", asList("string"), 1, 2, 0)},
                {"$x = 1; $x |= '1';", testStructs("(|= $x '1')", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x |= true;", testStructs("(|= $x true)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x |= 1.2;", testStructs("(|= $x 1.2)", "\\.\\.", asList("int"), 1, 2, 0)},
                {
                        "$x = true; $x |= 1;",
                        testStructs("(|= $x 1)", "\\.\\.", asList("int", "trueType"), 1, 2, 0)
                },
                {
                        "$x = true; $x |= true;",
                        testStructs("(|= $x true)", "\\.\\.", asList("int", "trueType"), 1, 2, 0)
                },
                {
                        "$x = '1'; $x |= 1;",
                        testStructs("(|= $x 1)", "\\.\\.", asList("int", "string"), 1, 2, 0)
                },
                //also numeric strings result in a string
                {
                        "$x = '1'; $x |= '1';",
                        testStructs("(|= $x '1')", "\\.\\.", asList("string"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x |= 1;",
                        testStructs("(|= $x 1)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x |= 1.5;",
                        testStructs("(|= $x 1.5)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                {
                        "$x = 1 ? 1: 1.5; $x |= $x;",
                        testStructs("(|= $x $x)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                // &=
                {"$x = 1; $x &= 1;", testStructs("(&= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 'a'; $x &= 'b';", testStructs("(&= $x 'b')", "\\.\\.", asList("string"), 1, 2, 0)},
                {"$x = 1; $x &= '1';", testStructs("(&= $x '1')", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x &= true;", testStructs("(&= $x true)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x &= 1.2;", testStructs("(&= $x 1.2)", "\\.\\.", asList("int"), 1, 2, 0)},
                {
                        "$x = true; $x &= 1;",
                        testStructs("(&= $x 1)", "\\.\\.", asList("int", "trueType"), 1, 2, 0)
                },
                {
                        "$x = true; $x &= true;",
                        testStructs("(&= $x true)", "\\.\\.", asList("int", "trueType"), 1, 2, 0)
                },
                {
                        "$x = '1'; $x &= 1;",
                        testStructs("(&= $x 1)", "\\.\\.", asList("int", "string"), 1, 2, 0)
                },
                //also numeric strings result in a string
                {
                        "$x = '1'; $x &= '1';",
                        testStructs("(&= $x '1')", "\\.\\.", asList("string"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x &= 1;",
                        testStructs("(&= $x 1)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x &= 1.5;",
                        testStructs("(&= $x 1.5)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                {
                        "$x = 1 ? 1: 1.5; $x &= $x;",
                        testStructs("(&= $x $x)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                // ^=
                {"$x = 1; $x ^= 1;", testStructs("(^= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 'a'; $x ^= 'b';", testStructs("(^= $x 'b')", "\\.\\.", asList("string"), 1, 2, 0)},
                {"$x = 1; $x ^= '1';", testStructs("(^= $x '1')", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x ^= true;", testStructs("(^= $x true)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x ^= 1.2;", testStructs("(^= $x 1.2)", "\\.\\.", asList("int"), 1, 2, 0)},
                {
                        "$x = true; $x ^= 1;",
                        testStructs("(^= $x 1)", "\\.\\.", asList("int", "trueType"), 1, 2, 0)
                },
                {
                        "$x = true; $x ^= true;",
                        testStructs("(^= $x true)", "\\.\\.", asList("int", "trueType"), 1, 2, 0)
                },
                {
                        "$x = '1'; $x ^= 1;",
                        testStructs("(^= $x 1)", "\\.\\.", asList("int", "string"), 1, 2, 0)
                },
                //also numeric strings result in a string
                {
                        "$x = '1'; $x ^= '1';",
                        testStructs("(^= $x '1')", "\\.\\.", asList("string"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x ^= 1;",
                        testStructs("(^= $x 1)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x ^= 1.5;",
                        testStructs("(^= $x 1.5)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                {
                        "$x = 1 ? 1: 1.5; $x ^= $x;",
                        testStructs("(^= $x $x)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                // <<=
                {"$x = 1; $x <<= 1;", testStructs("(<<= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x <<= '1';", testStructs("(<<= $x '1')", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x <<= true;", testStructs("(<<= $x true)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x <<= 1.2;", testStructs("(<<= $x 1.2)", "\\.\\.", asList("int"), 1, 2, 0)},
                {
                        "$x = true; $x <<= 1;",
                        testStructs("(<<= $x 1)", "\\.\\.", asList("int", "trueType"), 1, 2, 0)
                },
                {
                        "$x = true; $x <<= true;",
                        testStructs("(<<= $x true)", "\\.\\.", asList("int", "trueType"), 1, 2, 0)
                },
                {
                        "$x = '1'; $x <<= 1;",
                        testStructs("(<<= $x 1)", "\\.\\.", asList("int", "string"), 1, 2, 0)
                },
                //no special string overload for shift operators
                {
                        "$x = '1'; $x <<= '1';",
                        testStructs("(<<= $x '1')", "\\.\\.", asList("int", "string"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x <<= 1;",
                        testStructs("(<<= $x 1)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x <<= 1.5;",
                        testStructs("(<<= $x 1.5)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                {
                        "$x = 1 ? 1: 1.5; $x <<= $x;",
                        testStructs("(<<= $x $x)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                // >>=
                {"$x = 1; $x >>= 1;", testStructs("(>>= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x >>= '1';", testStructs("(>>= $x '1')", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x >>= true;", testStructs("(>>= $x true)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1; $x >>= 1.2;", testStructs("(>>= $x 1.2)", "\\.\\.", asList("int"), 1, 2, 0)},
                {
                        "$x = true; $x >>= 1;",
                        testStructs("(>>= $x 1)", "\\.\\.", asList("int", "trueType"), 1, 2, 0)
                },
                {
                        "$x = true; $x >>= true;",
                        testStructs("(>>= $x true)", "\\.\\.", asList("int", "trueType"), 1, 2, 0)
                },
                {
                        "$x = '1'; $x >>= 1;",
                        testStructs("(>>= $x 1)", "\\.\\.", asList("int", "string"), 1, 2, 0)
                },
                //no special string overload for shift operators
                {
                        "$x = '1'; $x >>= '1';",
                        testStructs("(>>= $x '1')", "\\.\\.", asList("int", "string"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x >>= 1;",
                        testStructs("(>>= $x 1)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                {
                        "$x = 1.2; $x >>= 1.5;",
                        testStructs("(>>= $x 1.5)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                {
                        "$x = 1 ? 1: 1.5; $x >>= $x;",
                        testStructs("(>>= $x $x)", "\\.\\.", asList("int", "float"), 1, 2, 0)
                },
                // .=
                {"$x = 'a'; $x .= 'b';", testStructs("(.= $x 'b')", "\\.\\.", asList("string"), 1, 2, 0)},
                {"$x = 'a'; $x .= 1;", testStructs("(.= $x 1)", "\\.\\.", asList("string"), 1, 2, 0)},
                {"$x = 'a'; $x .= 1.3;", testStructs("(.= $x 1.3)", "\\.\\.", asList("string"), 1, 2, 0)},
                {"$x = 'a'; $x .= false;", testStructs("(.= $x false)", "\\.\\.", asList("string"), 1, 2, 0)},
                {"$x = 1; $x .= 'b';", testStructs("(.= $x 'b')", "\\.\\.", asList("string", "int"), 1, 2, 0)},
                {"$x = 1.2; $x .= 'b';", testStructs("(.= $x 'b')", "\\.\\.", asList("string", "float"), 1, 2, 0)},
                {"$x = true; $x .= 'b';", testStructs("(.= $x 'b')", "\\.\\.", asList("string", "trueType"), 1, 2, 0)},
                // ?
                {"true ? 1 : 1.3;", testStructs("(? true 1 1.3)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"false ? 1 : 1.3;", testStructs("(? false 1 1.3)", "\\.\\.", asList("float"), 1, 0, 0)},
                {"$x = (bool) 1; $x ? 1 : 1.3;", testStructs("(? $x 1 1.3)", "\\.\\.", num, 1, 2, 0)},
                {
                        "$x = (bool) 1; $x ? [1] : 1.3;",
                        testStructs("(? $x (array 1) 1.3)", "\\.\\.", asList("float", "array"), 1, 2, 0)
                },
                {"1 ? 1 : 1.3;", testStructs("(? 1 1 1.3)", "\\.\\.", num, 1, 0, 0)},
                // ||
                {"false || false;", testStructs("(|| false false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"true || true;", testStructs("(|| true true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"$x = (bool) true; true || $x;", testStructs("(|| true $x)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"true || 0;", testStructs("(|| true 0)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"$x = (bool) true; $x || true;", testStructs("(|| $x true)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"0 || true;", testStructs("(|| 0 true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"$x = (bool) true; $x || $x;", testStructs("(|| $x $x)", "\\.\\.", bool, 1, 2, 0)},
                {"0 || 0;", testStructs("(|| 0 0)", "\\.\\.", bool, 1, 0, 0)},
                // &&
                {"false && false;", testStructs("(&& false false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {
                        "$x = (bool) true; false && $x;",
                        testStructs("(&& false $x)", "\\.\\.", asList("falseType"), 1, 2, 0)
                },
                {"false && 0;", testStructs("(&& false 0)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {
                        "$x = (bool) true; $x && false;",
                        testStructs("(&& $x false)", "\\.\\.", asList("falseType"), 1, 2, 0)
                },
                {"0 && false;", testStructs("(&& 0 false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"true && true;", testStructs("(&& true true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"$x = (bool) true; $x && $x;", testStructs("(&& $x $x)", "\\.\\.", bool, 1, 2, 0)},
                {"0 && 0;", testStructs("(&& 0 0)", "\\.\\.", bool, 1, 0, 0)},
                // |
                {"2 | 1;", testStructs("(| 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"'a' | 'b';", testStructs("(| 'a' 'b')", "\\.\\.", asList("string"), 1, 0, 0)},
                {"2 | 1.2;", testStructs("(| 2 1.2)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2.5 | 1;", testStructs("(| 2.5 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2.5 | 1.5;", testStructs("(| 2.5 1.5)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"'2.5' | 1;", testStructs("(| '2.5' 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2 | '1';", testStructs("(| 2 '1')", "\\.\\.", asList("int"), 1, 0, 0)},
                {"$x = 1 ? 1 : 1.5; $x | $x;", testStructs("(| $x $x)", "\\.\\.", asList("int"), 1, 2, 0)},
                // &
                {"2 & 1;", testStructs("(& 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"'a' & 'b';", testStructs("(& 'a' 'b')", "\\.\\.", asList("string"), 1, 0, 0)},
                {"2 & 1.2;", testStructs("(& 2 1.2)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2.5 & 1;", testStructs("(& 2.5 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2.5 & 1.5;", testStructs("(& 2.5 1.5)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"'2.5' & 1;", testStructs("(& '2.5' 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2 & '1';", testStructs("(& 2 '1')", "\\.\\.", asList("int"), 1, 0, 0)},
                {"$x = 1 ? 1 : 1.5; $x & $x;", testStructs("(& $x $x)", "\\.\\.", asList("int"), 1, 2, 0)},
                // ^
                {"2 ^ 1;", testStructs("(^ 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"'a' ^ 'b';", testStructs("(^ 'a' 'b')", "\\.\\.", asList("string"), 1, 0, 0)},
                {"2 ^ 1.2;", testStructs("(^ 2 1.2)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2.5 ^ 1;", testStructs("(^ 2.5 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2.5 ^ 1.5;", testStructs("(^ 2.5 1.5)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"'2.5' ^ 1;", testStructs("(^ '2.5' 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2 ^ '1';", testStructs("(^ 2 '1')", "\\.\\.", asList("int"), 1, 0, 0)},
                {"$x = 1 ? 1 : 1.5; $x ^ $x;", testStructs("(^ $x $x)", "\\.\\.", asList("int"), 1, 2, 0)},
                // ==
                {"false == 1;", testStructs("(== false 1)", "\\.\\.", bool, 1, 0, 0)},
                {"1.2 == [];", testStructs("(== 1.2 array)", "\\.\\.", bool, 1, 0, 0)},
                // ===
                {"false === 1;", testStructs("(=== false 1)", "\\.\\.", bool, 1, 0, 0)},
                {"1.2 === [];", testStructs("(=== 1.2 array)", "\\.\\.", bool, 1, 0, 0)},
                // !=
                {"false != 1;", testStructs("(!= false 1)", "\\.\\.", bool, 1, 0, 0)},
                {"1.2 != [];", testStructs("(!= 1.2 array)", "\\.\\.", bool, 1, 0, 0)},
                // !==
                {"false !== 1;", testStructs("(!== false 1)", "\\.\\.", bool, 1, 0, 0)},
                {"1.2 !== [];", testStructs("(!== 1.2 array)", "\\.\\.", bool, 1, 0, 0)},
                // <
                {"false < 1;", testStructs("(< false 1)", "\\.\\.", bool, 1, 0, 0)},
                {"1.2 < [];", testStructs("(< 1.2 array)", "\\.\\.", bool, 1, 0, 0)},
                // <=
                {"false <= 1;", testStructs("(<= false 1)", "\\.\\.", bool, 1, 0, 0)},
                {"1.2 <= [];", testStructs("(<= 1.2 array)", "\\.\\.", bool, 1, 0, 0)},
                // >
                {"false > 1;", testStructs("(> false 1)", "\\.\\.", bool, 1, 0, 0)},
                {"1.2 > [];", testStructs("(> 1.2 array)", "\\.\\.", bool, 1, 0, 0)},
                // >=
                {"false >= 1;", testStructs("(>= false 1)", "\\.\\.", bool, 1, 0, 0)},
                {"1.2 >= [];", testStructs("(>= 1.2 array)", "\\.\\.", bool, 1, 0, 0)},
        });
    }
}
