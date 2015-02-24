/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class UseTypeNameClashErrorTest from the TSPHP project.
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
public class UseTypeNameClashErrorTest extends AReferenceDefinitionErrorTest
{

    public UseTypeNameClashErrorTest(String testString, DefinitionIssueDto[] expectedLinesAndPositions) {
        super(testString, expectedLinesAndPositions);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<Object[]> collection = new ArrayList<>();
        //TODO rstoll TINS-161 inference OOP
//        DefinitionErrorDto[] errorDtos = new DefinitionErrorDto[]{new DefinitionErrorDto("z", 2, 1, "z", 3, 1)};
//        collection.addAll(Arrays.asList(new Object[][]{
//                {"namespace b; use \n b\\z; class \n z{} z $b;", errorDtos},
//                {"namespace b {use\n b\\z; class \n z{} z $b;}", errorDtos},
//                {"namespace b\\c {use\n b\\c\\z; class \n z{} z $b;}", errorDtos},
//                {"namespace b; use \n b\\z; interface \n z{} z $b;", errorDtos},
//                {"namespace b {use\n b\\z; interface \n z{} z $b;}", errorDtos},
//                {"namespace b\\c {use\n b\\c\\z; interface \n z{} z $b;}", errorDtos}
//        }));
//        errorDtos = new DefinitionErrorDto[]{new DefinitionErrorDto("Z", 2, 1, "z", 3, 1)};
//        collection.addAll(Arrays.asList(new Object[][]{
//                //case insensitive - see TSPHP-622
//                {"namespace b; use b\\z as\n Z; class \n z{} z $b;", errorDtos},
//                {"namespace b {use b\\z as\n Z; class \n z{} z $b;}", errorDtos},
//                {"namespace b; use b\\z as\n Z; interface \n z{} z $b;", errorDtos},
//                {"namespace b {use b\\z as\n Z; interface \n z{} z $b;}", errorDtos},
//        }));
        return collection;
    }
}
