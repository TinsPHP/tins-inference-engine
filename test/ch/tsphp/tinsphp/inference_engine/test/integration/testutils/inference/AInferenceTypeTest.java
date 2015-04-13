/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.TypeVariableConstraint;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import ch.tsphp.tinsphp.symbols.constraints.TypeConstraint;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

            List<IBinding> bindings = collectionScope.getBindings();
            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                    "too many or not enough bindings", 1, bindings.size());

            IBinding binding = bindings.get(0);
            Map<String, ITypeVariableConstraint> variable2TypeVariable = binding.getVariable2TypeVariable();
            Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                            "no type variableName defined for " + symbol.getAbsoluteName(),
                    variable2TypeVariable.containsKey(symbol.getAbsoluteName()));

            String typeVariable = variable2TypeVariable.get(symbol.getAbsoluteName()).getTypeVariable();
            ITypeVariableCollection typeVariables = binding.getTypeVariables();

            Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                            "no lower bound defined",
                    typeVariables.hasLowerBounds(typeVariable));

            Set<String> typeAbsoluteNames = new HashSet<>();
            getAbsoluteNames(typeAbsoluteNames, typeVariables, typeVariable, typeVariable);

            assertThat(typeAbsoluteNames, describedAs(testString + " -- " + testStruct.astText + " failed " +
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

    private void getAbsoluteNames(
            Set<String> typeAbsoluteNames, ITypeVariableCollection typeVariables, String originalVariable,
            String typeVariable) {
        for (IConstraint constraint : typeVariables.getLowerBounds(typeVariable)) {
            if (constraint instanceof TypeVariableConstraint) {
                String refTypeVariable = ((TypeVariableConstraint) constraint).getTypeVariable();
                if (!typeVariable.equals(refTypeVariable) && !originalVariable.equals(refTypeVariable)) {
                    getAbsoluteNames(typeAbsoluteNames, typeVariables, typeVariable, refTypeVariable);
                }
            } else {
                typeAbsoluteNames.add(((TypeConstraint) constraint).getTypeSymbol().getAbsoluteName());
            }
        }
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
