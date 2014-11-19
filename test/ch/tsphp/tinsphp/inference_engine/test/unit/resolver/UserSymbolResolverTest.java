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
import ch.tsphp.tinsphp.common.symbols.resolver.ISymbolResolver;
import ch.tsphp.tinsphp.inference_engine.resolver.UserSymbolResolver;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class UserSymbolResolverTest
{

    @Test
    public void resolveIdentifierFromItsScope_Standard_DelegatesToScope() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        IScope scope = mock(IScope.class);
        when(scope.resolve(ast)).thenReturn(symbol);
        when(ast.getScope()).thenReturn(scope);

        ISymbolResolver symbolResolver = createSymbolResolver();
        ISymbol result = symbolResolver.resolveIdentifierFromItsScope(ast);

        verify(scope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveIdentifierFromItsScope_NonExistingSymbol_ReturnsNull() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        IScope scope = mock(IScope.class);
        when(scope.resolve(ast)).thenReturn(null);
        when(ast.getScope()).thenReturn(scope);

        ISymbolResolver symbolResolver = createSymbolResolver();
        ISymbol result = symbolResolver.resolveIdentifierFromItsScope(ast);

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
    public void resolveIdentifierFromFallback_NonExistingSymbol_ReturnsNull() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        IGlobalNamespaceScope defaultNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(defaultNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(defaultNamespaceScope);
        ISymbol result = symbolResolver.resolveIdentifierFromFallback(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveAbsoluteIdentifier_Standard_DelegatesToGlobalNamespaceScope() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);
        ISymbol symbol = mock(ISymbol.class);
        when(globalNamespaceScope.resolve(ast)).thenReturn(symbol);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveAbsoluteIdentifier(ast);

        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveAbsoluteIdentifier_NonExistingAbsoluteSymbol_ReturnsNull() {
        String identifier = "\\nonExistingSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveAbsoluteIdentifier(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveAbsoluteIdentifier_NonExistingAbsoluteNamespaceSymbol_ReturnsNull() {
        String identifier = "\\nonExistingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveAbsoluteIdentifier(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveIdentifierFromItsNamespaceScope_Standard_DelegatesToNamespaceScope() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        ISymbol symbol = mock(ISymbol.class);
        when(namespaceScope.resolve(ast)).thenReturn(symbol);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveIdentifierFromItsNamespaceScope(ast);

        verify(namespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveIdentifierFromItsNamespaceScope_NonExistingLocalIdentifier_ReturnsNull() {
        String identifier = "nonExistingLocalSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.resolve(ast)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveIdentifierFromItsNamespaceScope(ast);

        assertThat(result, is(nullValue()));
    }

    private ITSPHPAst createAst(String name) {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        final String[] text = {name};
        when(ast.getText()).thenAnswer(
                new Answer<Object>()
                {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        return text[0];
                    }
                }
        );
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                text[0] = (String) invocationOnMock.getArguments()[0];
                return null;
            }
        }).when(ast).setText(anyString());
        return ast;
    }

    protected ISymbolResolver createSymbolResolver() {
        return createSymbolResolver(mock(IScopeHelper.class));
    }

    protected ISymbolResolver createSymbolResolver(IScopeHelper scopeHelper) {
        return createSymbolResolver(
                scopeHelper,
                mock(ILowerCaseStringMap.class),
                mock(IGlobalNamespaceScope.class));
    }

    private ISymbolResolver createSymbolResolver(IGlobalNamespaceScope defaultNamespaceScope) {
        return createSymbolResolver(
                mock(IScopeHelper.class),
                mock(ILowerCaseStringMap.class),
                defaultNamespaceScope);
    }

    protected ISymbolResolver createSymbolResolver(
            IScopeHelper theScopeHelper,
            ILowerCaseStringMap<IGlobalNamespaceScope> theGlobalNamespaceScopes,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        return new UserSymbolResolver(theScopeHelper,
                theGlobalNamespaceScopes, theGlobalDefaultNamespace

        );
    }
}

