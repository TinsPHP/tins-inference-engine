/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.inference_engine.resolver.PutAtTopVariableDeclarationCreator;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceNamespaceTypeTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AbsoluteTypeNameTestStruct;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static java.util.Arrays.asList;


@RunWith(Parameterized.class)
public class FunctionCallTest extends AInferenceNamespaceTypeTest
{

    public FunctionCallTest(String testString, AbsoluteTypeNameTestStruct[] theTestStructs) {
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

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return asList(new Object[][]{
                {"$a = strpos('hello','h');", testStructs("$a", "\\.\\.", asList("int", "falseType"), 1, 1, 0, 0)},
                //call a user defined function
                {
                        "function foo($x, $y){return bar($x, $y);}"
                                + "function bar($x, $y){return $x + $y;}"
                                + "$a = foo(1, 2);",
                        testStructs("$a", "\\.\\.", asList("int"), null, 1, 3, 0, 0)
                },
                //see TINS-463 multiple return and ConcurrentModificationException
                //function with two return calling twice user defined functions
                {
                        "function foo($x, $y, $z){ if($z){ return bar($x, $y);} return bar($x-1, $y); }" +
                                "function bar($x, $y){ return $x + $y; }"
                                + "$a = foo(1, 2, false);",
                        testStructs("$a", "\\.\\.", asList("int"), null, 1, 3, 0, 0)
                },
                //see TINS-463 multiple return and ConcurrentModificationException
                //function with multiple return constant and parameter
                {
                        ""
                                + "function foo($x, $y, $z){ "
                                + "  if ($x < 10) {"
                                + "    return 1;"
                                + "  } else if ($x > 10) {"
                                + "     return $y;"
                                + "  } else { "
                                + "     echo $z; return $z;"
                                + "  } "
                                + "}"
                                + "$a = foo(true, false, 'hello');"
                                + "$b = foo(true, 1.2, 'hello');",
                        new AbsoluteTypeNameTestStruct[]{
                                testStruct("$a", "\\.\\.", asList("int", "falseType", "string"), null, 1, 3, 0, 0),
                                testStruct("$b", "\\.\\.", asList("int", "float", "string"), null, 1, 4, 0, 0)
                        }
                },
                //see TINS-463 multiple return and ConcurrentModificationException
                //function with multiple return constant via local variables
                {
                        ""
                                + "function foo($x){ "
                                + "  if ($x < 10) {"
                                + "    $a = 'hello';"
                                + "    return $a;"
                                + "  } else if ($x > 10) {"
                                + "     $b = 1;"
                                + "     return $b;"
                                + "  } else { "
                                + "     $c = 1.2;"
                                + "     return $c;"
                                + "  } "
                                + "}"
                                + "$a = foo(true, false, 'hello');",
                        testStructs("$a", "\\.\\.", asList("string", "int", "float"), null, 1, 2, 0, 0)
                },
                //TODO rstoll TINS-494 ambiguous overloads calculated
                //see TINS-463 multiple return and ConcurrentModificationException
                //function with multiple return constant via local parameters
//                {
//                        ""
//                                + "function foo($x, $y, $z){ "
//                                + "  if ($x) {"
//                                + "    return $y || $z;"
//                                + "  }"
//                                + "  return $y && $z;"
//                                + "}"
//                                + "$a = foo(true, false, true);",
//                        testStructs("$a", "\\.\\.", asList("trueType", "falseType"), null, 1, 2, 0, 0)
//                },
                //see TINS-463 multiple return and ConcurrentModificationException
                //function with multiple return not constant via local parameters
                {
                        ""
                                + "function foo($x, $y, $z){ "
                                + "  if ($x) {"
                                + "    return $y + $z;"
                                + "  }"
                                + "  return $y - $z;"
                                + "}"
                                + "$a = foo(true, 1, 2);",
                        testStructs("$a", "\\.\\.", asList("int"), null, 1, 2, 0, 0)
                },
                //see TINS-463 multiple return and ConcurrentModificationException
                //function with multiple return non constant via user defined function
                {
                        "function bar($x){ return $x; return $x; }"
                                + "function foo($x){ "
                                + "  if ($x) {"
                                + "    return bar($x);"
                                + "  }"
                                + "  return bar($x);"
                                + "}"
                                + "$a = foo(true);",
                        testStructs("$a", "\\.\\.", asList("trueType"), null, 1, 3, 0, 0)
                },
                //see TINS-463 multiple return and ConcurrentModificationException
                //function with multiple return non constant via user defined function
                {
                        "function bar($x, $y){ if($x){return $x;}return $y;}"
                                + "function foo($x, $y, $z){ "
                                + "  if ($x) {"
                                + "    return bar($y, $z);"
                                + "  }"
                                + "  return bar($y, $z);"
                                + "}"
                                + "$a = foo(true, false, 2);",
                        testStructs("$a", "\\.\\.", asList("falseType","int"), null, 1, 3, 0, 0)
                },
        });
    }
}
