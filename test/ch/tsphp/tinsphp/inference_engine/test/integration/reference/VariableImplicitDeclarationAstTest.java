/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.inference_engine.resolver.PutAtTopVariableDeclarationCreator;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceAstTest;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class VariableImplicitDeclarationAstTest extends AReferenceAstTest
{

    public VariableImplicitDeclarationAstTest(String testString, String theExpectedResult) {
        super(testString, theExpectedResult);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    protected IVariableDeclarationCreator createVariableDeclarationCreator(
            ISymbolFactory theSymbolFactory,
            IAstModificationHelper theAstModificationHelper,
            IDefinitionPhaseController theDefinitionPhaseController) {
        return new PutAtTopVariableDeclarationCreator(theSymbolFactory, theAstModificationHelper,
                theDefinitionPhaseController);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                {
                        "function foo(){echo 'hi'; $a=1; return;}",
                        "(namespace \\ (nBody (function fMod (type tMod ?) foo() params (block "
                                + "(vars (type tMod ?) $a) (echo 'hi') (expr (= $a 1)) return"
                                + "))))"
                }
        });
    }
}
