/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.resolver;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ISymbolResolver;
import ch.tsphp.tinsphp.common.symbols.ITypeSymbolResolver;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;

public class UserTypeSymbolResolver implements ITypeSymbolResolver
{

    protected ISymbolResolver symbolResolver;
    private ITypeSymbolResolver nextSymbolResolver;

    public UserTypeSymbolResolver(
            IScopeHelper theScopeHelper,
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter,
            ILowerCaseStringMap<IGlobalNamespaceScope> theGlobalNamespaceScopes,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {

        // We use the UserSymbolResolver (without any following members in the chain) since resolving class/interface
        // types is nothing else than resolving class like symbols
        symbolResolver = new UserSymbolResolver(theScopeHelper, theSymbolFactory, theInferenceErrorReporter,
                theGlobalNamespaceScopes, theGlobalDefaultNamespace

        );
    }

    @Override
    public ITypeSymbol resolveTypeFor(ITSPHPAst ast) {
        ITypeSymbol typeSymbol;
        switch (ast.getType()) {
            case TokenTypes.TYPE_NAME:
                typeSymbol = resolveTypeName(ast);
                break;
            case TokenTypes.Identifier:
                typeSymbol = resolveTypeIdentifier(ast);
                break;
            default:
                typeSymbol = null;
                break;
        }
        if (typeSymbol == null && nextSymbolResolver != null) {
            typeSymbol = nextSymbolResolver.resolveTypeFor(ast);
        }
        return typeSymbol;
    }

    private ITypeSymbol resolveTypeIdentifier(ITSPHPAst identifier) {
        return (ITypeSymbol) symbolResolver.resolveLocalIdentifier(identifier);
    }

    private ITypeSymbol resolveTypeName(ITSPHPAst typeName) {
        return (ITypeSymbol) symbolResolver.resolveClassLikeIdentifier(typeName);
    }

    @Override
    public void setNextInChain(ITypeSymbolResolver next) {
        nextSymbolResolver = next;
    }
}
