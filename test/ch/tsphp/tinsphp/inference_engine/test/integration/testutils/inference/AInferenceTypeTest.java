/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.Arrays;

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
            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                            "wrong scope",
                    testStruct.astScope, ScopeTestHelper.getEnclosingScopeNames(symbol.getDefinitionScope()));

            ITypeSymbol typeSymbol = symbol.getType();
            Assert.assertNotNull(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + ")." +
                    " " +
                    "typeSymbol was null", typeSymbol);
            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                    "wrong type scope", testStruct.absoluteTypeName, typeSymbol.getAbsoluteName());
            ++counter;
        }
    }

    protected static AbsoluteTypeNameTestStruct testStruct(String astText, String definitionScope,
            String absoluteTypeName, Integer... astAccessOrder) {
        return new AbsoluteTypeNameTestStruct(
                astText, definitionScope, Arrays.asList(astAccessOrder), absoluteTypeName);
    }

    protected static AbsoluteTypeNameTestStruct[] testStructs(
            String astText, String definitionScope, String absoluteTypeName, Integer... astAccessOrder) {
        return new AbsoluteTypeNameTestStruct[]{
                testStruct(astText, definitionScope, absoluteTypeName, astAccessOrder)
        };

    }
}
