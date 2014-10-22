/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AScopeTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.scopes;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.inference_engine.scopes.AScope;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AScopeTest
{
    protected IScopeHelper scopeHelper;

    @Before
    public void setUp() {
        scopeHelper = mock(IScopeHelper.class);
    }

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
    public void getEnclosingScope_Standard_ReturnEnclosingScope() {
        IScope enclosingScope = mock(IScope.class);

        AScope scope = createScope("scope", enclosingScope);
        IScope result = scope.getEnclosingScope();

        assertThat(result, is(enclosingScope));
    }

    @Test
    public void getScopeName_Standard_ReturnName() {
        String name = "scope";

        AScope scope = createScope(name, mock(IScope.class));
        String result = scope.getScopeName();

        assertThat(result, is(name));
    }

    @Test
    public void getSymbols_NothingDefined_ReturnEmptyMap() {
        //no arrange needed

        AScope scope = createScope("scope", mock(IScope.class));
        Map<String, List<ISymbol>> result = scope.getSymbols();

        assertThat(result.size(), is(0));
    }

    protected AScope createScope(String name, IScope enclosingScope) {
        return new DummyScope(scopeHelper, name, enclosingScope);
    }
}
