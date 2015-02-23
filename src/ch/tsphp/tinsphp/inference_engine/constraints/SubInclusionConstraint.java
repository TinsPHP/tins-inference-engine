/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.IConstraint;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.ITypeSymbol;

public class SubInclusionConstraint implements IConstraint
{
    private ISymbol symbol;
    private ITypeSymbol parentType;

    public SubInclusionConstraint(ISymbol theSymbol, ITypeSymbol theParentType) {
        symbol = theSymbol;
        parentType = theParentType;
    }

    public ISymbol getSymbol() {

        return symbol;
    }

    public ITypeSymbol getParentType() {
        return parentType;
    }
}
