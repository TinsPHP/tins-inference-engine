/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ReturnCheckHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceAstTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.EnumSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class FunctionReturnsAstTest extends AReferenceAstTest
{

    public FunctionReturnsAstTest(String testString, String theExpectedResult) {
        super(testString, theExpectedResult);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Override
    protected void checkReferences() {
        //we expect one exception since the function does not return in all cases
        assertTrue(testString + " failed. Exceptions expected but none occurred." + exceptions,
                inferenceErrorReporter.hasFound(EnumSet.allOf(EIssueSeverity.class)));
        assertFalse(testString + " failed. reference walker exceptions occurred.",
                reference.hasFound(EnumSet.allOf(EIssueSeverity.class)));

        verifyReferences();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return ReturnCheckHelper.getImplicitReturnAst(
                "function foo(){", "}",
                "(namespace \\ (nBody (function fMod (type tMod ?) foo() params (block ", "))))"
        );
    }
}
