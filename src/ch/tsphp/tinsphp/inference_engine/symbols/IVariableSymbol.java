/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class IVariableSymbol from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.symbols;

import ch.tsphp.common.symbols.modifiers.ICanBeFalseable;
import ch.tsphp.common.symbols.modifiers.ICanBeNullable;
import ch.tsphp.common.symbols.modifiers.ICanBeStatic;
import ch.tsphp.common.symbols.modifiers.ICanHaveCastModifier;

public interface IVariableSymbol extends ISymbolWithAccessModifier, ICanBeStatic, ICanHaveCastModifier,
        ICanBeFalseable, ICanBeNullable
{
    TypeWithModifiersDto toTypeWithModifiersDto();
}