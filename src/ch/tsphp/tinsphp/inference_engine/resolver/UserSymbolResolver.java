/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.resolver;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolResolver;

public class UserSymbolResolver implements ISymbolResolver
{
    private ISymbolResolver nextSymbolResolver;
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
    public ISymbol resolveIdentifier(ITSPHPAst identifier) {
        ISymbol symbol = identifier.getScope().resolve(identifier);
        if (symbol == null && nextSymbolResolver != null) {
            symbol = nextSymbolResolver.resolveIdentifier(identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveIdentifierWithFallback(ITSPHPAst identifier) {
        ISymbol symbol = resolveIdentifier(identifier);
        if (symbol == null) {
            symbol = resolveIdentifierFromFallback(identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveIdentifierFromFallback(ITSPHPAst identifier) {
        ISymbol symbol = globalDefaultNamespace.resolve(identifier);
        if (symbol == null && nextSymbolResolver != null) {
            symbol = nextSymbolResolver.resolveIdentifierFromFallback(identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveConstantLikeIdentifier(ITSPHPAst identifier) {
        ISymbol symbol;
        if (scopeHelper.isAbsoluteIdentifier(identifier.getText())) {
            symbol = resolveAbsoluteIdentifier(identifier);
        } else {
            if (scopeHelper.isRelativeIdentifier(identifier.getText())) {
                symbol = resolveIdentifierCompriseAlias(identifier);
                if (symbol == null) {
                    symbol = resolveRelativeIdentifier(identifier);
                }
            } else {
                symbol = resolveIdentifierWithFallback(identifier);
            }
        }

        if (symbol == null && nextSymbolResolver != null) {
            nextSymbolResolver.resolveConstantLikeIdentifier(identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveClassLikeIdentifier(ITSPHPAst itsphpAst) {
        //TODO TINS-161 inference OOP
        return null;
    }

    private ISymbol resolveAbsoluteIdentifier(ITSPHPAst typeAst) {
        IGlobalNamespaceScope scope = scopeHelper.getCorrespondingGlobalNamespace(
                globalNamespaceScopes, typeAst.getText());

        ISymbol symbol = null;
        if (scope != null) {
            symbol = scope.resolve(typeAst);
        }
        return symbol;
    }

    private ISymbol resolveIdentifierCompriseAlias(ITSPHPAst identifier) {
        //TODO rstoll TINS-179 reference phase - resolve use
        return null;
    }

    private ISymbol resolveRelativeIdentifier(ITSPHPAst identifier) {
        IGlobalNamespaceScope enclosingGlobalNamespaceScope =
                (IGlobalNamespaceScope) scopeHelper.getEnclosingNamespaceScope(identifier).getEnclosingScope();

        String relativeName = identifier.getText();
        String absoluteName = enclosingGlobalNamespaceScope.getScopeName() + relativeName;
        IGlobalNamespaceScope scope = scopeHelper.getCorrespondingGlobalNamespace(globalNamespaceScopes, absoluteName);

        ISymbol typeSymbol = null;
        if (scope != null) {
            identifier.setText(absoluteName);
            typeSymbol = scope.resolve(identifier);
            identifier.setText(relativeName);
        }
        return typeSymbol;
    }

    @Override
    public void setNextInChain(ISymbolResolver next) {
        nextSymbolResolver = next;
    }
}
