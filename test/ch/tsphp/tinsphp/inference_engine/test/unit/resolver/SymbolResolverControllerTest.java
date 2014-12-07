/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.resolver;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolver;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolverController;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IAliasTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.resolver.SymbolResolverController;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class SymbolResolverControllerTest
{

    @Test
    public void resolveConstantLikeIdentifier_LocalIdentifier_DelegatesToUserResolverFromItsScope() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isLocalIdentifier(identifier)).thenReturn(true);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbol symbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsScope(ast)).thenReturn(symbol);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);

        ISymbolResolverController symbolResolverController = createSymbolResolver(
                userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isLocalIdentifier(identifier);
        verify(userSymbolResolver).resolveIdentifierFromItsScope(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveConstantLikeIdentifier_LocalIdentifierNotFoundInCurrentScope_DelegatesToFallback() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isLocalIdentifier(identifier)).thenReturn(true);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        symbolResolvers.add(coreSymbolResolver);
        ISymbol symbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveIdentifierFromFallback(ast)).thenReturn(symbol);

        ISymbolResolverController symbolResolverController = createSymbolResolver(
                userSymbolResolver, symbolResolvers, scopeHelper);
        ISymbol result = symbolResolverController.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isLocalIdentifier(identifier);
        InOrder inOrder = Mockito.inOrder(userSymbolResolver, coreSymbolResolver);
        inOrder.verify(userSymbolResolver).resolveIdentifierFromItsScope(ast);
        inOrder.verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(userSymbolResolver).resolveIdentifierFromFallback(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveConstantLikeIdentifier_NonExistingLocalIdentifier_DelegatesToAllAndReturnsNull() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isLocalIdentifier(identifier)).thenReturn(true);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers, scopeHelper);
        ISymbol result = symbolResolverController.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isLocalIdentifier(identifier);
        InOrder inOrder = Mockito.inOrder(
                userSymbolResolver, coreSymbolResolver, additionalSymbolResolver1, additionalSymbolResolver2);
        inOrder.verify(userSymbolResolver).resolveIdentifierFromItsScope(ast);
        inOrder.verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        inOrder.verify(additionalSymbolResolver1).resolveIdentifierFromItsScope(ast);
        inOrder.verify(additionalSymbolResolver2).resolveIdentifierFromItsScope(ast);
        inOrder.verify(userSymbolResolver).resolveIdentifierFromFallback(ast);
        inOrder.verify(coreSymbolResolver).resolveIdentifierFromFallback(ast);
        inOrder.verify(additionalSymbolResolver1).resolveIdentifierFromFallback(ast);
        inOrder.verify(additionalSymbolResolver2).resolveIdentifierFromFallback(ast);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveConstantLikeIdentifier_AbsoluteIdentifier_DelegatesToUserResolverAbsoluteIdentifier() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbol symbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveAbsoluteIdentifier(ast)).thenReturn(symbol);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);

        ISymbolResolverController symbolResolverController = createSymbolResolver(
                userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveConstantLikeIdentifier_NonExistingAbsoluteIdentifier_DelegatesToAllAndReturnsNull() {
        String identifier = "\\nonExistingSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers, scopeHelper);
        ISymbol result = symbolResolverController.resolveConstantLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        InOrder inOrder = Mockito.inOrder(
                userSymbolResolver, coreSymbolResolver, additionalSymbolResolver1, additionalSymbolResolver2);
        inOrder.verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(coreSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver1).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver2).resolveAbsoluteIdentifier(ast);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveConstantLikeIdentifier_RelativeIdentifierNoAlias_DelegatesToUserResolverAbsoluteIdentifier() {
        String identifier = "name\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);

        //no use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbol symbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveAbsoluteIdentifier(ast)).thenReturn(symbol);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);


        //act
        ISymbolResolverController symbolResolverController = createSymbolResolver(
                userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveConstantLikeIdentifier(ast);


        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is(symbol));
    }


    @Test
    public void resolveConstantLikeIdentifier_RelativeIdentifierNoAlias_PassesAbsoluteNameButDoesNotChangeAst() {
        String identifier = "name\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);

        //no use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getScopeName()).thenReturn("\\a\\");
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);


        //act
        ISymbolResolverController symbolResolverController = createSymbolResolver(scopeHelper);
        symbolResolverController.resolveConstantLikeIdentifier(ast);


        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(ast, times(2)).setText(argumentCaptor.capture());
        List<String> allValues = argumentCaptor.getAllValues();
        assertThat(allValues.get(0), is("\\a\\" + identifier));
        assertThat(allValues.get(1), is(identifier));
    }


    @Test
    public void resolveConstantLikeIdentifier_NonExistingRelativeIdentifier_DelegatesToAllAndReturnsNull() {
        String identifier = "existingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);

        //no use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers, scopeHelper);
        ISymbol result = symbolResolverController.resolveConstantLikeIdentifier(ast);


        //assert
        verify(scopeHelper).isRelativeIdentifier(identifier);
        InOrder inOrder = Mockito.inOrder(
                userSymbolResolver, coreSymbolResolver, additionalSymbolResolver1, additionalSymbolResolver2);
        inOrder.verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(coreSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver1).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver2).resolveAbsoluteIdentifier(ast);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveClassLikeIdentifier_AbsoluteIdentifier_DelegatesToUserResolverAbsoluteIdentifier() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbol symbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveAbsoluteIdentifier(ast)).thenReturn(symbol);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);

        ISymbolResolverController symbolResolverController = createSymbolResolver(
                userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingAbsoluteIdentifier_DelegatesToAllAndReturnsNull() {
        String identifier = "\\nonExistingSymbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(true);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);

        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        InOrder inOrder = Mockito.inOrder(
                userSymbolResolver, coreSymbolResolver, additionalSymbolResolver1, additionalSymbolResolver2);
        inOrder.verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(coreSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver1).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver2).resolveAbsoluteIdentifier(ast);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveClassLikeIdentifier_RelativeIdentifierNoAlias_DelegatesToUserResolverAbsoluteIdentifier() {
        String identifier = "name\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);

        //no use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbol symbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveAbsoluteIdentifier(ast)).thenReturn(symbol);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);


        //act
        ISymbolResolverController symbolResolverController = createSymbolResolver(
                userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is(symbol));
    }


    @Test
    public void resolveClassLikeIdentifier_RelativeIdentifierNoAlias_PassesAbsoluteNameButDoesNotChangeAst() {
        String identifier = "name\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);

        //no use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getScopeName()).thenReturn("\\a\\");
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);


        //act
        ISymbolResolverController symbolResolverController = createSymbolResolver(scopeHelper);
        symbolResolverController.resolveClassLikeIdentifier(ast);


        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(ast, times(2)).setText(argumentCaptor.capture());
        List<String> allValues = argumentCaptor.getAllValues();
        assertThat(allValues.get(0), is("\\a\\" + identifier));
        assertThat(allValues.get(1), is(identifier));
    }


    @Test
    public void resolveClassLikeIdentifier_NonExistingRelativeIdentifier_DelegatesToAllAndReturnsNull() {
        String identifier = "existingNamespace\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);

        //no use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        //assert
        verify(scopeHelper).isRelativeIdentifier(identifier);
        InOrder inOrder = Mockito.inOrder(
                userSymbolResolver, coreSymbolResolver, additionalSymbolResolver1, additionalSymbolResolver2);
        inOrder.verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(coreSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver1).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver2).resolveAbsoluteIdentifier(ast);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void
    resolveClassLikeIdentifier_RelativeIdentifierAndAliasIsNoRealType_ResolvesUsingTypeNameDelegatesToUserAbsoluteIdentifier() {
        String useIdentifier = "a\\name";
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);

        //use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);

        //resolve alias
        when(useDefinition.isDefinedEarlierThan(ast)).thenReturn(true);
        ISymbol symbol = mock(ISymbol.class);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        IAliasTypeSymbol typeSymbol = mock(IAliasTypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(typeSymbol.getName()).thenReturn(useIdentifier);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbol foundSymbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveAbsoluteIdentifier(ast)).thenReturn(foundSymbol);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(alias);
        verify(useDefinition).isDefinedEarlierThan(ast);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(ast, times(2)).setText(captor.capture());
        List<String> values = captor.getAllValues();
        assertThat(values.get(0), is("\\a\\name\\Symbol"));
        assertThat(values.get(1), is("name\\Symbol"));
        verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is(foundSymbol));
    }

    @Test
    public void
    resolveClassLikeIdentifier_NonExistingRelativeIdentifierAndAliasIsNoRealType_DelegatesToAllAndReturnsNull() {
        String useIdentifier = "a\\name";
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);

        //use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);

        //resolve alias
        when(useDefinition.isDefinedEarlierThan(ast)).thenReturn(true);
        ISymbol symbol = mock(ISymbol.class);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        IAliasTypeSymbol typeSymbol = mock(IAliasTypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(typeSymbol.getName()).thenReturn(useIdentifier);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        //assert
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(alias);
        verify(useDefinition).isDefinedEarlierThan(ast);
        InOrder inOrder = Mockito.inOrder(
                userSymbolResolver, coreSymbolResolver, additionalSymbolResolver1, additionalSymbolResolver2);
        inOrder.verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(coreSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver1).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver2).resolveAbsoluteIdentifier(ast);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void
    resolveClassLikeIdentifier_RelativeIdentifierAndAliasIsType_ResolvesUsingScopeAndTypeNameAndDelegatesToUserAbsoluteIdentifier() {
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);

        //use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);

        //resolve alias
        when(useDefinition.isDefinedEarlierThan(ast)).thenReturn(true);
        ISymbol symbol = mock(ISymbol.class);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(typeSymbol.getName()).thenReturn(alias);
        IScope scope = mock(IScope.class);
        when(typeSymbol.getDefinitionScope()).thenReturn(scope);
        when(scope.getScopeName()).thenReturn("\\a\\");

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbol foundSymbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveAbsoluteIdentifier(ast)).thenReturn(foundSymbol);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(alias);
        verify(useDefinition).isDefinedEarlierThan(ast);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(ast, times(2)).setText(captor.capture());
        List<String> values = captor.getAllValues();
        assertThat(values.get(0), is("\\a\\name\\Symbol"));
        assertThat(values.get(1), is("name\\Symbol"));
        verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is(foundSymbol));
    }

    @Test
    public void
    resolveClassLikeIdentifier_NonExistingRelativeIdentifierAndAliasIsType_DelegatesToAllAndReturnsNull() {
        String alias = "name";
        String identifier = alias + "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(true);

        //use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias)).thenReturn(useDefinition);

        //resolve alias
        when(useDefinition.isDefinedEarlierThan(ast)).thenReturn(true);
        ISymbol symbol = mock(ISymbol.class);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        when(typeSymbol.getName()).thenReturn(alias);
        IScope scope = mock(IScope.class);
        when(typeSymbol.getDefinitionScope()).thenReturn(scope);
        when(scope.getScopeName()).thenReturn("\\a\\");

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        //assert
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(alias);
        verify(useDefinition).isDefinedEarlierThan(ast);
        InOrder inOrder = Mockito.inOrder(
                userSymbolResolver, coreSymbolResolver, additionalSymbolResolver1, additionalSymbolResolver2);
        inOrder.verify(userSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(coreSymbolResolver).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver1).resolveAbsoluteIdentifier(ast);
        inOrder.verify(additionalSymbolResolver2).resolveAbsoluteIdentifier(ast);
        assertThat(result, is(nullValue()));
    }


    @Test
    public void resolveClassLikeIdentifier_LocalIdentifierNoAlias_DelegatesToUserResolverFromNamespaceScope() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);

        //no use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbol symbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsNamespaceScope(ast)).thenReturn(symbol);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);


        //act
        ISymbolResolverController symbolResolverController = createSymbolResolver(
                userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveClassLikeIdentifier_NonExistingLocalIdentifierNoAlias_DelegatesToAllAndReturnsNull() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);

        //no use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(null);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        //assert
        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        InOrder inOrder = Mockito.inOrder(
                userSymbolResolver, coreSymbolResolver, additionalSymbolResolver1, additionalSymbolResolver2);
        inOrder.verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        inOrder.verify(coreSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        inOrder.verify(additionalSymbolResolver1).resolveIdentifierFromItsNamespaceScope(ast);
        inOrder.verify(additionalSymbolResolver2).resolveIdentifierFromItsNamespaceScope(ast);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveClassLikeIdentifier_LocalIdentifierCompriseAliasOnly_DelegatesToUserAbsoluteIdentifier() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);

        //use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(namespaceScope.resolve(ast)).thenReturn(null);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(useDefinition);

        //resolve alias
        when(useDefinition.isDefinedEarlierThan(ast)).thenReturn(true);
        ISymbol symbol = mock(ISymbol.class);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        verify(scopeHelper).isAbsoluteIdentifier(identifier);
        verify(scopeHelper).isRelativeIdentifier(identifier);
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        InOrder inOrder = Mockito.inOrder(
                userSymbolResolver, coreSymbolResolver, additionalSymbolResolver1, additionalSymbolResolver2);
        inOrder.verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        inOrder.verify(coreSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        inOrder.verify(additionalSymbolResolver1).resolveIdentifierFromItsNamespaceScope(ast);
        inOrder.verify(additionalSymbolResolver2).resolveIdentifierFromItsNamespaceScope(ast);
        assertThat(result, is((ISymbol) typeSymbol));
    }


    @Test
    public void resolveClassLikeIdentifier_LocalIdentifierAndAliasButNoTypeNameClash_UsesAlias() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);

        //use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(namespaceScope.resolve(ast)).thenReturn(null);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(useDefinition);

        //resolve alias
        when(useDefinition.isDefinedEarlierThan(ast)).thenReturn(true);
        ISymbol symbol = mock(ISymbol.class);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        IScope useDefinitionScope = mock(IScope.class);
        when(useDefinition.getScope()).thenReturn(useDefinitionScope);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbol localSymbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsNamespaceScope(ast)).thenReturn(localSymbol);
        IScope localSymbolScope = mock(IScope.class);
        ITSPHPAst localDefinitionAst = mock(ITSPHPAst.class);
        when(localSymbol.getDefinitionScope()).thenReturn(localSymbolScope);
        when(localSymbol.getDefinitionAst()).thenReturn(localDefinitionAst);
        when(useDefinition.isDefinedEarlierThan(localDefinitionAst)).thenReturn(true);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        //is only called for ast not for localDefinitionAst since no type name clash happened
        verify(useDefinition, times(1)).isDefinedEarlierThan(ast);
        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is((ISymbol) typeSymbol));
    }

    @Test
    public void resolveClassLikeIdentifier_LocalIdentifierTypeNameClashAndAliasFirstDefined_UsesAlias() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);

        //use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(namespaceScope.resolve(ast)).thenReturn(null);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(useDefinition);

        //resolve alias
        when(useDefinition.isDefinedEarlierThan(ast)).thenReturn(true);
        ISymbol symbol = mock(ISymbol.class);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        IScope scope = mock(IScope.class);
        when(useDefinition.getScope()).thenReturn(scope);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbol localSymbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsNamespaceScope(ast)).thenReturn(localSymbol);
        when(localSymbol.getDefinitionScope()).thenReturn(scope);
        ITSPHPAst localDefinitionAst = mock(ITSPHPAst.class);
        when(localSymbol.getDefinitionAst()).thenReturn(localDefinitionAst);
        when(useDefinition.isDefinedEarlierThan(localDefinitionAst)).thenReturn(true);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        ArgumentCaptor<ITSPHPAst> captor = ArgumentCaptor.forClass(ITSPHPAst.class);
        verify(useDefinition, times(2)).isDefinedEarlierThan(captor.capture());
        List<ITSPHPAst> values = captor.getAllValues();
        assertThat(values.get(0), is(localDefinitionAst));
        assertThat(values.get(1), is(ast));
        verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is((ISymbol) typeSymbol));
    }


    @Test
    public void
    resolveClassLikeIdentifier_LocalIdentifierTypeNameClashAndTypeSymbolFirstDefined_UsesTypeSymbol() {
        String identifier = "Symbol";
        ITSPHPAst ast = createAst(identifier);
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        when(scopeHelper.isAbsoluteIdentifier(identifier)).thenReturn(false);
        when(scopeHelper.isRelativeIdentifier(identifier)).thenReturn(false);

        //use definition
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(scopeHelper.getEnclosingNamespaceScope(ast)).thenReturn(namespaceScope);
        when(namespaceScope.resolve(ast)).thenReturn(null);
        ITSPHPAst useDefinition = mock(ITSPHPAst.class);
        when(namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier)).thenReturn(useDefinition);

        //resolve alias
        when(useDefinition.isDefinedEarlierThan(ast)).thenReturn(true);
        ISymbol symbol = mock(ISymbol.class);
        when(useDefinition.getSymbol()).thenReturn(symbol);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbol.getType()).thenReturn(typeSymbol);
        IScope scope = mock(IScope.class);
        when(useDefinition.getScope()).thenReturn(scope);

        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbol localSymbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsNamespaceScope(ast)).thenReturn(localSymbol);
        when(localSymbol.getDefinitionScope()).thenReturn(scope);
        ITSPHPAst localDefinitionAst = mock(ITSPHPAst.class);
        when(localSymbol.getDefinitionAst()).thenReturn(localDefinitionAst);
        when(useDefinition.isDefinedEarlierThan(localDefinitionAst)).thenReturn(false);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);


        //act
        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, scopeHelper);
        ISymbol result = symbolResolverController.resolveClassLikeIdentifier(ast);


        verify(namespaceScope).getCaseInsensitiveFirstUseDefinitionAst(identifier);
        //only called once since the type symbol is used - no alias resolving should happen
        verify(useDefinition, times(1)).isDefinedEarlierThan(localDefinitionAst);
        verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verifyZeroInteractions(coreSymbolResolver);
        assertThat(result, is(localSymbol));
    }

    @Test
    public void resolveIdentifierFromItsNamespaceScope_UserFindsIt_DelegatesToUserResolverOnly() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);
        ISymbol symbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsNamespaceScope(ast)).thenReturn(symbol);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers);
        ISymbol result = symbolResolverController.resolveIdentifierFromItsNamespaceScope(ast);

        verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verifyZeroInteractions(coreSymbolResolver);
        verifyZeroInteractions(additionalSymbolResolver1);
        verifyZeroInteractions(additionalSymbolResolver2);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveIdentifierFromItsNamespaceScope_UserDoesNotFindItButCore_DelegatesToUserAndCoreResolver() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);
        ISymbol symbol = mock(ISymbol.class);
        when(coreSymbolResolver.resolveIdentifierFromItsNamespaceScope(ast)).thenReturn(symbol);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers);
        ISymbol result = symbolResolverController.resolveIdentifierFromItsNamespaceScope(ast);

        verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verify(coreSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verifyZeroInteractions(additionalSymbolResolver1);
        verifyZeroInteractions(additionalSymbolResolver2);
        assertThat(result, is(symbol));
    }

    @Test
    public void
    resolveIdentifierFromItsNamespaceScope_UserAndCoreDoNotFindItButAdditional1_DelegatesToUserCoreAndAdditional1Resolver() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);
        ISymbol symbol = mock(ISymbol.class);
        when(additionalSymbolResolver1.resolveIdentifierFromItsNamespaceScope(ast)).thenReturn(symbol);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers);
        ISymbol result = symbolResolverController.resolveIdentifierFromItsNamespaceScope(ast);

        verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verify(coreSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsNamespaceScope(ast);
        verifyZeroInteractions(additionalSymbolResolver2);
        assertThat(result, is(symbol));
    }

    @Test
    public void
    resolveIdentifierFromItsNamespaceScope_UserCoreAndAdditional1DoNotFindItButAdditional2_DelegatesToAllResolvers() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);
        ISymbol symbol = mock(ISymbol.class);
        when(additionalSymbolResolver2.resolveIdentifierFromItsNamespaceScope(ast)).thenReturn(symbol);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers);
        ISymbol result = symbolResolverController.resolveIdentifierFromItsNamespaceScope(ast);

        verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verify(coreSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsNamespaceScope(ast);
        verify(additionalSymbolResolver2).resolveIdentifierFromItsNamespaceScope(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveIdentifierFromItsNamespaceScope_NonExistingSymbol_DelegatesToAllResolversAndReturnsNull() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers);
        ISymbol result = symbolResolverController.resolveIdentifierFromItsNamespaceScope(ast);

        verify(userSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verify(coreSymbolResolver).resolveIdentifierFromItsNamespaceScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsNamespaceScope(ast);
        verify(additionalSymbolResolver2).resolveIdentifierFromItsNamespaceScope(ast);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveIdentifierFromItsScope_UserFindsIt_DelegatesToUserResolverOnly() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);
        ISymbol symbol = mock(ISymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsScope(ast)).thenReturn(symbol);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers);
        ISymbol result = symbolResolverController.resolveIdentifierFromItsScope(ast);

        verify(userSymbolResolver).resolveIdentifierFromItsScope(ast);
        verifyZeroInteractions(coreSymbolResolver);
        verifyZeroInteractions(additionalSymbolResolver1);
        verifyZeroInteractions(additionalSymbolResolver2);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveIdentifierFromItsScope_UserDoesNotFindItButCore_DelegatesToUserAndCoreResolver() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);
        ISymbol symbol = mock(ISymbol.class);
        when(coreSymbolResolver.resolveIdentifierFromItsScope(ast)).thenReturn(symbol);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers);
        ISymbol result = symbolResolverController.resolveIdentifierFromItsScope(ast);

        verify(userSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verifyZeroInteractions(additionalSymbolResolver1);
        verifyZeroInteractions(additionalSymbolResolver2);
        assertThat(result, is(symbol));
    }

    @Test
    public void
    resolveIdentifierFromItsScope_UserAndCoreDoNotFindItButAdditional1_DelegatesToUserCoreAndAdditional1Resolver() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);
        ISymbol symbol = mock(ISymbol.class);
        when(additionalSymbolResolver1.resolveIdentifierFromItsScope(ast)).thenReturn(symbol);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers);
        ISymbol result = symbolResolverController.resolveIdentifierFromItsScope(ast);

        verify(userSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsScope(ast);
        verifyZeroInteractions(additionalSymbolResolver2);
        assertThat(result, is(symbol));
    }

    @Test
    public void
    resolveIdentifierFromItsScope_UserCoreAndAdditional1DoNotFindItButAdditional2_DelegatesToAllResolvers() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);
        ISymbol symbol = mock(ISymbol.class);
        when(additionalSymbolResolver2.resolveIdentifierFromItsScope(ast)).thenReturn(symbol);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers);
        ISymbol result = symbolResolverController.resolveIdentifierFromItsScope(ast);

        verify(userSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver2).resolveIdentifierFromItsScope(ast);
        assertThat(result, is(symbol));
    }

    @Test
    public void resolveIdentifierFromItsScope_NonExistingSymbol_DelegatesToAllResolversAndReturnsNull() {
        String identifier = "\\Symbol";
        ITSPHPAst ast = createAst(identifier);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        List<ISymbolResolver> symbolResolvers = new ArrayList<>();
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        symbolResolvers.add(coreSymbolResolver);
        symbolResolvers.add(additionalSymbolResolver1);
        symbolResolvers.add(additionalSymbolResolver2);

        ISymbolResolverController symbolResolverController
                = createSymbolResolver(userSymbolResolver, symbolResolvers);
        ISymbol result = symbolResolverController.resolveIdentifierFromItsScope(ast);

        verify(userSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver2).resolveIdentifierFromItsScope(ast);
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


    private ISymbolResolverController createSymbolResolver(IScopeHelper scopeHelper) {
        return createSymbolResolver(
                mock(ISymbolResolver.class),
                scopeHelper);
    }

    private ISymbolResolverController createSymbolResolver(
            ISymbolResolver userSymbolResolver,
            List<ISymbolResolver> additionalSymbolResolvers) {
        return createSymbolResolver(
                userSymbolResolver,
                additionalSymbolResolvers,
                mock(IScopeHelper.class));
    }

    private ISymbolResolverController createSymbolResolver(
            ISymbolResolver userSymbolResolver, IScopeHelper scopeHelper) {
        return createSymbolResolver(
                userSymbolResolver,
                new ArrayList<ISymbolResolver>(),
                scopeHelper);
    }

    private ISymbolResolverController createSymbolResolver(
            ISymbolResolver userSymbolResolver,
            List<ISymbolResolver> additionalSymbolResolvers,
            IScopeHelper scopeHelper) {
        return createSymbolResolver(
                userSymbolResolver,
                additionalSymbolResolvers,
                scopeHelper,
                mock(ISymbolFactory.class),
                mock(IInferenceErrorReporter.class));
    }

    protected ISymbolResolverController createSymbolResolver(
            ISymbolResolver theUserSymbolResolver,
            List<ISymbolResolver> additionalSymbolResolvers,
            IScopeHelper theScopeHelper,
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter) {

        return new SymbolResolverController(
                theUserSymbolResolver,
                additionalSymbolResolvers,
                theScopeHelper,
                theSymbolFactory,
                theInferenceErrorReporter);
    }
}
