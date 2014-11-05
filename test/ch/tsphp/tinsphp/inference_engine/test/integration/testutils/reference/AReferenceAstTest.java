/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference;

import org.junit.Assert;
import org.junit.Ignore;

@Ignore
public abstract class AReferenceAstTest extends AReferenceTest
{

    protected final String expectedResult;

    public AReferenceAstTest(String testString, String theExpectedResult) {
        super(testString);
        expectedResult = theExpectedResult;
    }

    @Override
    protected void verifyReferences() {
        if (ast != null) {
            Assert.assertEquals(testString + " failed.", expectedResult, ast.toStringTree());
        } else {
            Assert.assertNull(expectedResult);
        }
    }
}
