/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.DescribedAs.describedAs;

@Ignore
public class AInferenceTypeTest extends AInferenceTest
{

    protected AbsoluteTypeNameTestStruct[] testStructs;

    public AInferenceTypeTest(String testString, AbsoluteTypeNameTestStruct[] theTestStructs) {
        super(testString);
        testStructs = theTestStructs;
    }

    @Override
    protected void assertsInInferencePhase() {
        int counter = 0;
        for (AbsoluteTypeNameTestStruct testStruct : testStructs) {
            ITSPHPAst testCandidate = ScopeTestHelper.getAst(ast, testString, testStruct);

            Assert.assertNotNull(testString + " failed. testCandidate is null. should be " + testStruct.astText,
                    testCandidate);
            Assert.assertEquals(testString + " failed. wrong ast text (testStruct Nr " + counter + ")",
                    testStruct.astText, testCandidate.toStringTree());

            ISymbol symbol = testCandidate.getSymbol();
            Assert.assertNotNull(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + ")." +
                    " symbol was null", symbol);

            IScope definitionScope = symbol.getDefinitionScope();
            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                            "wrong scope",
                    testStruct.astScope, ScopeTestHelper.getEnclosingScopeNames(definitionScope));

            IConstraintCollection collectionScope = getConstraintCollection(definitionScope);

            List<IOverloadBindings> overloadBindingsList = collectionScope.getBindings();
            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                    "too many or not enough overloadBindings", 1, overloadBindingsList.size());

            IOverloadBindings overloadBindings = overloadBindingsList.get(0);

            Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                            "no type variableId defined for " + symbol.getAbsoluteName(),
                    overloadBindings.containsVariable(symbol.getAbsoluteName()));

            ITypeVariableReference reference = overloadBindings.getTypeVariableReference(symbol.getAbsoluteName());

            Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                    " type was not fixed.", reference.hasFixedType());

            String typeVariable = reference.getTypeVariable();

            Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                            "no lower type bound defined",
                    overloadBindings.hasLowerTypeBounds(typeVariable));

            IUnionTypeSymbol lowerTypeBounds = overloadBindings.getLowerTypeBounds(typeVariable);
            assertThat(lowerTypeBounds.getTypeSymbols().keySet(), describedAs(testString + " -- " + testStruct
                            .astText + " failed " +
                            "(testStruct Nr " + counter + "). wrong lower types. \nExpected: " + testStruct.lowerTypes,
                    containsInAnyOrder(testStruct.lowerTypes.toArray())));

            ++counter;
        }
    }

    private IConstraintCollection getConstraintCollection(IScope definitionScope) {
        IScope scope = definitionScope;
        while (!(scope instanceof IConstraintCollection)) {
            scope = scope.getEnclosingScope();
        }
        return (IConstraintCollection) scope;
    }

    protected static AbsoluteTypeNameTestStruct testStruct(
            String astText,
            String definitionScope,
            List<String> lowerTypes,
            List<String> upperTypes,
            Integer... astAccessOrder) {
        return new AbsoluteTypeNameTestStruct(
                astText, definitionScope, Arrays.asList(astAccessOrder), lowerTypes, upperTypes);
    }

    protected static AbsoluteTypeNameTestStruct[] testStructs(
            String astText,
            String definitionScope,
            List<String> lowerTypes,
            List<String> upperTypes,
            Integer... astAccessOrder) {
        return new AbsoluteTypeNameTestStruct[]{
                testStruct(astText, definitionScope, lowerTypes, upperTypes, astAccessOrder)
        };

    }
}
