/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ParameterDoubleDefinitionErrorTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.tinsphp.common.issues.DefinitionIssueDto;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceDefinitionErrorTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class ParameterDoubleDefinitionErrorTest extends AReferenceDefinitionErrorTest
{

    private static List<Object[]> collection;

    public ParameterDoubleDefinitionErrorTest(String testString, DefinitionIssueDto[] expectedLinesAndPositions) {
        super(testString, expectedLinesAndPositions);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        collection = new ArrayList<>();

        addVariations("function foo(", "){return ;}");
        addVariations("namespace a; function foo(", "){return ;}");
        addVariations("namespace a\\b\\z{ function foo(", "){return ;}}");
        //TODO rstoll TINS-161 inference OOP
//        addVariations("class a{ function void foo(", "){return ;}}");
//        addVariations("namespace a; class b{function void foo(", "){return ;}}");
//        addVariations("namespace a\\b\\z{ class m{function void foo(", "){return ;}}}");
        return collection;
    }

    public static void addVariations(String prefix, String appendix) {
        DefinitionIssueDto[] errorDto = new DefinitionIssueDto[]{new DefinitionIssueDto("$a", 2, 1, "$a", 3, 1)};
        DefinitionIssueDto[] errorDtoTwo = new DefinitionIssueDto[]{
                new DefinitionIssueDto("$a", 2, 1, "$a", 3, 1),
                new DefinitionIssueDto("$a", 2, 1, "$a", 4, 1)
        };
        String[] types = new String[]{"array"};
        for (String type : types) {
            collection.add(new Object[]{
                    prefix + type + "\n $a, \n $a" + appendix,
                    errorDto
            });
            collection.add(new Object[]{
                    prefix + type + "\n $a, \\Exception \n $a=1, \\ErrorException\n $a=3" + appendix,
                    errorDtoTwo
            });
        }
    }
}
