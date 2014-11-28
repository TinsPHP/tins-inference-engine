/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.resolver;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.symbols.erroneous.IErroneousSymbol;
import ch.tsphp.tinsphp.common.symbols.erroneous.IErroneousVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.resolver.AlreadyDefinedAsTypeResultDto;
import ch.tsphp.tinsphp.common.symbols.resolver.DoubleDefinitionCheckResultDto;
import ch.tsphp.tinsphp.common.symbols.resolver.ForwardReferenceCheckResultDto;
import ch.tsphp.tinsphp.common.symbols.resolver.ISymbolCheckController;
import ch.tsphp.tinsphp.common.symbols.resolver.ISymbolResolver;
import ch.tsphp.tinsphp.common.symbols.resolver.ITypeSymbolResolver;
import ch.tsphp.tinsphp.common.symbols.resolver.VariableInitialisedResultDto;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;

import java.util.List;

public class SymbolCheckController implements ISymbolCheckController
{
    private final ITypeSymbolResolver typeSymbolResolver;
    private final ISymbolResolver userSymbolResolver;
    private final ISymbolResolver coreSymbolResolver;
    private final List<ISymbolResolver> symbolResolvers;


    public SymbolCheckController(
            ITypeSymbolResolver theTypeSymbolResolver, ISymbolResolver theUserSymbolResolver,
            ISymbolResolver theCoreSymbolResolver,
            List<ISymbolResolver> additionalSymbolResolvers) {
        typeSymbolResolver = theTypeSymbolResolver;
        userSymbolResolver = theUserSymbolResolver;
        coreSymbolResolver = theCoreSymbolResolver;
        symbolResolvers = additionalSymbolResolvers;
    }

    @Override
    public ForwardReferenceCheckResultDto isNotForwardReference(ITSPHPAst identifier) {
        ISymbol symbol = identifier.getSymbol();
        ForwardReferenceCheckResultDto result = new ForwardReferenceCheckResultDto();
        result.isNotForwardReference = true;

        // only check if not already an error occurred in conjunction with this identifier
        // (for instance missing declaration)
        if (!(symbol instanceof IErroneousSymbol)) {
            result.definitionAst = symbol.getDefinitionAst();
            result.isNotForwardReference = result.definitionAst.isDefinedEarlierThan(identifier);
        }
        return result;
    }

    @Override
    public DoubleDefinitionCheckResultDto isNotDoubleDefinition(ITSPHPAst identifier) {
        ISymbol firstDefinitionSymbol = coreSymbolResolver.resolveIdentifierFromItsScope(identifier);
        if (firstDefinitionSymbol == null) {
            for (ISymbolResolver symbolResolver : symbolResolvers) {
                firstDefinitionSymbol = symbolResolver.resolveIdentifierFromItsScope(identifier);
                if (firstDefinitionSymbol != null) {
                    break;
                }
            }
        }
        if (firstDefinitionSymbol == null) {
            firstDefinitionSymbol = userSymbolResolver.resolveIdentifierFromItsScope(identifier);
        }
        return createDoubleDefinitionCheckResultDto(firstDefinitionSymbol, identifier.getSymbol());
    }

    private DoubleDefinitionCheckResultDto createDoubleDefinitionCheckResultDto(
            ISymbol foundSymbol, ISymbol givenSymbol) {
        assert foundSymbol != null;
        return new DoubleDefinitionCheckResultDto(foundSymbol.equals(givenSymbol), foundSymbol);
    }

    @Override
    public DoubleDefinitionCheckResultDto isNotDoubleDefinitionCaseInsensitive(ITSPHPAst identifier) {
        ISymbol firstDefinitionSymbol = coreSymbolResolver.resolveIdentifierFromItsScopeCaseInsensitive(identifier);
        if (firstDefinitionSymbol == null) {
            for (ISymbolResolver symbolResolver : symbolResolvers) {
                firstDefinitionSymbol = symbolResolver.resolveIdentifierFromItsScopeCaseInsensitive(identifier);
                if (firstDefinitionSymbol != null) {
                    break;
                }
            }
        }
        if (firstDefinitionSymbol == null) {
            firstDefinitionSymbol = userSymbolResolver.resolveIdentifierFromItsScopeCaseInsensitive(identifier);
        }
        return createDoubleDefinitionCheckResultDto(firstDefinitionSymbol, identifier.getSymbol());
    }

    @Override
    public DoubleDefinitionCheckResultDto isNotDoubleUseDefinition(ITSPHPAst alias) {
        ISymbol symbol = alias.getSymbol();
        ISymbol firstSymbol = ((INamespaceScope) symbol.getDefinitionScope())
                .getCaseInsensitiveFirstUseSymbol(alias.getText());

        return createDoubleDefinitionCheckResultDto(firstSymbol, symbol);
    }

    @Override
    public AlreadyDefinedAsTypeResultDto isNotAlreadyDefinedAsType(ITSPHPAst alias) {
        ITypeSymbol typeSymbol = typeSymbolResolver.resolveTypeFor(alias);
        AlreadyDefinedAsTypeResultDto result = new AlreadyDefinedAsTypeResultDto(typeSymbol == null, typeSymbol);

        if (!result.isNotAlreadyDefinedAsType) {
            @SuppressWarnings("ConstantConditions")
            boolean isUseDefinedEarlier = alias.isDefinedEarlierThan(typeSymbol.getDefinitionAst());
            boolean isUseInDifferentNamespaceStatement = alias.getScope() != typeSymbol.getDefinitionScope();

            //There is no type name clash in the following situation: namespace{use a as b;} namespace{ class b{}}
            //because: use is defined earlier and the use statement is in a different namespace statement
            result.isNotAlreadyDefinedAsType = isUseDefinedEarlier && isUseInDifferentNamespaceStatement;
        }
        return result;
    }

    @Override
    public VariableInitialisedResultDto isVariableInitialised(ITSPHPAst variableId) {
        VariableInitialisedResultDto result = new VariableInitialisedResultDto();
        result.isFullyInitialised = true;
        ISymbol symbol = variableId.getSymbol();
        if (!(symbol instanceof IErroneousVariableSymbol)) {
            IScope scope = variableId.getScope();
            result.isFullyInitialised = scope.isFullyInitialised(symbol) || isLeftHandSideOfAssignment(variableId);
            if (!result.isFullyInitialised) {
                result.isPartiallyInitialised = scope.isPartiallyInitialised(symbol);
            }
        }
        return result;
    }

    private boolean isLeftHandSideOfAssignment(ITSPHPAst variableId) {
        ITSPHPAst parent = (ITSPHPAst) variableId.getParent();
        return parent.getType() == TokenTypes.Assign && parent.getChild(0).equals(variableId);
    }

}
