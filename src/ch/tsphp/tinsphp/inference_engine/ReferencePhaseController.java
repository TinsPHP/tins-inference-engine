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

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.exceptions.ReferenceException;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ISymbolResolver;
import ch.tsphp.tinsphp.common.symbols.ITypeSymbolResolver;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;

public class ReferencePhaseController implements IReferencePhaseController
{
    private final ISymbolFactory symbolFactory;
    private final IInferenceErrorReporter inferenceErrorReporter;
    private final IAstModificationHelper astModificationHelper;
    private final IModifierHelper modifierHelper;
    private final ISymbolResolver symbolResolver;
    private final ITypeSymbolResolver typeSymbolResolver;
    private final IScopeHelper scopeHelper;
    private final ICore core;
    private final IGlobalNamespaceScope globalDefaultNamespace;

    public ReferencePhaseController(
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter,
            IAstModificationHelper theAstModificationHelper,
            ISymbolResolver theSymbolResolver,
            ITypeSymbolResolver theTypeSymbolResolver,
            IScopeHelper theScopeHelper,
            ICore theCore,
            IModifierHelper theModifierHelper,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        symbolFactory = theSymbolFactory;
        inferenceErrorReporter = theInferenceErrorReporter;
        astModificationHelper = theAstModificationHelper;
        symbolResolver = theSymbolResolver;
        typeSymbolResolver = theTypeSymbolResolver;
        scopeHelper = theScopeHelper;
        core = theCore;
        modifierHelper = theModifierHelper;
        globalDefaultNamespace = theGlobalDefaultNamespace;
    }


    @Override
    public IVariableSymbol resolveConstant(ITSPHPAst ast) {
        IVariableSymbol symbol = (IVariableSymbol) symbolResolver.resolveConstantLikeIdentifier(ast);
        if (symbol == null) {
            ReferenceException exception = inferenceErrorReporter.notDefined(ast);
            symbol = symbolFactory.createErroneousVariableSymbol(ast, exception);
        }
        return symbol;
    }


    //TODO rstoll TINS-223 reference phase - resolve this and self
//    @Override
//    public IVariableSymbol resolveThisSelf(ITSPHPAst ast) {
//        return resolveThis(getEnclosingClass(ast), ast);
//    }

//    private IClassTypeSymbol getEnclosingClass(ITSPHPAst ast) {
//        IClassTypeSymbol classTypeSymbol = symbolResolver.getEnclosingClass(ast);
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

    //TODO TINS-208 reference phase - resolve variables
//    @Override
//    public IVariableSymbol resolveVariable(ITSPHPAst ast) {
//        ISymbol symbol = ast.getScope().resolve(ast);
//        if (symbol == null) {
//            ReferenceException exception = typeCheckErrorReporter.notDefined(ast);
//            symbol = symbolFactory.createErroneousVariableSymbol(ast, exception);
//        }
//        return (IVariableSymbol) symbol;
//    }

