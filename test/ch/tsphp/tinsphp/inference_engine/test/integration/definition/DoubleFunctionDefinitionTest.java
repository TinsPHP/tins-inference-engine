/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class DoubleFunctionDefinitionTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.definition;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ADoubleDefinitionTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class DoubleFunctionDefinitionTest extends ADoubleDefinitionTest
{

    public DoubleFunctionDefinitionTest(String testString, String theNamespace, String theIdentifier, int howMany) {
        super(testString, theNamespace, theIdentifier, howMany);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<Object[]> collection = new ArrayList<>();
        String a = "function a(){}";
        String a2 = "function a($a){}";
        String A = "function A(){}";
        collection.addAll(getDifferentNamespaces(a + "", "a()", 1));
        collection.addAll(getDifferentNamespaces(A + "", "A()", 1));
        collection.addAll(getDifferentNamespaces(a + " " + A + "", "A()", 1));
        collection.addAll(getDifferentNamespaces(a + " " + a + "", "a()", 2));
        collection.addAll(getDifferentNamespaces(a + " " + A + " " + a + "", "a()", 2));
        //doesn't matter if parameter list is different
        collection.addAll(getDifferentNamespaces(a + " " + a2 + " " + a + "", "a()", 3));
        return collection;
    }
}
