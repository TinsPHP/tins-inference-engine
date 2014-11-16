/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.resolver;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IAliasTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ISymbolResolver;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;

public class UserSymbolResolver implements ISymbolResolver
{
    private final IScopeHelper scopeHelper;
    private final ISymbolFactory symbolFactory;
    private final ILowerCaseStringMap<IGlobalNamespaceScope> globalNamespaceScopes;
    private final IGlobalNamespaceScope globalDefaultNamespace;

    private ISymbolResolver nextSymbolResolver;

    public UserSymbolResolver(
            IScopeHelper theScopeHelper,
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter,
            ILowerCaseStringMap<IGlobalNamespaceScope> theGlobalNamespaceScopes,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        scopeHelper = theScopeHelper;
        symbolFactory = theSymbolFactory;
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
        if (scopeHelper.isLocalIdentifier(identifier.getText())) {
            //forward to nextSymbolResolver within resolveIdentifierWithFallback if necessary
            symbol = resolveIdentifierWithFallback(identifier);
        } else {
            //forward to nextSymbolResolver within resolveClassLikeIdentifier if necessary
            symbol = resolveClassLikeIdentifier(identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveClassLikeIdentifier(ITSPHPAst identifier) {
        ISymbol symbol;
        if (scopeHelper.isAbsoluteIdentifier(identifier.getText())) {
            symbol = resolveAbsoluteIdentifier(identifier);
        } else if (scopeHelper.isRelativeIdentifier(identifier.getText())) {
            symbol = resolveRelativeIdentifierConsiderAlias(identifier);
        } else {
            symbol = resolveLocalIdentifierConsiderAlias(identifier);
        }

        if (symbol == null && nextSymbolResolver != null) {
            nextSymbolResolver.resolveClassLikeIdentifier(identifier);
        }
        return symbol;
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

    private ISymbol resolveRelativeIdentifierConsiderAlias(ITSPHPAst identifier) {
        ISymbol symbol;

        String alias = getPotentialAlias(identifier.getText());
        INamespaceScope namespaceScope = scopeHelper.getEnclosingNamespaceScope(identifier);
        ITSPHPAst useDefinition = namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias);

        if (useDefinition != null) {
            symbol = resolveAlias(useDefinition, alias, identifier);
        } else {
            symbol = resolveRelativeIdentifier(identifier, namespaceScope);
        }

        return symbol;
    }

    private ISymbol resolveLocalIdentifierConsiderAlias(ITSPHPAst identifier) {
        INamespaceScope namespaceScope = scopeHelper.getEnclosingNamespaceScope(identifier);
        ISymbol symbol = namespaceScope.resolve(identifier);

        ITSPHPAst useDefinition = namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier.getText());
        useDefinition = checkTypeNameClashAndRecoverIfNecessary(useDefinition, symbol);

        if (useDefinition != null) {
            symbol = resolveAlias(useDefinition, identifier.getText(), identifier);
        }
        return symbol;
    }

    private String getPotentialAlias(String typeName) {
        String alias = typeName;
        int backslashPosition = typeName.indexOf("\\");
        if (backslashPosition != -1) {
            alias = typeName.substring(0, backslashPosition);
        }
        return alias;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private ITSPHPAst checkTypeNameClashAndRecoverIfNecessary(ITSPHPAst useDefinition, ISymbol typeSymbol) {
        ITSPHPAst resultingUseDefinition = useDefinition;
        if (hasTypeNameClash(useDefinition, typeSymbol)) {
            ITSPHPAst typeDefinition = typeSymbol.getDefinitionAst();
            if (!useDefinition.isDefinedEarlierThan(typeDefinition)) {
                //we do not use the alias if it was defined later than the typeSymbol
                resultingUseDefinition = null;
            }
        }
        return resultingUseDefinition;
    }

    private boolean hasTypeNameClash(ITSPHPAst useDefinition, ISymbol symbol) {
        return useDefinition != null && symbol != null && symbol.getDefinitionScope().equals(useDefinition.getScope());
    }

    private ISymbol resolveAlias(ITSPHPAst useDefinition, String alias, ITSPHPAst typeAst) {
        ISymbol symbol;

        //TODO rstoll  TINS-232 - reference phase - forward reference check use definition
//        if (useDefinition.isDefinedEarlierThan(typeAst)) {
        symbol = useDefinition.getSymbol().getType();
        String originalName = typeAst.getText();
        if (isUsedAsNamespace(alias, originalName)) {
            String typeName = getAbsoluteTypeName((ITypeSymbol) symbol, alias, originalName);

            IGlobalNamespaceScope globalNamespaceScope =
                    scopeHelper.getCorrespondingGlobalNamespace(globalNamespaceScopes, typeName);

            if (globalNamespaceScope != null) {
                typeAst.setText(typeName);
                symbol = globalNamespaceScope.resolve(typeAst);
                typeAst.setText(originalName);
            } else {
                symbol = null;
            }

        }
        //TODO rstoll  TINS-232 - reference phase - forward reference check use definition
//        } else {
//            DefinitionException ex = inferenceErrorReporter.aliasForwardReference(typeAst, useDefinition);
//            symbol = symbolFactory.createErroneousTypeSymbol(typeAst, ex);
//        }
        return symbol;
    }

    private String getAbsoluteTypeName(ITypeSymbol typeSymbol, String alias, String typeName) {
        String fullTypeName;
        //alias does not point to a real type
        if (typeSymbol instanceof IAliasTypeSymbol) {
            fullTypeName = typeSymbol.getName() + "\\";
        } else {
            fullTypeName = typeSymbol.getDefinitionScope().getScopeName() + typeSymbol.getName() + "\\";
        }
        if (!fullTypeName.substring(0, 1).equals("\\")) {
            fullTypeName = "\\" + fullTypeName;
        }
        return fullTypeName + typeName.substring(alias.length() + 1);
    }

    private boolean isUsedAsNamespace(String alias, String typeName) {
        return !alias.equals(typeName);
    }

    private ISymbol resolveRelativeIdentifier(ITSPHPAst identifier, INamespaceScope enclosingScope) {
        String relativeName = identifier.getText();
        String absoluteName = enclosingScope.getScopeName() + relativeName;
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