    //TODO rstoll TINS-224 reference phase - resolve types
//    @Override
//    public IScalarTypeSymbol resolveScalarType(ITSPHPAst typeAst, ITSPHPAst typeModifierAst) {
//        ITypeSymbol typeSymbol = resolveType(
//                typeAst,
//                typeModifierAst,
//                new IResolveTypeCaller()
//                {
//                    @Override
//                    public ITypeSymbol resolve(ITSPHPAst type) {
//                        return (ITypeSymbol) globalDefaultNamespace.resolve(type);
//                    }
//                },
//                new ISymbolCreateCaller()
//                {
//                    @Override
//                    public ITypeSymbol create(
//                            ITSPHPAst typeAst, ITypeSymbol typeSymbolWithoutModifier, IModifierSet modifiers) {
//
//                        IScalarTypeSymbol scalarTypeSymbol = (IScalarTypeSymbol) typeSymbolWithoutModifier;
//                        String defaultValueAsString = scalarTypeSymbol.getDefaultValueAsString();
//                        if (modifiers.isNullable()) {
//                            defaultValueAsString = "null";
//                        } else if (modifiers.isFalseable()) {
//                            defaultValueAsString = "false";
//                        }
//                        return symbolFactory.createScalarTypeSymbol(
//                                typeAst.getText(),
//                                scalarTypeSymbol.getTokenTypeForCasting(),
//                                scalarTypeSymbol.getParentTypeSymbols(),
//                                scalarTypeSymbol.getDefaultValueTokenType(),
//                                defaultValueAsString
//                        );
//                    }
//                }
//        );
//        return (IScalarTypeSymbol) typeSymbol;
//    }
//
//    @Override
//    public ITypeSymbol resolvePrimitiveType(ITSPHPAst typeAst, ITSPHPAst typeModifierAst) {
//        return resolveType(
//                typeAst,
//                typeModifierAst,
//                new IResolveTypeCaller()
//                {
//                    @Override
//                    public ITypeSymbol resolve(ITSPHPAst type) {
//                        return (ITypeSymbol) globalDefaultNamespace.resolve(type);
//                    }
//                },
//                new ISymbolCreateCaller()
//                {
//                    @Override
//                    public ITypeSymbol create(
//                            ITSPHPAst typeAst, ITypeSymbol typeSymbolWithoutModifier, IModifierSet modifiers) {
//                        return symbolFactory.createPseudoTypeSymbol(typeAst.getText());
//                    }
//                }
//        );
//    }
//
//    private ITypeSymbol resolveType(ITSPHPAst typeAst, ITSPHPAst typeModifierAst,
//            IResolveTypeCaller resolveCaller, ISymbolCreateCaller symbolCreateCaller) {
//
//        IModifierSet modifiers = typeModifierAst != null
//                ? modifierHelper.getModifiers(typeModifierAst)
//                : new ModifierSet();
//
//        String typeName = typeAst.getText();
//        String typeNameWithoutModifiers = typeName;
//
//        boolean isFalseableOrNullableOrBoth = false;
//        if (isFalseableAndTypeNameIsNot(modifiers, typeNameWithoutModifiers)) {
//            isFalseableOrNullableOrBoth = true;
//            typeName += "!";
//        }
//        if (isNullableAndTypeNameIsNot(modifiers, typeNameWithoutModifiers)) {
//            isFalseableOrNullableOrBoth = true;
//            typeName += "?";
//        }
//
//        typeAst.setText(typeName);
//        ITypeSymbol typeSymbol = resolveCaller.resolve(typeAst);
//
//        if (isFalseableOrNullableOrBoth && typeSymbol == null) {
//
//            typeAst.setText(typeNameWithoutModifiers);
//            ITypeSymbol typeSymbolWithoutModifier = resolveCaller.resolve(typeAst);
//            if (typeSymbolWithoutModifier != null) {
//                typeAst.setText(typeName);
//                ITypeSymbol newType = symbolCreateCaller.create(typeAst, typeSymbolWithoutModifier, modifiers);
//                if (modifiers.isFalseable()) {
//                    newType.addModifier(TSPHPDefinitionWalker.LogicNot);
//                }
//                if (modifiers.isNullable()) {
//                    newType.addModifier(TSPHPDefinitionWalker.QuestionMark);
//                }
//                typeSymbolWithoutModifier.getDefinitionScope().define(newType);
//                core.addExplicitCastFromTo(newType, typeSymbolWithoutModifier);
//                core.addImplicitCastFromTo(typeSymbolWithoutModifier, newType);
//                typeSymbol = newType;
//            }
//        }
//
//        if (typeSymbol == null) {
//            rewriteNameToAbsoluteType(typeAst);
//            ReferenceException ex = typeCheckErrorReporter.unknownType(typeAst);
//            typeSymbol = symbolFactory.createErroneousTypeSymbol(typeAst, ex);
//
//        }
//        return typeSymbol;
//
//    }
//
//    private boolean isFalseableAndTypeNameIsNot(IModifierSet modifiers, String typeNameWithoutModifiers) {
//        return modifiers.isFalseable()
//                && !typeNameWithoutModifiers.endsWith("!")
//                && !typeNameWithoutModifiers.endsWith("!?");
//    }
//
//    private boolean isNullableAndTypeNameIsNot(IModifierSet modifiers, String typeNameWithoutModifiers) {
//        return modifiers.isNullable() && !typeNameWithoutModifiers.endsWith("?");
//    }
//
//    /**
//     * Return the absolute name of a type which could not be found (prefix the enclosing namespace).
//     */
//    private void rewriteNameToAbsoluteType(ITSPHPAst typeAst) {
//        String typeName = typeAst.getText();
//        if (!symbolResolver.isAbsolute(typeName)) {
//            String namespace = symbolResolver.getEnclosingGlobalNamespaceScope(typeAst.getScope()).getScopeName();
//            typeAst.setText(namespace + typeName);
//        }
//    }
//
//    @Override
//    public ITypeSymbol resolveType(ITSPHPAst typeAst, ITSPHPAst typeModifierAst) {
//        ITypeSymbol symbol = resolveType(typeAst, typeModifierAst, new IResolveTypeCaller()
//                {
//                    @Override
//                    public ITypeSymbol resolve(ITSPHPAst typeAst) {
//                        return (ITypeSymbol) symbolResolver.resolveConstantLikeIdentifier(typeAst);
//                    }
//                },
//                new ISymbolCreateCaller()
//                {
//                    @Override
//                    public ITypeSymbol create(
//                            ITSPHPAst typeAst, ITypeSymbol typeSymbolWithoutModifier, IModifierSet modifiers) {
//
//                        IAstHelper astHelper = AstHelperRegistry.get();
//                        ITSPHPAst cMod = astHelper.createAst(TSPHPReferenceWalker.CLASS_MODIFIER, "cMod");
//                        for (int modifier : modifiers) {
//                            cMod.addChild(astHelper.createAst(modifier, "m" + modifier));
//                        }
//                        ITSPHPAst identifier =
//                                astHelper.createAst(TSPHPReferenceWalker.Identifier, typeAst.getText());
//
//                        if (typeSymbolWithoutModifier instanceof IClassTypeSymbol) {
//                            return symbolFactory.createClassTypeSymbol(
//                                    cMod, identifier, ((IClassTypeSymbol) typeSymbolWithoutModifier)
//                                            .getEnclosingScope());
//                        } else if (typeSymbolWithoutModifier instanceof IInterfaceTypeSymbol) {
//                            return symbolFactory.createInterfaceTypeSymbol(
//                                    cMod, identifier, ((IInterfaceTypeSymbol) typeSymbolWithoutModifier)
//                                            .getEnclosingScope());
//                        }
//                        return null;
//                    }
//                }
//        );
//
//        if (symbol instanceof IAliasTypeSymbol) {
//            typeAst.setText(symbol.getName());
//            ReferenceException ex = typeCheckErrorReporter.unknownType(typeAst);
//            symbol = symbolFactory.createErroneousTypeSymbol(symbol.getDefinitionAst(), ex);
//        }
//
//        return symbol;
//    }
//
//    @Override
//    public IErroneousTypeSymbol createErroneousTypeSymbol(ITSPHPErrorAst erroneousTypeAst) {
//        return symbolFactory.createErroneousTypeSymbol(erroneousTypeAst, erroneousTypeAst.getException());
//    }
//
//    @Override
//    public IErroneousTypeSymbol createErroneousTypeSymbol(ITSPHPAst typeAst, RecognitionException ex) {
//        return symbolFactory.createErroneousTypeSymbol(typeAst, new TSPHPException(ex));
//    }

