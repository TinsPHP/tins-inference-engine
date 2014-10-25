/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ExpressionTest from the TSPHP project.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class ExpressionTest extends ADefinitionScopeTest
{

    public ExpressionTest(String testString, ScopeTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        List<Object[]> collection = new ArrayList<>();

        collection.addAll(ScopeTestHelper.testStringsDefaultNamespace());
        collection.addAll(ScopeTestHelper.testStrings("namespace a;", "", "\\a\\.\\a\\", new Integer[]{1}));
        collection.addAll(ScopeTestHelper.testStrings("namespace a\\b{", "}", "\\a\\b\\.\\a\\b\\", new Integer[]{1}));

        //for header does not build a conditional scope
        collection.addAll(Arrays.asList(new Object[][]{
                {"for($a=1;;){}", new ScopeTestStruct[]{
                        new ScopeTestStruct("$a", "\\.\\.", Arrays.asList(1, 0, 0, 0, 0))
                }},
                {"for($a=1, $b=1;;){}", new ScopeTestStruct[]{
                        new ScopeTestStruct("$a", "\\.\\.", Arrays.asList(1, 0, 0, 0, 0)),
                        new ScopeTestStruct("$b", "\\.\\.", Arrays.asList(1, 0, 0, 1, 0))
                }},
                {"for(;$a=1;){}", new ScopeTestStruct[]{
                        new ScopeTestStruct("$a", "\\.\\.", Arrays.asList(1, 0, 1, 0, 0)),
                }},
                {"for(;$a=1, $b=1;){}", new ScopeTestStruct[]{
                        new ScopeTestStruct("$a", "\\.\\.", Arrays.asList(1, 0, 1, 0, 0)),
                        new ScopeTestStruct("$b", "\\.\\.", Arrays.asList(1, 0, 1, 1, 0))
                }},
                {"for(;; $a=1){}", new ScopeTestStruct[]{
                        new ScopeTestStruct("$a", "\\.\\.", Arrays.asList(1, 0, 2, 0, 0)),
                }},
                {"for(;;$a=1, $b=1){}", new ScopeTestStruct[]{
                        new ScopeTestStruct("$a", "\\.\\.", Arrays.asList(1, 0, 2, 0, 0)),
                        new ScopeTestStruct("$b", "\\.\\.", Arrays.asList(1, 0, 2, 1, 0))
                }},
        }));

        //nBody function block
        collection.addAll(ScopeTestHelper.testStrings("function foo(){", "}", "\\.\\.foo()",
                new Integer[]{1, 0, 4}));

        //TODO rstoll TINS-161 inference OOP
        //nBody class classBody mDecl block
//        collection.addAll(ScopeTestHelper.testStrings("class a{ function void foo(){", "}}",
//                "\\.\\.a.foo()", new Integer[]{1, 0, 4, 0, 4}));

        return collection;
    }
}
