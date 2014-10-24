/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ConstantDeclarationTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.definition;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ADefinitionSymbolTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ConstantHelper;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class ConstantDeclarationTest extends ADefinitionSymbolTest
{

    public ConstantDeclarationTest(String testString, String expectedResult) {
        super(testString, expectedResult);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<Object[]> collection = new ArrayList<>();

        collection.addAll(ConstantHelper.testStrings("", "", "", "\\.\\.", true));
        collection.addAll(ConstantHelper.testStrings("namespace a {", "}", "", "\\a\\.\\a\\.", true));
        collection.addAll(ConstantHelper.testStrings("namespace a\\b {", "}", "", "\\a\\b\\.\\a\\b\\.", true));

        //TODO rstoll TINS-161 inference OOP
//        SortedSet<Integer> modifiers = new TreeSet<>();
//        modifiers.add(TinsPHPDefinitionWalker.QuestionMark);
//        String none = ModifierHelper.getModifiersAsString(modifiers);
//        modifiers = new TreeSet<>();
//        modifiers.add(TinsPHPDefinitionWalker.Abstract);
//        modifiers.add(TinsPHPDefinitionWalker.QuestionMark);
//        String abstr = ModifierHelper.getModifiersAsString(modifiers);
//
//        //class constants
//        collection.addAll(ConstantHelper.testStrings(
//                "namespace a\\b\\c; class f{", "}", "\\a\\b\\c\\.\\a\\b\\c\\.f" + none + " ",
//                "\\a\\b\\c\\.\\a\\b\\c\\.f.", true));
//
//        //interface constants
//        collection.addAll(ConstantHelper.testStrings(
//                "namespace a\\b\\c; interface f{", "}",
//                "\\a\\b\\c\\.\\a\\b\\c\\.f" + abstr + " ",
//                "\\a\\b\\c\\.\\a\\b\\c\\.f.", true));

        return collection;
    }
}
