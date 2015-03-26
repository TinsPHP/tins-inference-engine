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

import java.util.Arrays;
import java.util.Collection;


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
        return Arrays.asList(new Object[][]{
                // or
                {"false or false;", testStructs("(or false false)", "", "false", 1, 0, 0)},
                {"$x = (bool) true; true or $x;", testStructs("(or true $x)", "", "true", 1, 2, 0)},
                {"$x = (bool) true; $x or true;", testStructs("(or $x true)", "", "true", 1, 2, 0)},
                {"$x = (bool) true; $x or $x;", testStructs("(or $x $x)", "", "bool", 1, 2, 0)},
                // xor
                {"false xor true;", testStructs("(xor false true)", "", "true", 1, 0, 0)},
                {"true xor false;", testStructs("(xor true false)", "", "true", 1, 0, 0)},
                {"false xor false;", testStructs("(xor false false)", "", "false", 1, 0, 0)},
                {"true xor true;", testStructs("(xor true true)", "", "false", 1, 0, 0)},
                {"$x = (bool) true; $x xor $x;", testStructs("(xor $x $x)", "", "bool", 1, 2, 0)},
                // and
                {"$x = (bool) true; false and $x;", testStructs("(and false $x)", "", "false", 1, 2, 0)},
                {"$x = (bool) true; $x and false;", testStructs("(and $x false)", "", "false", 1, 2, 0)},
                {"true and true;", testStructs("(and true true)", "", "true", 1, 0, 0)},
                {"$x = (bool) true; $x and $x;", testStructs("(and $x $x)", "", "bool", 1, 2, 0)},
                // +=
                {"$x = true; $x += false;", testStructs("(+= $x false)", "", "int", 1, 2, 0)},
                {"$x = 1; $x += 1;", testStructs("(+= $x 1)", "", "int", 1, 2, 0)},
                {"$x = 1.3; $x += 1.3;", testStructs("(+= $x 1.3)", "", "float", 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x += $x;", testStructs("(+= $x $x)", "", "num", 1, 2, 0)},
                {"$x = []; $x += [1];", testStructs("(+= $x (array 1))", "", "array", 1, 2, 0)},
                // -=
                {"$x = true; $x -= false;", testStructs("(-= $x false)", "", "int", 1, 2, 0)},
                {"$x = 1; $x -= 1;", testStructs("(-= $x 1)", "", "int", 1, 2, 0)},
                {"$x = 1.3; $x -= 1.3;", testStructs("(-= $x 1.3)", "", "float", 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x -= $x;", testStructs("(-= $x $x)", "", "num", 1, 2, 0)},
                // *=
                {"$x = true; $x *= false;", testStructs("(*= $x false)", "", "int", 1, 2, 0)},
                {"$x = 1; $x *= 1;", testStructs("(*= $x 1)", "", "int", 1, 2, 0)},
                {"$x = 1.3; $x *= 1.3;", testStructs("(*= $x 1.3)", "", "float", 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x *= $x;", testStructs("(*= $x $x)", "", "num", 1, 2, 0)},
                // /=
                {"$x = true; $x /= false;", testStructs("(/= $x false)", "", "{int V false}", 1, 2, 0)},
                {"$x = 1.3; $x /= 1.3;", testStructs("(/= $x 1.3)", "", "{false V float}", 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x /= $x;", testStructs("(/= $x $x)", "", "{num V false}", 1, 2, 0)},
                // %=
                {"$x = 1; $x %= 1;", testStructs("(%= $x 1)", "", "{int V false}", 1, 2, 0)},
                // |=
                {"$x = 1; $x |= 1;", testStructs("(|= $x 1)", "", "int", 1, 2, 0)},
                {"$x = 'a'; $x |= 'b';", testStructs("(|= $x 'b')", "", "string", 1, 2, 0)},
                // &=
                {"$x = 1; $x &= 1;", testStructs("(&= $x 1)", "", "int", 1, 2, 0)},
                {"$x = 'a'; $x &= 'b';", testStructs("(&= $x 'b')", "", "string", 1, 2, 0)},
                // ^=
                {"$x = 1; $x ^= 1;", testStructs("(^= $x 1)", "", "int", 1, 2, 0)},
                {"$x = 'a'; $x ^= 'b';", testStructs("(^= $x 'b')", "", "string", 1, 2, 0)},
                // <<=
                {"$x = 1; $x <<= 1;", testStructs("(<<= $x 1)", "", "int", 1, 2, 0)},
                // >>=
                {"$x = 1; $x >>= 1;", testStructs("(>>= $x 1)", "", "int", 1, 2, 0)},
                // .=
                {"$x = 'a'; $x .= 'b';", testStructs("(.= $x 'b')", "", "string", 1, 2, 0)},
                // ?
                {"true ? 1 : 1.3;", testStructs("(? true 1 1.3)", "", "int", 1, 0, 0)},
                {"false ? 1 : 1.3;", testStructs("(? false 1 1.3)", "", "float", 1, 0, 0)},
                {"$x = (bool) 1; $x ? 1 : 1.3;", testStructs("(? $x 1 1.3)", "", "{int V float}", 1, 2, 0)},
                {"$x = (bool) 1; $x ? [1] : 1.3;", testStructs("(? $x (array 1) 1.3)", "", "{float V array}", 1, 2, 0)},
                // ||
                {"false || false;", testStructs("(|| false false)", "", "false", 1, 0, 0)},
                {"$x = (bool) true; true || $x;", testStructs("(|| true $x)", "", "true", 1, 2, 0)},
                {"$x = (bool) true; $x || true;", testStructs("(|| $x true)", "", "true", 1, 2, 0)},
                {"$x = (bool) true; $x || $x;", testStructs("(|| $x $x)", "", "bool", 1, 2, 0)},
                // &&
                {"$x = (bool) true; false && $x;", testStructs("(&& false $x)", "", "false", 1, 2, 0)},
                {"$x = (bool) true; $x && false;", testStructs("(&& $x false)", "", "false", 1, 2, 0)},
                {"true && true;", testStructs("(&& true true)", "", "true", 1, 0, 0)},
                {"$x = (bool) true; $x && $x;", testStructs("(&& $x $x)", "", "bool", 1, 2, 0)},
                // |
                {"2 | 1;", testStructs("(| 2 1)", "", "int", 1, 0, 0)},
                {"'a' | 'b';", testStructs("(| 'a' 'b')", "", "string", 1, 0, 0)},
                // &
                {"2 & 1;", testStructs("(& 2 1)", "", "int", 1, 0, 0)},
                {"'a' & 'b';", testStructs("(& 'a' 'b')", "", "string", 1, 0, 0)},
                // ^
                {"2 ^ 1;", testStructs("(^ 2 1)", "", "int", 1, 0, 0)},
                {"'a' ^ 'b';", testStructs("(^ 'a' 'b')", "", "string", 1, 0, 0)},
                // ==
                {"false == 1;", testStructs("(== false 1)", "", "bool", 1, 0, 0)},
                {"1.2 == [];", testStructs("(== 1.2 array)", "", "bool", 1, 0, 0)},
                // ===
                {"false === 1;", testStructs("(=== false 1)", "", "bool", 1, 0, 0)},
                {"1.2 === [];", testStructs("(=== 1.2 array)", "", "bool", 1, 0, 0)},
                // !=
                {"false != 1;", testStructs("(!= false 1)", "", "bool", 1, 0, 0)},
                {"1.2 != [];", testStructs("(!= 1.2 array)", "", "bool", 1, 0, 0)},
                // !==
                {"false !== 1;", testStructs("(!== false 1)", "", "bool", 1, 0, 0)},
                {"1.2 !== [];", testStructs("(!== 1.2 array)", "", "bool", 1, 0, 0)},
                // <
                {"false < 1;", testStructs("(< false 1)", "", "bool", 1, 0, 0)},
                {"1.2 < [];", testStructs("(< 1.2 array)", "", "bool", 1, 0, 0)},
                // <=
                {"false <= 1;", testStructs("(<= false 1)", "", "bool", 1, 0, 0)},
                {"1.2 <= [];", testStructs("(<= 1.2 array)", "", "bool", 1, 0, 0)},
                // >
                {"false > 1;", testStructs("(> false 1)", "", "bool", 1, 0, 0)},
                {"1.2 > [];", testStructs("(> 1.2 array)", "", "bool", 1, 0, 0)},
                // >=
                {"false >= 1;", testStructs("(>= false 1)", "", "bool", 1, 0, 0)},
                {"1.2 >= [];", testStructs("(>= 1.2 array)", "", "bool", 1, 0, 0)},
                // >>
                {"2 >> 1;", testStructs("(>> 2 1)", "", "int", 1, 0, 0)},
                // <<
                {"2 << 1;", testStructs("(<< 2 1)", "", "int", 1, 0, 0)},
                // +
                {"true + false;", testStructs("(+ true false)", "", "int", 1, 0, 0)},
                {"2 + 1;", testStructs("(+ 2 1)", "", "int", 1, 0, 0)},
                {"2.1 + 1.5;", testStructs("(+ 2.1 1.5)", "", "float", 1, 0, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x + $x;", testStructs("(+ $x $x)", "", "num", 1, 2, 0)},
                {"[] + [1,2];", testStructs("(+ array (array 1 2))", "", "array", 1, 0, 0)},
                // -
                {"true - false;", testStructs("(- true false)", "", "int", 1, 0, 0)},
                {"2 - 1;", testStructs("(- 2 1)", "", "int", 1, 0, 0)},
                {"2.1 - 1.5;", testStructs("(- 2.1 1.5)", "", "float", 1, 0, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x - $x;", testStructs("(- $x $x)", "", "num", 1, 2, 0)},
                // .
                {"'a'.'b';", testStructs("(. 'a' 'b')", "", "string", 1, 0, 0)},
                // *
                {"true * false;", testStructs("(* true false)", "", "int", 1, 0, 0)},
                {"2 * 1;", testStructs("(* 2 1)", "", "int", 1, 0, 0)},
                {"2.1 * 1.5;", testStructs("(* 2.1 1.5)", "", "float", 1, 0, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x * $x;", testStructs("(* $x $x)", "", "num", 1, 2, 0)},
                // /
                {"true / false;", testStructs("(/ true false)", "", "{int V false}", 1, 0, 0)},
                {"2.1 / 1.5;", testStructs("(/ 2.1 1.5)", "", "{false V float}", 1, 0, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x / $x;", testStructs("(/ $x $x)", "", "{num V false}", 1, 2, 0)},
                // %
                {"2 % 1;", testStructs("(% 2 1)", "", "{int V false}", 1, 0, 0)},
                //instanceof
                {"$x = 1; 1 instanceof $x;", testStructs("(instanceof 1 $x)", "", "bool", 1, 2, 0)},
                // casting
                {"(bool) 1;", testStructs("(casting (type tMod bool) 1)", "", "bool", 1, 0, 0)},
                {"(int) 1;", testStructs("(casting (type tMod int) 1)", "", "int", 1, 0, 0)},
                {"(float) 1;", testStructs("(casting (type tMod float) 1)", "", "float", 1, 0, 0)},
                {"(string) 1;", testStructs("(casting (type tMod string) 1)", "", "string", 1, 0, 0)},
                {"(array) 1;", testStructs("(casting (type tMod array) 1)", "", "array", 1, 0, 0)},
                // preIncr
                {"$x = false; ++$x;", testStructs("(preIncr $x)", "", "bool", 1, 2, 0)},
                {"$x = true; ++$x;", testStructs("(preIncr $x)", "", "bool", 1, 2, 0)},
                {"$x = 1; ++$x;", testStructs("(preIncr $x)", "", "int", 1, 2, 0)},
                {"$x = 1.5; ++$x;", testStructs("(preIncr $x)", "", "float", 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; ++$x;", testStructs("(preIncr $x)", "", "num", 1, 2, 0)},
                //preDecr
                {"$x = false; --$x;", testStructs("(preDecr $x)", "", "bool", 1, 2, 0)},
                {"$x = true; --$x;", testStructs("(preDecr $x)", "", "bool", 1, 2, 0)},
                {"$x = 1; --$x;", testStructs("(preDecr $x)", "", "int", 1, 2, 0)},
                {"$x = 1.5; --$x;", testStructs("(preDecr $x)", "", "float", 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; --$x;", testStructs("(preDecr $x)", "", "num", 1, 2, 0)},
                // postIncr
                {"$x = false; $x++;", testStructs("(postIncr $x)", "", "bool", 1, 2, 0)},
                {"$x = true; $x++;", testStructs("(postIncr $x)", "", "bool", 1, 2, 0)},
                {"$x = 1; $x++;", testStructs("(postIncr $x)", "", "int", 1, 2, 0)},
                {"$x = 1.5; $x++;", testStructs("(postIncr $x)", "", "float", 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x++;", testStructs("(postIncr $x)", "", "num", 1, 2, 0)},
                //postDecr
                {"$x = false; $x--;", testStructs("(postDecr $x)", "", "bool", 1, 2, 0)},
                {"$x = true; $x--;", testStructs("(postDecr $x)", "", "bool", 1, 2, 0)},
                {"$x = 1; $x--;", testStructs("(postDecr $x)", "", "int", 1, 2, 0)},
                {"$x = 1.5; $x--;", testStructs("(postDecr $x)", "", "float", 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x--;", testStructs("(postDecr $x)", "", "num", 1, 2, 0)},
                //@
                {"@false;", testStructs("(@ false)", "", "false", 1, 0, 0)},
                {"@true;", testStructs("(@ true)", "", "true", 1, 0, 0)},
                {"@1;", testStructs("(@ 1)", "", "int", 1, 0, 0)},
                {"@1.3;", testStructs("(@ 1.3)", "", "float", 1, 0, 0)},
                {"@'a';", testStructs("(@ 'a')", "", "string", 1, 0, 0)},
                {"@[1];", testStructs("(@ (array 1))", "", "array", 1, 0, 0)},
                //~
                {"~1;", testStructs("(~ 1)", "", "int", 1, 0, 0)},
                {"~'a';", testStructs("(~ 'a')", "", "string", 1, 0, 0)},
                //uMinus
                {"-false;", testStructs("(uMinus false)", "", "int", 1, 0, 0)},
                {"-true;", testStructs("(uMinus true)", "", "int", 1, 0, 0)},
                {"-1;", testStructs("(uMinus 1)", "", "int", 1, 0, 0)},
                {"-1.5;", testStructs("(uMinus 1.5)", "", "float", 1, 0, 0)},
                //uPlus
                {"+false;", testStructs("(uPlus false)", "", "int", 1, 0, 0)},
                {"+true;", testStructs("(uPlus true)", "", "int", 1, 0, 0)},
                {"+1;", testStructs("(uPlus 1)", "", "int", 1, 0, 0)},
                {"+1.5;", testStructs("(uPlus 1.5)", "", "float", 1, 0, 0)},
        });
    }
}
