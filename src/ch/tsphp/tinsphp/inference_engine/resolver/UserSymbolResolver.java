/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.resolver;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.ICaseInsensitiveScope;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.resolver.ISymbolResolver;

public class UserSymbolResolver implements ISymbolResolver
{
    private final IScopeHelper scopeHelper;
    private final ILowerCaseStringMap<IGlobalNamespaceScope> globalNamespaceScopes;
    private final IGlobalNamespaceScope globalDefaultNamespace;

    public UserSymbolResolver(
            IScopeHelper theScopeHelper,
            ILowerCaseStringMap<IGlobalNamespaceScope> theGlobalNamespaceScopes,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        scopeHelper = theScopeHelper;
        globalNamespaceScopes = theGlobalNamespaceScopes;
        globalDefaultNamespace = theGlobalDefaultNamespace;
    }

    @Override
    public ISymbol resolveIdentifierFromItsScope(ITSPHPAst identifier) {
        return identifier.getScope().resolve(identifier);
    }

    @Override
    public ISymbol resolveIdentifierFromItsScopeCaseInsensitive(ITSPHPAst identifier) {
        ISymbol symbol;
        IScope scope = identifier.getScope();
        if (scope instanceof ICaseInsensitiveScope) {
            symbol = ((ICaseInsensitiveScope) scope).resolveCaseInsensitive(identifier);
        } else {
            symbol = scope.resolve(identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveIdentifierFromFallback(ITSPHPAst identifier) {
        return globalDefaultNamespace.resolve(identifier);
    }

    @Override
    public ISymbol resolveAbsoluteIdentifier(ITSPHPAst identifier) {
        ISymbol symbol = null;

        IGlobalNamespaceScope scope =
                scopeHelper.getCorrespondingGlobalNamespace(globalNamespaceScopes, identifier.getText());

        if (scope != null) {
            symbol = scope.resolve(identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveIdentifierFromItsNamespaceScope(ITSPHPAst identifier) {
        return scopeHelper.getEnclosingNamespaceScope(identifier).resolve(identifier);
    }

}
