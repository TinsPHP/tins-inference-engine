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
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AInferenceTypeTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.AbsoluteTypeNameTestStruct;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static java.util.Arrays.asList;


@RunWith(Parameterized.class)
public class VariableDeclarationTest extends AInferenceTypeTest
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
                {"$a = null;", testStructs("$a", "\\.\\.", asList("null"), null, 1, 1, 0, 0)},
                {"$a = false;", testStructs("$a", "\\.\\.", asList("false"), null, 1, 1, 0, 0)},
                {"$a = true;", testStructs("$a", "\\.\\.", asList("true"), null, 1, 1, 0, 0)},
                {"$a = 1;", testStructs("$a", "\\.\\.", asList("int"), null, 1, 1, 0, 0)},
                {"$a = 1.4;", testStructs("$a", "\\.\\.", asList("float"), null, 1, 1, 0, 0)},
                {"$a = 'h';", testStructs("$a", "\\.\\.", asList("string"), null, 1, 1, 0, 0)},
                {
                        "$a = 1; $b = $a;", new AbsoluteTypeNameTestStruct[]{
                        testStruct("$a", "\\.\\.", asList("int"), null, 1, 2, 0, 0),
                        testStruct("$b", "\\.\\.", asList("int"), null, 1, 3, 0, 0)}
                },
                {
                        "$a = 1;\n $b = $a;\n $b = 1.2;\n $a = $b;", new AbsoluteTypeNameTestStruct[]{
                        testStruct("$a", "\\.\\.", asList("int", "float"), null, 1, 2, 0, 0),
                        testStruct("$b", "\\.\\.", asList("int", "float"), null, 1, 3, 0, 0),
                        testStruct("$b", "\\.\\.", asList("int", "float"), null, 1, 4, 0, 0),
                        testStruct("$a", "\\.\\.", asList("int", "float"), null, 1, 5, 0, 0)}
                },
        });
    }
}
