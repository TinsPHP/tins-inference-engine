/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.symbols;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.modifiers.IModifierSet;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.symbols.MethodSymbol;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodSymbolTest
{
    @Test
    public void getParameters_NonAdded_ReturnEmptyList() {
        //no arrange necessary

        IMethodSymbol methodSymbol = createMethodSymbol();
        List<IVariableSymbol> parameters = methodSymbol.getParameters();

        assertThat(parameters, is(empty()));
    }

    @Test
    public void getParameters_OneAdded_ReturnListWithIt() {
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);

        IMethodSymbol methodSymbol = createMethodSymbol();
        methodSymbol.addParameter(variableSymbol);
        List<IVariableSymbol> parameters = methodSymbol.getParameters();

        assertThat(parameters, contains(variableSymbol));
    }

    @Test
    public void getParameters_TwoAdded_ReturnListWithBoth() {
        IVariableSymbol variableSymbol1 = mock(IVariableSymbol.class);
        IVariableSymbol variableSymbol2 = mock(IVariableSymbol.class);

        IMethodSymbol methodSymbol = createMethodSymbol();
        methodSymbol.addParameter(variableSymbol1);
        methodSymbol.addParameter(variableSymbol2);
        List<IVariableSymbol> parameters = methodSymbol.getParameters();

        assertThat(parameters, contains(variableSymbol1, variableSymbol2));
    }

    @Test
    public void isFullyInitialised_NothingDefined_ReturnsFalse() {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn("foo");

        IMethodSymbol methodSymbol = createMethodSymbol();
        boolean result = methodSymbol.isFullyInitialised(symbol);

        assertThat(result, is(false));
    }

    @Test
    public void isFullyInitialised_PartiallyDefined_ReturnsFalse() {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn("foo");

        IMethodSymbol methodSymbol = createMethodSymbol();
        methodSymbol.addToInitialisedSymbols(symbol, false);
        boolean result = methodSymbol.isFullyInitialised(symbol);

        assertThat(result, is(false));
    }

    @Test
    public void isFullyInitialised_FullyDefined_ReturnsTrue() {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn("foo");

        IMethodSymbol methodSymbol = createMethodSymbol();
        methodSymbol.addToInitialisedSymbols(symbol, true);
        boolean result = methodSymbol.isFullyInitialised(symbol);

        assertThat(result, is(true));
    }

    @Test
    public void isPartiallyInitialised_NothingDefined_ReturnsFalse() {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn("foo");

        IMethodSymbol methodSymbol = createMethodSymbol();
        boolean result = methodSymbol.isPartiallyInitialised(symbol);

        assertThat(result, is(false));
    }

    @Test
    public void isPartiallyInitialised_PartiallyDefined_ReturnTrue() {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn("foo");

        IMethodSymbol methodSymbol = createMethodSymbol();
        methodSymbol.addToInitialisedSymbols(symbol, false);
        boolean result = methodSymbol.isPartiallyInitialised(symbol);

        assertThat(result, is(true));
    }

    @Test
    public void isPartiallyInitialised_FullyDefined_ReturnsFalse() {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn("foo");

        IMethodSymbol methodSymbol = createMethodSymbol();
        methodSymbol.addToInitialisedSymbols(symbol, true);
        boolean result = methodSymbol.isPartiallyInitialised(symbol);

        assertThat(result, is(false));
    }

    protected IMethodSymbol createMethodSymbol() {
        return new MethodSymbol(mock(IScopeHelper.class), mock(ITSPHPAst.class), mock(IModifierSet.class),
                mock(IModifierSet.class), "foo", mock(IScope.class));
    }
}
