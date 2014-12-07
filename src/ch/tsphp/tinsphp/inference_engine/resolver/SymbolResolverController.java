/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.resolver;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.exceptions.DefinitionException;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolver;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolverController;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IAliasTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.erroneous.ILazySymbolResolver;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;

import java.util.List;

public class SymbolResolverController implements ISymbolResolverController
{
    private static final FromItsScopeResolverDelegate FROM_ITS_SCOPE_DELEGATE
            = new FromItsScopeResolverDelegate();
    private static final FromFallbackResolverDelegate FROM_FALLBACK_DELEGATE
            = new FromFallbackResolverDelegate();
    private static final FromItsNamespaceScopeResolverDelegate FROM_ITS_NAMESPACE_SCOPE_DELEGATE
            = new FromItsNamespaceScopeResolverDelegate();
    private static final AbsoluteResolverDelegate ABSOLUTE_RESOLVER_DELEGATE = new AbsoluteResolverDelegate();
    private static final FromSuperGlobalScopeResolverDelegate FROM_SUPER_GLOBAL_SCOPE_DELEGATE
            = new FromSuperGlobalScopeResolverDelegate();

    private final IScopeHelper scopeHelper;
    private final ISymbolFactory symbolFactory;
    private final IInferenceErrorReporter inferenceErrorReporter;

    private final ISymbolResolver userSymbolResolver;
    private final List<ISymbolResolver> symbolResolvers;


    public SymbolResolverController(
            ISymbolResolver theUserSymbolResolver,
            List<ISymbolResolver> additionalSymbolResolvers,
            IScopeHelper theScopeHelper,
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter) {
        userSymbolResolver = theUserSymbolResolver;
        symbolResolvers = additionalSymbolResolvers;
        scopeHelper = theScopeHelper;
        symbolFactory = theSymbolFactory;
        inferenceErrorReporter = theInferenceErrorReporter;
    }

