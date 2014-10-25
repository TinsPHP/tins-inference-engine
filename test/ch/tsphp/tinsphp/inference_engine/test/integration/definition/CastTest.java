/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class CastTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.definition;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestStruct;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ADefinitionScopeTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class CastTest extends ADefinitionScopeTest
{

    public CastTest(String testString, ScopeTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<Object[]> collection = new ArrayList<>();

        collection.addAll(ScopeTestHelper.getVariations("", "", "(bool) $a", "bool", "\\.\\.",
                new Integer[]{1}, new Integer[]{0, 1}));

        collection.addAll(ScopeTestHelper.getVariations("namespace{", "}", "(int) $a", "int", "\\.\\.",
                new Integer[]{1}, new Integer[]{0, 1}));

        collection.addAll(ScopeTestHelper.getVariations("namespace a;", "", "(float) $a", "float",
                "\\a\\.\\a\\.", new Integer[]{1}, new Integer[]{0, 1}));

        collection.addAll(ScopeTestHelper.getVariations("namespace a\\b;", "", "(string) $a", "string",
                "\\a\\b\\.\\a\\b\\.", new Integer[]{1}, new Integer[]{0, 1}));

        //nBody function block
        collection.addAll(ScopeTestHelper.getVariations("function foo(){", "}", "(string) $a", "string",
                "\\.\\.foo().", new Integer[]{1, 0, 4}, new Integer[]{0, 1}));

        //TODO rstoll TINS-161 inference OOP
        //nBody class classBody mDecl block
//        collection.addAll(ScopeTestHelper.getVariations("class a{ function void foo(){", "}}", "(bool) $a", "bool",
//                "\\.\\.a.foo().", new Integer[]{1, 0, 4, 0, 4}, new Integer[]{0, 1}));
        return collection;
    }
}
