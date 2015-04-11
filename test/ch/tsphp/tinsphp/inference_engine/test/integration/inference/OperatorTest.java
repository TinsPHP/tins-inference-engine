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
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceTypeTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AbsoluteTypeNameTestStruct;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static java.util.Arrays.asList;

@RunWith(Parameterized.class)
public class OperatorTest extends AInferenceTypeTest
{

    public OperatorTest(String testString, AbsoluteTypeNameTestStruct[] theTestStructs) {
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
        return asList(new Object[][]{
                // or
                {"false or false;", testStructs("(or false false)", "\\.\\.", asList("false"), null, 1, 0, 0)},
                {"$x = (bool) true; true or $x;", testStructs("(or true $x)", "\\.\\.", asList("true"), null, 1, 2, 0)},
                {"$x = (bool) true; $x or true;", testStructs("(or $x true)", "\\.\\.", asList("true"), null, 1, 2, 0)},
                {"$x = (bool) true; $x or $x;", testStructs("(or $x $x)", "\\.\\.", asList("bool"), null, 1, 2, 0)},
                // xor
                {"false xor true;", testStructs("(xor false true)", "\\.\\.", asList("true"), null, 1, 0, 0)},
                {"true xor false;", testStructs("(xor true false)", "\\.\\.", asList("true"), null, 1, 0, 0)},
                {"false xor false;", testStructs("(xor false false)", "\\.\\.", asList("false"), null, 1, 0, 0)},
                {"true xor true;", testStructs("(xor true true)", "\\.\\.", asList("false"), null, 1, 0, 0)},
                {"$x = (bool) true; $x xor $x;", testStructs("(xor $x $x)", "\\.\\.", asList("bool"), null, 1, 2, 0)},
                // and
                {
                        "$x = (bool) true; false and $x;",
                        testStructs("(and false $x)", "\\.\\.", asList("false"), null, 1, 2, 0)
                },
                {
                        "$x = (bool) true; $x and false;",
                        testStructs("(and $x false)", "\\.\\.", asList("false"), null, 1, 2, 0)
                },
                {"true and true;", testStructs("(and true true)", "\\.\\.", asList("true"), null, 1, 0, 0)},
                {"$x = (bool) true; $x and $x;", testStructs("(and $x $x)", "\\.\\.", asList("bool"), null, 1, 2, 0)},
                // +=
                //TODO rstoll TINS-347 create overloads for conversion constraints
//                {"$x = true; $x += false;", testStructs("(+= $x false)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1; $x += 1;", testStructs("(+= $x 1)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1.3; $x += 1.3;", testStructs("(+= $x 1.3)", "\\.\\.", asList("float"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x += $x;",
                        testStructs("(+= $x $x)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                {"$x = []; $x += [1];", testStructs("(+= $x (array 1))", "\\.\\.", asList("array"), null, 1, 2, 0)},
                // -=
                //TODO rstoll TINS-347 create overloads for conversion constraints
//                {"$x = true; $x -= false;", testStructs("(-= $x false)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1; $x -= 1;", testStructs("(-= $x 1)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1.3; $x -= 1.3;", testStructs("(-= $x 1.3)", "\\.\\.", asList("float"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x -= $x;",
                        testStructs("(-= $x $x)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                // *=
                //TODO rstoll TINS-347 create overloads for conversion constraints
//                {"$x = true; $x *= false;", testStructs("(*= $x false)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1; $x *= 1;", testStructs("(*= $x 1)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1.3; $x *= 1.3;", testStructs("(*= $x 1.3)", "\\.\\.", asList("float"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x *= $x;",
                        testStructs("(*= $x $x)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                // /=
                //TODO rstoll TINS-347 create overloads for conversion constraints
//                {
//                        "$x = true; $x /= false;",
//                        testStructs("(/= $x false)", "\\.\\.", asList("(int | false)"), null, 1, 2, 0)
//                },
                {
                        "$x = 1.3; $x /= 1.3;",
                        testStructs("(/= $x 1.3)", "\\.\\.", asList("false", "float"), null, 1, 2, 0)
                },
                //TODO rstoll TINS-347 create overloads for conversion constraints
//                {
//                        "$x = (bool) 1 ? 1 : 1.5; $x /= $x;",
//                        testStructs("(/= $x $x)", "\\.\\.", asList("num", "false"), null, 1, 2, 0)
//                },
                // %=
                {"$x = 1; $x %= 1;", testStructs("(%= $x 1)", "\\.\\.", asList("(int | false)"), null, 1, 2, 0)},
                // |=
                {"$x = 1; $x |= 1;", testStructs("(|= $x 1)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 'a'; $x |= 'b';", testStructs("(|= $x 'b')", "\\.\\.", asList("string"), null, 1, 2, 0)},
                // &=
                {"$x = 1; $x &= 1;", testStructs("(&= $x 1)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 'a'; $x &= 'b';", testStructs("(&= $x 'b')", "\\.\\.", asList("string"), null, 1, 2, 0)},
                // ^=
                {"$x = 1; $x ^= 1;", testStructs("(^= $x 1)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 'a'; $x ^= 'b';", testStructs("(^= $x 'b')", "\\.\\.", asList("string"), null, 1, 2, 0)},
                // <<=
                {"$x = 1; $x <<= 1;", testStructs("(<<= $x 1)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                // >>=
                {"$x = 1; $x >>= 1;", testStructs("(>>= $x 1)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                // .=
                {"$x = 'a'; $x .= 'b';", testStructs("(.= $x 'b')", "\\.\\.", asList("string"), null, 1, 2, 0)},
                // ?
                {"true ? 1 : 1.3;", testStructs("(? true 1 1.3)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"false ? 1 : 1.3;", testStructs("(? false 1 1.3)", "\\.\\.", asList("float"), null, 1, 0, 0)},
                {
                        "$x = (bool) 1; $x ? 1 : 1.3;",
                        testStructs("(? $x 1 1.3)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                {
                        "$x = (bool) 1; $x ? [1] : 1.3;",
                        testStructs("(? $x (array 1) 1.3)", "\\.\\.", asList("float", "array"), null, 1, 2, 0)
                },
                // ||
                {"false || false;", testStructs("(|| false false)", "\\.\\.", asList("false"), null, 1, 0, 0)},
                {"$x = (bool) true; true || $x;", testStructs("(|| true $x)", "\\.\\.", asList("true"), null, 1, 2, 0)},
                {"$x = (bool) true; $x || true;", testStructs("(|| $x true)", "\\.\\.", asList("true"), null, 1, 2, 0)},
                {"$x = (bool) true; $x || $x;", testStructs("(|| $x $x)", "\\.\\.", asList("bool"), null, 1, 2, 0)},
                // &&
                {
                        "$x = (bool) true; false && $x;",
                        testStructs("(&& false $x)", "\\.\\.", asList("false"), null, 1, 2, 0)
                },
                {
                        "$x = (bool) true; $x && false;",
                        testStructs("(&& $x false)", "\\.\\.", asList("false"), null, 1, 2, 0)
                },
                {"true && true;", testStructs("(&& true true)", "\\.\\.", asList("true"), null, 1, 0, 0)},
                {"$x = (bool) true; $x && $x;", testStructs("(&& $x $x)", "\\.\\.", asList("bool"), null, 1, 2, 0)},
                // |
                {"2 | 1;", testStructs("(| 2 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"'a' | 'b';", testStructs("(| 'a' 'b')", "\\.\\.", asList("string"), null, 1, 0, 0)},
                // &
                {"2 & 1;", testStructs("(& 2 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"'a' & 'b';", testStructs("(& 'a' 'b')", "\\.\\.", asList("string"), null, 1, 0, 0)},
                // ^
                {"2 ^ 1;", testStructs("(^ 2 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"'a' ^ 'b';", testStructs("(^ 'a' 'b')", "\\.\\.", asList("string"), null, 1, 0, 0)},
                // ==
                {"false == 1;", testStructs("(== false 1)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                {"1.2 == [];", testStructs("(== 1.2 array)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                // ===
                {"false === 1;", testStructs("(=== false 1)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                {"1.2 === [];", testStructs("(=== 1.2 array)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                // !=
                {"false != 1;", testStructs("(!= false 1)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                {"1.2 != [];", testStructs("(!= 1.2 array)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                // !==
                {"false !== 1;", testStructs("(!== false 1)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                {"1.2 !== [];", testStructs("(!== 1.2 array)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                // <
                {"false < 1;", testStructs("(< false 1)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                {"1.2 < [];", testStructs("(< 1.2 array)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                // <=
                {"false <= 1;", testStructs("(<= false 1)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                {"1.2 <= [];", testStructs("(<= 1.2 array)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                // >
                {"false > 1;", testStructs("(> false 1)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                {"1.2 > [];", testStructs("(> 1.2 array)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                // >=
                {"false >= 1;", testStructs("(>= false 1)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                {"1.2 >= [];", testStructs("(>= 1.2 array)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                // >>
                {"2 >> 1;", testStructs("(>> 2 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                // <<
                {"2 << 1;", testStructs("(<< 2 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                // +
                {"true + false;", testStructs("(+ true false)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"2 + 1;", testStructs("(+ 2 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"2.1 + 1.5;", testStructs("(+ 2.1 1.5)", "\\.\\.", asList("float"), null, 1, 0, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x + $x;",
                        testStructs("(+ $x $x)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                {"[] + [1,2];", testStructs("(+ array (array 1 2))", "\\.\\.", asList("array"), null, 1, 0, 0)},
                // -
                {"true - false;", testStructs("(- true false)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"2 - 1;", testStructs("(- 2 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"2.1 - 1.5;", testStructs("(- 2.1 1.5)", "\\.\\.", asList("float"), null, 1, 0, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x - $x;",
                        testStructs("(- $x $x)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                // .
                {"'a'.'b';", testStructs("(. 'a' 'b')", "\\.\\.", asList("string"), null, 1, 0, 0)},
                // *
                {"true * false;", testStructs("(* true false)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"2 * 1;", testStructs("(* 2 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"2.1 * 1.5;", testStructs("(* 2.1 1.5)", "\\.\\.", asList("float"), null, 1, 0, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x * $x;",
                        testStructs("(* $x $x)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                // /
                {"true / false;", testStructs("(/ true false)", "\\.\\.", asList("(int | false)"), null, 1, 0, 0)},
                {"2.1 / 1.5;", testStructs("(/ 2.1 1.5)", "\\.\\.", asList("false", "float"), null, 1, 0, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x / $x;",
                        testStructs("(/ $x $x)", "\\.\\.", asList("int", "float", "false"), null, 1, 2, 0)
                },
                // %
                {"2 % 1;", testStructs("(% 2 1)", "\\.\\.", asList("(int | false)"), null, 1, 0, 0)},
                //instanceof
                {
                        "$x = 1; 1 instanceof $x;",
                        testStructs("(instanceof 1 $x)", "\\.\\.", asList("bool"), null, 1, 2, 0)
                },
                // casting
                {"(bool) 1;", testStructs("(casting (type tMod bool) 1)", "\\.\\.", asList("bool"), null, 1, 0, 0)},
                {"(int) 1;", testStructs("(casting (type tMod int) 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"(float) 1;", testStructs("(casting (type tMod float) 1)", "\\.\\.", asList("float"), null, 1, 0, 0)},
                {
                        "(string) 1;",
                        testStructs("(casting (type tMod string) 1)", "\\.\\.", asList("string"), null, 1, 0, 0)
                },
                {"(array) 1;", testStructs("(casting (type tMod array) 1)", "\\.\\.", asList("array"), null, 1, 0, 0)},
                // preIncr
                {"$x = false; ++$x;", testStructs("(preIncr $x)", "\\.\\.", asList("false"), null, 1, 2, 0)},
                {"$x = true; ++$x;", testStructs("(preIncr $x)", "\\.\\.", asList("true"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? false : true; ++$x;",
                        testStructs("(preIncr $x)", "\\.\\.", asList("false", "true"), null, 1, 2, 0)
                },
                {"$x = 1; ++$x;", testStructs("(preIncr $x)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1.5; ++$x;", testStructs("(preIncr $x)", "\\.\\.", asList("float"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; ++$x;",
                        testStructs("(preIncr $x)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                //preDecr
                {"$x = false; --$x;", testStructs("(preDecr $x)", "\\.\\.", asList("false"), null, 1, 2, 0)},
                {"$x = true; --$x;", testStructs("(preDecr $x)", "\\.\\.", asList("true"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? false : true; --$x;",
                        testStructs("(preDecr $x)", "\\.\\.", asList("false", "true"), null, 1, 2, 0)
                },
                {"$x = 1; --$x;", testStructs("(preDecr $x)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1.5; --$x;", testStructs("(preDecr $x)", "\\.\\.", asList("float"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; --$x;",
                        testStructs("(preDecr $x)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                // postIncr
                {"$x = false; $x++;", testStructs("(postIncr $x)", "\\.\\.", asList("false"), null, 1, 2, 0)},
                {"$x = true; $x++;", testStructs("(postIncr $x)", "\\.\\.", asList("true"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? false : true; $x++;",
                        testStructs("(postIncr $x)", "\\.\\.", asList("false", "true"), null, 1, 2, 0)
                },
                {"$x = 1; $x++;", testStructs("(postIncr $x)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1.5; $x++;", testStructs("(postIncr $x)", "\\.\\.", asList("float"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x++;",
                        testStructs("(postIncr $x)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                //postDecr
                {"$x = false; $x--;", testStructs("(postDecr $x)", "\\.\\.", asList("false"), null, 1, 2, 0)},
                {"$x = true; $x--;", testStructs("(postDecr $x)", "\\.\\.", asList("true"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? false : true; $x--;",
                        testStructs("(postDecr $x)", "\\.\\.", asList("false", "true"), null, 1, 2, 0)
                },
                {"$x = 1; $x--;", testStructs("(postDecr $x)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1.5; $x--;", testStructs("(postDecr $x)", "\\.\\.", asList("float"), null, 1, 2, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x--;",
                        testStructs("(postDecr $x)", "\\.\\.", asList("int", "float"), null, 1, 2, 0)
                },
                //@
                {"@false;", testStructs("(@ false)", "\\.\\.", asList("false"), null, 1, 0, 0)},
                {"@true;", testStructs("(@ true)", "\\.\\.", asList("true"), null, 1, 0, 0)},
                {"@1;", testStructs("(@ 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"@1.3;", testStructs("(@ 1.3)", "\\.\\.", asList("float"), null, 1, 0, 0)},
                {"@'a';", testStructs("(@ 'a')", "\\.\\.", asList("string"), null, 1, 0, 0)},
                {"@[1];", testStructs("(@ (array 1))", "\\.\\.", asList("array"), null, 1, 0, 0)},
                //~
                {"~1;", testStructs("(~ 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"~'a';", testStructs("(~ 'a')", "\\.\\.", asList("string"), null, 1, 0, 0)},
                //uMinus
                {"-false;", testStructs("(uMinus false)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"-true;", testStructs("(uMinus true)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"-1;", testStructs("(uMinus 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"-1.5;", testStructs("(uMinus 1.5)", "\\.\\.", asList("float"), null, 1, 0, 0)},
                //uPlus
                {"+false;", testStructs("(uPlus false)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"+true;", testStructs("(uPlus true)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"+1;", testStructs("(uPlus 1)", "\\.\\.", asList("int"), null, 1, 0, 0)},
                {"+1.5;", testStructs("(uPlus 1.5)", "\\.\\.", asList("float"), null, 1, 0, 0)},
        });
    }
}
