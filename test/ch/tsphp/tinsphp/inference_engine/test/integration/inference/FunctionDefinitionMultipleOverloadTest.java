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
        return asList(new Object[][]{
                {
                        "function foo($x, $y){return $x + $y;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "int x int -> int",
                                "float x float -> float",
                                "float x {as (float | int)} -> float",
                                "{as (float | int)} x float -> float",
                                "{as T1} x {as T1} -> T1 \\ T1 <: (float | int)",
                                "array x array -> array"
                        ), 1, 0, 2)
                },
                {
                        "function foo($x){return $x + 1;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "int -> int",
                                "float -> float",
                                //TODO TINS-494 ambiguous overloads calculated
                                "float -> float",
                                "{as (float | int)} -> float",
                                "{as T1} -> T1 \\ int <: T1 <: (float | int)"
                        ), 1, 0, 2)
                },
                {
                        "function foo($x){return $x + 1.5;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "float -> float",
                                //TODO TINS-494 ambiguous overloads calculated
                                "float -> float",
                                "{as (float | int)} -> float",
                                "{as T1} -> T1 \\ float <: T1 <: (float | int)"
                        ), 1, 0, 2)
                },
                {
                        "function foo($x){return $x + ('1' + '1.5');}",
                        testStructs("foo()", "\\.\\.", asList(
                                "float -> float",
                                //TODO TINS-494 ambiguous overloads calculated
                                "float -> float",
                                "{as (float | int)} -> float",
                                "{as T4} -> T4 \\ float <: T4 <: (float | int)"
                        ), 1, 0, 2)
                },
                {
                        "function foo($x){return $x + true;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "float -> float",
                                "{as T1} -> T1 \\ int <: T1 <: (float | int)"
                        ), 1, 0, 2)
                },
                //TODO TINS-494 ambiguous overloads calculated
//                {
//                        "function foo($x, $y, $z){return $x + $y + $z;}",
//                        testStructs("foo()", "\\.\\.", asList(
//                                "int x int x int -> int",
//                                "int x int x float -> float",
//                                "int x int x {as T4} -> T4 \\ int <: T4 <: (float | int)",
//                                "float x float x float -> float",
//                                "float x float x {as T4} -> T4 \\ float <: T4 <: (float | int)",
//                                "{as int} x {as int} x int -> int",
//                                "{as float} x {as float} x float -> float",
//                                "{as (float | int)} x {as (float | int)} x {as T4} -> T4 \\ T4 <: (float | int)",
//                                "array x array x array -> array"
//                        ), 1, 0, 2)
//                },
//                {
//                        "function foo($x, $y, $a, $b){return $a * ($x + $y) - $a * $b;}",
//                        testStructs("foo()", "\\.\\.", asList(
//                                "int x int x int x int -> int",
//                                "int x int x int x {as int} -> int",
//                                "int x int x float x float -> float",
//                                "int x int x float x {as float} -> float",
//                                "int x int x int x int -> int",
//                                "int x int x float x float -> float",
//                                "int x int x {as int} x {as int} -> int",
//                                "float x float x float x float -> float",
//                                "float x float x float x {as float} -> float",
//                                "float x float x int x int -> float",
//                                "float x float x float x float -> float",
//                                "float x float x {as float} x {as float} -> float",
//                                "{as int} x {as int} x int x int -> int",
//                                "{as int} x {as int} x int x {as int} -> int",
//                                "{as float} x {as float} x float x float -> float",
//                                "{as float} x {as float} x float x {as float} -> float",
//                                "{as (float | int)} x {as (float | int)} x int x int -> int",
//                                "{as (float | int)} x {as (float | int)} x float x float -> float",
//                                "{as (float | int)} x {as (float | int)} x {as int} x {as int} -> int"
//                        ), 1, 0, 2)
//                },
                {
                        "function foo($x, $y){return $x / $y;}",
                        testStructs("foo()", "\\.\\.", asList(
                                "float x float -> (falseType | float)",
                                "float x {as float} -> (falseType | float)",
                                "{as float} x float -> (falseType | float)",
                                "{as (float | int)} x {as (float | int)} -> (falseType | float | int)"
                        ), 1, 0, 2)
                },
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
                            .append("Expected:\n").append(testStruct.signatures.get(i)).append("\n")
                            .append("But the following signatures were defined:");
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
                for (String signature : testStruct.signatures) {
                    stringBuilder.append("\n").append(signature);
                }
                stringBuilder.append("\nActual:");
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
