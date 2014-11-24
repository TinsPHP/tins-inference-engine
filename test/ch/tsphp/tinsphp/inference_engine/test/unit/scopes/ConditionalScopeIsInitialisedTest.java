/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ConditionalScopeIsInitialisedTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.scopes;

import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IConditionalScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.scopes.ConditionalScope;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ConditionalScopeIsInitialisedTest
{

    private IScopeHelper scopeHelper;

    @Before
    public void setUp() {
        scopeHelper = mock(IScopeHelper.class);
    }

    @Test
    public void isFullyInitialised_IsNotAtAllAndNamespaceScopeReturnTrue_DelegateToEnclosingScopeAndReturnTrue() {
        testIsFullyInitialised_IsNotInitialisedAtAll(mock(INamespaceScope.class), true);
    }

    @Test
    public void isFullyInitialised_IsNotAtAllAndNamespaceScopeReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
        testIsFullyInitialised_IsNotInitialisedAtAll(mock(INamespaceScope.class), false);
    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void isFullyInitialised_IsNotAtAllAndInMethodReturnTrue_DelegateToEnclosingScopeAndReturnTrue() {
//        testIsFullyInitialised_IsNotInitialisedAtAll(mock(IMethodSymbol.class), true);
//    }
//
//    @Test
//    public void isFullyInitialised_IsNotAtAllAndInMethodReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
//        testIsFullyInitialised_IsNotInitialisedAtAll(mock(IMethodSymbol.class), false);
//    }


    @Test
    public void isFullyInitialised_IsNotAtAllAndInConditionalScopeReturnTrue_DelegateToEnclosingScopeAndReturnTrue() {
        testIsFullyInitialised_IsNotInitialisedAtAll(mock(IConditionalScope.class), true);
    }

    @Test
    public void isFullyInitialised_IsNotAtAllAndInConditionalScopeReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
        testIsFullyInitialised_IsNotInitialisedAtAll(mock(IConditionalScope.class), false);
    }

    private void testIsFullyInitialised_IsNotInitialisedAtAll(IScope enclosingScope, boolean resultOfEnclosingScope) {
        ISymbol symbol = mock(ISymbol.class);
        when(enclosingScope.isFullyInitialised(symbol)).thenReturn(resultOfEnclosingScope);

        IConditionalScope conditionalScope = createConditionalScope(enclosingScope);
        boolean result = conditionalScope.isFullyInitialised(symbol);

        verify(enclosingScope).isFullyInitialised(symbol);
        assertThat(result, is(resultOfEnclosingScope));
    }

    @Test
    public void isFullyInitialised_IsPartiallyAndInNamespaceScopeReturnTrue_DelegateToEnclosingScopeAndReturnTrue() {
        testIsFullyInitialised_IsPartiallyInitialised(mock(INamespaceScope.class), true);
    }

    @Test
    public void isFullyInitialised_IsPartiallyAndInNamespaceScopeReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
        testIsFullyInitialised_IsPartiallyInitialised(mock(INamespaceScope.class), false);
    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void isFullyInitialised_IsPartiallyAndInMethodReturnTrue_DelegateToEnclosingScopeAndReturnTrue() {
//        testIsFullyInitialised_IsPartiallyInitialised(mock(IMethodSymbol.class), true);
//    }
//
//    @Test
//    public void isFullyInitialised_IsPartiallyAndInMethodReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
//        testIsFullyInitialised_IsPartiallyInitialised(mock(IMethodSymbol.class), false);
//    }

    @Test
    public void isFullyInitialised_IsPartiallyAndInConditionalReturnTrue_DelegateToEnclosingScopeAndReturnFalse() {
        testIsFullyInitialised_IsPartiallyInitialised(mock(IConditionalScope.class), true);
    }

    @Test
    public void isFullyInitialised_IsPartiallyAndInConditionalReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
        testIsFullyInitialised_IsPartiallyInitialised(mock(IConditionalScope.class), false);
    }

    private void testIsFullyInitialised_IsPartiallyInitialised(IScope enclosingScope, boolean resultOfEnclosingScope) {
        ISymbol symbol = createSymbol("$a");
        when(enclosingScope.isFullyInitialised(symbol)).thenReturn(resultOfEnclosingScope);

        IConditionalScope conditionalScope = createConditionalScope(enclosingScope);
        conditionalScope.addToInitialisedSymbols(symbol, false);
        boolean result = conditionalScope.isFullyInitialised(symbol);

        verify(enclosingScope).isFullyInitialised(symbol);
        assertThat(result, is(resultOfEnclosingScope));
    }

    @Test
    public void isFullyInitialised_IsFullyInitialisedInNamespaceScope_ReturnTrueDoesNotDelegateToEnclosingScope() {
        testIsFullyInitialised_IsFullyInitialised(mock(INamespaceScope.class));
    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void isFullyInitialised_IsFullyInitialisedInMethod_ReturnTrueDoesNotDelegateToEnclosingScope() {
//        testIsFullyInitialised_IsFullyInitialised(mock(IMethodSymbol.class));
//    }

    @Test
    public void isFullyInitialised_IsFullyInitialisedInConditionalScope_ReturnTrueDoesNotDelegateToEnclosingScope() {
        testIsFullyInitialised_IsFullyInitialised(mock(IConditionalScope.class));
    }

    private void testIsFullyInitialised_IsFullyInitialised(IScope enclosingScope) {
        ISymbol symbol = createSymbol("$a");

        IConditionalScope conditionalScope = createConditionalScope(enclosingScope);
        conditionalScope.addToInitialisedSymbols(symbol, true);
        boolean result = conditionalScope.isFullyInitialised(symbol);

        assertThat(result, is(true));
        verifyNoMoreInteractions(enclosingScope);
    }

    @Test
    public void isPartiallyInitialised_IsNotAtAllAndNamespaceScopeReturnTrue_DelegateToEnclosingScopeAndReturnTrue() {
        testIsPartiallyInitialised_IsNotInitialisedAtAll(mock(INamespaceScope.class), true);
    }

    @Test
    public void isPartiallyInitialised_IsNotAtAllAndNamespaceScopeReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
        testIsPartiallyInitialised_IsNotInitialisedAtAll(mock(INamespaceScope.class), false);
    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void isPartiallyInitialised_IsNotAtAllAndInMethodReturnTrue_DelegateToEnclosingScopeAndReturnTrue() {
//        testIsPartiallyInitialised_IsNotInitialisedAtAll(mock(IMethodSymbol.class), true);
//    }
//
//    @Test
//    public void isPartiallyInitialised_IsNotAtAllAndInMethodReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
//        testIsPartiallyInitialised_IsNotInitialisedAtAll(mock(IMethodSymbol.class), false);
//    }

    @Test
    public void
    isPartiallyInitialised_IsNotAtAllAndInConditionalScopeReturnTrue_DelegateToEnclosingScopeAndReturnTrue() {
        testIsPartiallyInitialised_IsNotInitialisedAtAll(mock(IConditionalScope.class), true);
    }

    @Test
    public void
    isPartiallyInitialised_IsNotAtAllAndInConditionalScopeReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
        testIsPartiallyInitialised_IsNotInitialisedAtAll(mock(IConditionalScope.class), false);
    }

    private void testIsPartiallyInitialised_IsNotInitialisedAtAll(IScope enclosingScope,
            boolean resultOfEnclosingScope) {
        ISymbol symbol = mock(ISymbol.class);
        when(enclosingScope.isPartiallyInitialised(symbol)).thenReturn(resultOfEnclosingScope);

        IConditionalScope conditionalScope = createConditionalScope(enclosingScope);
        boolean result = conditionalScope.isPartiallyInitialised(symbol);

        verify(enclosingScope).isPartiallyInitialised(symbol);
        assertThat(result, is(resultOfEnclosingScope));
    }

    @Test
    public void isPartiallyInitialised_IsFullyAndInNamespaceScopeReturnTrue_DelegateToEnclosingScopeAndReturnTrue() {
        testIsPartiallyInitialised_IsFullyInitialised(mock(INamespaceScope.class), true);
    }

    @Test
    public void isPartiallyInitialised_IsFullyAndInNamespaceScopeReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
        testIsPartiallyInitialised_IsFullyInitialised(mock(INamespaceScope.class), false);
    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void isPartiallyInitialised_IsFullyAndInMethodReturnTrue_DelegateToEnclosingScopeAndReturnTrue() {
//        testIsPartiallyInitialised_IsFullyInitialised(mock(IMethodSymbol.class), true);
//    }
//
//    @Test
//    public void isPartiallyInitialised_IsFullyAndInMethodReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
//        testIsPartiallyInitialised_IsFullyInitialised(mock(IMethodSymbol.class), false);
//    }

    @Test
    public void isPartiallyInitialised_IsFullyAndInConditionalReturnTrue_DelegateToEnclosingScopeAndReturnFalse() {
        testIsPartiallyInitialised_IsFullyInitialised(mock(IConditionalScope.class), true);
    }

    @Test
    public void isPartiallyInitialised_IsFullyAndInConditionalReturnFalse_DelegateToEnclosingScopeAndReturnFalse() {
        testIsPartiallyInitialised_IsFullyInitialised(mock(IConditionalScope.class), false);
    }

    private void testIsPartiallyInitialised_IsFullyInitialised(IScope enclosingScope, boolean resultOfEnclosingScope) {
        ISymbol symbol = createSymbol("$a");
        when(enclosingScope.isPartiallyInitialised(symbol)).thenReturn(resultOfEnclosingScope);

        IConditionalScope conditionalScope = createConditionalScope(enclosingScope);
        conditionalScope.addToInitialisedSymbols(symbol, true);
        boolean result = conditionalScope.isPartiallyInitialised(symbol);

        verify(enclosingScope).isPartiallyInitialised(symbol);
        assertThat(result, is(resultOfEnclosingScope));
    }

    @Test
    public void
    isPartiallyInitialised_IsPartiallyInitialisedInNamespaceScope_ReturnTrueDoesNotDelegateToEnclosingScope() {
        testIsPartiallyInitialised_IsPartiallyInitialised(mock(INamespaceScope.class));
    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void isPartiallyInitialised_IsPartiallyInitialisedInMethod_ReturnTrueDoesNotDelegateToEnclosingScope() {
//        testIsPartiallyInitialised_IsPartiallyInitialised(mock(IMethodSymbol.class));
//    }

    @Test
    public void
    isPartiallyInitialised_IsPartiallyInitialisedInConditionalScope_ReturnTrueDoesNotDelegateToEnclosingScope() {
        testIsPartiallyInitialised_IsPartiallyInitialised(mock(IConditionalScope.class));
    }

    private void testIsPartiallyInitialised_IsPartiallyInitialised(IScope enclosingScope) {
        ISymbol symbol = createSymbol("$a");

        IConditionalScope conditionalScope = createConditionalScope(enclosingScope);
        conditionalScope.addToInitialisedSymbols(symbol, false);
        boolean result = conditionalScope.isPartiallyInitialised(symbol);

        assertThat(result, is(true));
        verifyNoMoreInteractions(enclosingScope);
    }

    protected IConditionalScope createConditionalScope(IScope scope) {
        return new ConditionalScope(scopeHelper, scope);
    }

    private ISymbol createSymbol(String name) {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn(name);
        return symbol;
    }
}
