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

import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ATest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.AstTestHelper;
import ch.tsphp.tinsphp.symbols.ScalarTypeSymbol;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;
import ch.tsphp.tinsphp.symbols.scopes.GlobalNamespaceScope;
import ch.tsphp.tinsphp.symbols.scopes.ScopeHelper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;


public class GlobalNamespaceTest extends ATest
{

    @Test
    public void testResolveLengthLessThanNamespace() {
        ISymbol symbol = new ScalarTypeSymbol("int", null, TokenTypes.Bool, "false");

        GlobalNamespaceScope globalNamespace = createGlobalNamespaceScope("\\a\\b\\c");
        globalNamespace.define(symbol);
        ISymbol result = globalNamespace.resolve(AstTestHelper.getAstWithTokenText("int"));

        assertThat(result, is(symbol));
    }

    @Test
    public void testResolveLengthEqualToNamespace() {
        ISymbol symbol = new ScalarTypeSymbol("float", null, TokenTypes.Float, "0.0");

        GlobalNamespaceScope globalNamespace = createGlobalNamespaceScope("\\a\\b\\");
        globalNamespace.define(symbol);
        ISymbol result = globalNamespace.resolve(AstTestHelper.getAstWithTokenText("float"));

        assertThat(result, is(symbol));
    }

    @Test
    public void testResolveLengthGreaterThanNamespace() {
        ISymbol symbol = new ScalarTypeSymbol("float", null, TokenTypes.Float, "0.0");

        GlobalNamespaceScope globalNamespace = createGlobalNamespaceScope("\\");
        globalNamespace.define(symbol);
        ISymbol result = globalNamespace.resolve(AstTestHelper.getAstWithTokenText("float"));

        assertThat(result, is(symbol));
    }

    @Test
    public void testResolveAbsolute() {
        ISymbol symbol = new ScalarTypeSymbol("float", null, TokenTypes.Float, "0.0");

        GlobalNamespaceScope globalNamespace = createGlobalNamespaceScope("\\a\\b\\");
        globalNamespace.define(symbol);
        ISymbol result = globalNamespace.resolve(AstTestHelper.getAstWithTokenText("\\a\\b\\float"));

        assertThat(result, is(symbol));
    }

    @Test
    public void testResolveNotFound() {
        ISymbol symbol = new ScalarTypeSymbol("float", null, TokenTypes.Float, "0.0");

        GlobalNamespaceScope globalNamespace = createGlobalNamespaceScope("\\a\\b\\");
        globalNamespace.define(symbol);

        ISymbol result = globalNamespace.resolve(AstTestHelper.getAstWithTokenText("float2"));

        assertThat(result, is(nullValue()));
    }

    private GlobalNamespaceScope createGlobalNamespaceScope(String scopeName) {
        return new GlobalNamespaceScope(new ScopeHelper(), scopeName);
    }

}
