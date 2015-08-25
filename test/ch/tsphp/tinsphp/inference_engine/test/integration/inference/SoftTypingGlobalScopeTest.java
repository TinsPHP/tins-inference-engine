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
public class SoftTypingGlobalScopeTest extends AInferenceNamespaceTypeTest
{

    public SoftTypingGlobalScopeTest(String testString, AbsoluteTypeNameTestStruct[] theTestStructs) {
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
        //TODO TINS-666 soft typing erroneous for local/global variables
        //the typings below are not ideal as one can see
        return asList(new Object[][]{
                {
                        "$a = [1]; $a = 1.2; $b = $a + 1.5;",
                        testStructs("$b", "\\.\\.", asList("float", "int", "array"), 1, 4, 0, 0)
                },
                {
                        "$a = [1]; $a = 1.5; $b = $a + 1;",
                        testStructs("$b", "\\.\\.", asList("float", "int", "array"), 1, 4, 0, 0)
                },
                {
                        "$a = [1]; $a = 2; $b = $a + 1;",
                        testStructs("$b", "\\.\\.", asList("int", "float", "array"), 1, 4, 0, 0)
                },
                {
                        "$a = [1]; $a = '1'; $b = $a + 1;",
                        testStructs("$b", "\\.\\.", asList("float", "int", "array"), 1, 4, 0, 0)
                },
                {
                        "$a = [1]; $a = 1; $a = 1.2; $b = $a + 1;",
                        testStructs("$b", "\\.\\.", asList("float", "int", "array"), 1, 5, 0, 0)
                },
                {
                        "$a = [1]; $a = 1; $a = 1.2; $b = $a + 1.5;",
                        testStructs("$b", "\\.\\.", asList("float", "int", "array"), 1, 5, 0, 0)
                },
                {
                        "$a = false; $a = 1; $b = ~$a;",
                        testStructs("$b", "\\.\\.", asList("int", "string"), 1, 4, 0, 0)
                },
                {
                        "$a = true; $a = 1.5; $b = ~$a;",
                        testStructs("$b", "\\.\\.", asList("int", "string"), 1, 4, 0, 0)
                },
                {
                        "$a = null; $a = 'hello'; $b = ~$a;",
                        testStructs("$b", "\\.\\.", asList("string", "int"), 1, 4, 0, 0)
                },
                {
                        "$a = 1; $b = $a + 1; $a = 1.2;",
                        testStructs("$b", "\\.\\.", asList("int", "float", "array"), 1, 3, 0, 0)
                },
                {
                        "foreach(['h','e','l','l','o'] as $v){echo $v;}",
                        testStructs("$v", "\\.\\.", asList("mixed"), 1, 0, 1)
                },
                {
                        "$v = ''; foreach(['h','e','l','l','o'] as $v){echo $v;} $a = $v;",
                        new AbsoluteTypeNameTestStruct[]{
                                testStruct("$a", "\\.\\.", asList("mixed"), 1, 0, 1),
                                testStruct("$v", "\\.\\.", asList("mixed"), 1, 1, 1),
                        }
                },
        });
    }
}

