/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;

import java.util.Collection;

public class TempMethodSymbol implements IMinimalMethodSymbol
{
    private static final String ERROR_MESSAGE = "You are dealing with a temp method symbol";

    private final IMethodSymbol methodSymbol;
    private Collection<IFunctionType> tempOverloads;

    public TempMethodSymbol(IMethodSymbol theMethodSymbol, Collection<IFunctionType> theTempOverloads) {
        methodSymbol = theMethodSymbol;
        tempOverloads = theTempOverloads;
    }

    public void renewTempOverloads(Collection<IFunctionType> newTempOverloads) {
        tempOverloads = newTempOverloads;
    }

    @Override
    public void addOverload(IFunctionType overload) {
        methodSymbol.addOverload(overload);
    }

    @Override
    public Collection<IFunctionType> getOverloads() {
        Collection<IFunctionType> overloads = methodSymbol.getOverloads();
        if (overloads.size() == 0) {
            return tempOverloads;
        }
        return overloads;
    }

    @Override
    public String getAbsoluteName() {
        return methodSymbol.getAbsoluteName();
    }


    @Override
    public ITSPHPAst getDefinitionAst() {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }


    @Override
    public IScope getDefinitionScope() {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public void setDefinitionScope(IScope definitionScope) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public ITypeSymbol getType() {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public void setType(ITypeSymbol newType) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }
}
