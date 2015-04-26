/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.AstTestHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.ATypeSystemTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class ResolvePrimitiveTypeTest extends ATypeSystemTest
{

    private String type;

    public ResolvePrimitiveTypeTest(String theType) {
        type = theType;
    }

    @Test
    public void testResolveExistingType_ReturnExisting() {
        INamespaceScope scope = definitionPhaseController.defineNamespace("\\");
        ITSPHPAst ast = AstTestHelper.getAstWithTokenText(type, scope);

        ITypeSymbol result = referencePhaseController.resolvePrimitiveType(ast, null);

        assertThat(result, is(core.getPrimitiveTypes().get(type)));
    }

    @Test
    public void testResolveExistingTypeFromOtherNamespace_ReturnExisting() {
        INamespaceScope scope = definitionPhaseController.defineNamespace("\\a\\a\\");
        ITSPHPAst ast = AstTestHelper.getAstWithTokenText(type, scope);

        ITypeSymbol result = referencePhaseController.resolvePrimitiveType(ast, null);

        assertThat(result, is(core.getPrimitiveTypes().get(type)));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                {"bool"},
                {"int"},
                {"float"},
                {"string"},
                {"array"}
        });
    }
}
