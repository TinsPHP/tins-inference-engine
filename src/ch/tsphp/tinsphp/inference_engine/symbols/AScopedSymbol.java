/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AScopedSymbol from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.symbols;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.LowerCaseStringMap;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.modifiers.IModifierSet;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a symbol which is a scope at the same type, e.g. a class or a method.
 * <p/>
 * Adopted from the book "Language Implementation Patterns" by Terence Parr.
 */
public abstract class AScopedSymbol extends ASymbolWithModifier implements IScope
{
    //Warning! start code duplication - same as in AScope
    protected final IScopeHelper scopeHelper;
    protected final IScope enclosingScope;
    protected final Map<String, List<ISymbol>> symbols = new LowerCaseStringMap<>();
    protected final Map<String, Boolean> initialisedSymbols = new HashMap<>();
    //Warning! start code duplication - same as in AScope

    @SuppressWarnings("checkstyle:parameternumber")
    public AScopedSymbol(
            IScopeHelper theScopeHelper,
            ITSPHPAst definitionAst,
            IModifierSet modifiers,
            String name,
            IScope theEnclosingScope) {
        super(definitionAst, modifiers, name);
        enclosingScope = theEnclosingScope;
        scopeHelper = theScopeHelper;
    }

    @Override
    public void define(ISymbol symbol) {
        scopeHelper.define(this, symbol);
    }

    @Override
    public ISymbol resolve(ITSPHPAst ast) {
        return scopeHelper.resolve(this, ast);
    }

    @Override
    public boolean doubleDefinitionCheck(ISymbol symbol) {
        return scopeHelper.checkIsNotDoubleDefinition(symbols, symbol);
    }

    //Warning! start code duplication - same as in AScope
    @Override
    public IScope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public String getScopeName() {
        return name;
    }

    @Override
    public Map<String, List<ISymbol>> getSymbols() {
        return symbols;
    }
    //Warning! end code duplication - same as in AScope

    //Warning! start code duplication - same as in AScope
    @Override
    public void addToInitialisedSymbols(ISymbol symbol, boolean isFullyInitialised) {
        String symbolName = symbol.getName();
        if (!initialisedSymbols.containsKey(symbolName) || !initialisedSymbols.get(symbolName)) {
            initialisedSymbols.put(symbol.getName(), isFullyInitialised);
        }
    }

    @Override
    public Map<String, Boolean> getInitialisedSymbols() {
        return initialisedSymbols;
    }
    //Warning! end code duplication - same as in AScope
}
