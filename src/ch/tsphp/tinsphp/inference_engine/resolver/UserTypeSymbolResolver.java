/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.resolver;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.symbols.resolver.ISymbolResolverController;
import ch.tsphp.tinsphp.common.symbols.resolver.ITypeSymbolResolver;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;

public class UserTypeSymbolResolver implements ITypeSymbolResolver
{

    protected ISymbolResolverController symbolResolverController;
    private ITypeSymbolResolver nextSymbolResolver;

    public UserTypeSymbolResolver(ISymbolResolverController theSymbolResolverController) {
        symbolResolverController = theSymbolResolverController;
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
        return (ITypeSymbol) symbolResolverController.resolveIdentifierFromItsNamespaceScope(identifier);
    }

    private ITypeSymbol resolveTypeName(ITSPHPAst typeName) {
        return (ITypeSymbol) symbolResolverController.resolveClassLikeIdentifier(typeName);
    }

    @Override
    public void setNextInChain(ITypeSymbolResolver next) {
        nextSymbolResolver = next;
    }
}
