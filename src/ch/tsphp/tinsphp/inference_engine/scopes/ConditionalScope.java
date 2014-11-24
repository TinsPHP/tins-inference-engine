/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ConditionalScope from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.scopes;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IConditionalScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;

public class ConditionalScope extends AScope implements IConditionalScope
{
    public ConditionalScope(IScopeHelper scopeHelper, IScope enclosingScope) {
        super(scopeHelper, "cScope", enclosingScope);
    }

    @Override
    public void define(ISymbol symbol) {
        enclosingScope.define(symbol);
        symbol.setDefinitionScope(this);
    }

    @Override
    public boolean doubleDefinitionCheck(ISymbol symbol) {
        throw new UnsupportedOperationException("Is deprecated and should no longer be used");
    }

    @Override
    public ISymbol resolve(ITSPHPAst ast) {
        return enclosingScope.resolve(ast);
    }

    @Override
    public boolean isFullyInitialised(ISymbol symbol) {
        String symbolName = symbol.getName();
        return initialisedSymbols.containsKey(symbolName) && initialisedSymbols.get(symbolName)
                || enclosingScope.isFullyInitialised(symbol);
    }

    @Override
    public boolean isPartiallyInitialised(ISymbol symbol) {
        String symbolName = symbol.getName();
        return initialisedSymbols.containsKey(symbolName) && !initialisedSymbols.get(symbolName)
                || enclosingScope.isPartiallyInitialised(symbol);
    }
}
