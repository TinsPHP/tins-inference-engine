/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.symbols;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.modifiers.IModifierSet;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.symbols.AScopedSymbol;
import ch.tsphp.tinsphp.inference_engine.symbols.MethodSymbol;

import static org.mockito.Mockito.mock;

public class MethodSymbol_AScopedSymbol_LSPTest extends AScopedSymbolTest
{
    protected AScopedSymbol createScopedSymbol(IScopeHelper scopeHelper, ITSPHPAst definitionAst,
            IModifierSet modifiers, String name, IScope enclosingScope) {
        return new MethodSymbol(
                scopeHelper, definitionAst, modifiers, mock(IModifierSet.class), name, enclosingScope);
    }
}
