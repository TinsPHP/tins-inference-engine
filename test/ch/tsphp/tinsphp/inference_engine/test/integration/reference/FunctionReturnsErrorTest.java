/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class FunctionReturnsErrorTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.tinsphp.common.issues.ReferenceIssueDto;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ReturnCheckHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceErrorTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

@RunWith(Parameterized.class)
public class FunctionReturnsErrorTest extends AReferenceErrorTest
{

    public FunctionReturnsErrorTest(String testString, ReferenceIssueDto[] expectedLinesAndPositions) {
        super(testString, expectedLinesAndPositions);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return ReturnCheckHelper.getReferenceErrorPairs(
                "function \n foo(){", "}", new ReferenceIssueDto[]{new ReferenceIssueDto("foo()", 2, 1)});
    }
}
