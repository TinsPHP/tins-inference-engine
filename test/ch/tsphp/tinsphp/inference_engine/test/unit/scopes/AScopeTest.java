/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.scopes;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.scopes.AScope;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AScopeTest
{

    private class DummyScope extends AScope
    {
        public DummyScope(IScopeHelper theScopeHelper, String theScopeName, IScope theEnclosingScope) {
            super(theScopeHelper, theScopeName, theEnclosingScope);
        }

        @Override
        public void define(ISymbol sym) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean doubleDefinitionCheck(ISymbol symbol) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ISymbol resolve(ITSPHPAst typeAst) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isFullyInitialised(ISymbol symbol) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPartiallyInitialised(ISymbol symbol) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void getEnclosingScope_Standard_ReturnsScopePassedInConstructor() {
        IScope enclosingScope = mock(IScope.class);

        AScope scope = createScope(enclosingScope);
        IScope result = scope.getEnclosingScope();

        MatcherAssert.assertThat(result, CoreMatchers.is(enclosingScope));
    }

    @Test
    public void getScopeName_Standard_ReturnsNamePassedInConstructor() {
        String name = "foo";

        AScope scope = createScope(name);
        String result = scope.getScopeName();

        MatcherAssert.assertThat(result, CoreMatchers.is(name));
    }

    @Test
    public void getSymbols_NothingDefined_ReturnEmptyMap() {
        //no arrange needed

        AScope scope = createScope();
        Map<String, List<ISymbol>> result = scope.getSymbols();

        assertThat(result.size(), is(0));
    }

    @Test
    public void getInitialisedSymbols_NothingDefined_ReturnsEmptyMap() {
        //no arrange necessary

        AScope scope = createScope();
        Map<String, Boolean> result = scope.getInitialisedSymbols();

        MatcherAssert.assertThat(result.size(), CoreMatchers.is(0));
    }

    @Test
    public void getInitialisedSymbols_OnePartiallyDefined_ReturnsMapWithSymbolNameAndFalse() {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn("dummy");

        AScope scope = createScope();
        scope.addToInitialisedSymbols(symbol, false);
        Map<String, Boolean> result = scope.getInitialisedSymbols();

        MatcherAssert.assertThat(result, hasEntry("dummy", false));
    }

    @Test
    public void getInitialisedSymbols_TwoPartiallyDefined_ReturnsMapWithBothSymbolNameAndFalse() {
        ISymbol symbol1 = mock(ISymbol.class);
        when(symbol1.getName()).thenReturn("dummy1");
        ISymbol symbol2 = mock(ISymbol.class);
        when(symbol2.getName()).thenReturn("dummy2");

        AScope scope = createScope();
        scope.addToInitialisedSymbols(symbol1, false);
        scope.addToInitialisedSymbols(symbol2, false);
        Map<String, Boolean> result = scope.getInitialisedSymbols();

        MatcherAssert.assertThat(result, hasEntry("dummy1", false));
        MatcherAssert.assertThat(result, hasEntry("dummy2", false));
    }

    @Test
    public void getInitialisedSymbols_OnFullyDefined_ReturnsMapWithSymbolNameAndTrue() {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn("dummy");

        AScope scope = createScope();
        scope.addToInitialisedSymbols(symbol, true);
        Map<String, Boolean> result = scope.getInitialisedSymbols();

        MatcherAssert.assertThat(result, hasEntry("dummy", true));
    }

    @Test
    public void getInitialisedSymbols_TwoFullyDefined_ReturnsMapWithBothSymbolNameAndTrue() {
        ISymbol symbol1 = mock(ISymbol.class);
        when(symbol1.getName()).thenReturn("dummy1");
        ISymbol symbol2 = mock(ISymbol.class);
        when(symbol2.getName()).thenReturn("dummy2");

        AScope scope = createScope();
        scope.addToInitialisedSymbols(symbol1, true);
        scope.addToInitialisedSymbols(symbol2, true);
        Map<String, Boolean> result = scope.getInitialisedSymbols();

        MatcherAssert.assertThat(result, hasEntry("dummy1", true));
        MatcherAssert.assertThat(result, hasEntry("dummy2", true));
    }

    @Test
    public void getInitialisedSymbols_OnePartiallyOneFullyDefined_ReturnsMapWithBothAndCorrespondingState() {
        ISymbol symbol1 = mock(ISymbol.class);
        when(symbol1.getName()).thenReturn("dummy1");
        ISymbol symbol2 = mock(ISymbol.class);
        when(symbol2.getName()).thenReturn("dummy2");

        AScope scope = createScope();
        scope.addToInitialisedSymbols(symbol1, false);
        scope.addToInitialisedSymbols(symbol2, true);
        Map<String, Boolean> result = scope.getInitialisedSymbols();

        MatcherAssert.assertThat(result, hasEntry("dummy1", false));
        MatcherAssert.assertThat(result, hasEntry("dummy2", true));
    }

    @Test
    public void addToInitialisedSymbols_AlreadyInitialised_DoesNotThrowError() {
        ISymbol symbol1 = mock(ISymbol.class);
        when(symbol1.getName()).thenReturn("dummy");
        ISymbol symbol2 = mock(ISymbol.class);
        when(symbol2.getName()).thenReturn("dummy");

        AScope scope = createScope();
        scope.addToInitialisedSymbols(symbol1, false);
        scope.addToInitialisedSymbols(symbol2, false);

        //no assert necessary, just make sure no exetion is thrown
    }

    @Test
    public void addToInitialisedSymbols_UpdateFromPartiallyToFullyInitialised_R() {
        ISymbol symbol1 = mock(ISymbol.class);
        when(symbol1.getName()).thenReturn("dummy");
        ISymbol symbol2 = mock(ISymbol.class);
        when(symbol2.getName()).thenReturn("dummy");

        AScope scope = createScope();
        scope.addToInitialisedSymbols(symbol1, false);
        Map<String, Boolean> result = scope.getInitialisedSymbols();

        //assert
        MatcherAssert.assertThat(result, hasEntry("dummy", false));

        //act continued
        scope.addToInitialisedSymbols(symbol2, true);
        //assert 2
        MatcherAssert.assertThat(result, hasEntry("dummy", true));
    }

    protected AScope createScope() {
        return createScope(mock(IScope.class));
    }

    protected AScope createScope(IScope enclosingScope) {
        return createScope(mock(IScopeHelper.class), "foo", enclosingScope);
    }

    protected AScope createScope(String name) {
        return createScope(mock(IScopeHelper.class), name, mock(IScope.class));
    }

    protected AScope createScope(IScopeHelper scopeHelper, String name, IScope enclosingScope) {
        return new DummyScope(scopeHelper, name, enclosingScope);
    }
}
