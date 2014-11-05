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
    protected void verifyReferences() {
        verifyReferences(testStructs, ast, testString);
    }

    public static void verifyReferences(TypeScopeTestStruct[] scopeTestStructs, ITSPHPAst ast, String testString) {
        for (TypeScopeTestStruct testStruct : scopeTestStructs) {
            ITSPHPAst testCandidate = ScopeTestHelper.getAst(ast, testString, testStruct);
            Assert.assertNotNull(testString + " failed. testCandidate is null. should be " + testStruct.astText,
                    testCandidate);
            Assert.assertEquals(testString + " failed. wrong ast text,", testStruct.astText,
                    testCandidate.toStringTree());

            ISymbol symbol = testCandidate.getSymbol();
            Assert.assertNotNull(testString + " -- " + testStruct.astText + " failed. symbol was null", symbol);
            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed. wrong scope,",
                    testStruct.astScope, ScopeTestHelper.getEnclosingScopeNames(symbol.getDefinitionScope()));

            ITypeSymbol typeSymbol = symbol.getType();
            Assert.assertNotNull(testString + " -- " + testStruct.astText + " failed. type was null", typeSymbol);
            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed. wrong type scope,",
                    testStruct.typeScope, ScopeTestHelper.getEnclosingScopeNames(typeSymbol.getDefinitionScope()));
            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed. wrong type text,",
                    testStruct.typeText, typeSymbol.getName());
        }
    }
}
