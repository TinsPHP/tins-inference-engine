/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ConstantDoubleDefinitionErrorTest from the TSPHP project.
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
public class ConstantDoubleDefinitionErrorTest extends AReferenceDefinitionErrorTest
{

    public ConstantDoubleDefinitionErrorTest(String testString, DefinitionErrorDto[] expectedLinesAndPositions) {
        super(testString, expectedLinesAndPositions);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<Object[]> collection = new ArrayList<>();
        DefinitionErrorDto[] errorDto = new DefinitionErrorDto[]{new DefinitionErrorDto("a#", 2, 1, "a#", 3, 1)};
        DefinitionErrorDto[] errorDtoTwo = new DefinitionErrorDto[]{
                new DefinitionErrorDto("a#", 2, 1, "a#", 3, 1),
                new DefinitionErrorDto("a#", 2, 1, "a#", 4, 1)
        };

        collection.addAll(Arrays.asList(new Object[][]{
                {"const \n a=1; const \n a=1;", errorDto},
                {"const \n a=1; const \n a=1; const \n a=1;", errorDtoTwo},
                //not in same namespace scope but in same global namespace scope
                {"namespace{const \n a=1;} namespace{ const \n a=1;}", errorDto},
                {"namespace a{const \n a=1;} namespace a{ const \n a=1;}", errorDto},
                {"namespace a\\b{const \n a=1;} namespace a\\b{ const \n a=1;}", errorDto},
                {
                        "namespace{const \n a=1;} namespace{const \n a=1;} namespace{const \n a=1;}",
                        errorDtoTwo
                },
                {
                        "namespace a{const \n a=1;} namespace a{const \n a=1;}  namespace a{const \n a=1;}",
                        errorDtoTwo
                },
                {
                        "namespace a\\b{const \n a=1;} namespace a\\b{const \n a=1;}"
                                + "namespace a\\b{const \n a=1;}",
                        errorDtoTwo
                },
                //does not matter if it is a comma initialisation
                {"const \n a=1,\n a=1;", errorDto},
                {"const \n a=1,\n a=1,\n a=2;", errorDtoTwo},
        }));

        return collection;
    }
}
