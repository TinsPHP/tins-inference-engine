/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AReferenceScopeTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import org.junit.Assert;
import org.junit.Ignore;

@Ignore
public abstract class AReferenceTypeScopeTest extends AReferenceTest
{

    protected TypeScopeTestStruct[] testStructs;

    public AReferenceTypeScopeTest(String testString, TypeScopeTestStruct[] theTestStructs) {
        super(testString);
        testStructs = theTestStructs;
    }

    @Override
    protected void assertsInReferencePhase() {
        verifyReferences(testStructs, ast, testString);
    }

    public static void verifyReferences(TypeScopeTestStruct[] typeScopeTestStructs, ITSPHPAst ast, String testString) {
        int counter = 0;
        for (TypeScopeTestStruct testStruct : typeScopeTestStructs) {
            ITSPHPAst testCandidate = ScopeTestHelper.getAst(ast, testString, testStruct);
            Assert.assertNotNull(testString + " failed (testStruct Nr " + counter + "). testCandidate is null. "
                            + "should be " + testStruct.astText,
                    testCandidate);
            Assert.assertEquals(testString + " failed (testStruct Nr " + counter + "). wrong ast text,",
                    testStruct.astText,
                    testCandidate.toStringTree());

            ISymbol symbol = testCandidate.getSymbol();
            Assert.assertNotNull(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + ")." +
                    " symbol was null", symbol);
            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                            "wrong scope,",
                    testStruct.astScope, ScopeTestHelper.getEnclosingScopeNames(symbol.getDefinitionScope()));

            ITypeSymbol typeSymbol = symbol.getType();
            if (testStruct.typeText != null) {
                Assert.assertNotNull(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter +
                        "). type was null", typeSymbol);
                Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter +
                                "). wrong type scope,",
                        testStruct.typeScope, ScopeTestHelper.getEnclosingScopeNames(typeSymbol.getDefinitionScope()));
                Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter +
                                "). wrong type text,",
                        testStruct.typeText, typeSymbol.getName());
            } else {
                Assert.assertNull(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + ")" +
                        ". type was not null", typeSymbol.evalSelf());
            }
            ++counter;
        }
    }
}
