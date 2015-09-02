/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.SignatureTestStruct;
import org.antlr.runtime.RecognitionException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;


@RunWith(Parameterized.class)
public class FunctionDefinitionMultipleOverloadTest extends AInferenceTest
{
    private SignatureTestStruct[] testStructs;

    public FunctionDefinitionMultipleOverloadTest(String testString, SignatureTestStruct[] theTestStructs) {
        super(testString);
        testStructs = theTestStructs;
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Override
    protected void assertsInInferencePhase() {
        int counter = 0;
        for (SignatureTestStruct testStruct : testStructs) {
            ITSPHPAst testCandidate = ScopeTestHelper.getAst(ast, testString, testStruct);

            Assert.assertNotNull(testString + " failed. testCandidate is null. should be " + testStruct.astText,
                    testCandidate);
            Assert.assertEquals(testString + " failed. wrong ast text (testStruct Nr " + counter + ")",
                    testStruct.astText, testCandidate.toStringTree());

            ISymbol symbol = testCandidate.getSymbol();
            Assert.assertNotNull(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + ")." +
                    " symbol was null", symbol);

            List<String> signatures = getSignatures(counter, testStruct, symbol);
            int size = testStruct.signatures.size();

            for (int i = 0; i < size; ++i) {
                try {
                    assertThat(signatures, hasItem(testStruct.signatures.get(i)));
                } catch (AssertionError ex) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(testString).append(" \n-- ").append(testStruct.astText)
                            .append(" failed (testStruct Nr ").append(counter).append("). ")
                            .append("Error for functionType ").append(i).append("\n")
                            .append("Expected (").append(size).append(")")
                            .append("and the following was not part of the actual overloads:\n")
                            .append(testStruct.signatures.get(i)).append("\n\n")
                            .append("Actual (").append(signatures.size()).append("):");
                    Collections.sort(signatures);
                    for (String signature : signatures) {
                        stringBuilder.append("\n").append(signature);
                    }
                    Assert.fail(stringBuilder.toString());
                }
            }

            if (size != signatures.size()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(testString).append(" -- ").append(testStruct.astText)
                        .append(" failed (testStruct Nr ").append(counter)
                        .append("). too many or not enough overloads.\nExpected: ").append(size).append(" actual: ")
                        .append(signatures.size()).append("\n").append("Expected overloads:");

                Collections.sort(testStruct.signatures);
                for (String signature : testStruct.signatures) {
                    stringBuilder.append("\n").append(signature);
                }
                stringBuilder.append("\nActual:");
                Collections.sort(signatures);
                for (String signature : signatures) {
                    stringBuilder.append("\n").append(signature);
                }
                Assert.fail(stringBuilder.toString());
            }

            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                    "too many or not enough overloads", size, signatures.size());

            ++counter;
        }
    }

    protected List<String> getSignatures(int counter, SignatureTestStruct testStruct, ISymbol symbol) {
        Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + ")." +
                "symbol is not a constraint collection", symbol instanceof IConstraintCollection);

        IMethodSymbol methodSymbol = (IMethodSymbol) symbol;
        List<String> signatures = new ArrayList<>();
        for (IFunctionType functionType : methodSymbol.getOverloads()) {
            signatures.add(functionType.getSignature());
        }
        return signatures;
    }

    protected static SignatureTestStruct testStruct(
            String astText,
            String definitionScope,
            List<String> signatures,
            Integer... astAccessOrder) {
        return new SignatureTestStruct(
                astText, definitionScope, asList(astAccessOrder), signatures);
    }

    protected static SignatureTestStruct[] testStructs(
            String astText,
            String definitionScope,
            List<String> signatures,
            Integer... astAccessOrder) {
        return new SignatureTestStruct[]{
                testStruct(astText, definitionScope, signatures, astAccessOrder)
        };
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        String bool = "(falseType | trueType)";
        String asBool = "{as " + bool + "}";
        return asList(new Object[][]{
                {
                        "function foo($x, $y, $z){if ($x) { return $y || $z;  } return $y && $z;}",
                        testStructs("foo()", "\\.\\.", asList(
                                bool + " x " + bool + " x " + bool + " -> " + bool,
//                                bool + " x " + bool + " x falseType -> " + bool,
//                                bool + " x " + bool + " x trueType -> " + bool,
//                                bool + " x falseType x " + bool + " -> " + bool,
//                                bool + " x falseType x falseType -> " + bool,
//                                bool + " x falseType x falseType -> falseType",
//                                bool + " x falseType x trueType -> " + bool,
//                                bool + " x trueType x " + bool + " -> " + bool,
//                                bool + " x trueType x falseType -> " + bool,
//                                bool + " x trueType x trueType -> " + bool,
//                                bool + " x trueType x trueType -> trueType",
//                                asBool + " x falseType x trueType -> " + bool,
//                                asBool + " x falseType x " + asBool + " -> " + bool,
//                                asBool + " x trueType x falseType -> " + bool,
//                                asBool + " x trueType x " + asBool + " -> " + bool,
//                                asBool + " x " + asBool + " x falseType -> " + bool,
//                                asBool + " x " + asBool + " x trueType -> " + bool,
                                asBool + " x " + asBool + " x " + asBool + " -> " + bool
                        ), 1, 0, 2)
                },
                {
                        "function foo($x, $y){return $x + $y;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "int x int -> int",
                                "float x float -> float",
                                "{as T} x {as T} -> T \\ T <: (float | int)",
                                "array x array -> array"
                        ), 1, 0, 2)
                },
                {
                        "function foo($x){return $x + 1;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "int -> int",
                                "{as T} -> T \\ int <: T <: (float | int)"
                        ), 1, 0, 2)
                },
                {
                        "function foo($x){return $x + 1.5;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "float -> float",
                                "{as T} -> T \\ float <: T <: (float | int)"
                        ), 1, 0, 2)
                },
                //TODO TINS-550 constrain type variables of convertible type
                {
                        "function foo($x){return $x + (1 ? 1: 1.3);}",
                        testStructs("foo()", "\\.\\.", asList(
                                "{as (float | int)} -> (float | int)"
                        ), 1, 0, 2)
                },
                {
                        "function foo($x){return $x + true;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "{as T} -> T \\ int <: T <: (float | int)"
                        ), 1, 0, 2)
                },
                {
                        "function foo($x, $y, $z){return $x + $y + $z;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "array x array x array -> array",
                                "float x float x float -> float",
                                "int x int x int -> int",
                                "{as T2} x {as T2} x {as T1} -> T1 \\ T2 <: T1 <: (float | int), T2 <: (float | int)"
                        ), 1, 0, 2)
                },
                {
                        "function foo($x, $y, $z){return $x - $y - $z;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "float x float x float -> float",
                                "int x int x int -> int",
                                "{as T2} x {as T2} x {as T1} -> T1 \\ T2 <: T1 <: (float | int), T2 <: (float | int)"
                        ), 1, 0, 2)
                },
                {
                        "function foo($x, $y){return $x / $y;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "float x float -> (falseType | float)",
                                "float x {as (float | int)} -> (falseType | float)",
                                "int x int -> (falseType | float | int)",
                                "{as (float | int)} x float -> (falseType | float)",
                                "{as (float | int)} x {as (float | int)} -> (falseType | float | int)"
                        ), 1, 0, 2)
                },
                //see TINS-568 convertible types sometimes not generic
                {
                        "function foo9A($x, $y){return $x + ($y + 1);}",
                        testStructs("foo9A()", "\\.\\.", asList(
                                "int x int -> int",
                                "{as T1} x {as T2} -> T1 "
                                        + "\\ (int | T2) <: T1 <: (float | int), int <: T2 <: (float | int)"
                        ), 1, 0, 2)
                },
                {
                        "function foo9B($x, $y){return $x + $y + 1;}",
                        testStructs("foo9B()", "\\.\\.", asList(
                                "int x int -> int",
                                "float x float -> float",
                                "{as T} x {as T} -> (int | T) \\ T <: (float | int)"
                        ), 1, 0, 2)
                },
                {
                        "function foo9($x, $y){return $x + 1 + $y + 2;}",
                        testStructs("foo9()", "\\.\\.", asList(
                                "int x int -> int",
                                "{as T2} x {as T1} -> T1 "
                                        + "\\ (int | T2) <: T1 <: (float | int), int <: T2 <: (float | int)"
                        ), 1, 0, 2)
                },
                //see TINS-568 convertible types sometimes not generic
                {
                        "function foo10($x, $y, $z){$x += 1; return $x + $y +$z; }",
                        testStructs("foo10()", "\\.\\.", asList(
                                "int x int x int -> int",
                                "{as T2} x {as T2} x {as T1} -> T1 "
                                        + "\\ (int | T2) <: T1 <: (float | int), int <: T2 <: (float | int)"

                        ), 1, 0, 2)
                },
                //see TINS-414 fixing types and erroneous bounds
                {
                        "function foo15($x, $y){ if(true){return $y /= $x;} return $x + 1;}",
                        testStructs("foo15()", "\\.\\.", asList(
                                "float x (falseType | float) -> (falseType | float | int)",
                                "float x T -> (falseType | float | int | T) "
                                        + "\\ (falseType | float) <: T <: {as (float | int)}",
                                "{as (float | int)} x T -> T \\ (falseType | float | int) <: T <: {as (float | int)}"
                        ), 1, 0, 2)
                },
                {
                        "function foo16($x){$a = $x & 1; echo $x; return $a;}",
                        testStructs("foo16()", "\\.\\.", asList(
                                "int -> int",
                                "((array | {as int}) & {as string}) -> int"
                        ), 1, 0, 2)
                },
                {
                        "const NODE_WIDTH = 51;\n"
                                + "const NODE_HEIGHT = 23;\n"
                                + "\n"
                                + "function myArrayPop(array $array) {\n"
                                + "    $count = myCount($array);\n"
                                + "    if($count > 0){\n"
                                + "        return $array[$count-1];\n"
                                + "    }\n"
                                + "    return null;\n"
                                + "}\n"
                                + "\n"
                                + "function myCount($x) {\n"
                                + "    return 1;\n"
                                + "}\n"
                                + "\n"
                                + "function myArraySearch($needle, array $haystack){\n"
                                + "    return $haystack[0];\n"
                                + "}\n"
                                + "\n"
                                + "function myArrayReverse(array $array){\n"
                                + "    return [1];\n"
                                + "}\n"
                                + "\n"
                                + "function myArrayKeyExists($key, array $array){\n"
                                + "    return true;\n"
                                + "}\n"
                                + "\n"
                                + "/***************** A* implementation, **********\n"
                                + " * found here http://granularreverb.com/a_star.php\n"
                                + " * And slightly adapted (by-ref is not yet supported) - some variables were " +
                                "forward reference usages and other bugs\n"
                                + " */\n"
                                + "\n"
                                + "// A* algorithm by aaz, found at\n"
                                + "// http://althenia.net/svn/stackoverflow/a-star.php?rev=7\n"
                                + "// Binary min-heap with element values stored separately\n"
                                + "\n"
                                + "//original code: function heap_float(&$heap, &$values, $i, $index) {\n"
                                + "function heap_float($heap, $values, $i, $index) {\n"
                                + "    $j = 0;\n"
                                + "    for (; $i; $i = $j) {\n"
                                + "        $j = ($i + $i%2)/2 - 1;\n"
                                + "        if ($values[$heap[$j]] < $values[$index])\n"
                                + "            break;\n"
                                + "        $heap[$i] = $heap[$j];\n"
                                + "    }\n"
                                + "    $heap[$i] = $index;\n"
                                + "    return null;\n"
                                + "}\n"
                                + "\n"
                                + "//original code: function heap_push(&$heap, &$values, $index) {\n"
                                + "function heap_push($heap, $values, $index) {\n"
                                + "    heap_float($heap, $values, myCount($heap), $index);\n"
                                + "    return null;\n"
                                + "}\n"
                                + "\n"
                                + "//original code: function heap_raise(&$heap, &$values, $index) {\n"
                                + "function heap_raise($heap, $values, $index) {\n"
                                + "    heap_float($heap, $values, myArraySearch($index, $heap), $index);\n"
                                + "    return null;\n"
                                + "}\n"
                                + "\n"
                                + "//original code: function heap_pop(&$heap, &$values) {\n"
                                + "function heap_pop($heap, $values) {\n"
                                + "    $front = $heap[0];\n"
                                + "    $index = myArrayPop($heap);\n"
                                + "    $n = myCount($heap);\n"
                                + "    if ($n) {\n"
                                + "        $j = 0;\n"
                                + "        for ($i = 0;; $i = $j) {\n"
                                + "            $j = $i*2 + 1;\n"
                                + "            if ($j >= $n)\n"
                                + "                break;\n"
                                + "            if ($j+1 < $n && $values[$heap[$j+1]] < $values[$heap[$j]])\n"
                                + "                $j += 1;\n"
                                + "            if ($values[$index] < $values[$heap[$j]])\n"
                                + "                break;\n"
                                + "            $heap[$i] = $heap[$j];\n"
                                + "        }\n"
                                + "        $heap[$i] = $index;\n"
                                + "    }\n"
                                + "    return $front;\n"
                                + "}\n"
                                + "\n"
                                + "\n"
                                + "// A-star algorithm:\n"
                                + "//   $start, $target - node indexes\n"
                                + "//   $neighbors($i)     - map of neighbor index => step cost\n"
                                + "//   $heuristic($i, $j) - minimum cost between $i and $j\n"
                                + "\n"
                                + "function a_star($start, $target, $map) {\n"
                                + "    $open_heap = array($start); // binary min-heap of indexes with values in $f\n"
                                + "    $open      = array($start => TRUE); // set of indexes\n"
                                + "    $closed    = array();               // set of indexes\n"
                                + "\n"
                                + "    $g = [];\n"
                                + "    $h = [];\n"
                                + "    $f = [];\n"
                                + "    $from = [];\n"
                                + "\n"
                                + "    $g[$start] = 0;\n"
                                + "    $h[$start] = heuristic($start, $target);\n"
                                + "    $f[$start] = $g[$start] + $h[$start];\n"
                                + "\n"
                                + "    while ($open) {\n"
                                + "        $i = heap_pop($open_heap, $f);\n"
                                + "        //not yet supported\n"
                                + "        //unset($open[$i]);\n"
                                + "        $open[$i] = null;\n"
                                + "        $closed[$i] = TRUE;\n"
                                + "\n"
                                + "        if ($i == $target) {\n"
                                + "            $path = array();\n"
                                + "            for (; $i != $start; $i = $from[$i])\n"
                                + "                $path[myCount($path)] = $i;\n"
                                + "            return myArrayReverse($path);\n"
                                + "        }\n"
                                + "\n"
                                + "        foreach (neighbors($i, $map) as $j => $step)\n"
                                + "            if (!myArrayKeyExists($j, $closed))\n"
                                + "                if (!myArrayKeyExists($j, $open) || $g[$i] + $step < $g[$j]) {\n"
                                + "                    $g[$j] = $g[$i] + $step;\n"
                                + "                    $h[$j] = heuristic($j, $target);\n"
                                + "                    $f[$j] = $g[$j] + $h[$j];\n"
                                + "                    $from[$j] = $i;\n"
                                + "\n"
                                + "                    if (!myArrayKeyExists($j, $open)) {\n"
                                + "                        $open[$j] = TRUE;\n"
                                + "                        heap_push($open_heap, $f, $j);\n"
                                + "                    } else\n"
                                + "                        heap_raise($open_heap, $f, $j);\n"
                                + "                }\n"
                                + "    }\n"
                                + "\n"
                                + "    return FALSE;\n"
                                + "}\n"
                                + "\n"
                                + "function node($x, $y) {\n"
                                + "    return $y * NODE_WIDTH + $x;\n"
                                + "}\n"
                                + "\n"
                                + "function coord($i) {\n"
                                + "    $x = $i % NODE_WIDTH;\n"
                                + "    $y = ($i - $x) / NODE_WIDTH;\n"
                                + "    return array($x, $y);\n"
                                + "}\n"
                                + "\n"
                                + "function neighbors($i, $map) {\n"
                                + "    $arr = coord($i);\n"
                                + "    $x = $arr[0];\n"
                                + "    $y = $arr[1];\n"
                                + "    $neighbors = array();\n"
                                + "    if ($x-1 >= 0      && $map[$y][$x-1] == ' ') $neighbors[node($x-1, $y)] = 1;\n"
                                + "    if ($x+1 < NODE_WIDTH  && $map[$y][$x+1] == ' ') $neighbors[node($x+1, " +
                                "$y)] = 1;\n"
                                + "    if ($y-1 >= 0      && $map[$y-1][$x] == ' ') $neighbors[node($x, $y-1)] = 1;\n"
                                + "    if ($y+1 < NODE_HEIGHT && $map[$y+1][$x] == ' ') $neighbors[node($x, " +
                                "$y+1)] = 1;\n"
                                + "    return $neighbors;\n"
                                + "}\n"
                                + "\n"
                                + "function heuristic($i, $j) {\n"
                                + "    $arr_i = coord($i);\n"
                                + "    $arr_j = coord($j);\n"
                                + "    return abs($arr_i[0] - $arr_j[0]) + abs($arr_i[1] - $arr_j[1]);\n"
                                + "}\n",
                        new SignatureTestStruct[]{
                                testStruct("myArrayPop()", "\\.\\.", asList("array -> mixed"), 1, 2, 2),
                                testStruct("myCount()", "\\.\\.", asList("mixed -> int"), 1, 3, 2),
                                testStruct("myArraySearch()", "\\.\\.", asList("mixed x array -> mixed"), 1, 4, 2),
                                testStruct("myArrayReverse()", "\\.\\.", asList("array -> array"), 1, 5, 2),
                                testStruct("myArrayKeyExists()", "\\.\\.",
                                        asList("mixed x array -> trueType"), 1, 6, 2),
                                testStruct("heap_float()", "\\.\\.",
                                        asList("array x array x ({as (falseType | trueType)} | {as (float | int)}) "
                                                + "x {as int} -> nullType"), 1, 7, 2),
                                testStruct("heap_push()", "\\.\\.",
                                        asList("array x array x {as int} -> nullType"), 1, 8, 2),
                                testStruct("heap_raise()", "\\.\\.",
                                        asList("array x array x {as int} -> nullType"), 1, 9, 2),
                                testStruct("heap_pop()", "\\.\\.", asList("array x array -> mixed"), 1, 10, 2),
                                testStruct("a_star()", "\\.\\.",
                                        asList("(((array | {as int}) & {as (float | int)}) | {as int}) x "
                                                + "((array | {as int}) & {as (float | int)}) "
                                                + "x array -> (array | falseType)"), 1, 11, 2),
                                testStruct("node()", "\\.\\.",
                                        asList(
                                                "int x int -> int",
                                                "float x float -> float",
                                                "{as T1} x {as T2} -> T1 "
                                                        + "\\ T2 <: T1 <: (float | int), T2 <: (float | int)"),
                                        1, 12, 2),

                                testStruct("coord()", "\\.\\.",
                                        asList("int -> array", "((array | {as int}) & {as (float | int)}) -> array"),
                                        1, 13, 2),

                                testStruct("neighbors()", "\\.\\.",
                                        asList("((array | {as int}) & {as (float | int)}) x array -> array"), 1, 14, 2),
                                testStruct("heuristic()", "\\.\\.",
                                        asList("((array | {as int}) & {as (float | int)}) x "
                                                + "((array | {as int}) & {as (float | int)}) -> (float | int)"),
                                        1, 15, 2),
                        }
                }
        });
    }
}
