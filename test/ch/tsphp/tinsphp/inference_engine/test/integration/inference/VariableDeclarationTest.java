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
public class VariableDeclarationTest extends AInferenceNamespaceTypeTest
{

    public VariableDeclarationTest(String testString, AbsoluteTypeNameTestStruct[] theTestStructs) {
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
                {"$a = null;", testStructs("$a", "\\.\\.", asList("nullType"), 1, 1, 0, 0)},
                {"$a = false;", testStructs("$a", "\\.\\.", asList("falseType"), 1, 1, 0, 0)},
                {"$a = true;", testStructs("$a", "\\.\\.", asList("trueType"), 1, 1, 0, 0)},
                {"$a = 1;", testStructs("$a", "\\.\\.", asList("int"), 1, 1, 0, 0)},
                {"$a = 1.4;", testStructs("$a", "\\.\\.", asList("float"), 1, 1, 0, 0)},
                {"$a = 'h';", testStructs("$a", "\\.\\.", asList("string"), 1, 1, 0, 0)},
                {
                        "$a = 1; $b = $a;", new AbsoluteTypeNameTestStruct[]{
                        testStruct("$a", "\\.\\.", asList("int"), 1, 2, 0, 0),
                        testStruct("$b", "\\.\\.", asList("int"), 1, 3, 0, 0)}
                },
                {
                        "$a = 1;\n $b = $a;\n $b = 1.2;\n $a = $b;", new AbsoluteTypeNameTestStruct[]{
                        testStruct("$a", "\\.\\.", asList("int", "float"), 1, 2, 0, 0),
                        testStruct("$b", "\\.\\.", asList("int", "float"), 1, 3, 0, 0),
                        testStruct("$b", "\\.\\.", asList("int", "float"), 1, 4, 0, 0),
                        testStruct("$a", "\\.\\.", asList("int", "float"), 1, 5, 0, 0)}
                },
                //see TINS-278 $_GET is a type instead of a variable
                {"$a = $_GET;", testStructs("$a", "\\.\\.", asList("array"), 1, 1, 0, 0)},
                {"$a = $_GET + [2];", testStructs("$a", "\\.\\.", asList("array"), 1, 1, 0, 0)},
        });
    }
}
