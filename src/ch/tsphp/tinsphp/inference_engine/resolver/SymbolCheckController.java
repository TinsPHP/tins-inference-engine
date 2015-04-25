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
import ch.tsphp.tinsphp.common.checking.AlreadyDefinedAsTypeResultDto;
import ch.tsphp.tinsphp.common.checking.DoubleDefinitionCheckResultDto;
import ch.tsphp.tinsphp.common.checking.ForwardReferenceCheckResultDto;
import ch.tsphp.tinsphp.common.checking.ISymbolCheckController;
import ch.tsphp.tinsphp.common.checking.VariableInitialisedResultDto;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolver;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.symbols.erroneous.IErroneousSymbol;
import ch.tsphp.tinsphp.common.symbols.erroneous.IErroneousVariableSymbol;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;

import java.util.List;

public class SymbolCheckController implements ISymbolCheckController
{
    private final ISymbolResolver userSymbolResolver;
    private final List<ISymbolResolver> symbolResolvers;


    public SymbolCheckController(
            ISymbolResolver theUserSymbolResolver, List<ISymbolResolver> additionalSymbolResolvers) {
        userSymbolResolver = theUserSymbolResolver;
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
        ISymbol firstDefinitionSymbol = null;
        for (ISymbolResolver symbolResolver : symbolResolvers) {
            firstDefinitionSymbol = symbolResolver.resolveIdentifierFromItsScope(identifier);
            if (firstDefinitionSymbol != null) {
                break;
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
        ISymbol firstDefinitionSymbol = resolveIdentifierFromItsScopeCaseInsensitive(identifier);
        return createDoubleDefinitionCheckResultDto(firstDefinitionSymbol, identifier.getSymbol());
    }

    private ISymbol resolveIdentifierFromItsScopeCaseInsensitive(ITSPHPAst identifier) {
        ISymbol firstDefinitionSymbol = null;
        for (ISymbolResolver symbolResolver : symbolResolvers) {
            firstDefinitionSymbol = symbolResolver.resolveIdentifierFromItsScopeCaseInsensitive(identifier);
            if (firstDefinitionSymbol != null) {
                break;
            }
        }
        if (firstDefinitionSymbol == null) {
            firstDefinitionSymbol = userSymbolResolver.resolveIdentifierFromItsScopeCaseInsensitive(identifier);
        }
        return firstDefinitionSymbol;
    }

    @Override
    public DoubleDefinitionCheckResultDto isNotUseDoubleDefinition(ITSPHPAst alias) {
        ISymbol symbol = alias.getSymbol();
        ISymbol firstSymbol = ((INamespaceScope) symbol.getDefinitionScope())
                .getCaseInsensitiveFirstUseSymbol(alias.getText());

        return createDoubleDefinitionCheckResultDto(firstSymbol, symbol);
    }

    @Override
    public AlreadyDefinedAsTypeResultDto isNotAlreadyDefinedAsType(ITSPHPAst alias) {
        ITypeSymbol typeSymbol = (ITypeSymbol) resolveIdentifierFromItsScopeCaseInsensitive(alias);

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
        if (!(symbol instanceof IErroneousVariableSymbol) && isNotSuperGlobal(symbol)) {
            IScope scope = variableId.getScope();
            result.isFullyInitialised = scope.isFullyInitialised(symbol) || isLeftHandSideOfAssignment(variableId);
            if (!result.isFullyInitialised) {
                result.isPartiallyInitialised = scope.isPartiallyInitialised(symbol);
            }
        }
        return result;
    }

    private boolean isNotSuperGlobal(ISymbol symbol) {
        //quick and dirty, if it has got a namespace, then it cannot be a super global
        return symbol.getDefinitionScope() != null;
    }

    private boolean isLeftHandSideOfAssignment(ITSPHPAst variableId) {
        ITSPHPAst parent = (ITSPHPAst) variableId.getParent();
        return parent.getType() == TokenTypes.Assign && parent.getChild(0).equals(variableId);
    }

}
