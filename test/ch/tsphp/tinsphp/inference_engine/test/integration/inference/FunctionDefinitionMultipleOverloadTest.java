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
                }
        });
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
}
