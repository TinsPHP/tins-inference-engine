/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ScopeHelperTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.scopes;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.LowerCaseStringMap;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeHelper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScopeHelperTest
{

    public static final String SYMBOL_NAME = "symbolName";

    @Test
    public void isAbsoluteIdentifier_AbsoluteFromDefaultNamespace_ReturnsTrue() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isAbsoluteIdentifier("\\a");

        assertThat(result, is(true));
    }

    @Test
    public void isAbsoluteIdentifier_AbsoluteFromNamespace_ReturnsTrue() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isAbsoluteIdentifier("\\a\\a");

        assertThat(result, is(true));
    }

    @Test
    public void isAbsoluteIdentifier_AbsoluteFromSubNamespace_ReturnsTrue() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isAbsoluteIdentifier("\\a\\b\\a");

        assertThat(result, is(true));
    }

    @Test
    public void isAbsoluteIdentifier_RelativeFromSubNamespace_ReturnsFalse() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isAbsoluteIdentifier("a\\a");

        assertThat(result, is(false));
    }

    @Test
    public void isAbsoluteIdentifier_Local_ReturnsFalse() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isAbsoluteIdentifier("a");

        assertThat(result, is(false));
    }

    @Test
    public void isRelativeIdentifier_AbsoluteFromDefaultNamespace_ReturnsFalse() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isRelativeIdentifier("\\a");

        assertThat(result, is(false));
    }

    @Test
    public void isRelativeIdentifier_AbsoluteFromNamespace_ReturnsFalse() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isRelativeIdentifier("\\a\\a");

        assertThat(result, is(false));
    }

    @Test
    public void isRelativeIdentifier_AbsoluteFromSubNamespace_ReturnsFalse() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isRelativeIdentifier("\\a\\b\\a");

        assertThat(result, is(false));
    }

    @Test
    public void isRelativeIdentifier_RelativeFromSubNamespace_ReturnsTrue() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isRelativeIdentifier("a\\a");

        assertThat(result, is(true));
    }

    @Test
    public void isRelativeIdentifier_Local_ReturnsFalse() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isRelativeIdentifier("a");

        assertThat(result, is(false));
    }

    @Test
    public void isLocalIdentifier_AbsoluteFromDefaultNamespace_ReturnsFalse() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isLocalIdentifier("\\a");

        assertThat(result, is(false));
    }

    @Test
    public void isLocalIdentifier_AbsoluteFromNamespace_ReturnsFalse() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isLocalIdentifier("\\a\\a");

        assertThat(result, is(false));
    }

    @Test
    public void isLocalIdentifier_AbsoluteFromSubNamespace_ReturnsFalse() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isLocalIdentifier("\\a\\b\\a");

        assertThat(result, is(false));
    }

    @Test
    public void isLocalIdentifier_RelativeFromSubNamespace_ReturnsFalse() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isLocalIdentifier("a\\a");

        assertThat(result, is(false));
    }

    @Test
    public void isLocalIdentifier_Local_ReturnsTrue() {
        //no arrange needed

        IScopeHelper scopeHelper = createScopeHelper();
        boolean result = scopeHelper.isLocalIdentifier("a");

        assertThat(result, is(true));
    }

    @Test
    public void define_OneSymbol_AddToScopeSymbolsAndSetDefinitionScope() {
        Map<String, List<ISymbol>> symbols = new HashMap<>();
        IScope scope = createScope(symbols);
        ISymbol symbol = createSymbol(SYMBOL_NAME);

        IScopeHelper scopeHelper = createScopeHelper();
        scopeHelper.define(scope, symbol);

        assertThat(symbols, hasKey(SYMBOL_NAME));
        List<ISymbol> definedSymbols = symbols.get(SYMBOL_NAME);
        assertThat(definedSymbols.size(), is(1));
        assertThat(definedSymbols, hasItem(symbol));

        verify(symbol).setDefinitionScope(scope);
    }

    @Test
    public void define_TwoSymbolsSameName_AddBothToScopeSymbolsAndBothDefinitionScopeAreSet() {
        Map<String, List<ISymbol>> symbols = new HashMap<>();
        IScope scope = createScope(symbols);
        ISymbol symbol1 = createSymbol(SYMBOL_NAME);
        ISymbol symbol2 = createSymbol(SYMBOL_NAME);

        IScopeHelper scopeHelper = createScopeHelper();
        scopeHelper.define(scope, symbol1);
        scopeHelper.define(scope, symbol2);

        assertThat(symbols, hasKey(SYMBOL_NAME));
        List<ISymbol> definedSymbols = symbols.get(SYMBOL_NAME);
        assertThat(definedSymbols.size(), is(2));
        assertThat(definedSymbols, hasItems(symbol1, symbol2));

        verify(symbol1).setDefinitionScope(scope);
        verify(symbol2).setDefinitionScope(scope);
    }

    @Test
    public void define_TwoSymbolsDifferentName_AddBothToScopeSymbolsAndBothDefinitionScopeAreSet() {
        Map<String, List<ISymbol>> symbols = new HashMap<>();
        IScope scope = createScope(symbols);
        ISymbol symbol1 = createSymbol("symbolName1");
        ISymbol symbol2 = createSymbol("symbolName2");

        IScopeHelper scopeHelper = createScopeHelper();
        scopeHelper.define(scope, symbol1);
        scopeHelper.define(scope, symbol2);

        assertThat(symbols, hasKey("symbolName1"));
        List<ISymbol> definedSymbols1 = symbols.get("symbolName1");
        assertThat(definedSymbols1.size(), is(1));
        assertThat(definedSymbols1, hasItem(symbol1));
        List<ISymbol> definedSymbols2 = symbols.get("symbolName2");
        assertThat(definedSymbols2.size(), is(1));
        assertThat(definedSymbols2, hasItem(symbol2));

        verify(symbol1).setDefinitionScope(scope);
        verify(symbol2).setDefinitionScope(scope);
    }

    @Test
    public void resolve_NotInScope_ReturnNull() {
        IScope scope = createScope(new HashMap<String, List<ISymbol>>());
        ITSPHPAst ast = createAst("astText");

        IScopeHelper scopeHelper = createScopeHelper();
        ISymbol result = scopeHelper.resolve(scope, ast);

        assertNull(result);
    }

    @Test
    public void resolve_WrongCaseUseCaseSensitiveMap_ReturnNull() {
        Map<String, List<ISymbol>> symbols = createSymbolsForStandardName(createSymbol("AstText"));
        IScope scope = createScope(symbols);
        ITSPHPAst ast = createAst("astText");

        IScopeHelper scopeHelper = createScopeHelper();
        ISymbol result = scopeHelper.resolve(scope, ast);

        assertNull(result);
    }

    @Test
    public void resolve_DefinedInScope_ReturnCorrespondingSymbol() {
        ISymbol symbol = createSymbol(SYMBOL_NAME);
        Map<String, List<ISymbol>> symbols = createSymbolsForStandardName(symbol);
        IScope scope = createScope(symbols);
        ITSPHPAst ast = createAst(SYMBOL_NAME);

        IScopeHelper scopeHelper = createScopeHelper();
        ISymbol result = scopeHelper.resolve(scope, ast);

        assertThat(result, is(symbol));
    }

    @Test
    public void getCorrespondingGlobalNamespace_GlobalNamespaceNotInMap_ReturnNull() {
        ILowerCaseStringMap<IGlobalNamespaceScope> scopes = new LowerCaseStringMap<>();

        IScopeHelper scopeHelper = createScopeHelper();
        IGlobalNamespaceScope result = scopeHelper.getCorrespondingGlobalNamespace(scopes, "\\notExistingGlobalScope");

        assertNull(result);
    }

    @Test
    public void getCorrespondingGlobalNamespace_NamespaceInMapButNotSubNamespace_ReturnNull() {
        ILowerCaseStringMap<IGlobalNamespaceScope> scopes = new LowerCaseStringMap<>();
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        scopes.put("\\a\\", globalNamespaceScope);

        IScopeHelper scopeHelper = createScopeHelper();
        IGlobalNamespaceScope result = scopeHelper.getCorrespondingGlobalNamespace(scopes, "\\a\\b\\");

        assertNull(result);
    }

    @Test
    public void getCorrespondingGlobalNamespace_NamespaceName_ReturnCorrespondingGlobalNamespace() {
        ILowerCaseStringMap<IGlobalNamespaceScope> scopes = new LowerCaseStringMap<>();
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        scopes.put("\\a\\", globalNamespaceScope);

        IScopeHelper scopeHelper = createScopeHelper();
        IGlobalNamespaceScope result = scopeHelper.getCorrespondingGlobalNamespace(scopes, "\\a\\");

        assertThat(result, is(globalNamespaceScope));
    }

    @Test
    public void getCorrespondingGlobalNamespace_SubNamespaceName_ReturnCorrespondingGlobalNamespace() {
        ILowerCaseStringMap<IGlobalNamespaceScope> scopes = new LowerCaseStringMap<>();
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        scopes.put("\\a\\b\\", globalNamespaceScope);

        IScopeHelper scopeHelper = createScopeHelper();
        IGlobalNamespaceScope result = scopeHelper.getCorrespondingGlobalNamespace(scopes, "\\a\\b\\");

        assertThat(result, is(globalNamespaceScope));
    }

    @Test
    public void getEnclosingNamespaceScope_InNamespace_ReturnNamespace() {
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        ITSPHPAst ast = createAst(SYMBOL_NAME);
        when(ast.getScope()).thenReturn(namespaceScope);

        IScopeHelper scopeHelper = createScopeHelper();
        IScope result = scopeHelper.getEnclosingNamespaceScope(ast);

        assertThat(result, is((IScope) namespaceScope));
    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void getEnclosingNamespaceScope_InMethod_ReturnNamespace() {
//        INamespaceScope namespaceScope = mock(INamespaceScope.class);
//        ITSPHPAst ast = createAst(SYMBOL_NAME);
//        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
//        when(methodSymbol.getEnclosingScope()).thenReturn(namespaceScope);
//        when(ast.getScope()).thenReturn(methodSymbol);
//
//        IScopeHelper scopeHelper = createScopeHelper();
//        IScope result = scopeHelper.getEnclosingNamespaceScope(ast);
//
//        assertThat(result, is((IScope) namespaceScope));
//    }

    @Test
    public void getEnclosingNamespaceScope_AstHasNoScope_ReturnNull() {
        ITSPHPAst ast = createAst(SYMBOL_NAME);

        IScopeHelper scopeHelper = createScopeHelper();
        IScope result = scopeHelper.getEnclosingNamespaceScope(ast);

        assertNull(result);
    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void getEnclosingNamespaceScope_InMethodHasNoScope_ReturnNull() {
//        ITSPHPAst ast = createAst(SYMBOL_NAME);
//        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
//        when(methodSymbol.getEnclosingScope()).thenReturn(null);
//        when(ast.getScope()).thenReturn(methodSymbol);
//
//        IScopeHelper scopeHelper = createScopeHelper();
//        IScope result = scopeHelper.getEnclosingNamespaceScope(ast);
//
//        assertNull(result);
//    }

    protected ScopeHelper createScopeHelper() {
        return new ScopeHelper();
    }

    private Map<String, List<ISymbol>> createSymbolsForStandardName(ISymbol... symbols) {
        Map<String, List<ISymbol>> map = new HashMap<>();
        List<ISymbol> definedSymbols = new ArrayList<>();
        definedSymbols.addAll(Arrays.asList(symbols));
        map.put(SYMBOL_NAME, definedSymbols);

        return map;
    }

    private IScope createScope(Map<String, List<ISymbol>> symbols) {
        IScope scope = mock(IScope.class);
        when(scope.getSymbols()).thenReturn(symbols);
        return scope;
    }

    private ISymbol createSymbol(String symbolName) {
        ISymbol symbol = mock(ISymbol.class);
        when(symbol.getName()).thenReturn(symbolName);
        return symbol;
    }

    private ITSPHPAst createAst(String text) {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getText()).thenReturn(text);
        return ast;
    }

}
