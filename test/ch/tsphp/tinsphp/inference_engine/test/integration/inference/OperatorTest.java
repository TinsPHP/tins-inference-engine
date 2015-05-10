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
public class OperatorTest extends AInferenceNamespaceTypeTest
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
        List<String> bool = asList("falseType", "trueType");
        List<String> num = asList("int", "float");
        return asList(new Object[][]{
                // or
                {"false or false;", testStructs("(or false false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"$x = (bool) true; true or $x;", testStructs("(or true $x)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"$x = (bool) true; $x or true;", testStructs("(or $x true)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"$x = (bool) true; $x or $x;", testStructs("(or $x $x)", "\\.\\.", bool, 1, 2, 0)},
                // xor
                {"false xor true;", testStructs("(xor false true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"true xor false;", testStructs("(xor true false)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"false xor false;", testStructs("(xor false false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"true xor true;", testStructs("(xor true true)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"$x = (bool) true; $x xor $x;", testStructs("(xor $x $x)", "\\.\\.", bool, 1, 2, 0)},
                // and
                {
                        "$x = (bool) true; false and $x;",
                        testStructs("(and false $x)", "\\.\\.", asList("falseType"), 1, 2, 0)
                },
                {
                        "$x = (bool) true; $x and false;",
                        testStructs("(and $x false)", "\\.\\.", asList("falseType"), 1, 2, 0)
                },
                {"true and true;", testStructs("(and true true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"$x = (bool) true; $x and $x;", testStructs("(and $x $x)", "\\.\\.", bool, 1, 2, 0)},
                // +=
                //TODO rstoll TINS-347 create overloads for conversion constraints
//                {"$x = true; $x += false;", testStructs("(+= $x false)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1; $x += 1;", testStructs("(+= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1.3; $x += 1.3;", testStructs("(+= $x 1.3)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x += $x;", testStructs("(+= $x $x)", "\\.\\.", num, 1, 2, 0)},
                {"$x = []; $x += [1];", testStructs("(+= $x (array 1))", "\\.\\.", asList("array"), 1, 2, 0)},
                // -=
                //TODO rstoll TINS-347 create overloads for conversion constraints
//                {"$x = true; $x -= false;", testStructs("(-= $x false)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1; $x -= 1;", testStructs("(-= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1.3; $x -= 1.3;", testStructs("(-= $x 1.3)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x -= $x;", testStructs("(-= $x $x)", "\\.\\.", num, 1, 2, 0)},
                // *=
                //TODO rstoll TINS-347 create overloads for conversion constraints
//                {"$x = true; $x *= false;", testStructs("(*= $x false)", "\\.\\.", asList("int"), null, 1, 2, 0)},
                {"$x = 1; $x *= 1;", testStructs("(*= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1.3; $x *= 1.3;", testStructs("(*= $x 1.3)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x *= $x;", testStructs("(*= $x $x)", "\\.\\.", num, 1, 2, 0)},
                // /=
                //TODO rstoll TINS-347 create overloads for conversion constraints
//                {
//                        "$x = true; $x /= false;",
//                        testStructs("(/= $x false)", "\\.\\.", asList("int","falseType"), null, 1, 2, 0)
//                },
                {
                        "$x = 1.3; $x /= 1.3;",
                        testStructs("(/= $x 1.3)", "\\.\\.", asList("falseType", "float"), 1, 2, 0)
                },
                //TODO rstoll TINS-347 create overloads for conversion constraints
//                {
//                        "$x = (bool) 1 ? 1 : 1.5; $x /= $x;",
//                        testStructs("(/= $x $x)", "\\.\\.", asList("num", "falseType"), null, 1, 2, 0)
//                },
                // %=
                {"$x = 1; $x %= 1;", testStructs("(%= $x 1)", "\\.\\.", asList("int", "falseType"), 1, 2, 0)},
                // |=
                {"$x = 1; $x |= 1;", testStructs("(|= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 'a'; $x |= 'b';", testStructs("(|= $x 'b')", "\\.\\.", asList("string"), 1, 2, 0)},
                // &=
                {"$x = 1; $x &= 1;", testStructs("(&= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 'a'; $x &= 'b';", testStructs("(&= $x 'b')", "\\.\\.", asList("string"), 1, 2, 0)},
                // ^=
                {"$x = 1; $x ^= 1;", testStructs("(^= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 'a'; $x ^= 'b';", testStructs("(^= $x 'b')", "\\.\\.", asList("string"), 1, 2, 0)},
                // <<=
                {"$x = 1; $x <<= 1;", testStructs("(<<= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                // >>=
                {"$x = 1; $x >>= 1;", testStructs("(>>= $x 1)", "\\.\\.", asList("int"), 1, 2, 0)},
                // .=
                {"$x = 'a'; $x .= 'b';", testStructs("(.= $x 'b')", "\\.\\.", asList("string"), 1, 2, 0)},
                // ?
                {"true ? 1 : 1.3;", testStructs("(? true 1 1.3)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"false ? 1 : 1.3;", testStructs("(? false 1 1.3)", "\\.\\.", asList("float"), 1, 0, 0)},
                {"$x = (bool) 1; $x ? 1 : 1.3;", testStructs("(? $x 1 1.3)", "\\.\\.", num, 1, 2, 0)},
                {
                        "$x = (bool) 1; $x ? [1] : 1.3;",
                        testStructs("(? $x (array 1) 1.3)", "\\.\\.", asList("float", "array"), 1, 2, 0)
                },
                // ||
                {"false || false;", testStructs("(|| false false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"$x = (bool) true; true || $x;", testStructs("(|| true $x)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"$x = (bool) true; $x || true;", testStructs("(|| $x true)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"$x = (bool) true; $x || $x;", testStructs("(|| $x $x)", "\\.\\.", bool, 1, 2, 0)},
                // &&
                {
                        "$x = (bool) true; false && $x;",
                        testStructs("(&& false $x)", "\\.\\.", asList("falseType"), 1, 2, 0)
                },
                {
                        "$x = (bool) true; $x && false;",
                        testStructs("(&& $x false)", "\\.\\.", asList("falseType"), 1, 2, 0)
                },
                {"true && true;", testStructs("(&& true true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"$x = (bool) true; $x && $x;", testStructs("(&& $x $x)", "\\.\\.", bool, 1, 2, 0)},
                // |
                {"2 | 1;", testStructs("(| 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"'a' | 'b';", testStructs("(| 'a' 'b')", "\\.\\.", asList("string"), 1, 0, 0)},
                // &
                {"2 & 1;", testStructs("(& 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"'a' & 'b';", testStructs("(& 'a' 'b')", "\\.\\.", asList("string"), 1, 0, 0)},
                // ^
                {"2 ^ 1;", testStructs("(^ 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"'a' ^ 'b';", testStructs("(^ 'a' 'b')", "\\.\\.", asList("string"), 1, 0, 0)},
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
                // >>
                {"2 >> 1;", testStructs("(>> 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                // <<
                {"2 << 1;", testStructs("(<< 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                // +
                {"true + false;", testStructs("(+ true false)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2 + 1;", testStructs("(+ 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2.1 + 1.5;", testStructs("(+ 2.1 1.5)", "\\.\\.", asList("float"), 1, 0, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x + $x;", testStructs("(+ $x $x)", "\\.\\.", num, 1, 2, 0)},
                {"[] + [1,2];", testStructs("(+ array (array 1 2))", "\\.\\.", asList("array"), 1, 0, 0)},
                // -
                {"true - false;", testStructs("(- true false)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2 - 1;", testStructs("(- 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2.1 - 1.5;", testStructs("(- 2.1 1.5)", "\\.\\.", asList("float"), 1, 0, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x - $x;", testStructs("(- $x $x)", "\\.\\.", num, 1, 2, 0)},
                // .
                {"'a'.'b';", testStructs("(. 'a' 'b')", "\\.\\.", asList("string"), 1, 0, 0)},
                // *
                {"true * false;", testStructs("(* true false)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2 * 1;", testStructs("(* 2 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"2.1 * 1.5;", testStructs("(* 2.1 1.5)", "\\.\\.", asList("float"), 1, 0, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x * $x;", testStructs("(* $x $x)", "\\.\\.", num, 1, 2, 0)},
                // /
                {"true / false;", testStructs("(/ true false)", "\\.\\.", asList("int", "falseType"), 1, 0, 0)},
                {"2.1 / 1.5;", testStructs("(/ 2.1 1.5)", "\\.\\.", asList("falseType", "float"), 1, 0, 0)},
                {
                        "$x = (bool) 1 ? 1 : 1.5; $x / $x;",
                        testStructs("(/ $x $x)", "\\.\\.", asList("int", "float", "falseType"), 1, 2, 0)
                },
                // %
                {"2 % 1;", testStructs("(% 2 1)", "\\.\\.", asList("int", "falseType"), 1, 0, 0)},
                //instanceof
                {"$x = 1; 1 instanceof $x;", testStructs("(instanceof 1 $x)", "\\.\\.", bool, 1, 2, 0)},
                {
                        //see TINS-389 scope of type in instanceof not set
                        "$x = 1; $x instanceof Exception;",
                        testStructs("(instanceof $x Exception)", "\\.\\.", bool, 1, 2, 0)
                },
                // casting
                {"(bool) 1;", testStructs("(casting (type tMod bool) 1)", "\\.\\.", bool, 1, 0, 0)},
                {"(int) 1;", testStructs("(casting (type tMod int) 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"(float) 1;", testStructs("(casting (type tMod float) 1)", "\\.\\.", asList("float"), 1, 0, 0)},
                {"(string) 1;", testStructs("(casting (type tMod string) 1)", "\\.\\.", asList("string"), 1, 0, 0)},
                {"(array) 1;", testStructs("(casting (type tMod array) 1)", "\\.\\.", asList("array"), 1, 0, 0)},
                // preIncr
                {"$x = false; ++$x;", testStructs("(preIncr $x)", "\\.\\.", asList("falseType"), 1, 2, 0)},
                {"$x = true; ++$x;", testStructs("(preIncr $x)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"$x = (bool) 1 ? false : true; ++$x;", testStructs("(preIncr $x)", "\\.\\.", bool, 1, 2, 0)},
                {"$x = 1; ++$x;", testStructs("(preIncr $x)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1.5; ++$x;", testStructs("(preIncr $x)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; ++$x;", testStructs("(preIncr $x)", "\\.\\.", num, 1, 2, 0)},
                //preDecr
                {"$x = false; --$x;", testStructs("(preDecr $x)", "\\.\\.", asList("falseType"), 1, 2, 0)},
                {"$x = true; --$x;", testStructs("(preDecr $x)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"$x = " + "(bool) 1 ? false : true; --$x;", testStructs("(preDecr $x)", "\\.\\.", bool, 1, 2, 0)},
                {"$x = 1; --$x;", testStructs("(preDecr $x)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1.5; --$x;", testStructs("(preDecr $x)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; --$x;", testStructs("(preDecr $x)", "\\.\\.", num, 1, 2, 0)},
                // postIncr
                {"$x = false; $x++;", testStructs("(postIncr $x)", "\\.\\.", asList("falseType"), 1, 2, 0)},
                {"$x = true; $x++;", testStructs("(postIncr $x)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"$x = (bool) 1 ? false : true; $x++;", testStructs("(postIncr $x)", "\\.\\.", bool, 1, 2, 0)},
                {"$x = 1; $x++;", testStructs("(postIncr $x)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1.5; $x++;", testStructs("(postIncr $x)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x++;", testStructs("(postIncr $x)", "\\.\\.", num, 1, 2, 0)},
                //postDecr
                {"$x = false; $x--;", testStructs("(postDecr $x)", "\\.\\.", asList("falseType"), 1, 2, 0)},
                {"$x = true; $x--;", testStructs("(postDecr $x)", "\\.\\.", asList("trueType"), 1, 2, 0)},
                {"$x = (bool) 1 ? false : true; $x--;", testStructs("(postDecr $x)", "\\.\\.", bool, 1, 2, 0)},
                {"$x = 1; $x--;", testStructs("(postDecr $x)", "\\.\\.", asList("int"), 1, 2, 0)},
                {"$x = 1.5; $x--;", testStructs("(postDecr $x)", "\\.\\.", asList("float"), 1, 2, 0)},
                {"$x = (bool) 1 ? 1 : 1.5; $x--;", testStructs("(postDecr $x)", "\\.\\.", num, 1, 2, 0)},
                //@
                {"@false;", testStructs("(@ false)", "\\.\\.", asList("falseType"), 1, 0, 0)},
                {"@true;", testStructs("(@ true)", "\\.\\.", asList("trueType"), 1, 0, 0)},
                {"@1;", testStructs("(@ 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"@1.3;", testStructs("(@ 1.3)", "\\.\\.", asList("float"), 1, 0, 0)},
                {"@'a';", testStructs("(@ 'a')", "\\.\\.", asList("string"), 1, 0, 0)},
                {"@[1];", testStructs("(@ (array 1))", "\\.\\.", asList("array"), 1, 0, 0)},
                //~
                {"~1;", testStructs("(~ 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"~'a';", testStructs("(~ 'a')", "\\.\\.", asList("string"), 1, 0, 0)},
                //uMinus
                {"-false;", testStructs("(uMinus false)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"-true;", testStructs("(uMinus true)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"-1;", testStructs("(uMinus 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"-1.5;", testStructs("(uMinus 1.5)", "\\.\\.", asList("float"), 1, 0, 0)},
                //uPlus
                {"+false;", testStructs("(uPlus false)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"+true;", testStructs("(uPlus true)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"+1;", testStructs("(uPlus 1)", "\\.\\.", asList("int"), 1, 0, 0)},
                {"+1.5;", testStructs("(uPlus 1.5)", "\\.\\.", asList("float"), 1, 0, 0)},
                //if
                {"$x = true; if($x){}", testStructs("$x", "\\.\\.", asList("trueType"), 1, 2, 0)},
                //while
                {"$x = true; while($x){}", testStructs("$x", "\\.\\.", asList("trueType"), 1, 2, 0)},
                //do
                {"$x = true; do{}while($x);", testStructs("$x", "\\.\\.", asList("trueType"), 1, 2, 1)},
                //for
                {"$x = true; for(;$x;){}", testStructs("$x", "\\.\\.", asList("trueType"), 1, 2, 1, 0)},
                {"$x = true; for(;1,$x;){}", testStructs("$x", "\\.\\.", asList("trueType"), 1, 2, 1, 1)},
                {"$x = true; for(;1,2,$x;){}", testStructs("$x", "\\.\\.", asList("trueType"), 1, 2, 1, 2)},
                //foreach
                {
                        "$x = []; foreach($x as $k => $v){}", new AbsoluteTypeNameTestStruct[]{
                        testStruct("$x", "\\.\\.", asList("array"), 1, 4, 0),
                        testStruct("$v", "\\.\\.", asList("mixed"), 1, 4, 1),
                        testStruct("$k", "\\.\\.", asList("int", "string"), 1, 4, 2)}
                },
                //switch
                {"$x = true; switch($x){}", testStructs("$x", "\\.\\.", asList("trueType"), 1, 2, 0)},
                //try/catch
                {
                        "$x = null; try{}catch(Exception $x){}",
                        testStructs("$x", "\\.\\.", asList("nullType", "Exception"), 1, 2, 1, 1)
                },
                //echo
                {"$x = 'h'; echo $x;", testStructs("$x", "\\.\\.", asList("string"), 1, 2, 0)},
                //exit
                {"$x = 1; exit($x);", testStructs("$x", "\\.\\.", asList("int"), 1, 2, 0, 0)},
                {"$x = 'h'; exit($x);", testStructs("$x", "\\.\\.", asList("string"), 1, 2, 0, 0)},
                //throw
                //TODO rstoll TINS-395 add new operator
//                {"$x = 1; throw $x;", testStructs("$x", "\\.\\.", asList("int"), asList("int"), 1, 2, 0)}
        });
    }
}
