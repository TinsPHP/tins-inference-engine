/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingMatcherDto;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.OverloadBindingsMatcher.withVariableBindings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

@Ignore
public class AInferenceBindingTest extends AInferenceTest
{

    protected BindingTestStruct[] testStructs;

    public AInferenceBindingTest(String testString, BindingTestStruct[] theTestStructs) {
        super(testString);
        testStructs = theTestStructs;
    }

    @Override
    protected void assertsInInferencePhase() {
        int counter = 0;
        for (BindingTestStruct testStruct : testStructs) {
            ITSPHPAst testCandidate = ScopeTestHelper.getAst(ast, testString, testStruct);

            Assert.assertNotNull(testString + " failed. testCandidate is null. should be " + testStruct.astText,
                    testCandidate);
            Assert.assertEquals(testString + " failed. wrong ast text (testStruct Nr " + counter + ")",
                    testStruct.astText, testCandidate.toStringTree());

            ISymbol symbol = testCandidate.getSymbol();
            Assert.assertNotNull(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + ")." +
                    " symbol was null", symbol);

            Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + ")." +
                    "symbol is not a constraint collection", symbol instanceof IConstraintCollection);

            IConstraintCollection collection = (IConstraintCollection) symbol;
            List<IOverloadBindings> bindings = collection.getBindings();
            int size = testStruct.dtos.size();

            for (int i = 0; i < size; ++i) {
                try {
                    assertThat(bindings, hasItem(withVariableBindings(
                            testStruct.dtos.get(i)
                    )));
                } catch (AssertionError ex) {
                    Assert.fail(testString + " -- " + testStruct.astText
                            + " failed (testStruct Nr " + counter + "). "
                            + "Binding error for overloadBindings " + i + "\n"
                            + ex.getMessage());
                }
            }

            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                    "too many or not enough overloadBindings", size, bindings.size());


            ++counter;
        }
    }

    protected static List<BindingMatcherDto[]> matcherDtos(BindingMatcherDto[]... dtos) {
        List<BindingMatcherDto[]> list = new ArrayList<>(dtos.length);
        Collections.addAll(list, dtos);
        return list;
    }

    protected static BindingMatcherDto[] matcherDto(BindingMatcherDto... dtos) {
        return dtos;
    }

    protected static BindingTestStruct testStruct(
            String astText,
            String definitionScope,
            List<BindingMatcherDto[]> matcherDtos,
            Integer... astAccessOrder) {
        return new BindingTestStruct(
                astText, definitionScope, Arrays.asList(astAccessOrder), matcherDtos);
    }

    protected static BindingTestStruct[] testStructs(
            String astText,
            String definitionScope,
            List<BindingMatcherDto[]> matcherDtos,
            Integer... astAccessOrder) {
        return new BindingTestStruct[]{
                testStruct(astText, definitionScope, matcherDtos, astAccessOrder)
        };

    }
}
