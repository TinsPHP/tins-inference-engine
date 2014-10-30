/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class GlobalNamespaceTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.definition;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ADefinitionTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class GlobalNamespaceTest extends ADefinitionTest
{

    private String[] namespaces;

    public GlobalNamespaceTest(String testString, String[] theNamespaces) {
        super(testString);
        namespaces = theNamespaces;
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Override
    protected void verifyDefinitions() {
        ILowerCaseStringMap<IGlobalNamespaceScope> globalNamespaceScopes =
                definitionPhaseController.getGlobalNamespaceScopes();
        assertThat(testString + " failed. size wrong ", globalNamespaceScopes.size(), equalTo(namespaces.length));

        for (String namespace : namespaces) {
            assertTrue(testString + " failed. Global namespace " + namespace + " did not exists in "
                    + globalNamespaceScopes.keySet(), globalNamespaceScopes.containsKey(namespace));
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                {"$a=1;", new String[]{"\\"}},
                {"namespace{}", new String[]{"\\"}},
                {"namespace{} namespace{}", new String[]{"\\"}},
                {"namespace{} namespace b{} namespace a\\b{}", new String[]{"\\", "\\b\\", "\\a\\b\\"}},
                {"namespace{} namespace{}  namespace a\\b{}", new String[]{"\\", "\\a\\b\\"}},
                {"namespace{} namespace b{} namespace{} ", new String[]{"\\", "\\b\\"}},
                {"namespace{} namespace{} namespace{} ", new String[]{"\\"}},
                //default space is always created since int, float etc. is defined in default namespace
                {"namespace a;", new String[]{"\\", "\\a\\"}}
        });
    }
}
