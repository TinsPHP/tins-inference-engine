/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class VariableDefinitionConditionalScopesTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.definition;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TypeHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ADefinitionSymbolTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class VariableDefinitionConditionalScopesTest extends ADefinitionSymbolTest
{

    public VariableDefinitionConditionalScopesTest(String testString, String expectedResult) {
        super(testString, expectedResult);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        final List<Object[]> collection = new ArrayList<>();

        final String deflt = "\\.\\.";
        final String a = "\\a\\.\\a\\.";
        final String ab = "\\a\\b\\.\\a\\b\\.";

        //definition in foreach header
        collection.addAll(Arrays.asList(new Object[][]{
                {"foreach($a as $v){}", deflt + "cScope.? " + deflt + "cScope.$v"},
                {"namespace a{foreach($a as $v);}", a + "cScope.? " + a + "cScope.$v"},
                {"namespace a\\b{foreach($a as $v);}", ab + "cScope.? " + ab + "cScope.$v"},
                {
                        "foreach($a as $k => $v){}",
                        deflt + "cScope.? " + deflt + "cScope.$k " + deflt + "cScope.? " + deflt + "cScope.$v"
                },
                {
                        "namespace a{foreach($a as $k => $v);}",
                        a + "cScope.? " + a + "cScope.$k " + a + "cScope.? " + a + "cScope.$v"
                },
                {
                        "namespace a\\b{foreach($a as $k => $v);}",
                        ab + "cScope.? " + ab + "cScope.$k " + ab + "cScope.? " + ab + "cScope.$v"
                }
        }));

        //definition in catch header
        String[] types = TypeHelper.getClassInterfaceTypes();
        for (String type : types) {
            collection.addAll(Arrays.asList(new Object[][]{
                    {"try{}catch(" + type + " $e){}", deflt + type + " " + deflt + "$e"},
                    {"namespace a{ try{}catch(" + type + " $e){}}", a + type + " " + a + "$e"},
                    {"namespace a\\b{ try{}catch(" + type + " $e){}}", ab + type + " " + ab + "$e"}
            }));
        }
        return collection;
    }
}