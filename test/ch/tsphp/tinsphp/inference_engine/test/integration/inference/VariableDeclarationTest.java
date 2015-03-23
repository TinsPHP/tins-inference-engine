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

import java.util.Arrays;
import java.util.Collection;


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
        return Arrays.asList(new Object[][]{
                {"$a = null;", testStructs("$a", "\\.\\.", "null", 1, 1, 0, 0)},
                {"$a = false;", testStructs("$a", "\\.\\.", "false", 1, 1, 0, 0)},
                {"$a = true;", testStructs("$a", "\\.\\.", "true", 1, 1, 0, 0)},
                {"$a = 1;", testStructs("$a", "\\.\\.", "int", 1, 1, 0, 0)},
                {"$a = 1.4;", testStructs("$a", "\\.\\.", "float", 1, 1, 0, 0)},
                {"$a = 'h';", testStructs("$a", "\\.\\.", "string", 1, 1, 0, 0)},
                {
                        "$a = 1; $b = $a;", new AbsoluteTypeNameTestStruct[]{
                        testStruct("$a", "\\.\\.", "int", 1, 2, 0, 0),
                        testStruct("$b", "\\.\\.", "int", 1, 3, 0, 0)}
                },
                {
                        "$a = 1; $b = $a; $b = 1.2; $a = $b;", new AbsoluteTypeNameTestStruct[]{
                        testStruct("$a", "\\.\\.", "{int V float}", 1, 2, 0, 0),
                        testStruct("$b", "\\.\\.", "{int V float}", 1, 3, 0, 0),
                        testStruct("$b", "\\.\\.", "{int V float}", 1, 4, 0, 0),
                        testStruct("$a", "\\.\\.", "{int V float}", 1, 5, 0, 0)}
                },
        });
    }
}