    @Override
    public ISymbol resolveConstantLikeIdentifier(ITSPHPAst identifier) {
        ISymbol symbol;
        if (scopeHelper.isLocalIdentifier(identifier.getText())) {
            symbol = resolveIdentifierFromItsScopeWithFallback(identifier);
        } else {
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
            symbol = resolveIdentifierFromItsNamespaceScopeConsiderAlias(identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveVariableLikeIdentifier(ITSPHPAst identifier) {
        ISymbol symbol = resolve(FROM_ITS_SCOPE_DELEGATE, identifier);
        if (symbol == null) {
            symbol = resolve(FROM_FALLBACK_DELEGATE, identifier);
        }
        if (symbol == null) {
            symbol = resolve(FROM_SUPER_GLOBAL_SCOPE_DELEGATE, identifier);
        }
        return symbol;
    }

    private ISymbol resolveAbsoluteIdentifier(ITSPHPAst identifier) {
        return resolve(ABSOLUTE_RESOLVER_DELEGATE, identifier);
    }

    private ISymbol resolveRelativeIdentifierConsiderAlias(ITSPHPAst identifier) {
        String alias = getPotentialAlias(identifier.getText());
        INamespaceScope namespaceScope = scopeHelper.getEnclosingNamespaceScope(identifier);
        ITSPHPAst useDefinition = namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(alias);

        ISymbol symbol;
        if (useDefinition != null) {
            symbol = resolveAlias(useDefinition, alias, identifier);
        } else {
            symbol = resolveRelativeIdentifier(namespaceScope, identifier);
        }

        return symbol;
    }

    private String getPotentialAlias(String typeName) {
        String alias = typeName;
        int backslashPosition = typeName.indexOf('\\');
        if (backslashPosition != -1) {
            alias = typeName.substring(0, backslashPosition);
        }
        return alias;
    }

    private ISymbol resolveAlias(ITSPHPAst useDefinition, String alias, ITSPHPAst identifier) {
        ISymbol symbol;

        ILazySymbolResolver lazySymbolResolver = createLazySymbolResolver(useDefinition, identifier, alias);

        if (useDefinition.isDefinedEarlierThan(identifier)) {
            //forward to next symbol resolver within lazySymbolResolver.resolve();
            symbol = lazySymbolResolver.resolve();
        } else {
            //no forward to next symbol resolver since a forward usage of a use definition detected
            //resolve of the symbol lazily (with forward to next symbol resolverIfNecessary)
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
                    //forward to next symbol resolver within resolveAbsoluteIdentifier if necessary
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


    private ISymbol resolveRelativeIdentifier(INamespaceScope namespaceScope, ITSPHPAst identifier) {
        String relativeName = identifier.getText();
        String absoluteName = namespaceScope.getScopeName() + relativeName;
        identifier.setText(absoluteName);
        ISymbol symbol = resolveAbsoluteIdentifier(identifier);
        identifier.setText(relativeName);
        return symbol;
    }

    private ISymbol resolveIdentifierFromItsNamespaceScopeConsiderAlias(ITSPHPAst identifier) {
        ISymbol symbol = resolveIdentifierFromItsNamespaceScope(identifier);

        INamespaceScope namespaceScope = scopeHelper.getEnclosingNamespaceScope(identifier);
        ITSPHPAst useDefinition = namespaceScope.getCaseInsensitiveFirstUseDefinitionAst(identifier.getText());
        useDefinition = checkTypeNameClashAndRecoverIfNecessary(useDefinition, symbol);

        if (useDefinition != null) {
            symbol = resolveAlias(useDefinition, identifier.getText(), identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveIdentifierFromItsNamespaceScope(ITSPHPAst identifier) {
        return resolve(FROM_ITS_NAMESPACE_SCOPE_DELEGATE, identifier);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private ITSPHPAst checkTypeNameClashAndRecoverIfNecessary(ITSPHPAst useDefinition, ISymbol symbol) {
        ITSPHPAst resultingUseDefinition = useDefinition;
        if (hasTypeNameClash(useDefinition, symbol)) {
            ITSPHPAst definitionAst = symbol.getDefinitionAst();
            if (!useDefinition.isDefinedEarlierThan(definitionAst)) {
                //we do not use the alias if it was defined later than the symbol
                resultingUseDefinition = null;
            }
        }
        return resultingUseDefinition;
    }

    private boolean hasTypeNameClash(ITSPHPAst useDefinition, ISymbol symbol) {
        return useDefinition != null && symbol != null && symbol.getDefinitionScope().equals(useDefinition.getScope());
    }

    private ISymbol resolveIdentifierFromItsScopeWithFallback(ITSPHPAst identifier) {
        ISymbol symbol = resolveIdentifierFromItsScope(identifier);
        if (symbol == null) {
            symbol = resolveIdentifierFromFallback(identifier);
        }
        return symbol;
    }

    @Override
    public ISymbol resolveIdentifierFromItsScope(ITSPHPAst identifier) {
        return resolve(FROM_ITS_SCOPE_DELEGATE, identifier);
    }

    private ISymbol resolveIdentifierFromFallback(ITSPHPAst identifier) {
        return resolve(FROM_FALLBACK_DELEGATE, identifier);
    }

    private ISymbol resolve(IResolveDelegate resolveDelegate, ITSPHPAst identifier) {
        ISymbol symbol = resolveDelegate.resolve(userSymbolResolver, identifier);
        if (symbol == null) {
            for (ISymbolResolver symbolResolver : symbolResolvers) {
                symbol = resolveDelegate.resolve(symbolResolver, identifier);
                if (symbol != null) {
                    break;
                }
            }
        }
        return symbol;
    }

    private interface IResolveDelegate
    {
        ISymbol resolve(ISymbolResolver symbolResolver, ITSPHPAst identifier);
    }

    private static final class AbsoluteResolverDelegate implements IResolveDelegate
    {

        @Override
        public ISymbol resolve(ISymbolResolver symbolResolver, ITSPHPAst identifier) {
            return symbolResolver.resolveAbsoluteIdentifier(identifier);
        }
    }

    private static final class FromFallbackResolverDelegate implements IResolveDelegate
    {

        @Override
        public ISymbol resolve(ISymbolResolver symbolResolver, ITSPHPAst identifier) {
            return symbolResolver.resolveIdentifierFromFallback(identifier);
        }
    }

    private static final class FromItsScopeResolverDelegate implements IResolveDelegate
    {

        @Override
        public ISymbol resolve(ISymbolResolver symbolResolver, ITSPHPAst identifier) {
            return symbolResolver.resolveIdentifierFromItsScope(identifier);
        }
    }

    private static final class FromItsNamespaceScopeResolverDelegate implements IResolveDelegate
    {

        @Override
        public ISymbol resolve(ISymbolResolver symbolResolver, ITSPHPAst identifier) {
            return symbolResolver.resolveIdentifierFromItsNamespaceScope(identifier);
        }
    }

    private static final class FromSuperGlobalScopeResolverDelegate implements IResolveDelegate
    {

        @Override
        public ISymbol resolve(ISymbolResolver symbolResolver, ITSPHPAst identifier) {
            return symbolResolver.resolveIdentifierFromSuperGlobalScope(identifier);
        }
    }

}
