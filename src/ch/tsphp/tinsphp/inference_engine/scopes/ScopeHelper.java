/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ScopeHelper from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.scopes;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.utils.MapHelper;

import java.util.List;
import java.util.Map;

public class ScopeHelper implements IScopeHelper
{

    @Override
    public boolean isAbsoluteIdentifier(String identifier) {
        return identifier.substring(0, 1).equals("\\");
    }

    @Override
    public boolean isRelativeIdentifier(String identifier) {
        return identifier.indexOf("\\") > 0;
    }

    @Override
    public boolean isLocalIdentifier(String identifier) {
        return !identifier.contains("\\");
    }

    @Override
    public void define(IScope definitionScope, ISymbol symbol) {
        MapHelper.addToListMap(definitionScope.getSymbols(), symbol.getName(), symbol);
        symbol.setDefinitionScope(definitionScope);
    }

    @Override
    public ISymbol resolve(IScope scope, ITSPHPAst ast) {
        ISymbol symbol = null;
        Map<String, List<ISymbol>> symbols = scope.getSymbols();
        if (symbols.containsKey(ast.getText())) {
            symbol = symbols.get(ast.getText()).get(0);
        }
        return symbol;
    }

    @Override
    public IGlobalNamespaceScope getCorrespondingGlobalNamespace(
            ILowerCaseStringMap<IGlobalNamespaceScope> globalNamespaceScopes, String typeName) {
        int lastBackslashPosition = typeName.lastIndexOf('\\') + 1;
        String namespaceName = typeName.substring(0, lastBackslashPosition);
        return globalNamespaceScopes.get(namespaceName);
    }


    //Warning! start code duplication - same as in CoreSymbolResolver in core component
    @Override
    public INamespaceScope getEnclosingNamespaceScope(ITSPHPAst ast) {
        INamespaceScope namespaceScope = null;

        IScope scope = ast.getScope();
        while (scope != null && !(scope instanceof INamespaceScope)) {
            scope = scope.getEnclosingScope();
        }
        if (scope != null) {
            namespaceScope = (INamespaceScope) scope;
        }
        return namespaceScope;
    }
    //Warning! end code duplication - same as in CoreSymbolResolver in core component

}
