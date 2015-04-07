/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class GlobalNamespaceScopeTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.scopes;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.utils.MapHelper;
import ch.tsphp.tinsphp.inference_engine.scopes.GlobalNamespaceScope;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class GlobalNamespaceScopeTest
{

    public static final String GLOBAL_NAMESPACE_NAME = "\\globalNamespace\\";

    @Test
    public void define_Standard_DoesNotInteractWithTheSymbolOtherThanGetName() {
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        ISymbol symbol = createSymbol("symbol");

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);

        verify(symbol).getName();
        verifyNoMoreInteractions(symbol);
        verify(scopeHelper).define(globalNamespaceScope, symbol);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void doubleDefinitionCheck_Standard_DelegateToScopeHelper() {
        //no arrange necessary

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope();
        globalNamespaceScope.doubleDefinitionCheck(mock(ISymbol.class));

        //assert in annotation - no longer supported
    }

    @Test
    public void resolve_NothingDefined_ReturnNull() {
        ITSPHPAst ast = createAst("symbol");

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope();
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolve_AbsoluteTypeNothingDefined_ReturnNull() {
        ITSPHPAst ast = createAst(GLOBAL_NAMESPACE_NAME + "symbol");

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope();
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolve_CaseWrong_ReturnNull() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst("SYMBOL");
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        arrangeScopeHelperForDefine(scopeHelper);

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolve_AbsoluteTypeDifferentNamespace_ReturnNull() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst("\\otherNamespace\\symbol");
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        arrangeScopeHelperForDefine(scopeHelper);

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolve_AbsoluteTypeSubNamespace_ReturnNull() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst(GLOBAL_NAMESPACE_NAME + "a\\symbol");
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        arrangeScopeHelperForDefine(scopeHelper);

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolve_Standard_ReturnSymbol() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst("symbol");
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        arrangeScopeHelperForDefine(scopeHelper);

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertThat(result, is(symbol));
    }

    @Test
    public void resolve_AbsoluteType_ReturnSymbol() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst(GLOBAL_NAMESPACE_NAME + "symbol");
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        arrangeScopeHelperForDefine(scopeHelper);

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertThat(result, is(symbol));
    }


    @Test
    public void resolveCaseInsensitive_NothingDefined_ReturnNull() {
        ITSPHPAst ast = createAst("symbol");

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope();
        ISymbol result = globalNamespaceScope.resolveCaseInsensitive(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveCaseInsensitive_AbsoluteTypeNothingDefined_ReturnNull() {
        ITSPHPAst ast = createAst(GLOBAL_NAMESPACE_NAME + "symbol");

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope();
        ISymbol result = globalNamespaceScope.resolveCaseInsensitive(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveCaseInsensitive_CaseWrong_ReturnNull() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst("SYMBOL");
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        arrangeScopeHelperForDefine(scopeHelper);

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolveCaseInsensitive(ast);

        assertThat(result, is(symbol));
    }

    @Test
    public void resolveCaseInsensitive_AbsoluteTypeDifferentNamespace_ReturnNull() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst("\\otherNamespace\\symbol");
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        arrangeScopeHelperForDefine(scopeHelper);

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolveCaseInsensitive(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveCaseInsensitive_AbsoluteTypeSubNamespace_ReturnNull() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst(GLOBAL_NAMESPACE_NAME + "a\\symbol");
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        arrangeScopeHelperForDefine(scopeHelper);

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolveCaseInsensitive(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveCaseInsensitive_Standard_ReturnSymbol() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst("symbol");
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        arrangeScopeHelperForDefine(scopeHelper);

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolveCaseInsensitive(ast);

        assertThat(result, is(symbol));
    }

    @Test
    public void resolveCaseInsensitive_AbsoluteType_ReturnSymbol() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst(GLOBAL_NAMESPACE_NAME + "symbol");
        IScopeHelper scopeHelper = mock(IScopeHelper.class);
        arrangeScopeHelperForDefine(scopeHelper);

        IGlobalNamespaceScope globalNamespaceScope = createGlobalNamespaceScope(scopeHelper);
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolveCaseInsensitive(ast);

        assertThat(result, is(symbol));
    }

    private IGlobalNamespaceScope createGlobalNamespaceScope() {
        return createGlobalNamespaceScope(mock(IScopeHelper.class));
    }

    private IGlobalNamespaceScope createGlobalNamespaceScope(IScopeHelper scopeHelper) {
        return createGlobalNamespaceScope(scopeHelper, GLOBAL_NAMESPACE_NAME);
    }

    protected IGlobalNamespaceScope createGlobalNamespaceScope(IScopeHelper scopeHelper, String scopeName) {
        return new GlobalNamespaceScope(scopeHelper, scopeName);
    }

    private ISymbol createSymbol(String name) {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn(name);
        return symbol;
    }

    private ITSPHPAst createAst(String name) {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getText()).thenReturn(name);
        return ast;
    }

    private void arrangeScopeHelperForDefine(IScopeHelper scopeHelper) {
        final ArgumentCaptor<IScope> scopeCaptor = ArgumentCaptor.forClass(IScope.class);
        final ArgumentCaptor<ISymbol> symbolCaptor = ArgumentCaptor.forClass(ISymbol.class);

        doAnswer(new Answer()
        {
            public Object answer(InvocationOnMock invocation) {
                ISymbol symbol = symbolCaptor.getValue();
                MapHelper.addToListMap(scopeCaptor.getValue().getSymbols(), symbol.getName(), symbol);
                return null;
            }
        }).when(scopeHelper).define(scopeCaptor.capture(), symbolCaptor.capture());
    }
}
