/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.coverage.reference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.StatementHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;


@RunWith(Parameterized.class)
public class BreakContinueStatementTest extends AReferenceTest
{

    @Override
    protected void verifyReferences() {
        //nothing to check, should just not cause an error
    }

    public BreakContinueStatementTest(String testString) {
        super(testString);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<Object[]> collection = StatementHelper.getStatements("break;", "");
        collection.addAll(StatementHelper.getStatements("continue;", ""));
        return collection;
    }

}
