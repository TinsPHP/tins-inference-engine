/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class TestSymbolFactory from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.symbols.IModifierHelper;
import ch.tsphp.tinsphp.inference_engine.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.symbols.SymbolFactory;

import java.util.ArrayList;
import java.util.List;

public class TestSymbolFactory extends SymbolFactory
{

    private List<ICreateSymbolListener> listeners = new ArrayList<>();

    public TestSymbolFactory(IScopeHelper theScopeHelper, IModifierHelper theModifierHelper) {
        super(theScopeHelper, theModifierHelper);
    }

    //TODO rstoll TINS-161 inference OOP
//    @Override
//    public IInterfaceTypeSymbol createInterfaceTypeSymbol(ITSPHPAst modifier, ITSPHPAst identifier,
//            IScope currentScope) {
//        IInterfaceTypeSymbol symbol = super.createInterfaceTypeSymbol(modifier, identifier, currentScope);
//        updateListener(symbol);
//        return symbol;
//    }
//
//    @Override
//    public IClassTypeSymbol createClassTypeSymbol(ITSPHPAst classModifierAst, ITSPHPAst identifier,
//            IScope currentScope) {
//        IClassTypeSymbol symbol = super.createClassTypeSymbol(classModifierAst, identifier, currentScope);
//        updateListener(symbol);
//        return symbol;
//    }

    @Override
    public IMethodSymbol createMethodSymbol(ITSPHPAst methodModifier, ITSPHPAst returnTypeModifier,
            ITSPHPAst identifier,
            IScope currentScope) {
        IMethodSymbol symbol = super.createMethodSymbol(methodModifier, returnTypeModifier, identifier, currentScope);
        updateListener(symbol);
        return symbol;
    }

    @Override
    public IVariableSymbol createVariableSymbol(ITSPHPAst typeModifierAst, ITSPHPAst variableId) {
        IVariableSymbol symbol = super.createVariableSymbol(typeModifierAst, variableId);
        updateListener(symbol);
        return symbol;
    }

    public void registerListener(ICreateSymbolListener listener) {
        listeners.add(listener);
    }

    private void updateListener(ISymbol symbol) {
        for (ICreateSymbolListener listener : listeners) {
            listener.setNewlyCreatedSymbol(symbol);
        }
    }
}
