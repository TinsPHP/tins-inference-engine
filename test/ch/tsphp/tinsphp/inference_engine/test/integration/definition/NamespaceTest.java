/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class NamespaceTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.definition;

import ch.tsphp.common.IScope;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestNamespaceScopeFactory;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ADefinitionTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class NamespaceTest extends ADefinitionTest
{

    private String namespaces;

    public NamespaceTest(String testString, String theNamespaces) {
        super(testString);
        namespaces = theNamespaces;
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
        Assert.assertEquals(testString + " failed.", namespaces, getNamespacesAsString());
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        String deflt = "\\.\\.";
        String b = "\\b\\.\\b\\.";
        String ab = "\\a\\b\\.\\a\\b\\.";

        return Arrays.asList(new Object[][]{
                {"$a=1;", deflt},
                {"namespace{}", deflt},
                {"namespace a\\b;", ab},
                {"namespace a\\b{}", ab},
                {"namespace{} namespace{}", deflt + " " + deflt},
                {"namespace b{} namespace b{}", b + " " + b},
                {"namespace{} namespace b{} namespace a\\b{}", deflt + " " + b + " " + ab},
                {"namespace{} namespace{}  namespace a\\b{}", deflt + " " + deflt + " " + ab},
                {"namespace{} namespace b{} namespace{} ", deflt + " " + b + " " + deflt},
                {"namespace{} namespace{} namespace{} ", deflt + " " + deflt + " " + deflt},
                {"namespace b{} namespace b{} namespace a\\b{} ", b + " " + b + " " + ab},
                {"namespace b{} namespace{} namespace b{} ", b + " " + deflt + " " + b},
                {"namespace b{} namespace b{} namespace b{} ", b + " " + b + " " + b}
        });
    }

    private String getNamespacesAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        boolean isNotFirst = false;
        TestNamespaceScopeFactory scopeFactory = (TestNamespaceScopeFactory) symbolsInitialiser.getScopeFactory();
        for (IScope scope : scopeFactory.scopes) {
            if (isNotFirst) {
                stringBuilder.append(" ");
            }
            isNotFirst = true;
            stringBuilder.append(ScopeTestHelper.getEnclosingScopeNames(scope));

        }
        return stringBuilder.toString();
    }
}
