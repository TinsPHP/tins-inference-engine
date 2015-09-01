/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.OverloadApplicationDto;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.inference_engine.resolver.PutAtTopVariableDeclarationCreator;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceOverloadTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.OverloadTestStruct;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.antlr.runtime.RecognitionException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;

import static ch.tsphp.tinsphp.common.TinsPHPConstants.RETURN_VARIABLE_NAME;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingCollectionMatcher.varBinding;
import static java.util.Arrays.asList;


@RunWith(Parameterized.class)
public class AppliedOverloadTest extends AInferenceOverloadTest
{

    public AppliedOverloadTest(String testString, OverloadTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    protected IVariableDeclarationCreator createVariableDeclarationCreator(
            ISymbolFactory theSymbolFactory,
            IAstModificationHelper theAstModificationHelper,
            IDefinitionPhaseController theDefinitionPhaseController) {
        return new PutAtTopVariableDeclarationCreator(
                theSymbolFactory, theAstModificationHelper, theDefinitionPhaseController);
    }

    @Override
    protected List<IFunctionType> getOverloads(int counter, OverloadTestStruct testStruct, ISymbol symbol) {
        IScope definitionScope = symbol.getDefinitionScope();
        Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                        "wrong scope",
                testStruct.astScope, ScopeTestHelper.getEnclosingScopeNames(definitionScope));

        IConstraintCollection collectionScope;
        if (definitionScope instanceof INamespaceScope) {
            collectionScope = definitionPhaseController.getGlobalDefaultNamespace();
        } else {
            collectionScope = (IConstraintCollection) definitionScope;
        }

        List<IBindingCollection> bindingCollections = collectionScope.getBindings();
        Assert.assertEquals(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                "too many or not enough bindingCollection", 1, bindingCollections.size());

        IBindingCollection bindingCollection = bindingCollections.get(0);

        Assert.assertTrue(testString + " -- " + testStruct.astText + " failed (testStruct Nr " + counter + "). " +
                        "no type variableId defined for " + symbol.getAbsoluteName(),
                bindingCollection.containsVariable(symbol.getAbsoluteName()));

        OverloadApplicationDto dto = bindingCollection.getAppliedOverload(symbol.getAbsoluteName());
        return asList(dto.overload);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return asList(new Object[][]{
                {
                        "$a = strpos('hello','h');",
                        testStructs("(fCall strpos() (args 'hello' 'h'))", "\\.\\.",
                                functionDtos("strpos", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("string"), true),
                                        varBinding("$rhs", "Trhs", null, asList("string"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "Treturn", asList("int", "falseType"), null,
                                                true)
                                )), 1, 1, 0, 1)
                },
                {
                        "1 + 1;",
                        testStructs("(+ 1 1)", "\\.\\.",
                                functionDtos("+", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("int"), true),
                                        varBinding("$rhs", "Trhs", null, asList("int"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "Treturn", asList("int"), null, true)
                                )), 1, 0, 0)
                },
                {
                        "[1] + [1,2];",
                        testStructs("(+ (array 1) (array 1 2))", "\\.\\.",
                                functionDtos("+", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("array"), true),
                                        varBinding("$rhs", "Trhs", null, asList("array"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "Treturn", asList("array"), null, true)
                                )), 1, 0, 0)
                },
                {
                        "namespace a{const a = 'hi';} namespace b{ echo \\a\\a;}",
                        testStructs("(echo \\a\\a#)", "\\b\\.\\b\\.",
                                functionDtos("echo", 1, bindingDtos(
                                        varBinding("$expr", "Texpr", null, asList("string"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "Treturn", asList("mixed"), null, true)
                                )), 1, 1, 0)
                },
                //overloads with convertible types
                {
                        "1 + 1.2;",
                        testStructs("(+ 1 1.2)", "\\.\\.",
                                functionDtos("+", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("{as T}"), true),
                                        varBinding("$rhs", "Trhs", null, asList("{as T}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("(float | int)"), false)
                                )), 1, 0, 0)
                },
                {
                        "1.2 + 1;",
                        testStructs("(+ 1.2 1)", "\\.\\.",
                                functionDtos("+", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("{as T}"), true),
                                        varBinding("$rhs", "Trhs", null, asList("{as T}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("(float | int)"), false)
                                )), 1, 0, 0)
                },
                {
                        "'1.2' + 1;",
                        testStructs("(+ '1.2' 1)", "\\.\\.",
                                functionDtos("+", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("{as T}"), true),
                                        varBinding("$rhs", "Trhs", null, asList("{as T}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("(float | int)"), false)
                                )), 1, 0, 0)
                },
                {
                        "'1' + 1.2;",
                        testStructs("(+ '1' 1.2)", "\\.\\.",
                                functionDtos("+", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("{as T}"), true),
                                        varBinding("$rhs", "Trhs", null, asList("{as T}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("(float | int)"), false)
                                )), 1, 0, 0)
                },
                {
                        "1 + '1.2';",
                        testStructs("(+ 1 '1.2')", "\\.\\.",
                                functionDtos("+", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("{as T}"), true),
                                        varBinding("$rhs", "Trhs", null, asList("{as T}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("(float | int)"), false)
                                )), 1, 0, 0)
                },
                {
                        "1.2 + '1';",
                        testStructs("(+ 1.2 '1')", "\\.\\.",
                                functionDtos("+", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("{as T}"), true),
                                        varBinding("$rhs", "Trhs", null, asList("{as T}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("(float | int)"), false)
                                )), 1, 0, 0)
                },
                {
                        "'1.2' + '1';",
                        testStructs("(+ '1.2' '1')", "\\.\\.",
                                functionDtos("+", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("{as T}"), true),
                                        varBinding("$rhs", "Trhs", null, asList("{as T}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("(float | int)"), false)
                                )), 1, 0, 0)
                },
                {
                        "true + null;",
                        testStructs("(+ true null)", "\\.\\.",
                                functionDtos("+", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("{as T}"), true),
                                        varBinding("$rhs", "Trhs", null, asList("{as T}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T", null, asList("(float | int)"), false)
                                )), 1, 0, 0)
                },
                {
                        "$x = 1 ? 1 : 1.5; $x / 1;",
                        testStructs("(/ $x 1)", "\\.\\.",
                                functionDtos("/", 2, bindingDtos(
                                        varBinding("$lhs", "Tlhs", null, asList("{as (float | int)}"), true),
                                        varBinding("$rhs", "Trhs", null, asList("{as (float | int)}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "Treturn",
                                                asList("falseType", "int", "float"), null, true)
                                )), 1, 2, 0)
                },
                {
                        "function foo9($x, $y, $z){return $x - $y - $z;}"
                                + "$c = foo9(1, 2, 3.5);",
                        testStructs("(fCall foo9() (args 1 2 3.5))", "\\.\\.",
                                functionDtos("foo9()", 3, bindingDtos(
                                        varBinding("foo9()$x", "V2", null, asList("{as T2}"), true),
                                        varBinding("foo9()$y", "V3", null, asList("{as T2}"), true),
                                        varBinding("foo9()$z", "V5", null, asList("{as T1}"), true),
                                        varBinding(RETURN_VARIABLE_NAME, "T1",
                                                asList("@T2"), asList("(float | int)"), false),
                                        varBinding("foo9()-@1|41", "T2", null, asList("@T1", "(float | int)"), false)
                                )), 1, 2, 0, 1)
                },
                //see TINS-680 solving dependencies is erroneous
                //TODO TINS-694 soft typing and pre-filtering
//                {
//                        "function force_balance_tags( $text) {\n"
//                                //in order that the function falls back to soft typing and hence has only one binding
//                                + "    $text = 1;"
//                                + "    $text = (string) myPregReplace('#<([0-9]{1})#', '&lt;$1', $text);\n"
//                                + "    $regex = [];\n"
//                                + "    while ( myPregMatch(\"/<(\\/?[\\w:]*)\\s*([^>]*)>/\", $text, $regex) ) {\n"
//                                + "    }\n"
//                                + "    return null;\n"
//                                + "}\n"
//                                + "\n"
//                                + "function myPregMatch($pattern, $subject, array $matches) {\n"
//                                + "    if ($pattern.$subject) {\n"
//                                + "        return 1;\n"
//                                + "    }\n"
//                                + "    return false;\n"
//                                + "}\n"
//                                + "\n"
//                                + "function myPregReplace($pattern, $replacement, $subject) {\n"
//                                + "    return str_replace($pattern, $replacement, $subject);\n"
//                                + "}",
//                        testStructs("(fCall myPregMatch() (args \"/<(\\/?[\\w:]*)\\s*([^>]*)>/\" $text $regex))",
//                                "\\.\\.force_balance_tags().", functionDtos("myPregMatch()", 3, bindingDtos(
//                                        varBinding("myPregMatch()$pattern", "V2", null, asList("{as string}"), true),
//                                        varBinding("myPregMatch()$subject", "V3", null, asList("{as string}"), true),
//                                        varBinding("myPregMatch()$matches", "V10", null, asList("mixed"), true),
//                                        varBinding(RETURN_VARIABLE_NAME, "V6", asList("falseType", "int"), null, true)
//                                )), 1, 0, 4, 4, 0)
//                }
        });
    }
}
