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
public class ProblematicExpressionTest extends AInferenceNamespaceTypeTest
{

    public ProblematicExpressionTest(String testString, AbsoluteTypeNameTestStruct[] theTestStructs) {
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
                //see TINS-410 super globals are not in binding
                {
                        "$_GET; $a = 1; //$a = 1; only because we do not have constraints in global otherwise",
                        testStructs("$_GET", "", asList("array"), 1, 1, 0)
                },
                {
                        "function foo(){$_GET; return 1;}",
                        testStructs("$_GET", "", asList("array"), 1, 0, 4, 0, 0)
                },
                {"$a = E_ALL;", testStructs("$a", "\\.\\.", asList("int"), 1, 1, 0, 0)},
                //predefined constants in a single statement also do not generate a constraint but should have a
                // predefined type
                {
                        "E_ALL; $a = 1; //$a = 1; only because we do not have constraints in global otherwise",
                        testStructs("E_ALL#", "", asList("int"), 1, 1, 0)
                },
                {
                        "function foo(){E_ALL; return 1;}",
                        testStructs("E_ALL#", "", asList("int"), 1, 0, 4, 0, 0)
                },
        });
    }
}
