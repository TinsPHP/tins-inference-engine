/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.resolver;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolResolver;
import ch.tsphp.tinsphp.inference_engine.resolver.UserSymbolResolver;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserSymbolResolverTest
{

    @Test
    public void resolveIdentifier_Standard_DelegatesToScope() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        IScope scope = mock(IScope.class);
        when(scope.resolve(ast)).thenReturn(symbol);
        when(ast.getScope()).thenReturn(scope);

        ISymbolResolver symbolResolver = createSymbolResolver();
        ISymbol result = symbolResolver.resolveIdentifier(ast);

        verify(scope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveIdentifier_NonExistingSymbol_DelegatesToNextInChain() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        IScope scope = mock(IScope.class);
        when(scope.resolve(ast)).thenReturn(null);
        when(ast.getScope()).thenReturn(scope);

        ISymbolResolver symbolResolver = createSymbolResolver();
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveIdentifier(ast);

        verify(nextSymbolResolver).resolveIdentifier(ast);
    }


    @Test
    public void resolveIdentifier_NonExistingSymbolAndLastMemberOfTheChain_ReturnsNull() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        IScope scope = mock(IScope.class);
        when(scope.resolve(ast)).thenReturn(null);
        when(ast.getScope()).thenReturn(scope);

        ISymbolResolver symbolResolver = createSymbolResolver();
        ISymbol result = symbolResolver.resolveIdentifier(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveIdentifierWithFallback_FoundInNormal_DelegatesToScopeOnly() {
        IScope scope = mock(IScope.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getScope()).thenReturn(scope);
        ISymbol symbol = mock(ISymbol.class);
        when(scope.resolve(ast)).thenReturn(symbol);

        ISymbolResolver symbolResolver = createSymbolResolver();
        ISymbol result = symbolResolver.resolveIdentifierWithFallback(ast);

        verify(scope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveIdentifierWithFallback_FoundInFallback_DelegatesToScopeOnly() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        IScope scope = mock(IScope.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getScope()).thenReturn(scope);
        ISymbol symbol = mock(ISymbol.class);
        when(scope.resolve(ast)).thenReturn(null);
        IGlobalNamespaceScope globalDefaultNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(globalDefaultNamespaceScope.resolve(ast)).thenReturn(symbol);

        ISymbolResolver symbolResolver = createSymbolResolver(
                mock(IScopeHelper.class), mock(ILowerCaseStringMap.class), globalDefaultNamespaceScope);
        symbolResolver.setNextInChain(nextSymbolResolver);
        ISymbol result = symbolResolver.resolveIdentifierWithFallback(ast);

        verify(scope).resolve(ast);
        verify(globalDefaultNamespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveIdentifierWithFallback_NonExistingSymbol_DelegatesToNextInChainTwice() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        IScope scope = mock(IScope.class);
        when(scope.resolve(ast)).thenReturn(null);
        when(ast.getScope()).thenReturn(scope);

        ISymbolResolver symbolResolver = createSymbolResolver();
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveIdentifierWithFallback(ast);

        verify(nextSymbolResolver).resolveIdentifier(ast);
        verify(nextSymbolResolver).resolveIdentifierFromFallback(ast);
    }

    @Test
    public void resolveIdentifierWithFallback_NonExistingSymbolAndLastMemberOfTheChain_ReturnsNull() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        IScope scope = mock(IScope.class);
        when(scope.resolve(ast)).thenReturn(null);
        when(ast.getScope()).thenReturn(scope);

        ISymbolResolver symbolResolver = createSymbolResolver();
        ISymbol result = symbolResolver.resolveIdentifier(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveIdentifierFromFallback_Standard_DelegatesToGlobalDefaultNamespace() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        IGlobalNamespaceScope globalDefaultNamespaceScope = mock(IGlobalNamespaceScope.class);
        ISymbol symbol = mock(ISymbol.class);
        when(globalDefaultNamespaceScope.resolve(ast)).thenReturn(symbol);

        ISymbolResolver symbolResolver = createSymbolResolver(globalDefaultNamespaceScope);
        ISymbol result = symbolResolver.resolveIdentifierFromFallback(ast);

        verify(globalDefaultNamespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveIdentifierFromFallback_NonExistingSymbol_DelegatesToNextInChain() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        IGlobalNamespaceScope defaultNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(defaultNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(defaultNamespaceScope);
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveIdentifierFromFallback(ast);

        verify(nextSymbolResolver).resolveIdentifierFromFallback(ast);
    }

    @Test
    public void resolveIdentifierFromFallback_NonExistingSymbolAndLastMemberInChain_ReturnsNull() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        IGlobalNamespaceScope defaultNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(defaultNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(defaultNamespaceScope);
        ISymbol result = symbolResolver.resolveIdentifierFromFallback(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveGlobalIdentifier_AbsoluteIdentifier_DelegatesToGlobalNamespaceScope() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);
        ISymbol symbol = mock(ISymbol.class);
        when(globalNamespaceScope.resolve(ast)).thenReturn(symbol);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveGlobalIdentifier(ast);

        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveGlobalIdentifier_RelativeIdentifier_DelegatesToGlobalNamespaceScope() {
        String identifier = "name\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        IGlobalNamespaceScope enclosingGlobalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(namespaceScope.getEnclosingScope()).thenReturn(enclosingGlobalNamespaceScope);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);
        ISymbol symbol = mock(ISymbol.class);
        when(globalNamespaceScope.resolve(ast)).thenReturn(symbol);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveGlobalIdentifier(ast);

        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveGlobalIdentifier_RelativeIdentifier_PassesAbsoluteNameButDoesNotChangeAst() {
        String identifier = "name\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        IGlobalNamespaceScope enclosingGlobalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(namespaceScope.getEnclosingScope()).thenReturn(enclosingGlobalNamespaceScope);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(enclosingGlobalNamespaceScope.getScopeName()).thenReturn("\\a\\");
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);


        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.resolveGlobalIdentifier(ast);

        verify(ast).setText("\\a\\" + identifier);
        verify(ast).setText(identifier);
        verify(scopeHelper).getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), eq("\\a\\" + identifier));
    }

    @Test
    public void resolveGlobalIdentifier_NonExistingAbsoluteSymbol_DelegatesToNextInChain() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        String identifier = "\\nonExistingSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveGlobalIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(nextSymbolResolver).resolveGlobalIdentifier(ast);
    }

    @Test
    public void resolveGlobalIdentifier_NonExistingAbsoluteSymbolAndLastMemberInChain_ReturnsNull() {
        String identifier = "\\nonExistingSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveGlobalIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveGlobalIdentifier_NonExistingAbsoluteNamespaceSymbol_DelegatesToNextInChain() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        String identifier = "\\nonExistingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveGlobalIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(nextSymbolResolver).resolveGlobalIdentifier(ast);
    }

    @Test
    public void resolveGlobalIdentifier_NonExistingAbsoluteNamespaceSymbolAndLastInChain_ReturnsNull() {
        String identifier = "\\nonExistingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveGlobalIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveGlobalIdentifier_NonExistingRelativeSymbol_DelegatesToNextInChain() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        String identifier = "existingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(namespaceScope.getEnclosingScope()).thenReturn(globalNamespaceScope);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveGlobalIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(nextSymbolResolver).resolveGlobalIdentifier(ast);
    }


    @Test
    public void resolveGlobalIdentifier_NonExistingRelativeSymbolAndLastInChain_ReturnsNull() {
        String identifier = "existingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(namespaceScope.getEnclosingScope()).thenReturn(globalNamespaceScope);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveGlobalIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveGlobalIdentifier_NonExistingRelativeNamespaceSymbol_DelegatesToNextInChain() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        String identifier = "nonExistingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(namespaceScope.getEnclosingScope()).thenReturn(globalNamespaceScope);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveGlobalIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(nextSymbolResolver).resolveGlobalIdentifier(ast);
    }

    @Test
    public void resolveGlobalIdentifier_NonExistingRelativeNamespaceSymbolAdLastInChain_ResultsNull() {
        String identifier = "nonExistingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(namespaceScope.getEnclosingScope()).thenReturn(globalNamespaceScope);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveGlobalIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveGlobalIdentifier_LocalIdentifier_ThrowsIllegalArgumentException() {
        String identifier = "nonExistingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.resolveGlobalIdentifier(ast);

        //assert in annotation
    }

    private ITSPHPAst createAst(String name) {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getText()).thenReturn(name);
        return ast;
    }


    protected ISymbolResolver createSymbolResolver() {
        return createSymbolResolver(mock(IScopeHelper.class));
    }

    protected ISymbolResolver createSymbolResolver(IScopeHelper scopeHelper) {
        return createSymbolResolver(scopeHelper, mock(ILowerCaseStringMap.class), mock(IGlobalNamespaceScope.class));
    }

    private ISymbolResolver createSymbolResolver(IGlobalNamespaceScope defaultNamespaceScope) {
        return createSymbolResolver(mock(IScopeHelper.class), mock(ILowerCaseStringMap.class), defaultNamespaceScope);
    }

    protected ISymbolResolver createSymbolResolver(
            IScopeHelper theScopeHelper,
            ILowerCaseStringMap<IGlobalNamespaceScope> theGlobalNamespaceScopes,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        return new UserSymbolResolver(theScopeHelper, theGlobalNamespaceScopes, theGlobalDefaultNamespace);
    }
}