    @Override
    public ITypeSymbol resolveUseType(ITSPHPAst typeName, ITSPHPAst alias) {
        //Alias is always pointing to a full type name. If user has omitted \ at the beginning, then we add it here
        String identifier = typeName.getText();
        if (!scopeHelper.isAbsoluteIdentifier(identifier)) {
            identifier = "\\" + identifier;
            typeName.setText(identifier);
        }

        ITypeSymbol aliasTypeSymbol = typeSymbolResolver.resolveTypeFor(typeName);
        if (aliasTypeSymbol == null) {
            aliasTypeSymbol = symbolFactory.createAliasTypeSymbol(typeName, typeName.getText());
        }
        return aliasTypeSymbol;
    }

    //TODO rstoll TINS-219 reference phase - check are variables initialised
//    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
//    @Override
//    public boolean checkVariableIsInitialised(ITSPHPAst variableId) {
//        IScope scope = variableId.getScope();
//        ISymbol symbol = variableId.getSymbol();
//        if (!(symbol instanceof IErroneousVariableSymbol)) {
//            if (!scope.isFullyInitialised(symbol) && isNotLeftHandSideOfAssignment(variableId)) {
//                ITypeCheckerErrorReporter errorReporter = typeCheckErrorReporter;
//                if (scope.isPartiallyInitialised(symbol)) {
//                    errorReporter.variablePartiallyInitialised(symbol.getDefinitionAst(), variableId);
//                } else {
//                    errorReporter.variableNotInitialised(symbol.getDefinitionAst(), variableId);
//                }
//                return false;
//            }
//        }
//        return true;
//    }
//
//    @Override
//    public void sendUpInitialisedSymbols(ITSPHPAst blockConditional) {
//        IScope scope = blockConditional.getScope();
//        Map<String, Boolean> enclosingInitialisedSymbols = scope.getEnclosingScope().getInitialisedSymbols();
//        for (Map.Entry<String, Boolean> entry : scope.getInitialisedSymbols().entrySet()) {
//            String symbolName = entry.getKey();
//            if (!enclosingInitialisedSymbols.containsKey(symbolName)) {
//                enclosingInitialisedSymbols.put(symbolName, false);
//            }
//        }
//    }
//
//
//    @Override
//    public void sendUpInitialisedSymbolsAfterIf(ITSPHPAst ifBlock, ITSPHPAst elseBlock) {
//        if (elseBlock != null) {
//            List<ITSPHPAst> conditionalBlocks = new ArrayList<>();
//            conditionalBlocks.add(ifBlock);
//            conditionalBlocks.add(elseBlock);
//            sendUpInitialisedSymbolsAfterTryCatch(conditionalBlocks);
//        } else {
//            IScope scope = ifBlock.getScope();
//            Map<String, Boolean> enclosingInitialisedSymbols = scope.getEnclosingScope().getInitialisedSymbols();
//            for (String symbolName : scope.getInitialisedSymbols().keySet()) {
//                if (!enclosingInitialisedSymbols.containsKey(symbolName)) {
//                    enclosingInitialisedSymbols.put(symbolName, false);
//                }
//            }
//        }
//    }
//
//    @Override
//    public void sendUpInitialisedSymbolsAfterSwitch(List<ITSPHPAst> conditionalBlocks, boolean hasDefaultLabel) {
//        if (hasDefaultLabel) {
//            sendUpInitialisedSymbolsAfterTryCatch(conditionalBlocks);
//        } else {
//            Map<String, Boolean> enclosingInitialisedSymbols =
//                    conditionalBlocks.get(0).getScope().getEnclosingScope().getInitialisedSymbols();
//            for (ITSPHPAst block : conditionalBlocks) {
//                for (String symbolName : block.getScope().getInitialisedSymbols().keySet()) {
//                    if (!enclosingInitialisedSymbols.containsKey(symbolName)) {
//                        //without default label they are only partially initialised
//                        enclosingInitialisedSymbols.put(symbolName, false);
//                    }
//                }
//            }
//        }
//    }
//
//    @Override
//    public void sendUpInitialisedSymbolsAfterTryCatch(List<ITSPHPAst> conditionalBlocks) {
//        if (conditionalBlocks.size() > 0) {
//            Set<String> allKeys = new HashSet<>();
//            Set<String> commonKeys = new HashSet<>();
//            boolean isFirst = true;
//            for (ITSPHPAst block : conditionalBlocks) {
//                Set<String> keys = block.getScope().getInitialisedSymbols().keySet();
//                allKeys.addAll(keys);
//                if (!isFirst) {
//                    commonKeys.retainAll(keys);
//                } else {
//                    commonKeys.addAll(keys);
//                    isFirst = false;
//                }
//            }
//
//            Map<String, Boolean> enclosingInitialisedSymbols =
//                    conditionalBlocks.get(0).getScope().getEnclosingScope().getInitialisedSymbols();
//
//            for (String symbolName : allKeys) {
//                if (!enclosingInitialisedSymbols.containsKey(symbolName)
//                        || !enclosingInitialisedSymbols.get(symbolName)) {
//
//                    boolean isFullyInitialised = commonKeys.contains(symbolName);
//                    if (isFullyInitialised) {
//                        for (ITSPHPAst block : conditionalBlocks) {
//                            if (!block.getScope().getInitialisedSymbols().get(symbolName)) {
//                                isFullyInitialised = false;
//                                break;
//                            }
//                        }
//                    }
//                    enclosingInitialisedSymbols.put(symbolName, isFullyInitialised);
//                }
//            }
//        }
//    }
//
//    private boolean isNotLeftHandSideOfAssignment(ITSPHPAst variableId) {
//        ITSPHPAst parent = (ITSPHPAst) variableId.getParent();
//        int type = parent.getType();
//        return type != TSPHPDefinitionWalker.Assign && type != TSPHPDefinitionWalker.CAST_ASSIGN
//                || !parent.getChild(0).equals(variableId);
//    }

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
        returnAst.setEvalType(core.getNullTypeSymbol());
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

//
//    /**
//     * "Delegate" to resolve a type - e.g. resolve a primitive type
//     */
//    private interface IResolveTypeCaller
//    {
//        ITypeSymbol resolve(ITSPHPAst typeAst);
//    }
//
//    private interface ISymbolCreateCaller
//    {
//        ITypeSymbol create(ITSPHPAst typeAst, ITypeSymbol typeSymbolWithoutModifier, IModifierSet modifiers);
//    }
}