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
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.scopes.GlobalNamespaceScope;
import ch.tsphp.tinsphp.inference_engine.utils.MapHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class GlobalNamespaceScopeTest
{

    public static final String GLOBAL_NAMESPACE_NAME = "\\globalNamespace\\";
    private IScopeHelper scopeHelper;

    @Before
    public void setUp() {
        scopeHelper = mock(IScopeHelper.class);
    }

    @Test
    public void define_Standard_DoesNotInteractWithTheSymbolOtherThanGetName() {
        ISymbol symbol = createSymbol("symbol");

        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
        globalNamespaceScope.define(symbol);

        verify(symbol).getName();
        verifyNoMoreInteractions(symbol);
        verify(scopeHelper).define(globalNamespaceScope, symbol);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void doubleDefinitionCheckCaseInsensitive_Standard_DelegateToScopeHelper() {
        ISymbol symbol = createSymbol("symbol");

        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
        globalNamespaceScope.doubleDefinitionCheckCaseInsensitive(symbol);

        ArgumentCaptor<ISymbol> argument = ArgumentCaptor.forClass(ISymbol.class);
        verify(scopeHelper).checkIsNotDoubleDefinition(Matchers.anyMap(), argument.capture());
        assertThat(argument.getValue(), is(symbol));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void doubleDefinitionCheck_Standard_DelegateToScopeHelper() {
        ISymbol symbol = createSymbol("symbol");

        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
        globalNamespaceScope.doubleDefinitionCheck(symbol);

        ArgumentCaptor<ISymbol> argument = ArgumentCaptor.forClass(ISymbol.class);
        verify(scopeHelper).checkIsNotDoubleDefinition(Matchers.anyMap(), argument.capture());
        assertThat(argument.getValue(), is(symbol));
    }

    @Test
    public void resolve_NothingDefined_ReturnNull() {
        ITSPHPAst ast = createAst("symbol");

        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertNull(result);
    }

    @Test
    public void resolve_AbsoluteTypeNothingDefined_ReturnNull() {
        ITSPHPAst ast = createAst(GLOBAL_NAMESPACE_NAME + "symbol");

        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertNull(result);
    }

    @Test
    public void resolve_CaseWrong_ReturnNull() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst("SYMBOL");
        arrangeScopeHelperForDefine();

        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertNull(result);
    }


    @Test
    public void resolve_AbsoluteTypeDifferentNamespace_ReturnNull() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst("\\otherNamespace\\symbol");
        arrangeScopeHelperForDefine();

        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertNull(result);
    }

    @Test
    public void resolve_AbsoluteTypeSubNamespace_ReturnNull() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst(GLOBAL_NAMESPACE_NAME + "a\\symbol");
        arrangeScopeHelperForDefine();

        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertNull(result);
    }

    @Test
    public void resolve_Standard_ReturnSymbol() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst("symbol");
        arrangeScopeHelperForDefine();

        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertThat(result, is(symbol));
    }

    @Test
    public void resolve_AbsoluteType_ReturnSymbol() {
        ISymbol symbol = createSymbol("symbol");
        ITSPHPAst ast = createAst(GLOBAL_NAMESPACE_NAME + "symbol");
        arrangeScopeHelperForDefine();

        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
        globalNamespaceScope.define(symbol);
        ISymbol result = globalNamespaceScope.resolve(ast);

        assertThat(result, is(symbol));
    }

    //TODO rstoll TINS-179 reference phase - use
//    @Test(expected = IllegalArgumentException.class)
//    public void getTypeSymbolWhichClashesWithUse_AbsoluteName_ReturnNull() {
//        ITSPHPAst ast = createAst("\\symbol");
//
//        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
//        globalNamespaceScope.getTypeSymbolWhichClashesWithUse(ast);
//
//        //Assert via the @Test(expected) annotation
//    }
//
//    @Test
//    public void getTypeSymbolWhichClashesWithUse_NothingDefined_ReturnNull() {
//        ITSPHPAst ast = createAst("symbol");
//
//        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
//        ISymbol result = globalNamespaceScope.getTypeSymbolWhichClashesWithUse(ast);
//
//        assertNull(result);
//    }
//
//    @Test
//    public void getTypeSymbolWhichClashesWithUse_SameCase_ReturnSymbol() {
//        ITypeSymbol symbol = createTypeSymbol("symbol");
//        ITSPHPAst ast = createAst("symbol");
//        arrangeScopeHelperForDefine();
//
//        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
//        globalNamespaceScope.define(symbol);
//        ITypeSymbol result = globalNamespaceScope.getTypeSymbolWhichClashesWithUse(ast);
//
//        assertThat(result, is(symbol));
//    }
//
//    @Test
//    public void getTypeSymbolWhichClashesWithUse_DifferentCase_ReturnSymbol() {
//        ITypeSymbol symbol = createTypeSymbol("symbol");
//        ITSPHPAst ast = createAst("SYmbol");
//        arrangeScopeHelperForDefine();
//
//        IGlobalNamespaceScope globalNamespaceScope = createGlobalScope();
//        globalNamespaceScope.define(symbol);
//        ITypeSymbol result = globalNamespaceScope.getTypeSymbolWhichClashesWithUse(ast);
//
//        assertThat(result, is(symbol));
//    }

    protected IGlobalNamespaceScope createGlobalScope() {
        return new GlobalNamespaceScope(scopeHelper, GLOBAL_NAMESPACE_NAME);
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

    private ITypeSymbol createTypeSymbol(String name) {
        ITypeSymbol symbol = mock(ITypeSymbol.class);
        when(symbol.getName()).thenReturn(name);
        return symbol;
    }

    private void arrangeScopeHelperForDefine() {
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
