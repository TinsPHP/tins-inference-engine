/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class FunctionReturnsTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ReturnCheckHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class FunctionReturnsTest extends AReferenceTest
{
    public FunctionReturnsTest(String testString) {
        super(testString);
    }

    @Override
    protected void verifyReferences() {
        //nothing to check, should just not cause an error
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<Object[]> collection = new ArrayList<>();
        collection.addAll(ReturnCheckHelper.getTestStringVariations("function foo(){", "}"));
        //same as "return null;"
        collection.add(new Object[]{"function foo(){return;}"});
        return collection;
    }
}
