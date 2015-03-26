/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ADefinitionScopeTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestStruct;
import org.junit.Assert;
import org.junit.Ignore;

@Ignore
public abstract class ADefinitionScopeTest extends ADefinitionTest
{

    protected ScopeTestStruct[] testStructs;

    public ADefinitionScopeTest(String testString, ScopeTestStruct[] theTestStructs) {
        super(testString);
        testStructs = theTestStructs;
    }

    @Override
    protected void checkNoIssuesInDefinitionPhase() {
        super.checkNoIssuesInDefinitionPhase();
        verifyDefinitions(testStructs, ast, testString);
    }

    public static void verifyDefinitions(ScopeTestStruct[] testStructs, ITSPHPAst ast, String testString) {
        int counter = 0;
        for (ScopeTestStruct testStruct : testStructs) {
            ITSPHPAst testCandidate = ScopeTestHelper.getAst(ast, testString, testStruct);
            Assert.assertNotNull(testString + " failed (testStruct Nr " + counter + "). testCandidate is null. should" +
                    " be " + testStruct.astText, testCandidate);
            Assert.assertEquals(testString + " failed (testStruct Nr " + counter + "). wrong ast text,",
                    testStruct.astText, testCandidate.toStringTree());

            Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                            "wrong scope,", testStruct.astScope,
                    ScopeTestHelper.getEnclosingScopeNames(testCandidate.getScope()));
            ++counter;
        }
    }
}
