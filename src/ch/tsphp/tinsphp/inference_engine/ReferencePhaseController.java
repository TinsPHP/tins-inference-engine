/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ReferencePhaseController from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ITSPHPErrorAst;
import ch.tsphp.common.exceptions.ReferenceException;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.common.symbols.modifiers.IModifierSet;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.checking.AlreadyDefinedAsTypeResultDto;
import ch.tsphp.tinsphp.common.checking.DoubleDefinitionCheckResultDto;
import ch.tsphp.tinsphp.common.checking.ForwardReferenceCheckResultDto;
import ch.tsphp.tinsphp.common.checking.ISymbolCheckController;
import ch.tsphp.tinsphp.common.checking.VariableInitialisedResultDto;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolverController;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.IOverloadSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.erroneous.IErroneousTypeSymbol;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import ch.tsphp.tinsphp.symbols.ModifierSet;
import ch.tsphp.tinsphp.symbols.PrimitiveTypeNames;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;
import org.antlr.runtime.RecognitionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReferencePhaseController implements IReferencePhaseController
{
    private final ISymbolFactory symbolFactory;
    private final IInferenceIssueReporter inferenceErrorReporter;
    private final IAstModificationHelper astModificationHelper;
    private final ISymbolResolverController symbolResolverController;
    private final ISymbolCheckController symbolCheckController;
    private final IVariableDeclarationCreator variableDeclarationCreator;
    private final IScopeHelper scopeHelper;
    private final IModifierHelper modifierHelper;
    private final Map<String, ITypeSymbol> primitiveTypes;
    private final Map<Integer, IOverloadSymbol> operators;
    private final IGlobalNamespaceScope globalDefaultNamespace;

    public ReferencePhaseController(
            ISymbolFactory theSymbolFactory,
            IInferenceIssueReporter theInferenceErrorReporter,
            IAstModificationHelper theAstModificationHelper,
            ISymbolResolverController theSymbolResolverController,
            ISymbolCheckController theSymbolCheckController,
            IVariableDeclarationCreator theVariableDeclarationCreator,
            IScopeHelper theScopeHelper,
            IModifierHelper theModifierHelper,
            ICore theCore,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        symbolFactory = theSymbolFactory;
        inferenceErrorReporter = theInferenceErrorReporter;
        astModificationHelper = theAstModificationHelper;
        symbolResolverController = theSymbolResolverController;
        symbolCheckController = theSymbolCheckController;
        variableDeclarationCreator = theVariableDeclarationCreator;
        scopeHelper = theScopeHelper;
        modifierHelper = theModifierHelper;
        primitiveTypes = theCore.getPrimitiveTypes();
        operators = theCore.getOperators();
        globalDefaultNamespace = theGlobalDefaultNamespace;
    }

    @Override
    public IVariableSymbol resolveConstant(ITSPHPAst identifier) {
        IVariableSymbol symbol = (IVariableSymbol) symbolResolverController.resolveConstantLikeIdentifier(identifier);
        if (symbol == null) {
            ReferenceException exception = inferenceErrorReporter.notDefined(identifier);
            symbol = symbolFactory.createErroneousVariableSymbol(identifier, exception);
        }
        return symbol;
    }


    //TODO rstoll TINS-223 reference phase - resolve this and self
//    @Override
//    public IVariableSymbol resolveThisSelf(ITSPHPAst ast) {
//        return resolveThis(getEnclosingClass(ast), ast);
//    }

//    private IClassTypeSymbol getEnclosingClass(ITSPHPAst ast) {
//        IClassTypeSymbol classTypeSymbol = symbolResolverController.getEnclosingClass(ast);
//        if (classTypeSymbol == null) {
//            ReferenceException ex = typeCheckErrorReporter.notInClass(ast);
//            classTypeSymbol = symbolFactory.createErroneousTypeSymbol(ast, ex);
//        }
//        return classTypeSymbol;
//    }
//
//    private IVariableSymbol resolveThis(IClassTypeSymbol classTypeSymbol, ITSPHPAst $this) {
//        IVariableSymbol variableSymbol;
//        if (classTypeSymbol != null) {
//            variableSymbol = classTypeSymbol.getThis();
//            if (variableSymbol == null) {
//                variableSymbol = symbolFactory.createThisSymbol($this, classTypeSymbol);
//                classTypeSymbol.setThis(variableSymbol);
//            }
//        } else {
//            ReferenceException exception = typeCheckErrorReporter.notInClass($this);
//            variableSymbol = symbolFactory.createErroneousVariableSymbol($this, exception);
//        }
//        return variableSymbol;
//    }

    //TODO rstoll TINS-225 reference phase - resolve parent
//    @Override
//    public IVariableSymbol resolveParent(ITSPHPAst ast) {
//        return resolveThis(getParent(ast), ast);
//    }
//
//    private IClassTypeSymbol getParent(ITSPHPAst ast) {
//        IClassTypeSymbol classTypeSymbol = getEnclosingClass(ast);
//        IClassTypeSymbol parent = classTypeSymbol.getParent();
//        if (parent == null) {
//            TypeCheckerException ex = typeCheckErrorReporter.noParentClass(ast);
//            parent = symbolFactory.createErroneousTypeSymbol(ast, ex);
//        }
//        return parent;
//    }

    @Override
    public IVariableSymbol resolveVariable(ITSPHPAst variableId) {
        IVariableSymbol variableSymbol =
                (IVariableSymbol) symbolResolverController.resolveVariableLikeIdentifier(variableId);
        if (variableSymbol == null) {
            variableSymbol = variableDeclarationCreator.create(variableId);
        }
        return variableSymbol;
    }

    @Override
    public IMethodSymbol resolveFunction(ITSPHPAst identifier) {
        IMethodSymbol methodSymbol = (IMethodSymbol) symbolResolverController.resolveConstantLikeIdentifier(identifier);
        if (methodSymbol == null) {
            ReferenceException exception = inferenceErrorReporter.notDefined(identifier);
            methodSymbol = symbolFactory.createErroneousMethodSymbol(identifier, exception);
        }
        return methodSymbol;
    }

    @Override
    public IOverloadSymbol resolveOperator(ITSPHPAst operator) {
        return operators.get(operator.getType());
    }

    @Override
    public ITypeSymbol resolvePrimitiveType(ITSPHPAst typeAst, ITSPHPAst typeModifierAst) {
        return resolveType(
                typeAst,
                typeModifierAst,
                new IResolveTypeCaller()
                {
                    @Override
                    public ITypeSymbol resolve(ITSPHPAst type) {
                        String typeName = type.getText();
                        return primitiveTypes.get(typeName.charAt(0) == '\\' ? typeName : "\\" + typeName);
                    }
                }
        );
    }

    @Override
    public ITypeSymbol resolveType(ITSPHPAst typeAst, ITSPHPAst typeModifierAst) {
        ITypeSymbol symbol = resolveType(
                typeAst,
                typeModifierAst,
                new IResolveTypeCaller()
                {
                    @Override
                    public ITypeSymbol resolve(ITSPHPAst typeAst) {
                        return (ITypeSymbol) symbolResolverController.resolveClassLikeIdentifier(typeAst);
                    }
                }
        );

        //TODO rstoll LazyEvaluatedTypeSymbol has to be considered something similar to the code below but with
        //ILazySymbolResolver
//        if (symbol instanceof IAliasTypeSymbol) {
//            typeAst.setText(symbol.getName());
//            ReferenceException ex = inferenceErrorReporter.unknownType(typeAst);
//            symbol = symbolFactory.createErroneousTypeSymbol(symbol.getDefinitionAst(), ex);
//        }

        return symbol;
    }

    private ITypeSymbol resolveType(ITSPHPAst typeAst, ITSPHPAst typeModifierAst, IResolveTypeCaller resolveCaller) {

        ITypeSymbol typeSymbol = resolveCaller.resolve(typeAst);

        if (typeSymbol == null) {
            ReferenceException ex = inferenceErrorReporter.unknownType(typeAst);
            typeSymbol = symbolFactory.createErroneousTypeSymbol(typeAst, ex);
        }

        typeSymbol = createUnionTypeIfNullableOrFalseable(typeModifierAst, typeSymbol);

        return typeSymbol;
    }

    private ITypeSymbol createUnionTypeIfNullableOrFalseable(ITSPHPAst typeModifierAst, ITypeSymbol typeSymbol) {
        ITypeSymbol result = typeSymbol;

        IModifierSet modifiers = typeModifierAst != null
                ? modifierHelper.getModifiers(typeModifierAst)
                : new ModifierSet();

        final int maxNumberOfTypesInUnion = 3;
        Map<String, ITypeSymbol> types = new HashMap<>(maxNumberOfTypesInUnion);
        ITypeSymbol nullTypeSymbol = primitiveTypes.get(PrimitiveTypeNames.NULL);
        if (modifiers.isNullable() && typeSymbol != nullTypeSymbol) {
            types.put(PrimitiveTypeNames.NULL, nullTypeSymbol);
        }

        ITypeSymbol falseTypeSymbol = primitiveTypes.get(PrimitiveTypeNames.FALSE);
        if (modifiers.isFalseable() && typeSymbol != falseTypeSymbol) {
            types.put(PrimitiveTypeNames.FALSE, falseTypeSymbol);
        }

        if (!types.isEmpty()) {
            types.put(getAbsoluteType(typeSymbol), typeSymbol);
            result = symbolFactory.createUnionTypeSymbol(types);
        }
        return result;
    }

    private String getAbsoluteType(ITypeSymbol typeSymbol) {
        return typeSymbol.getDefinitionScope().getScopeName() + typeSymbol.getName();
    }

    @Override
    public ITypeSymbol resolveUseType(ITSPHPAst typeName, ITSPHPAst alias) {
        //Alias is always pointing to a full type name. If user has omitted \ at the beginning, then we add it here
        String identifier = typeName.getText();
        if (!scopeHelper.isAbsoluteIdentifier(identifier)) {
            identifier = "\\" + identifier;
            typeName.setText(identifier);
        }

        ITypeSymbol aliasTypeSymbol =
                (ITypeSymbol) symbolResolverController.resolveIdentifierFromItsNamespaceScope(typeName);

        if (aliasTypeSymbol == null) {
            aliasTypeSymbol = symbolFactory.createAliasTypeSymbol(typeName, typeName.getText());
        }
        return aliasTypeSymbol;
    }

    @Override
    public ITypeSymbol resolvePrimitiveLiteral(ITSPHPAst literal) {
        ITypeSymbol typeSymbol;
        switch (literal.getType()) {
            case TokenTypes.Null:
                typeSymbol = primitiveTypes.get(PrimitiveTypeNames.NULL);
                break;
            case TokenTypes.False:
                typeSymbol = primitiveTypes.get(PrimitiveTypeNames.FALSE);
                break;
            case TokenTypes.True:
                typeSymbol = primitiveTypes.get(PrimitiveTypeNames.TRUE);
                break;
            case TokenTypes.Int:
                typeSymbol = primitiveTypes.get(PrimitiveTypeNames.INT);
                break;
            case TokenTypes.Float:
                typeSymbol = primitiveTypes.get(PrimitiveTypeNames.FLOAT);
                break;
            case TokenTypes.String:
                typeSymbol = primitiveTypes.get(PrimitiveTypeNames.STRING);
                break;
            case TokenTypes.TypeArray:
                typeSymbol = primitiveTypes.get(PrimitiveTypeNames.ARRAY);
                break;
            default:
                typeSymbol = null;
                break;
        }
        return typeSymbol;
    }

    @Override
    public IErroneousTypeSymbol createErroneousTypeSymbol(ITSPHPErrorAst erroneousTypeAst) {
        return symbolFactory.createErroneousTypeSymbol(erroneousTypeAst, erroneousTypeAst.getException());
    }

    @Override
    public IErroneousTypeSymbol createErroneousTypeSymbol(ITSPHPAst typeAst, RecognitionException ex) {
        return symbolFactory.createErroneousTypeSymbol(typeAst, new TSPHPException(ex));
    }

    @Override
    public boolean checkIsNotDoubleDefinition(ITSPHPAst identifier) {
        DoubleDefinitionCheckResultDto result = symbolCheckController.isNotDoubleDefinition(identifier);
        if (!result.isNotDoubleDefinition) {
            inferenceErrorReporter.alreadyDefined(result.existingSymbol, identifier.getSymbol());
        }
        return result.isNotDoubleDefinition;
    }

    @Override
    public boolean checkIsNotDoubleDefinitionCaseInsensitive(ITSPHPAst identifier) {
        DoubleDefinitionCheckResultDto result
                = symbolCheckController.isNotDoubleDefinitionCaseInsensitive(identifier);
        if (!result.isNotDoubleDefinition) {
            inferenceErrorReporter.alreadyDefined(result.existingSymbol, identifier.getSymbol());
        }
        return result.isNotDoubleDefinition;
    }

    @Override
    public boolean checkUseDefinition(ITSPHPAst alias) {
        boolean isNotDoubleDefined = checkIsNotDoubleUseDefinition(alias);
        return isNotDoubleDefined && isNotAlreadyDefinedAsType(alias);
    }

    private boolean checkIsNotDoubleUseDefinition(ITSPHPAst alias) {
        DoubleDefinitionCheckResultDto result = symbolCheckController.isNotUseDoubleDefinition(alias);
        if (!result.isNotDoubleDefinition) {
            inferenceErrorReporter.alreadyDefined(result.existingSymbol, alias.getSymbol());
        }
        return result.isNotDoubleDefinition;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private boolean isNotAlreadyDefinedAsType(ITSPHPAst alias) {
        AlreadyDefinedAsTypeResultDto result = symbolCheckController.isNotAlreadyDefinedAsType(alias);
        if (!result.isNotAlreadyDefinedAsType) {
            inferenceErrorReporter.determineAlreadyDefined(alias.getSymbol(), result.typeSymbol);
        }
        return result.isNotAlreadyDefinedAsType;
    }

    @Override
    public boolean checkIsNotForwardReference(ITSPHPAst identifier) {
        ForwardReferenceCheckResultDto result = symbolCheckController.isNotForwardReference(identifier);
        if (!result.isNotForwardReference) {
            inferenceErrorReporter.forwardReference(result.definitionAst, identifier);
        }
        return result.isNotForwardReference;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public boolean checkIsVariableInitialised(ITSPHPAst variableId) {
        VariableInitialisedResultDto result = symbolCheckController.isVariableInitialised(variableId);
        if (!result.isFullyInitialised) {
            if (result.isPartiallyInitialised) {
                inferenceErrorReporter.variablePartiallyInitialised(
                        variableId.getSymbol().getDefinitionAst(), variableId);
            } else {
                inferenceErrorReporter.variableNotInitialised(variableId.getSymbol().getDefinitionAst(), variableId);
            }
        }
        return result.isFullyInitialised;
    }

    @Override
    public void transferInitialisedSymbolsFromGlobalDefault(ITSPHPAst namespace) {
        //no need to transfer symbols from default to default
        if (!namespace.getText().equals("\\")) {
            transferInitialisedSymbolsFromTo(
                    globalDefaultNamespace.getInitialisedSymbols(), namespace.getScope().getInitialisedSymbols());
        }
    }

    @Override
    public void transferInitialisedSymbolsToGlobalDefault(ITSPHPAst namespace) {
        //no need to transfer symbols from default to default
        if (!namespace.getText().equals("\\")) {
            transferInitialisedSymbolsFromTo(
                    namespace.getScope().getInitialisedSymbols(), globalDefaultNamespace.getInitialisedSymbols());
        }
    }

    private void transferInitialisedSymbolsFromTo(Map<String, Boolean> from, Map<String, Boolean> to) {
        for (Map.Entry<String, Boolean> entry : from.entrySet()) {
            String symbolName = entry.getKey();
            if (doesNotContainOrIsPartiallyInitialised(to, symbolName)) {
                to.put(symbolName, entry.getValue());
            }
        }
    }

    @Override
    public void sendUpInitialisedSymbols(ITSPHPAst blockConditional) {
        IScope scope = blockConditional.getScope();
        Map<String, Boolean> enclosingInitialisedSymbols = scope.getEnclosingScope().getInitialisedSymbols();
        sendUptInitialisedSymbolsAsPartiallyInitialised(
                scope.getInitialisedSymbols().keySet(), enclosingInitialisedSymbols);
    }

    private void sendUptInitialisedSymbolsAsPartiallyInitialised(
            Set<String> scopeInitialisedSymbols, Map<String, Boolean> enclosingInitialisedSymbols) {
        for (String symbolName : scopeInitialisedSymbols) {
            if (!enclosingInitialisedSymbols.containsKey(symbolName)) {
                enclosingInitialisedSymbols.put(symbolName, false);
            }
        }
    }

    @Override
    public void sendUpInitialisedSymbolsAfterIf(ITSPHPAst ifBlock, ITSPHPAst elseBlock) {
        if (elseBlock != null) {
            List<ITSPHPAst> conditionalBlocks = new ArrayList<>();
            conditionalBlocks.add(ifBlock);
            conditionalBlocks.add(elseBlock);
            sendUpInitialisedSymbolsAfterTryCatch(conditionalBlocks);
        } else {
            IScope scope = ifBlock.getScope();
            sendUptInitialisedSymbolsAsPartiallyInitialised(
                    scope.getInitialisedSymbols().keySet(),
                    scope.getEnclosingScope().getInitialisedSymbols()
            );
        }
    }

    @Override
    public void sendUpInitialisedSymbolsAfterSwitch(List<ITSPHPAst> conditionalBlocks, boolean hasDefaultLabel) {
        if (hasDefaultLabel) {
            sendUpInitialisedSymbolsAfterTryCatch(conditionalBlocks);
        } else {
            Map<String, Boolean> enclosingInitialisedSymbols =
                    conditionalBlocks.get(0).getScope().getEnclosingScope().getInitialisedSymbols();
            for (ITSPHPAst block : conditionalBlocks) {
                //without default label they are only partially initialised
                sendUptInitialisedSymbolsAsPartiallyInitialised(
                        block.getScope().getInitialisedSymbols().keySet(),
                        enclosingInitialisedSymbols
                );
            }
        }
    }

    @Override
    public void sendUpInitialisedSymbolsAfterTryCatch(List<ITSPHPAst> conditionalBlocks) {
        if (conditionalBlocks.size() > 0) {
            Set<String> allKeys = new HashSet<>();
            Set<String> commonKeys = new HashSet<>();
            boolean isFirst = true;
            for (ITSPHPAst block : conditionalBlocks) {
                Set<String> keys = block.getScope().getInitialisedSymbols().keySet();
                allKeys.addAll(keys);
                if (!isFirst) {
                    commonKeys.retainAll(keys);
                } else {
                    commonKeys.addAll(keys);
                    isFirst = false;
                }
            }

            Map<String, Boolean> enclosingInitialisedSymbols =
                    conditionalBlocks.get(0).getScope().getEnclosingScope().getInitialisedSymbols();

            for (String symbolName : allKeys) {
                if (doesNotContainOrIsPartiallyInitialised(enclosingInitialisedSymbols, symbolName)) {
                    boolean isFullyInitialised = commonKeys.contains(symbolName);
                    if (isFullyInitialised) {
                        for (ITSPHPAst block : conditionalBlocks) {
                            if (!block.getScope().getInitialisedSymbols().get(symbolName)) {
                                isFullyInitialised = false;
                                break;
                            }
                        }
                    }
                    enclosingInitialisedSymbols.put(symbolName, isFullyInitialised);
                }
            }
        }
    }

    private boolean doesNotContainOrIsPartiallyInitialised(Map<String, Boolean> enclosingInitialisedSymbols,
            String symbolName) {
        return !enclosingInitialisedSymbols.containsKey(symbolName)
                || !enclosingInitialisedSymbols.get(symbolName);
    }


    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void addImplicitReturnStatementIfRequired(
            boolean isReturning, boolean hasAtLeastOneReturnOrThrow, ITSPHPAst identifier, ITSPHPAst block) {
        if (!isReturning) {
            addReturnNullAtTheEndOfScope(block);
            if (hasAtLeastOneReturnOrThrow) {
                inferenceErrorReporter.partialReturnFromFunction(identifier);
            } else {
                inferenceErrorReporter.noReturnFromFunction(identifier);
            }
        }
    }

    private void addReturnNullAtTheEndOfScope(ITSPHPAst block) {
        ITSPHPAst returnAst = astModificationHelper.getNullReturnStatement();
        returnAst.setScope(block.getScope());
        returnAst.setEvalType(primitiveTypes.get(PrimitiveTypeNames.NULL));
        block.addChild(returnAst);
    }

    //TODO rstoll TINS-228 - reference phase - evaluate if methods return
//    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
//    @Override
//    public void checkReturnsFromMethod(boolean isReturning, boolean hasAtLeastOneReturnOrThrow,
// ITSPHPAst identifier) {
//        if (!isReturning) {
//            if (hasAtLeastOneReturnOrThrow) {
//                typeCheckErrorReporter.partialReturnFromMethod(identifier);
//            } else {
//                typeCheckErrorReporter.noReturnFromMethod(identifier);
//            }
//        }
//    }


    /**
     * "Delegate" to resolve a type - e.g. resolve a primitive type
     */
    private interface IResolveTypeCaller
    {
        ITypeSymbol resolve(ITSPHPAst typeAst);
    }
}
