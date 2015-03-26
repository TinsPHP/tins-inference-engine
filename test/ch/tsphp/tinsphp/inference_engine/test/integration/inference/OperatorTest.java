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
                {"$x = true; $x = false; true or $x;", testStructs("(or true $x)", "", "true", 1, 3, 0)},
                {"$x = true; $x = false; $x or true;", testStructs("(or $x true)", "", "true", 1, 3, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = true; $x = false; $x or $x;", testStructs("(or $x $x)", "", "bool", 1, 3, 0)},
                // xor
                {"false xor true;", testStructs("(xor false true)", "", "true", 1, 0, 0)},
                {"true xor false;", testStructs("(xor true false)", "", "true", 1, 0, 0)},
                {"false xor false;", testStructs("(xor false false)", "", "false", 1, 0, 0)},
                {"true xor true;", testStructs("(xor true true)", "", "false", 1, 0, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = true; $x = false; $x xor $x;", testStructs("(xor $x $x)", "", "bool", 1, 3, 0)},
                // and
                {"$x = true; $x = false; false and $x;", testStructs("(and false $x)", "", "false", 1, 3, 0)},
                {"$x = true; $x = false; $x and false;", testStructs("(and $x false)", "", "false", 1, 3, 0)},
                {"true and true;", testStructs("(and true true)", "", "true", 1, 0, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = true; $x = false; $x and $x;", testStructs("(and $x $x)", "", "bool", 1, 3, 0)},
                // +=
                {"$x = true; $x += false;", testStructs("(+= $x false)", "", "int", 1, 2, 0)},
                {"$x = 1; $x += 1;", testStructs("(+= $x 1)", "", "int", 1, 2, 0)},
                {"$x = 1.3; $x += 1.3;", testStructs("(+= $x 1.3)", "", "float", 1, 2, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = 1; $x = 1.3; $x += $x;", testStructs("(+= $x $x)", "", "num", 1, 3, 0)},
                {"$x = []; $x += [1];", testStructs("(+= $x (array 1))", "", "array", 1, 2, 0)},
                // -=
                {"$x = true; $x -= false;", testStructs("(-= $x false)", "", "int", 1, 2, 0)},
                {"$x = 1; $x -= 1;", testStructs("(-= $x 1)", "", "int", 1, 2, 0)},
                {"$x = 1.3; $x -= 1.3;", testStructs("(-= $x 1.3)", "", "float", 1, 2, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = 1; $x = 1.3; $x -= $x;", testStructs("(-= $x $x)", "", "num", 1, 3, 0)},
                // *=
                {"$x = true; $x *= false;", testStructs("(*= $x false)", "", "int", 1, 2, 0)},
                {"$x = 1; $x *= 1;", testStructs("(*= $x 1)", "", "int", 1, 2, 0)},
                {"$x = 1.3; $x *= 1.3;", testStructs("(*= $x 1.3)", "", "float", 1, 2, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = 1; $x = 1.3; $x *= $x;", testStructs("(*= $x $x)", "", "num", 1, 3, 0)},
                // /=
                {"$x = true; $x /= false;", testStructs("(/= $x false)", "", "{int V false}", 1, 2, 0)},
                {"$x = 1.3; $x /= 1.3;", testStructs("(/= $x 1.3)", "", "{false V float}", 1, 2, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = 1; $x = 1.3; $x /= $x;", testStructs("(/= $x $x)", "", "{num V false}", 1, 3, 0)},
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
                //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
                // ||
                {"false || false;", testStructs("(|| false false)", "", "false", 1, 0, 0)},
                {"$x = true; $x = false; true || $x;", testStructs("(|| true $x)", "", "true", 1, 3, 0)},
                {"$x = true; $x = false; $x || true;", testStructs("(|| $x true)", "", "true", 1, 3, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = true; $x = false; $x || $x;", testStructs("(|| $x $x)", "", "bool", 1, 3, 0)},
                // &&
                {"$x = true; $x = false; false && $x;", testStructs("(&& false $x)", "", "false", 1, 3, 0)},
                {"$x = true; $x = false; $x && false;", testStructs("(&& $x false)", "", "false", 1, 3, 0)},
                {"true && true;", testStructs("(&& true true)", "", "true", 1, 0, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = true; $x = false; $x && $x;", testStructs("(&& $x $x)", "", "bool", 1, 3, 0)},
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
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = 1; $x = 2.1; $x + $x;", testStructs("(+ $x $x)", "", "num", 1, 3, 0)},
                {"[] + [1,2];", testStructs("(+ array (array 1 2))", "", "array", 1, 0, 0)},
                // -
                {"true - false;", testStructs("(- true false)", "", "int", 1, 0, 0)},
                {"2 - 1;", testStructs("(- 2 1)", "", "int", 1, 0, 0)},
                {"2.1 - 1.5;", testStructs("(- 2.1 1.5)", "", "float", 1, 0, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = 1; $x = 2.1; $x - $x;", testStructs("(- $x $x)", "", "num", 1, 3, 0)},
                // .
                {"'a'.'b';", testStructs("(. 'a' 'b')", "", "string", 1, 0, 0)},
                // *
                {"true * false;", testStructs("(* true false)", "", "int", 1, 0, 0)},
                {"2 * 1;", testStructs("(* 2 1)", "", "int", 1, 0, 0)},
                {"2.1 * 1.5;", testStructs("(* 2.1 1.5)", "", "float", 1, 0, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = 1; $x = 2.1; $x * $x;", testStructs("(* $x $x)", "", "num", 1, 3, 0)},
                // /
                {"true / false;", testStructs("(/ true false)", "", "{int V false}", 1, 0, 0)},
                {"2.1 / 1.5;", testStructs("(/ 2.1 1.5)", "", "{false V float}", 1, 0, 0)},
                //TODO rstoll TINS-348 inference procedural - solve parametric function constraints
//                {"$x = 1; $x = 2.1; $x / $x;", testStructs("(/ $x $x)", "", "{num V false}", 1, 3, 0)},
                // %
                {"2 % 1;", testStructs("(% 2 1)", "", "{int V false}", 1, 0, 0)},
                // instanceof
                //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
                // preIncr
                {"$a = false; ++$a;", testStructs("(preIncr $a)", "", "bool", 1, 2, 0)},
                {"$a = true; ++$a;", testStructs("(preIncr $a)", "", "bool", 1, 2, 0)},
                {"$a = 1; ++$a;", testStructs("(preIncr $a)", "", "int", 1, 2, 0)},
                {"$a = 1.5; ++$a;", testStructs("(preIncr $a)", "", "float", 1, 2, 0)},
                //TODO num -> num
                //preDecr
                {"$a = false; --$a;", testStructs("(preDecr $a)", "", "bool", 1, 2, 0)},
                {"$a = true; --$a;", testStructs("(preDecr $a)", "", "bool", 1, 2, 0)},
                {"$a = 1; --$a;", testStructs("(preDecr $a)", "", "int", 1, 2, 0)},
                {"$a = 1.5; --$a;", testStructs("(preDecr $a)", "", "float", 1, 2, 0)},
                //TODO num -> num
                // postIncr
                {"$a = false; $a++;", testStructs("(postIncr $a)", "", "bool", 1, 2, 0)},
                {"$a = true; $a++;", testStructs("(postIncr $a)", "", "bool", 1, 2, 0)},
                {"$a = 1; $a++;", testStructs("(postIncr $a)", "", "int", 1, 2, 0)},
                {"$a = 1.5; $a++;", testStructs("(postIncr $a)", "", "float", 1, 2, 0)},
                //TODO num -> num
                //postDecr
                {"$a = false; $a--;", testStructs("(postDecr $a)", "", "bool", 1, 2, 0)},
                {"$a = true; $a--;", testStructs("(postDecr $a)", "", "bool", 1, 2, 0)},
                {"$a = 1; $a--;", testStructs("(postDecr $a)", "", "int", 1, 2, 0)},
                {"$a = 1.5; $a--;", testStructs("(postDecr $a)", "", "float", 1, 2, 0)},
                //TODO num -> num
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
