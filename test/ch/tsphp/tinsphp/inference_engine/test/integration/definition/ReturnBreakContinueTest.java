/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class ReturnBreakContinueTest extends ADefinitionScopeTest
{

    public ReturnBreakContinueTest(String testString, ScopeTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<Object[]> collection = new ArrayList<>();

        collection.addAll(getVariations("", "", "\\.\\.", new Integer[]{1}));
        collection.addAll(getVariations("namespace{", "}", "\\.\\.", new Integer[]{1}));

        //nBody function block
        collection.addAll(getVariations("function foo(){", "}",
                "\\.\\.foo().", new Integer[]{1, 0, 4}));

        //TODO rstoll TINS-161 inference OOP
        //nBody class classBody mDecl block
//        collection.addAll(getVariations("class a{ function void foo(){", "}}",
//                "\\.\\.a.foo().", new Integer[]{1, 0, 4, 0, 4}));
        return collection;
    }

    public static Collection<Object[]> getVariations(String prefix, String appendix,
            String fullScopeName, Integer[] accessToScope) {
        Integer[] stepIn = new Integer[]{};
        return Arrays.asList(new Object[][]{
                {prefix + "return;" + appendix, new ScopeTestStruct[]{
                        new ScopeTestStruct("return", fullScopeName,
                                ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0)),
                }},
                {prefix + "return 1;" + appendix, new ScopeTestStruct[]{
                        new ScopeTestStruct("(return 1)", fullScopeName,
                                ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0)),
                }},
                {prefix + "break;" + appendix, new ScopeTestStruct[]{
                        new ScopeTestStruct("break", fullScopeName,
                                ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0)),
                }},
                {prefix + "break 1;" + appendix, new ScopeTestStruct[]{
                        new ScopeTestStruct("(break 1)", fullScopeName,
                                ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0)),
                }},
                {prefix + "continue;" + appendix, new ScopeTestStruct[]{
                        new ScopeTestStruct("continue", fullScopeName,
                                ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0)),
                }},
                {prefix + "continue 1;" + appendix, new ScopeTestStruct[]{
                        new ScopeTestStruct("(continue 1)", fullScopeName,
                                ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0)),
                }},
        });
    }
}
