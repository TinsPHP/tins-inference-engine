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
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IAliasTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ISymbolResolver;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.resolver.UserSymbolResolver;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.exceptions.base.MockitoAssertionError;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
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
    public void resolveIdentifierWithFallback_IsInCurrentScope_DelegatesToScopeOnly() {
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
    public void resolveIdentifierWithFallback_NotFoundInCurrentScope_DelegatesToFallback() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        IScope scope = mock(IScope.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getScope()).thenReturn(scope);
        ISymbol symbol = mock(ISymbol.class);
        when(scope.resolve(ast)).thenReturn(null);
        IGlobalNamespaceScope globalDefaultNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(globalDefaultNamespaceScope.resolve(ast)).thenReturn(symbol);

        ISymbolResolver symbolResolver = createSymbolResolver(
                mock(IScopeHelper.class),
                mock(ISymbolFactory.class),
                mock(IInferenceErrorReporter.class),
                mock(ILowerCaseStringMap.class),
                globalDefaultNamespaceScope);
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
    public void resolveConstantLikeIdentifier_AbsoluteIdentifier_DelegatesToGlobalNamespaceScope() {
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
        ISymbol result = symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveConstantLikeIdentifier_RelativeIdentifierNoAlias_DelegatesToGlobalNamespaceScope() {
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
        ISymbol result = symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveConstantLikeIdentifier_RelativeIdentifierNoAlias_PassesAbsoluteNameButDoesNotChangeAst() {
        String identifier = "name\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getScopeName()).thenReturn("\\a\\");
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);


        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.resolveConstantLikeIdentifier(ast);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(ast, times(2)).setText(argumentCaptor.capture());
        List<String> allValues = argumentCaptor.getAllValues();
        assertThat(allValues.get(0), is("\\a\\" + identifier));
        assertThat(allValues.get(1), is(identifier));
        verify(scopeHelper).getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), eq("\\a\\" + identifier));
    }

    @Test
    public void resolveConstantLikeIdentifier_LocalIdentifierFoundInCurrentScope_DelegatesToScopeOnly() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isLocalIdentifier(identifier)).thenReturn(true);
        ISymbol symbol = mock(ISymbol.class);
        IScope scope = mock(IScope.class);
        when(scope.resolve(ast)).thenReturn(symbol);
        when(ast.getScope()).thenReturn(scope);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isLocalIdentifier(identifier);
        verify(scope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveConstantLikeIdentifier_LocalIdentifierNotFoundInCurrentScope_DelegatesToFallback() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isLocalIdentifier(identifier)).thenReturn(true);
        IScope scope = mock(IScope.class);
        when(ast.getScope()).thenReturn(scope);
        ISymbol symbol = mock(ISymbol.class);
        when(scope.resolve(ast)).thenReturn(null);
        IGlobalNamespaceScope globalDefaultNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(globalDefaultNamespaceScope.resolve(ast)).thenReturn(symbol);

        ISymbolResolver symbolResolver = createSymbolResolver(
                scopeHelper,
                mock(ISymbolFactory.class),
                mock(IInferenceErrorReporter.class),
                mock(ILowerCaseStringMap.class),
                globalDefaultNamespaceScope);
        ISymbol result = symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isLocalIdentifier(identifier);
        verify(scope).resolve(ast);
        verify(globalDefaultNamespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveConstantLikeIdentifier_NonExistingAbsoluteSymbol_DelegatesToNextInChainClassLikeIdentifier() {
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
        symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(nextSymbolResolver).resolveClassLikeIdentifier(ast);
    }

    @Test
    public void resolveConstantLikeIdentifier_NonExistingAbsoluteSymbolAndLastMemberInChain_ReturnsNull() {
        String identifier = "\\nonExistingSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void
    resolveConstantLikeIdentifier_NonExistingAbsoluteNamespaceSymbol_DelegatesToNextInChainClassLikeIdentifier() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        String identifier = "\\nonExistingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(nextSymbolResolver).resolveClassLikeIdentifier(ast);
    }

    @Test
    public void resolveConstantLikeIdentifier_NonExistingAbsoluteNamespaceSymbolAndLastInChain_ReturnsNull() {
        String identifier = "\\nonExistingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveConstantLikeIdentifier_NonExistingRelativeSymbol_DelegatesToNextInChainClassLikeIdentifier() {
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
        symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(nextSymbolResolver).resolveClassLikeIdentifier(ast);
    }

    @Test
    public void resolveConstantLikeIdentifier_NonExistingRelativeSymbolAndLastInChain_ReturnsNull() {
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
        ISymbol result = symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void
    resolveConstantLikeIdentifier_NonExistingRelativeNamespaceSymbol_DelegatesNextInChainClassLikeIdentifier() {
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
        symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(nextSymbolResolver).resolveClassLikeIdentifier(ast);
    }

    @Test
    public void resolveConstantLikeIdentifier_NonExistingRelativeNamespaceSymbolAdLastInChain_ResultsNull() {
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
        ISymbol result = symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void
    resolveConstantLikeIdentifier_NonExistingLocalIdentifier_DelegatesToResolveIdentifierWithFallback() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        String identifier = "nonExistingLocalSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isLocalIdentifier(identifier)).thenReturn(true);
        IScope scope = mock(IScope.class);
        when(scope.resolve(ast)).thenReturn(null);
        when(ast.getScope()).thenReturn(scope);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isLocalIdentifier(identifier);
        verify(nextSymbolResolver).resolveIdentifier(ast);
        verify(nextSymbolResolver).resolveIdentifierFromFallback(ast);
        try {
            verify(nextSymbolResolver).resolveConstantLikeIdentifier(ast);
            fail("resolveConstantLikeIdentifier called even though it was a local identifier "
                    + "- should only delegate to resolveIdentifierWithFallback");
        } catch (MockitoAssertionError er) {
            //that ok, we expect that resolveConstantLikeIdentifier was not called
        }
    }

    @Test
    public void resolveConstantLikeIdentifier_NonExistingLocalIdentifierAndLastInChain_ReturnsNull() {
        String identifier = "nonExistingLocalSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isLocalIdentifier(identifier)).thenReturn(true);
        IScope scope = mock(IScope.class);
        when(scope.resolve(ast)).thenReturn(null);
        when(ast.getScope()).thenReturn(scope);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isLocalIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveClassLikeIdentifier_AbsoluteIdentifier_DelegatesToGlobalNamespaceScope() {
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
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveClassLikeIdentifier_RelativeIdentifierNoAlias_DelegatesToGlobalNamespaceScope() {
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
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveClassLikeIdentifier_RelativeIdentifierNoAlias_PassesAbsoluteNameButDoesNotChangeAst() {
        String identifier = "name\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getScopeName()).thenReturn("\\a\\");
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);


        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.resolveClassLikeIdentifier(ast);
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(ast, times(2)).setText(argumentCaptor.capture());
        List<String> allValues = argumentCaptor.getAllValues();
        assertThat(allValues.get(0), is("\\a\\" + identifier));
        assertThat(allValues.get(1), is(identifier));
        verify(scopeHelper).getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), eq("\\a\\" + identifier));
    }

    @Test
    public void
    resolveClassLikeIdentifier_RelativeIdentifierAndAliasIsNoRealTypeDoesNotFindGlobalNamespace_ResolvesUsingTypeNameAndReturnsNull() {
        String useIdentifier = "a\\name";
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ISymbol symbol = mock(ISymbol.class);
        IAliasTypeSymbol typeSymbol = mock(IAliasTypeSymbol.class);
        when(typeSymbol.getName()).thenReturn(useIdentifier);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString())).thenReturn(null);


        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(alias);
        verify(typeSymbol).getName();
        verify(scopeHelper).getCorrespondingGlobalNamespace(
                any(ILowerCaseStringMap.class), eq("\\a\\name\\Symbol"));
        assertThat(result, is(nullValue()));
    }

    @Test
    public void
    resolveClassLikeIdentifier_RelativeIdentifierAndAliasIsNoRealTypeFindGlobalNamespaceButNotSymbol_ResolvesUsingTypeNameAndReturnsNull() {
        String useIdentifier = "a\\name";
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ISymbol symbol = mock(ISymbol.class);
        IAliasTypeSymbol typeSymbol = mock(IAliasTypeSymbol.class);
        when(typeSymbol.getName()).thenReturn(useIdentifier);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(
                any(ILowerCaseStringMap.class), anyString())).thenReturn(globalNamespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(alias);
        verify(typeSymbol).getName();
        verify(scopeHelper).getCorrespondingGlobalNamespace(
                any(ILowerCaseStringMap.class), eq("\\a\\name\\Symbol"));
        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(nullValue()));
    }


    @Test
    public void
    resolveClassLikeIdentifier_RelativeIdentifierAndAliasIsNoRealTypeAndFindSymbol_ResolvesUsingTypeNameAndReturnsSymbol() {
        String useIdentifier = "a\\name";
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ISymbol symbol = mock(ISymbol.class);
        IAliasTypeSymbol typeSymbol = mock(IAliasTypeSymbol.class);
        when(typeSymbol.getName()).thenReturn(useIdentifier);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(
                any(ILowerCaseStringMap.class), anyString())).thenReturn(globalNamespaceScope);
        ISymbol symbol2 = mock(ISymbol.class);
        when(globalNamespaceScope.resolve(ast)).thenReturn(symbol2);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(alias);
        verify(typeSymbol).getName();
        verify(scopeHelper).getCorrespondingGlobalNamespace(
                any(ILowerCaseStringMap.class), eq("\\a\\name\\Symbol"));
        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(symbol2));
    }

    @Test
    public void
    resolveClassLikeIdentifier_RelativeIdentifierAndAliasIsTypeDoesNotFindGlobalNamespace_ResolvesUsingScopeAndTypeNameAndReturnsNull() {
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ISymbol symbol = mock(ISymbol.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(typeSymbol.getName()).thenReturn(alias);
        IScope scope = mock(IScope.class);
        when(typeSymbol.getDefinitionScope()).thenReturn(scope);
        when(scope.getScopeName()).thenReturn("\\a\\");
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        when(scopeHelper.getCorrespondingGlobalNamespace(
                any(ILowerCaseStringMap.class), anyString())).thenReturn(null);


        //act
        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(alias);
        verify(typeSymbol).getName();
        verify(scope).getScopeName();
        verify(scopeHelper).getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), eq("\\a\\name\\Symbol"));
        assertThat(result, is(nullValue()));
    }

    @Test
    public void
    resolveClassLikeIdentifier_RelativeIdentifierAndAliasIsTypeFindGlobalNamespaceButNotSymbol_ResolvesUsingScopeAndTypeNameAndReturnsNull() {
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ISymbol symbol = mock(ISymbol.class);

        //type symbol
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(typeSymbol.getName()).thenReturn(alias);
        IScope scope = mock(IScope.class);
        when(typeSymbol.getDefinitionScope()).thenReturn(scope);
        when(scope.getScopeName()).thenReturn("\\a\\");
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);

        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(
                any(ILowerCaseStringMap.class), anyString())).thenReturn(globalNamespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);


        //act
        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(alias);
        verify(typeSymbol).getName();
        verify(scope).getScopeName();
        verify(scopeHelper).getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), eq("\\a\\name\\Symbol"));
        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void
    resolveClassLikeIdentifier_RelativeIdentifierAndAliasIsTypeFindSymbol_ResolvesUsingScopeAndTypeNameAndReturnsNull
            () {
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ISymbol symbol = mock(ISymbol.class);

        //type symbol
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(typeSymbol.getName()).thenReturn(alias);
        IScope scope = mock(IScope.class);
        when(typeSymbol.getDefinitionScope()).thenReturn(scope);
        when(scope.getScopeName()).thenReturn("\\a\\");
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);

        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(
                any(ILowerCaseStringMap.class), anyString())).thenReturn(globalNamespaceScope);
        ISymbol symbol1 = mock(ISymbol.class);
        when(globalNamespaceScope.resolve(ast)).thenReturn(symbol1);


        //act
        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(alias);
        verify(typeSymbol).getName();
        verify(scope).getScopeName();
        verify(scopeHelper).getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), eq("\\a\\name\\Symbol"));
        verify(globalNamespaceScope).resolve(ast);
        assertThat(result, is(symbol1));
    }


    @Test
    public void
    resolveClassLikeIdentifier_RelativeIdentifierAndAlias_PassesAbsoluteNameButDoesNotChangeAst() {
        String useIdentifier = "a\\name";
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ISymbol symbol = mock(ISymbol.class);
        IAliasTypeSymbol typeSymbol = mock(IAliasTypeSymbol.class);
        when(typeSymbol.getName()).thenReturn(useIdentifier);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(
                any(ILowerCaseStringMap.class), anyString())).thenReturn(globalNamespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.resolveClassLikeIdentifier(ast);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(ast, times(2)).setText(argumentCaptor.capture());
        List<String> allValues = argumentCaptor.getAllValues();
        assertThat(allValues.get(0), is("\\a\\name\\Symbol"));
        assertThat(allValues.get(1), is(identifier));
    }

    @Test
    public void resolveClassLikeIdentifier_LocalIdentifierNoAlias_DelegatesToNamespaceScope() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        ISymbol symbol = mock(ISymbol.class);
        when(namespaceScope.resolve(ast)).thenReturn(symbol);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(namespaceScope).resolve(ast);
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveClassLikeIdentifier_LocalIdentifierCompriseAliasOnly_ResolvesAlias() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.resolve(ast)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(useDefinition);
        ISymbol symbol = mock(ISymbol.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(namespaceScope).resolve(ast);
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        assertThat(result, is((ISymbol) typeSymbol));
    }

    @Test
    public void resolveClassLikeIdentifier_LocalIdentifierAndAliasButNoTypeNameClash_UsesAlias() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);

        //Local symbol
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        ISymbol localSymbol = mock(ISymbol.class);
        IScope localSymbolScope = mock(IScope.class);
        ITSPHPAst localDefinitionAst = mock(ITSPHPAst.class);
        when(localSymbol.getDefinitionScope()).thenReturn(localSymbolScope);
        when(localSymbol.getDefinitionAst()).thenReturn(localDefinitionAst);
        when(namespaceScope.resolve(ast)).thenReturn(localSymbol);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        //alias through use definition
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(useDefinition);
        ISymbol symbol = mock(ISymbol.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        IScope useDefinitionScope = mock(IScope.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        when(useDefinition.getScope()).thenReturn(useDefinitionScope);
        when(useDefinition.isDefinedEarlierThan(localDefinitionAst)).thenReturn(true);


        //act
        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);


        try {
            verify(useDefinition).isDefinedEarlierThan(localDefinitionAst);
            fail("isDefinedEarlierThan should not have been called because the type symbol was defined in "
                    + "another namespace scope than the use declaration.");
        } catch (MockitoAssertionError ex) {
            //that's good
        }
        verify(namespaceScope).resolve(ast);
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        assertThat(result, is((ISymbol) typeSymbol));
    }

    @Test
    public void resolveClassLikeIdentifier_LocalIdentifierTypeNameClashAndAliasFirstDefined_UsesAlias() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);

        //Local symbol
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        ISymbol localSymbol = mock(ISymbol.class);
        IScope scope = mock(IScope.class);
        ITSPHPAst localDefinitionAst = mock(ITSPHPAst.class);
        when(localSymbol.getDefinitionScope()).thenReturn(scope);
        when(localSymbol.getDefinitionAst()).thenReturn(localDefinitionAst);
        when(namespaceScope.resolve(ast)).thenReturn(localSymbol);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        //alias through use definition
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(useDefinition);
        ISymbol symbol = mock(ISymbol.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        when(useDefinition.getScope()).thenReturn(scope);
        when(useDefinition.isDefinedEarlierThan(localDefinitionAst)).thenReturn(true);


        //act
        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);


        verify(useDefinition).isDefinedEarlierThan(localDefinitionAst);
        verify(namespaceScope).resolve(ast);
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        assertThat(result, is((ISymbol) typeSymbol));
    }

    @Test
    public void resolveClassLikeIdentifier_LocalIdentifierTypeNameClashAndTypeSymbolFirstDefined_UsesTypeSymbol() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);

        //Local symbol
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        ISymbol localSymbol = mock(ISymbol.class);
        IScope scope = mock(IScope.class);
        ITSPHPAst localDefinitionAst = mock(ITSPHPAst.class);
        when(localSymbol.getDefinitionScope()).thenReturn(scope);
        when(localSymbol.getDefinitionAst()).thenReturn(localDefinitionAst);
        when(namespaceScope.resolve(ast)).thenReturn(localSymbol);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        //alias through use definition
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(useDefinition);
        ISymbol symbol = mock(ISymbol.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        when(useDefinition.getScope()).thenReturn(scope);
        when(useDefinition.isDefinedEarlierThan(localDefinitionAst)).thenReturn(false);


        //act
        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);


        verify(useDefinition).isDefinedEarlierThan(localDefinitionAst);
        verify(namespaceScope).resolve(ast);
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        assertThat(result, is(localSymbol));
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingAbsoluteSymbol_DelegatesToNextInChain() {
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
        symbolResolver.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(nextSymbolResolver).resolveClassLikeIdentifier(ast);
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingAbsoluteSymbolAndLastMemberInChain_ReturnsNull() {
        String identifier = "\\nonExistingSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString()))
                .thenReturn(globalNamespaceScope);
        when(globalNamespaceScope.resolve(ast)).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingAbsoluteNamespaceSymbol_DelegatesToNextInChain() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        String identifier = "\\nonExistingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(nextSymbolResolver).resolveClassLikeIdentifier(ast);
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingAbsoluteNamespaceSymbolAndLastInChain_ReturnsNull() {
        String identifier = "\\nonExistingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        when(scopeHelper.getCorrespondingGlobalNamespace(any(ILowerCaseStringMap.class), anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingRelativeSymbol_DelegatesToNextInChain() {
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
        symbolResolver.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(nextSymbolResolver).resolveClassLikeIdentifier(ast);
    }


    @Test
    public void resolveClassLikeIdentifier_NonExistingRelativeSymbolAndLastInChain_ReturnsNull() {
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
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingRelativeNamespaceSymbol_DelegatesToNextInChain() {
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
        symbolResolver.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(nextSymbolResolver).resolveClassLikeIdentifier(ast);
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingRelativeNamespaceSymbolAdLastInChain_ResultsNull() {
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
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isRelativeIdentifier(identifier);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingLocalIdentifier_DelegatesToNextInChain() {
        ISymbolResolver nextSymbolResolver = mock(ISymbolResolver.class);
        String identifier = "nonExistingLocalSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.resolve(ast)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        symbolResolver.setNextInChain(nextSymbolResolver);
        symbolResolver.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(nextSymbolResolver).resolveClassLikeIdentifier(ast);
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingLocalIdentifierAndLastInChain_ReturnsNull() {
        String identifier = "nonExistingLocalSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.resolve(ast)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(anyString())).thenReturn(null);

        ISymbolResolver symbolResolver = createSymbolResolver(scopeHelper);
        ISymbol result = symbolResolver.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(scopeHelper).isRelativeIdentifier(identifier);
        assertThat(result, is(nullValue()));
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
        return createSymbolResolver(
                scopeHelper,
                mock(ISymbolFactory.class),
                mock(IInferenceErrorReporter.class),
                mock(ILowerCaseStringMap.class),
                mock(IGlobalNamespaceScope.class));
    }

    private ISymbolResolver createSymbolResolver(IGlobalNamespaceScope defaultNamespaceScope) {
        return createSymbolResolver(
                mock(IScopeHelper.class),
                mock(ISymbolFactory.class),
                mock(IInferenceErrorReporter.class),
                mock(ILowerCaseStringMap.class),
                defaultNamespaceScope);
    }

    protected ISymbolResolver createSymbolResolver(
            IScopeHelper theScopeHelper,
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter,
            ILowerCaseStringMap<IGlobalNamespaceScope> theGlobalNamespaceScopes,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        return new UserSymbolResolver(theScopeHelper, theSymbolFactory, theInferenceErrorReporter,
                theGlobalNamespaceScopes, theGlobalDefaultNamespace

        );
    }
}

