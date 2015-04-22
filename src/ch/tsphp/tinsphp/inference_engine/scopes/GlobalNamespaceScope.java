/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class GlobalNamespaceScope from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.scopes;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.LowerCaseStringMap;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.utils.MapHelper;

import java.util.ArrayList;
import java.util.List;


public class GlobalNamespaceScope extends AScope implements IGlobalNamespaceScope
{

    private final ILowerCaseStringMap<List<ISymbol>> symbolsCaseInsensitive = new LowerCaseStringMap<>();
    //Warning! start code duplication - same as in MethodSymbol
    private final List<IConstraint> lowerBoundConstraints = new ArrayList<>();
    private List<IOverloadBindings> bindings;
    //Warning! end code duplication - same as in MethodSymbol


    public GlobalNamespaceScope(IScopeHelper scopeHelper, String scopeName) {
        super(scopeHelper, scopeName, null);
    }

    @Override
    public void define(ISymbol symbol) {
        scopeHelper.define(this, symbol);
        MapHelper.addToListMap(symbolsCaseInsensitive, symbol.getName(), symbol);
    }

    @Override
    @Deprecated
    public boolean doubleDefinitionCheck(ISymbol symbol) {
        throw new UnsupportedOperationException("Is deprecated and should no longer be used");
    }

    @Override
    public ISymbol resolve(ITSPHPAst identifier) {
        ISymbol symbol = null;
        String typeName = getTypeNameWithoutNamespacePrefix(identifier.getText());
        if (symbols.containsKey(typeName)) {
            symbol = symbols.get(typeName).get(0);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveCaseInsensitive(ITSPHPAst identifier) {
        ISymbol symbol = null;
        String typeName = getTypeNameWithoutNamespacePrefix(identifier.getText());
        if (symbolsCaseInsensitive.containsKey(typeName)) {
            symbol = symbolsCaseInsensitive.get(typeName).get(0);
        }
        return symbol;
    }

    //Warning! start code duplication - same as in MethodSymbol
    @Override
    public boolean isFullyInitialised(ISymbol symbol) {
        String symbolName = symbol.getName();
        return initialisedSymbols.containsKey(symbolName) && initialisedSymbols.get(symbolName);
    }

    @Override
    public boolean isPartiallyInitialised(ISymbol symbol) {
        String symbolName = symbol.getName();
        return initialisedSymbols.containsKey(symbolName) && !initialisedSymbols.get(symbolName);
    }
    //Warning! end code duplication - same as in MethodSymbol

    private String getTypeNameWithoutNamespacePrefix(String typeName) {
        String typeNameWithoutPrefix = typeName;
        int scopeNameLength = scopeName.length();
        if (typeName.length() > scopeNameLength && typeName.substring(0, scopeNameLength).equals(scopeName)) {
            typeNameWithoutPrefix = typeName.substring(scopeNameLength);
        }
        return typeNameWithoutPrefix;
    }

    //Warning! start code duplication - same as in MethodSymbol
    @Override
    public List<IConstraint> getConstraints() {
        return lowerBoundConstraints;
    }

    @Override
    public void addConstraint(IConstraint constraint) {
        lowerBoundConstraints.add(constraint);
    }

    @Override
    public List<IOverloadBindings> getBindings() {
        return bindings;
    }

    @Override
    public void setBindings(List<IOverloadBindings> theBindings) {
        bindings = theBindings;
    }
    //Warning! end code duplication - same as in MethodSymbol
}
