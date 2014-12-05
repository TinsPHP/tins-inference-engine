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

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IConditionalScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.scopes.ConditionalScope;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConditionalScopeTest
{

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
        return new ConditionalScope(mock(IScopeHelper.class), scope);
    }

    //TODO rstoll TINS-161 inference OOP
//    @SuppressWarnings("unchecked")
//    private IMethodSymbol createMethodSymbol(ILowerCaseStringMap symbols) {
//        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
//        when(methodSymbol.getSymbols()).thenReturn(symbols);
//        return methodSymbol;
//    }

    private ISymbol createSymbol(String name) {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn(name);
        return symbol;
    }

}
