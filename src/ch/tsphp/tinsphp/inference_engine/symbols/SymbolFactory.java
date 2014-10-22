/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class SymbolFactory from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.symbols;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeHelper;

public class SymbolFactory implements ISymbolFactory
{
    private final IScopeHelper scopeHelper;
    private final IModifierHelper modifierHelper;
    private ITypeSymbol mixedTypeSymbol = null;

    public SymbolFactory(IScopeHelper theScopeHelper, IModifierHelper theModifierHelper) {
        scopeHelper = theScopeHelper;
        modifierHelper = theModifierHelper;
    }

//    @Override
//    public void setMixedTypeSymbol(ITypeSymbol typeSymbol) {
//        mixedTypeSymbol = typeSymbol;
//    }
//
//    @Override
//    public INullTypeSymbol createNullTypeSymbol() {
//        return new NullTypeSymbol();
//
//    }
//
//    @Override
//    public IVoidTypeSymbol createVoidTypeSymbol() {
//        return new VoidTypeSymbol();
//
//    }
//
//    @Override
//    @SuppressWarnings("checkstyle:parameternumber")
//    public IScalarTypeSymbol createScalarTypeSymbol(
//            String name,
//            int tokenTypeForCasting,
//            Set<ITypeSymbol> parentTypeSymbol,
//            int defaultValueTokenType,
//            String defaultValue) {
//
//        return new ScalarTypeSymbol(
//                name,
//                parentTypeSymbol,
//                tokenTypeForCasting,
//                defaultValueTokenType,
//                defaultValue);
//    }
//
//    @Override
//    public IArrayTypeSymbol createArrayTypeSymbol(String name, int tokenType,
//            ITypeSymbol keyValue, ITypeSymbol valueType) {
//        return new ArrayTypeSymbol(name, tokenType, keyValue, valueType, mixedTypeSymbol);
//    }
//
//    @Override
//    public IPseudoTypeSymbol createPseudoTypeSymbol(String name) {
//        return new PseudoTypeSymbol(name, mixedTypeSymbol);
//    }
//
//    @Override
//    public IAliasSymbol createAliasSymbol(ITSPHPAst useDefinition, String alias) {
//        return new AliasSymbol(useDefinition, alias);
//    }
//
//    @Override
//    public IAliasTypeSymbol createAliasTypeSymbol(ITSPHPAst definitionAst, String name) {
//        return new AliasTypeSymbol(definitionAst, name, mixedTypeSymbol);
//    }
//
//    @Override
//    public IInterfaceTypeSymbol createInterfaceTypeSymbol(ITSPHPAst modifier, ITSPHPAst identifier,
//            IScope currentScope) {
//        return new InterfaceTypeSymbol(
//                scopeHelper,
//                identifier,
//                modifierHelper.getModifiers(modifier),
//                identifier.getText(),
//                currentScope,
//                mixedTypeSymbol);
//    }
//
//    @Override
//    public IClassTypeSymbol createClassTypeSymbol(ITSPHPAst classModifierAst, ITSPHPAst identifier,
//            IScope currentScope) {
//        return new ClassTypeSymbol(
//                scopeHelper,
//                identifier,
//                modifierHelper.getModifiers(classModifierAst),
//                identifier.getText(),
//                currentScope,
//                mixedTypeSymbol);
//    }
//
//    @Override
//    public IMethodSymbol createMethodSymbol(ITSPHPAst methodModifier, ITSPHPAst returnTypeModifier,
//            ITSPHPAst identifier, IScope currentScope) {
//        return new MethodSymbol(
//                scopeHelper,
//                identifier,
//                modifierHelper.getModifiers(methodModifier),
//                modifierHelper.getModifiers(returnTypeModifier),
//                identifier.getText(),
//                currentScope);
//    }
//
//    @Override
//    public IVariableSymbol createThisSymbol(ITSPHPAst variableId, IPolymorphicTypeSymbol polymorphicTypeSymbol) {
//        return new ThisSymbol(variableId, variableId.getText(), polymorphicTypeSymbol);
//    }
//
//    @Override
//    public IVariableSymbol createVariableSymbol(ITSPHPAst typeModifier, ITSPHPAst variableId) {
//        IModifierSet modifiers = typeModifier != null ? modifierHelper.getModifiers(typeModifier) : new ModifierSet();
//        return new VariableSymbol(variableId, modifiers, variableId.getText());
//    }
//
//    @Override
//    public IErroneousTypeSymbol createErroneousTypeSymbol(ITSPHPAst ast, TSPHPException exception) {
//        IMethodSymbol methodSymbol = createErroneousMethodSymbol(ast, exception);
//        return new ErroneousTypeSymbol(ast, exception, methodSymbol);
//    }
//
//    @Override
//    public IErroneousMethodSymbol createErroneousMethodSymbol(ITSPHPAst ast, TSPHPException ex) {
//        return new ErroneousMethodSymbol(ast, ex);
//    }
//
//    @Override
//    public IVariableSymbol createErroneousVariableSymbol(ITSPHPAst ast, TSPHPException exception) {
//        IVariableSymbol variableSymbol = new ErroneousVariableSymbol(ast, exception);
//        variableSymbol.setType(createErroneousTypeSymbol(ast, exception));
//        return variableSymbol;
//    }
}