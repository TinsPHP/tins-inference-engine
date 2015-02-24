/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class NamespaceErrorTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.definition;

import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ADefinitionTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class NamespaceErrorTest extends ADefinitionTest
{


    public NamespaceErrorTest(String testString) {
        super(testString);

    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Override
    protected void checkNoIssuesDuringParsing() {
        assertTrue(testString.replaceAll("\n", " ") + " failed - parser exception expected but non was thrown",
                parser.hasFound(EnumSet.allOf(EIssueSeverity.class)));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                //see TSPHP-736 - wrong syntax can mess up namespace scope generation which causes NullPointerException
                //switch without case"
                {"function void foo(){if(true){return;}else{ $a; switch($a){return;}}}"},
                //class in class
                {" class B{$ class A extends B{}"},
                //function in function
                {"function void foo(){$ function void bar(){}"},
        });
    }

    @Override
    protected void registerParserErrorLogger() {
        //no need to write parser errors to the console, we expect some
    }
}
