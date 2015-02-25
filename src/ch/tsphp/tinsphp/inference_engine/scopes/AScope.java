/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AScope from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.scopes;

import ch.tsphp.common.IConstraint;
import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.utils.MapHelper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides some helper methods for scopes.
 * <p/>
 * Adopted from the book "Language Implementation Patterns" by Terence Parr.
 */
public abstract class AScope implements IScope
{
    protected final String scopeName;

    //Warning! start code duplication - same as in AScopedSymbol
    protected final IScopeHelper scopeHelper;
    protected final IScope enclosingScope;
    protected final Map<String, List<ISymbol>> symbols = new LinkedHashMap<>();
    protected final Map<String, Boolean> initialisedSymbols = new HashMap<>();
    protected final Map<String, List<IConstraint>> constraints = new HashMap<>();
    protected final Map<String, IUnionTypeSymbol> constraintSolvingResults = new HashMap<>();
    //Warning! end code duplication - same as in AScopedSymbol

    public AScope(IScopeHelper theScopeHelper, String theScopeName, IScope theEnclosingScope) {
        scopeHelper = theScopeHelper;
        scopeName = theScopeName;
        enclosingScope = theEnclosingScope;
    }

    //Warning! start code duplication - same as in AScopedSymbol
    @Override
    public IScope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public String getScopeName() {
        return scopeName;
    }

    @Override
    public Map<String, List<ISymbol>> getSymbols() {
        return symbols;
    }
    //Warning! end code duplication - same as in AScopedSymbol


    //Warning! start code duplication - same as in AScopedSymbol
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
    //Warning! end code duplication - same as in AScopedSymbol


    //Warning! start code duplication - same as in AScopedSymbol
    @Override
    public Map<String, List<IConstraint>> getConstraints() {
        return constraints;
    }

    @Override
    public List<IConstraint> getConstraintsForVariable(String variableId) {
        return constraints.get(variableId);
    }

    @Override
    public void addConstraint(String variableId, IConstraint constraint) {
        MapHelper.addToListMap(constraints, variableId, constraint);
    }
    //Warning! end code duplication - same as in AScopedSymbol


    //Warning! start code duplication - same as in AScopedSymbol
    @Override
    public IUnionTypeSymbol getResultOfConstraintSolving(String variableId) {
        return constraintSolvingResults.get(variableId);
    }

    @Override
    public void setResultOfConstraintSolving(String variableId, IUnionTypeSymbol unionTypeSymbol) {
        if (!constraintSolvingResults.containsKey(variableId)) {
            constraintSolvingResults.put(variableId, unionTypeSymbol);
        } else {
            throw new IllegalStateException(
                    "the constraint solving results already contain a solution for " + variableId);
        }
    }
    //Warning! end code duplication - same as in AScopedSymbol

}
