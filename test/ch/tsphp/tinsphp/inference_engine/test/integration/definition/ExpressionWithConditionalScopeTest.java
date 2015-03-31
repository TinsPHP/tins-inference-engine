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
public class ExpressionWithConditionalScopeTest extends ADefinitionScopeTest
{

    public ExpressionWithConditionalScopeTest(String testString, ScopeTestStruct[] theTestStructs) {
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

    public static Collection<Object[]> getVariations(
            String prefix, String appendix, String fullScopeName, Integer[] accessToScope) {
        Collection<Object[]> collection = new ArrayList<>();

        Integer[] stepIn = new Integer[]{};

        ScopeTestStruct[] scopeTestStructs = {
                new ScopeTestStruct("$a", fullScopeName,
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 0)),
                new ScopeTestStruct("$b", fullScopeName + "cScope.",
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 1))
        };


        collection.addAll(Arrays.asList(new Object[][]{
                {prefix + " $a or $b;" + appendix, scopeTestStructs},
                {prefix + " $a and $b;" + appendix, scopeTestStructs},
                {prefix + " $a || $b;" + appendix, scopeTestStructs},
                {prefix + " $a && $b;" + appendix, scopeTestStructs},
                {prefix + " $x ? $a : $b;" + appendix, new ScopeTestStruct[]{
                        new ScopeTestStruct("$x", fullScopeName,
                                ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 0)),
                        new ScopeTestStruct("$a", fullScopeName + "cScope.",
                                ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 1)),
                        new ScopeTestStruct("$b", fullScopeName + "cScope.",
                                ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 2)),
                }},
        }));

        ScopeTestStruct[] logicTestStruct = {
                new ScopeTestStruct("$b", fullScopeName + "cScope.",
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 1))
        };

        ScopeTestStruct[] binaryTestStruct = {
                new ScopeTestStruct("$a", fullScopeName + "cScope.",
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 1, 0)),
                new ScopeTestStruct("$b", fullScopeName + "cScope.",
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 1, 1)),
        };

        ScopeTestStruct[] unaryTestStruct = {
                new ScopeTestStruct("$b", fullScopeName + "cScope.",
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 1, 0))
        };

        ScopeTestStruct[] ternaryTestStruct = new ScopeTestStruct[]{
                new ScopeTestStruct("$x", fullScopeName + "cScope.",
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 1)),
                new ScopeTestStruct("$b", fullScopeName + "cScope.",
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 2)),
        };

        ScopeTestStruct[] ternaryBinaryTestStruct = new ScopeTestStruct[]{
                new ScopeTestStruct("$a", fullScopeName + "cScope.",
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 2, 0)),
                new ScopeTestStruct("$b", fullScopeName + "cScope.",
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 2, 1)),
        };

        ScopeTestStruct[] ternaryUnaryTestStruct = {
                new ScopeTestStruct("$b", fullScopeName + "cScope.",
                        ScopeTestHelper.getAstAccessOrder(accessToScope, stepIn, 0, 0, 2, 0))
        };

        Object[][] operators = new Object[][]{
                {"or", logicTestStruct, binaryTestStruct, unaryTestStruct},
                {"and", logicTestStruct, binaryTestStruct, unaryTestStruct},
                {"||", logicTestStruct, binaryTestStruct, unaryTestStruct},
                {"&&", logicTestStruct, binaryTestStruct, unaryTestStruct},
                {"? $x : ", ternaryTestStruct, ternaryBinaryTestStruct, ternaryUnaryTestStruct}
        };


        String[] binaryOperators = new String[]{
                "xor",
                "=", "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", ".=", "<<=", ">>=",
                "|", "^", "&",
                "==", "!=", "===", "!==",
                "<", "<=", ">", ">=",
                "<<", ">>",
                "+", "-", ".",
                "*", "/", "%",
                "instanceof",
        };

        String[] unaryOperators = new String[]{
                "++$b", "--$b", "@$b", "~$b", "!$b", "-$b", "+$b", "$b++", "$b--", "$b[0]",
        };

        String[] literals = new String[]{
                "null", "false", "true", "1", "1.3", "'hello'", "[1]"
        };


        for (Object[] op : operators) {
            //see TINS-364 logic operator and conditional scope
            // testing whether it succeeds after condition was a literal
            for (String literal : literals) {
                collection.add(new Object[]{prefix + " " + literal + " " + op[0] + " $b;" + appendix, op[1]});
            }

            //see TINS-364 logic operator and conditional scope
            //testing whether it succeeds when variable are further below in the AST
            for (String op2 : binaryOperators) {
                collection.add(new Object[]{
                        prefix + " true " + op[0] + "($a " + op2 + " $b);" + appendix, op[2]});
            }
            for (String op2 : unaryOperators) {
                collection.add(new Object[]{
                        prefix + " true " + op[0] + " " + op2 + ";" + appendix, op[3]});
            }
            collection.add(new Object[]{prefix + " true " + op[0] + " exit($b);" + appendix, op[3]});
        }

        return collection;
    }
}
