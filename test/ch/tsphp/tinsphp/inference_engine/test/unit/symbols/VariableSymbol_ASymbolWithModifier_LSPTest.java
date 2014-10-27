/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.symbols;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.modifiers.IModifierSet;
import ch.tsphp.tinsphp.inference_engine.symbols.ASymbolWithModifier;
import ch.tsphp.tinsphp.inference_engine.symbols.VariableSymbol;

public class VariableSymbol_ASymbolWithModifier_LSPTest extends ASymbolWithModifierTest
{

    @Override
    protected ASymbolWithModifier createSymbolWithModifier(ITSPHPAst definitionAst, IModifierSet modifiers,
            String name) {
        return new VariableSymbol(definitionAst, modifiers, name);
    }
}
