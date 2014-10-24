/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class VariableSymbol from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.symbols;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.modifiers.IModifierSet;

public class VariableSymbol extends ASymbolWithAccessModifier implements IVariableSymbol
{

    public VariableSymbol(ITSPHPAst definitionAst, IModifierSet modifiers, String name) {
        super(definitionAst, modifiers, name);
    }

    @Override
    public boolean isStatic() {
        return modifiers.isStatic();
    }

    @Override
    public boolean isAlwaysCasting() {
        return modifiers.isAlwaysCasting();
    }

    @Override
    public boolean isFalseable() {
        return modifiers.isFalseable();
    }

    @Override
    public boolean isNullable() {
        return modifiers.isNullable();
    }

    @Override
    public TypeWithModifiersDto toTypeWithModifiersDto() {
        return new TypeWithModifiersDto(getType(), modifiers);
    }
}
