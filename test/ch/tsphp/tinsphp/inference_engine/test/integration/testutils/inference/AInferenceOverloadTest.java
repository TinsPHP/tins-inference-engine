/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingMatcherDto;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.FunctionMatcherDto;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.FunctionTypeMatcher.isFunctionType;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

@Ignore
public class AInferenceOverloadTest extends AInferenceTest
{

    protected OverloadTestStruct[] testStructs;

    public AInferenceOverloadTest(String testString, OverloadTestStruct[] theTestStructs) {
        super(testString);
        testStructs = theTestStructs;
    }

    @Override
    protected void assertsInInferencePhase() {
        int counter = 0;
        for (OverloadTestStruct testStruct : testStructs) {
            ITSPHPAst testCandidate = ScopeTestHelper.getAst(ast, testString, testStruct);

            Assert.assertNotNull(testString + " failed. testCandidate is null. should be " + testStruct.astText,
                    testCandidate);
            Assert.assertEquals(testString + " failed. wrong ast text (testStruct Nr " + counter + ")",
                    testStruct.astText, testCandidate.toStringTree());

            ISymbol symbol = testCandidate.getSymbol();
            Assert.assertNotNull(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + ")." +
                    " symbol was null", symbol);

            Collection<IFunctionType> overloads = getOverloads(counter, testStruct, symbol);
            int size = testStruct.dtos.size();

            for (int i = 0; i < size; ++i) {
                try {
                    assertThat(overloads, hasItem(isFunctionType(testStruct.dtos.get(i))));
//                 assertThat(overloads.get(i).getParameters().size() + 1, is(testStruct.dtos.get(i).bindings.length));
                } catch (AssertionError ex) {
                    System.out.println(testString + " \n-- " + testStruct.astText
                            + " failed (testStruct Nr " + counter + "). Error for functionType " + i);
                    throw ex;
                }
            }

            if (size != overloads.size()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(testString).append(" -- ").append(testStruct.astText)
                        .append(" failed (testStruct Nr ").append(counter)
                        .append("). too many or not enough overloads.\nExpected: ").append(size).append(" actual: ")
                        .append(overloads.size()).append("\n").append("Expected overloads:");
                for (FunctionMatcherDto dto : testStruct.dtos) {
                    stringBuilder.append(isFunctionType(dto).toString());
                }
                stringBuilder.append("\nActual overloads:\n");
                for (IFunctionType overload : overloads) {
                    stringBuilder.append(overload.toString()).append("\n");
                }


                Assert.fail(stringBuilder.toString());
            }

            ++counter;
        }
    }

    protected Collection<IFunctionType> getOverloads(int counter, OverloadTestStruct testStruct, ISymbol symbol) {
        Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + ")." +
                "symbol is not a constraint collection", symbol instanceof IConstraintCollection);

        IMethodSymbol methodSymbol = (IMethodSymbol) symbol;
        return methodSymbol.getOverloads();
    }


    protected static List<FunctionMatcherDto> functionDtos(FunctionMatcherDto... dtos) {
        List<FunctionMatcherDto> list = new ArrayList<>(dtos.length);
        Collections.addAll(list, dtos);
        return list;
    }

    protected static List<FunctionMatcherDto> functionDtos(
            String name, int numberOfNonOptionalParameter, BindingMatcherDto... dtos) {
        return asList(functionDto(name, numberOfNonOptionalParameter, dtos));
    }

    protected static FunctionMatcherDto functionDto(
            String name, int numberOfNonOptionalParameter, BindingMatcherDto... dtos) {
        return new FunctionMatcherDto(name, numberOfNonOptionalParameter, dtos);
    }

    protected static BindingMatcherDto[] bindingDtos(BindingMatcherDto... dtos) {
        return dtos;
    }

    protected static OverloadTestStruct testStruct(
            String astText,
            String definitionScope,
            List<FunctionMatcherDto> matcherDtos,
            Integer... astAccessOrder) {
        return new OverloadTestStruct(
                astText, definitionScope, asList(astAccessOrder), matcherDtos);
    }

    protected static OverloadTestStruct[] testStructs(
            String astText,
            String definitionScope,
            List<FunctionMatcherDto> matcherDtos,
            Integer... astAccessOrder) {
        return new OverloadTestStruct[]{
                testStruct(astText, definitionScope, matcherDtos, astAccessOrder)
        };
    }
}
