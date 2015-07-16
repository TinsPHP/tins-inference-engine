/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.symbols.IContainerTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.DescribedAs.describedAs;

@Ignore
public class AInferenceNamespaceTypeTest extends AInferenceTest
{

    protected AbsoluteTypeNameTestStruct[] testStructs;

    public AInferenceNamespaceTypeTest(String testString, AbsoluteTypeNameTestStruct[] theTestStructs) {
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

            IConstraintCollection collectionScope = getEnclosingMethodSymbol(testCandidate.getScope());
            if (collectionScope == null) {
                collectionScope = definitionPhaseController.getGlobalDefaultNamespace();
            }

            List<IBindingCollection> bindingCollections = collectionScope.getBindings();
            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                    "too many or not enough bindingCollection", 1, bindingCollections.size());

            IBindingCollection bindingCollection = bindingCollections.get(0);

            if (bindingCollection.containsVariable(symbol.getAbsoluteName())) {
                checkBinding(counter, testStruct, symbol, bindingCollection);
            } else {
                ITypeSymbol typeSymbol = symbol.getType();
                Assert.assertNotNull(testString + " -- " + testStruct.astText
                        + " failed (testStruct Nr " + counter + "). no type variableId defined for"
                        + symbol.getAbsoluteName() + "neither a predefined type specified!", typeSymbol);

                if (testStruct.types.size() > 1) {
                    Assert.assertTrue(testString + " -- " + testStruct.astText
                            + " failed (testStruct Nr " + counter + "). multiple types expected "
                            + "but type is not a container type", typeSymbol instanceof IContainerTypeSymbol);

                    IContainerTypeSymbol containerTypeSymbol = (IContainerTypeSymbol) typeSymbol;
                    Map<String, ITypeSymbol> typeSymbols = containerTypeSymbol.getTypeSymbols();
                    assertThat(typeSymbols.keySet(),
                            describedAs(testString + " -- " + testStruct.astText
                                            + " failed (testStruct Nr " + counter + "). wrong lower types.\n"
                                            + "Expected: " + testStruct.types,
                                    containsInAnyOrder(testStruct.types.toArray())));
                } else {
                    Assert.assertEquals(testString + " -- " + testStruct.astText
                                    + " failed (testStruct Nr " + counter + ")", testStruct.types.get(0),
                            typeSymbol.getAbsoluteName());
                }
            }

            ++counter;
        }
    }

    public static IMethodSymbol getEnclosingMethodSymbol(IScope definitionScope) {
        IScope scope = definitionScope;
        while (scope != null && !(scope instanceof IMethodSymbol)) {
            scope = scope.getEnclosingScope();
        }
        return (IMethodSymbol) scope;
    }

    private void checkBinding(
            int counter, AbsoluteTypeNameTestStruct testStruct, ISymbol symbol, IBindingCollection bindingCollection) {

        ITypeVariableReference reference = bindingCollection.getTypeVariableReference(symbol.getAbsoluteName());

        Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                " type was not fixed.", reference.hasFixedType());

        String typeVariable = reference.getTypeVariable();
        Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                        "no lower type bound defined",
                bindingCollection.hasLowerTypeBounds(typeVariable));

        IUnionTypeSymbol lowerTypeBounds = bindingCollection.getLowerTypeBounds(typeVariable);
        assertThat(lowerTypeBounds.getTypeSymbols().keySet(),
                describedAs(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). "
                                + "wrong lower types. \nExpected: " + testStruct.types,
                        containsInAnyOrder(testStruct.types.toArray())));

        IUnionTypeSymbol upperTypeBounds = bindingCollection.getLowerTypeBounds(typeVariable);
        assertThat(upperTypeBounds.getTypeSymbols().keySet(),
                describedAs(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). "
                                + "wrong lower types. \nExpected: " + testStruct.types,
                        containsInAnyOrder(testStruct.types.toArray())));
    }

    protected static AbsoluteTypeNameTestStruct testStruct(
            String astText,
            String definitionScope,
            List<String> lowerTypes,
            Integer... astAccessOrder) {
        return new AbsoluteTypeNameTestStruct(
                astText, definitionScope, Arrays.asList(astAccessOrder), lowerTypes);
    }

    protected static AbsoluteTypeNameTestStruct[] testStructs(
            String astText,
            String definitionScope,
            List<String> lowerTypes,
            Integer... astAccessOrder) {
        return new AbsoluteTypeNameTestStruct[]{
                testStruct(astText, definitionScope, lowerTypes, astAccessOrder)
        };

    }
}
