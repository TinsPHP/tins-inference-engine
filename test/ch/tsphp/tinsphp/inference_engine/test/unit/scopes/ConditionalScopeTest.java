/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ConditionalScopeTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.scopes;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IAlreadyDefinedMethodCaller;
import ch.tsphp.tinsphp.common.scopes.IConditionalScope;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.scopes.ConditionalScope;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConditionalScopeTest
{
    private IScopeHelper scopeHelper;

    @Before
    public void setUp() {
        scopeHelper = mock(IScopeHelper.class);
    }

    @Test
    public void define_Standard_DelegateToEnclosingScopeAndSetDefinitionScope() {
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        ISymbol symbol = mock(ISymbol.class);

        IConditionalScope conditionalScope = createConditionalScope(namespaceScope);
        conditionalScope.define(symbol);

        verify(namespaceScope).define(symbol);
        verify(symbol).setDefinitionScope(conditionalScope);
    }

    @Test
    public void define_TwoSymbolsWithSameName_DelegateToEnclosingScopeAndSetDefinitionScopeForBoth() {
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        ISymbol symbol = createSymbol("a");
        ISymbol symbol2 = createSymbol("a");

        IConditionalScope conditionalScope = createConditionalScope(namespaceScope);
        conditionalScope.define(symbol);
        conditionalScope.define(symbol2);

        verify(namespaceScope).define(symbol);
        verify(symbol).setDefinitionScope(conditionalScope);
        verify(namespaceScope).define(symbol2);
        verify(symbol2).setDefinitionScope(conditionalScope);
    }


    @Test(expected = UnsupportedOperationException.class)
    public void doubleDefinitionCheck_Standard_DelegateToScopeHelper() {
        //no arrange necessary

        IConditionalScope conditionalScope = createConditionalScope(mock(IScope.class));
        conditionalScope.doubleDefinitionCheck(mock(ISymbol.class));

        //assert in annotation - no longer supported
    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void doubleDefinitionCheck_InMethod_DelegateToScopeHelperAndUseGlobalNamespace() {
//        ILowerCaseStringMap symbols = mock(ILowerCaseStringMap.class);
//        IMethodSymbol methodSymbol = createMethodSymbol(symbols);
//        ISymbol symbol = createSymbol("a", createAst(methodSymbol));
//
//        IConditionalScope conditionalScope = createConditionalScope(methodSymbol);
//        conditionalScope.doubleDefinitionCheck(symbol);
//
//        verifyScopeWasUsed(methodSymbol, symbols, symbol);
//    }
//
//
//    @Test
//    public void doubleDefinitionCheck_InConditionalScopeInMethod_DelegateToScopeHelperAndUseGlobalNamespace() {
//        ILowerCaseStringMap symbols = mock(ILowerCaseStringMap.class);
//        IMethodSymbol methodSymbol = createMethodSymbol(symbols);
//        IConditionalScope conditionalScopeOuter = createConditionalScope(methodSymbol);
//        ISymbol symbol = createSymbol("a", createAst(conditionalScopeOuter));
//
//        IConditionalScope conditionalScope = createConditionalScope(conditionalScopeOuter);
//        conditionalScope.doubleDefinitionCheck(symbol);
//
//        verifyScopeWasUsed(methodSymbol, symbols, symbol);
//    }
//
//    @SuppressWarnings({"unchecked", "ThrowableResultOfMethodCallIgnored"})
//    @Test
//    public void doubleDefinitionCheck_ScopeHelperCallsAlreadyDefinedMethodCaller_DefinedInOuterScopeIsCalled() {
//        ILowerCaseStringMap symbols = mock(ILowerCaseStringMap.class);
//        IMethodSymbol methodSymbol = createMethodSymbol(symbols);
//        ITypeCheckerErrorReporter typeCheckerErrorReporter = mock(ITypeCheckerErrorReporter.class);
//
//        IConditionalScope conditionalScopeOuter = createConditionalScope(methodSymbol);
//        final ISymbol symbol = createSymbol("a", createAst(conditionalScopeOuter));
//
//        final ISymbol earlierDefinedSymbol = mock(ISymbol.class);
//        when(scopeHelper.checkIsNotDoubleDefinition(
//                Matchers.anyMap(), Matchers.any(ISymbol.class), Matchers.any(IAlreadyDefinedMethodCaller.class)))
//                .thenAnswer(new Answer()
//                {
//                    public Object answer(InvocationOnMock invocation) {
//                        Object[] args = invocation.getArguments();
//                        ((IAlreadyDefinedMethodCaller) args[2])
//                                .callAccordingAlreadyDefinedMethod(earlierDefinedSymbol, symbol);
//                        return false;
//                    }
//                });
//
//        IConditionalScope conditionalScope = createConditionalScope(conditionalScopeOuter, typeCheckerErrorReporter);
//        conditionalScope.doubleDefinitionCheck(symbol);
//
//        verify(typeCheckerErrorReporter).definedInOuterScope(earlierDefinedSymbol, symbol);
//    }

    @Test
    public void resolve_InNamespaceScope_DelegateToEnclosingScope() {
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);

        IConditionalScope conditionalScope = createConditionalScope(namespaceScope);
        conditionalScope.resolve(ast);

        verify(namespaceScope).resolve(ast);
    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void resolve_InMethod_DelegateToEnclosingScope() {
//        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
//        ITSPHPAst ast = mock(ITSPHPAst.class);
//
//        IConditionalScope conditionalScope = createConditionalScope(methodSymbol);
//        conditionalScope.resolve(ast);
//
//        verify(methodSymbol).resolve(ast);
//    }

    @Test
    public void resolve_InConditionalScope_DelegateToEnclosingScope() {
        IConditionalScope outerScope = mock(IConditionalScope.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);

        IConditionalScope conditionalScope = createConditionalScope(outerScope);
        conditionalScope.resolve(ast);

        verify(outerScope).resolve(ast);
    }

    protected IConditionalScope createConditionalScope(IScope scope) {
        return new ConditionalScope(scopeHelper, scope);
    }

    @SuppressWarnings("unchecked")
    private void verifyScopeWasUsed(IScope scope, ILowerCaseStringMap symbols, ISymbol symbol) {
        verify(scope).getSymbols();
        ArgumentCaptor<ILowerCaseStringMap> symbolsArg = ArgumentCaptor.forClass(ILowerCaseStringMap.class);
        ArgumentCaptor<ISymbol> symbolArg = ArgumentCaptor.forClass(ISymbol.class);
        verify(scopeHelper).checkIsNotDoubleDefinition(symbolsArg.capture(), symbolArg.capture(),
                Matchers.any(IAlreadyDefinedMethodCaller.class));
        assertThat(symbolsArg.getValue(), is(symbols));
        assertThat(symbolArg.getValue(), is(symbol));
    }

    private INamespaceScope createNamespaceScope(IGlobalNamespaceScope globalNamespaceScope) {
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(namespaceScope.getEnclosingScope()).thenReturn(globalNamespaceScope);
        return namespaceScope;
    }

    private ITSPHPAst createAst(IScope scope) {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getScope()).thenReturn(scope);
        return ast;
    }


    //TODO rstoll TINS-161 inference OOP
//    @SuppressWarnings("unchecked")
//    private IMethodSymbol createMethodSymbol(ILowerCaseStringMap symbols) {
//        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
//        when(methodSymbol.getSymbols()).thenReturn(symbols);
//        return methodSymbol;
//    }

    @SuppressWarnings("unchecked")
    private IGlobalNamespaceScope createGlobalNamespaceScope(ILowerCaseStringMap symbols) {
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        when(globalNamespaceScope.getSymbols()).thenReturn(symbols);
        return globalNamespaceScope;
    }

    private ISymbol createSymbol(String name) {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn(name);
        return symbol;
    }

    private ISymbol createSymbol(String name, ITSPHPAst ast) {
        ISymbol symbol = createSymbol(name);
        when(symbol.getDefinitionAst()).thenReturn(ast);
        return symbol;
    }
}
