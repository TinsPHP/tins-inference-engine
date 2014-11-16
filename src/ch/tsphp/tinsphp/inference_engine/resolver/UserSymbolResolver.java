/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.resolver;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.exceptions.DefinitionException;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IAliasTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ISymbolResolver;
import ch.tsphp.tinsphp.common.symbols.erroneous.ILazySymbolResolver;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;

public class UserSymbolResolver implements ISymbolResolver
{
    private final IScopeHelper scopeHelper;
    private final ISymbolFactory symbolFactory;
    private final IInferenceErrorReporter inferenceErrorReporter;
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
        inferenceErrorReporter = theInferenceErrorReporter;
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
            //forward to nextSymbolResolver within resolveAbsoluteIdentifier if necessary
            symbol = resolveAbsoluteIdentifier(identifier);
        } else if (scopeHelper.isRelativeIdentifier(identifier.getText())) {
            //forward to nextSymbolResolver within resolveRelativeIdentifierConsiderAlias if necessary
            symbol = resolveRelativeIdentifierConsiderAlias(identifier);
        } else {
            //forward to nextSymbolResolver within resolveLocalIdentifierConsiderAlias if necessary
            symbol = resolveLocalIdentifierConsiderAlias(identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveAbsoluteIdentifier(ITSPHPAst identifier) {
        ISymbol symbol = null;

        IGlobalNamespaceScope scope = scopeHelper.getCorrespondingGlobalNamespace(
                globalNamespaceScopes, identifier.getText());
        if (scope != null) {
            symbol = scope.resolve(identifier);
        }

        if (symbol == null && nextSymbolResolver != null) {
            symbol = nextSymbolResolver.resolveAbsoluteIdentifier(identifier);
        }
        return symbol;
    }

    private ISymbol resolveRelativeIdentifierConsiderAlias(ITSPHPAst identifier) {
        String alias = getPotentialAlias(identifier.getText());
        INamespaceScope namespaceScope = scopeHelper.getEnclosingNamespaceScope(identifier);
        ITSPHPAst useDefinition = namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias);

        ISymbol symbol;
        if (useDefinition != null) {
            //forward to nextSymbolResolver within resolveAlias if necessary
            symbol = resolveAlias(useDefinition, alias, identifier);
        } else {
            //forward to nextSymbolResolver within resolveRelativeIdentifier if necessary
            symbol = resolveRelativeIdentifier(identifier);
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

    private ISymbol resolveAlias(ITSPHPAst useDefinition, String alias, ITSPHPAst identifier) {
        ISymbol symbol;

        ILazySymbolResolver lazySymbolResolver = createLazySymbolResolver(useDefinition, identifier, alias);

        if (useDefinition.isDefinedEarlierThan(identifier)) {
            //forward to nextSymbolResolver within lazySymbolResolver.resolve();
            symbol = lazySymbolResolver.resolve();
        } else {
            //no forward to nextSymbolResolver since a forward usage of a use definition detected
            //resolve of the symbol lazily (with forward to nextSymbolResolverIfNecessary)
            DefinitionException ex = inferenceErrorReporter.aliasForwardReference(identifier, useDefinition);
            symbol = symbolFactory.createErroneousLazySymbol(lazySymbolResolver, identifier, ex);
        }
        return symbol;
    }

    private ILazySymbolResolver createLazySymbolResolver(
            final ITSPHPAst useDefinition,
            final ITSPHPAst identifier,
            final String alias) {
        return new ILazySymbolResolver()
        {
            @Override
            public ISymbol resolve() {
                ISymbol symbol = useDefinition.getSymbol().getType();
                String originalName = identifier.getText();
                if (isUsedAsNamespace(alias, originalName)) {
                    String typeName = getAbsoluteTypeName((ITypeSymbol) symbol, alias, originalName);
                    identifier.setText(typeName);
                    //forward to nextSymbolResolver within resolveAbsoluteIdentifier if necessary
                    symbol = resolveAbsoluteIdentifier(identifier);
                    identifier.setText(originalName);
                }
                return symbol;
            }
        };
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

    @Override
    public ISymbol resolveRelativeIdentifier(ITSPHPAst identifier) {
        String relativeName = identifier.getText();
        String absoluteName = scopeHelper.getEnclosingNamespaceScope(identifier).getScopeName() + relativeName;
        identifier.setText(absoluteName);
        ISymbol symbol = resolveAbsoluteIdentifier(identifier);
        identifier.setText(relativeName);
        return symbol;
    }

    private ISymbol resolveLocalIdentifierConsiderAlias(ITSPHPAst identifier) {
        ISymbol symbol = resolveLocalIdentifier(identifier);

        INamespaceScope namespaceScope = scopeHelper.getEnclosingNamespaceScope(identifier);
        ITSPHPAst useDefinition = namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier.getText());
        useDefinition = checkTypeNameClashAndRecoverIfNecessary(useDefinition, symbol);

        if (useDefinition != null) {
            symbol = resolveAlias(useDefinition, identifier.getText(), identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveLocalIdentifier(ITSPHPAst identifier) {
        ISymbol symbol = scopeHelper.getEnclosingNamespaceScope(identifier).resolve(identifier);

        if (symbol == null && nextSymbolResolver != null) {
            symbol = nextSymbolResolver.resolveLocalIdentifier(identifier);
        }
        return symbol;
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

    @Override
    public void setNextInChain(ISymbolResolver next) {
        nextSymbolResolver = next;
    }
}
