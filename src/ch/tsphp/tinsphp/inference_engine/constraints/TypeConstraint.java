/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;

public class TypeConstraint implements IConstraint
{
    private ITypeSymbol typeSymbol;

    public TypeConstraint(ITypeSymbol theTypeSymbol) {
        typeSymbol = theTypeSymbol;
    }

    public ITypeSymbol getTypeSymbol() {
        return typeSymbol;
    }

}
