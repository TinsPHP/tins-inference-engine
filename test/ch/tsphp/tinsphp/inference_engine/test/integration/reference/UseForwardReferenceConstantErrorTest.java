/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class UseForwardReferenceConstantErrorTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.tinsphp.inference_engine.error.DefinitionErrorDto;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceDefinitionErrorTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class UseForwardReferenceConstantErrorTest extends AReferenceDefinitionErrorTest
{

    public UseForwardReferenceConstantErrorTest(String testString, DefinitionErrorDto[] expectedLinesAndPositions) {
        super(testString, expectedLinesAndPositions);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<Object[]> collection = new ArrayList<>();
        collection.addAll(getVariations("namespace{", "}"));
        collection.addAll(getVariations("namespace a{", "}"));
        collection.addAll(getVariations("namespace a\\b\\z{", "}"));
        return collection;
    }

    public static Collection<Object[]> getVariations(String prefix, String appendix) {
        List<Object[]> collection = new ArrayList<>();

        DefinitionErrorDto[] errorDto = new DefinitionErrorDto[]{new DefinitionErrorDto("B\\b#", 2, 1, "B", 3, 1)};
        DefinitionErrorDto[] twoErrorDto = new DefinitionErrorDto[]{
                new DefinitionErrorDto("B\\b#", 2, 1, "B", 4, 1),
                new DefinitionErrorDto("B\\b#", 3, 1, "B", 4, 1)
        };

        String[][] namespaces = new String[][]{
                {"B", "\\B"},
                {"A\\B", "A\\B"},
                {"A\\C\\B", "A\\C\\B"},
                {"A\\B", "\\A\\B"},
                {"A\\C\\B", "\\A\\C\\B"},
        };

        for (String[] namespace : namespaces) {

            String newAppendix = appendix + "namespace " + namespace[0] + "{ const b = 1;} ";
            String use = namespace[1];
            collection.addAll(Arrays.asList(new Object[][]{
                    {prefix + "echo \n B\\b; use \n " + use + ";" + newAppendix, errorDto},
                    {prefix + "echo \n B\\b; use " + use + " as \n B;" + newAppendix, errorDto},
                    //More than one
                    {prefix + "echo \n B\\b; echo \n B\\b; use \n " + use + ";" + newAppendix, twoErrorDto},
                    {prefix + "echo \n B\\b; echo \n B\\b; use " + use + " as \n B;" + newAppendix, twoErrorDto},
            }));
        }

        return collection;
    }
}
