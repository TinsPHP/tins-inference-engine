/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class DefinitionPhaseControllerTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.inference_engine.DefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.IDefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.inference_engine.scopes.INamespaceScope;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeFactory;
import ch.tsphp.tinsphp.inference_engine.symbols.ISymbolFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefinitionPhaseControllerTest
{

    private ISymbolFactory symbolFactory;
    private IScopeFactory scopeFactory;

    private IGlobalNamespaceScope globalDefaultScope;


    @Before
    public void setUp() {
        symbolFactory = mock(ISymbolFactory.class);
        scopeFactory = mock(IScopeFactory.class);
    }

    @Test
    public void getGlobalDefaultNamespace_FirstCall_UsesScopeFactory() {
        initScopeFactoryForGlobalDefaultNamespace();

        IDefinitionPhaseController controller = createDefinitionPhaseController();
        IGlobalNamespaceScope scope = controller.getGlobalDefaultNamespace();

        assertThat(scope, is(globalDefaultScope));
        verify(scopeFactory).createGlobalNamespaceScope("\\");
    }

    @Test
    public void getGlobalDefaultNamespace_SecondCall_UsesScopeFactoryOnlyOnce() {
        initScopeFactoryForGlobalDefaultNamespace();

        IDefinitionPhaseController controller = createDefinitionPhaseController();
        IGlobalNamespaceScope scope = controller.getGlobalDefaultNamespace();

        assertThat(scope, is(globalDefaultScope));
        verify(scopeFactory, times(1)).createGlobalNamespaceScope("\\");

        scope = controller.getGlobalDefaultNamespace();

        assertThat(scope, is(globalDefaultScope));
        verify(scopeFactory, times(1)).createGlobalNamespaceScope("\\");
    }

    @Test
    public void defineNamespace_FirstCall_UsesScopeFactory() {
        NamespaceAndGlobalPair namespaceAndGlobal = initDefineNamespace("name");

        IDefinitionPhaseController controller = createDefinitionPhaseController();
        INamespaceScope namespaceScope = controller.defineNamespace("name");

        assertThat(namespaceScope, is(namespaceAndGlobal.namespaceScope));
        verify(scopeFactory, times(1)).createGlobalNamespaceScope("name");
        verify(scopeFactory).createNamespaceScope("name", namespaceAndGlobal.globalNamespaceScope);
    }

    @Test
    public void defineNamespace_SecondCall_GlobalNamespaceOnlyCreatedOnce() {
        NamespaceAndGlobalPair namespaceAndGlobal = initDefineNamespace("name");
        INamespaceScope secondNamespaceScope = mock(INamespaceScope.class);
        when(scopeFactory.createNamespaceScope("name", namespaceAndGlobal.globalNamespaceScope))
                .thenReturn(namespaceAndGlobal.namespaceScope)
                .thenReturn(secondNamespaceScope);

        IDefinitionPhaseController controller = createDefinitionPhaseController();
        INamespaceScope namespaceScope1 = controller.defineNamespace("name");
        INamespaceScope namespaceScope2 = controller.defineNamespace("name");

        assertThat(namespaceScope1, is(namespaceAndGlobal.namespaceScope));
        assertThat(namespaceScope2, is(secondNamespaceScope));

        verify(scopeFactory, times(1)).createGlobalNamespaceScope("name");
        verify(scopeFactory, times(2)).createNamespaceScope("name", namespaceAndGlobal.globalNamespaceScope);
    }

    @Test
    public void getGlobalNamespaceScopes_NothingDefined_ContainsGlobalDefaultNamespace() {
        initScopeFactoryForGlobalDefaultNamespace();

        IDefinitionPhaseController controller = createDefinitionPhaseController();
        ILowerCaseStringMap<IGlobalNamespaceScope> scopes = controller.getGlobalNamespaceScopes();

        assertThat(scopes, hasEntry("\\", globalDefaultScope));
        verify(scopeFactory, times(1)).createGlobalNamespaceScope("\\");
    }

    @Test
    public void getGlobalNamespaceScopes_DefineAdditionalNamespace_ContainsBoth() {
        NamespaceAndGlobalPair namespaceAndGlobal = initDefineNamespace("name");

        IDefinitionPhaseController controller = createDefinitionPhaseController();
        controller.defineNamespace("name");
        ILowerCaseStringMap<IGlobalNamespaceScope> scopes = controller.getGlobalNamespaceScopes();

        assertThat(scopes, hasEntry("\\", globalDefaultScope));
        assertThat(scopes, hasEntry("name", namespaceAndGlobal.globalNamespaceScope));
    }

    //TODO rstoll TINS-163 definition phase - use
//    @Test
//    public void defineUse_standard_SetScopeForTypeAndCreateAliasSymbolAndDefineIt() {
//        INamespaceScope namespaceScope = mock(INamespaceScope.class);
//        ITSPHPAst typeAst = mock(ITSPHPAst.class);
//        ITSPHPAst aliasAst = mock(ITSPHPAst.class);
//        when(aliasAst.getText()).thenReturn("alias");
//        IAliasSymbol aliasSymbol = mock(IAliasSymbol.class);
//        when(symbolFactory.createAliasSymbol(aliasAst, "alias")).thenReturn(aliasSymbol);
//
//        IDefinitionPhaseController controller = createDefinitionPhaseController();
//        controller.defineUse(namespaceScope, typeAst, aliasAst);
//
//        verify(typeAst).setScope(namespaceScope);
//        verify(aliasAst).setSymbol(aliasSymbol);
//        verify(aliasAst).setScope(namespaceScope);
//        verify(namespaceScope).defineUse(aliasSymbol);
//        verify(symbolFactory).createAliasSymbol(aliasAst, "alias");
//    }

    //TODO rstoll TINS-154 definition phase - variables
//    @Test
//    public void defineVariable_Standard_SetScopeForTypeAndCreateVariableSymbolAndDefineIt() {
//        INamespaceScope namespaceScope = mock(INamespaceScope.class);
//        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
//        ITSPHPAst typeAst = mock(ITSPHPAst.class);
//        ITSPHPAst identifierAst = mock(ITSPHPAst.class);
//        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
//        when(symbolFactory.createVariableSymbol(modifierAst, identifierAst)).thenReturn(variableSymbol);
//
//        IDefinitionPhaseController controller = createDefinitionPhaseController();
//        controller.defineVariable(namespaceScope, modifierAst, typeAst, identifierAst);
//
//        verify(typeAst).setScope(namespaceScope);
//        verifyScopeSymbolAndDefine(namespaceScope, identifierAst, variableSymbol);
//        verify(symbolFactory).createVariableSymbol(modifierAst, identifierAst);
//    }

    //TODO rstoll TINS-156 definition phase - constants
//    @Test
//    public void defineConstant_Standard_SetScopeForTypeAndCreateVariableSymbolAndDefineIt() {
//        INamespaceScope namespaceScope = mock(INamespaceScope.class);
//        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
//        ITSPHPAst typeAst = mock(ITSPHPAst.class);
//        ITSPHPAst identifierAst = mock(ITSPHPAst.class);
//        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
//        when(symbolFactory.createVariableSymbol(modifierAst, identifierAst)).thenReturn(variableSymbol);
//
//        IDefinitionPhaseController controller = createDefinitionPhaseController();
//        controller.defineConstant(namespaceScope, modifierAst, typeAst, identifierAst);
//
//        verify(typeAst).setScope(namespaceScope);
//        verifyScopeSymbolAndDefine(namespaceScope, identifierAst, variableSymbol);
//        verify(symbolFactory).createVariableSymbol(modifierAst, identifierAst);
//    }

    //TODO rstoll TINS-161 inference OOP
//    @Test
//    public void defineInterface_NoExtends_SetScopeForIdentifierAndCreateInterfaceSymbolAndDefineIt() {
//        defineInterface(mock(ITSPHPAst.class));
//    }
//
//    private INamespaceScope defineInterface(ITSPHPAst extendsAst) {
//        INamespaceScope namespaceScope = mock(INamespaceScope.class);
//        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
//        ITSPHPAst identifierAst = mock(ITSPHPAst.class);
//        IInterfaceTypeSymbol interfaceTypeSymbol = mock(IInterfaceTypeSymbol.class);
//        when(symbolFactory.createInterfaceTypeSymbol(modifierAst, identifierAst, namespaceScope))
//                .thenReturn(interfaceTypeSymbol);
//
//        IDefinitionPhaseController controller = createDefinitionPhaseController();
//        IInterfaceTypeSymbol interfaceSymbol = controller.defineInterface(namespaceScope,
//                modifierAst, identifierAst, extendsAst);
//
//        assertThat(interfaceSymbol, is(interfaceTypeSymbol));
//        verifyScopeSymbolAndDefine(namespaceScope, identifierAst, interfaceTypeSymbol);
//        verify(symbolFactory).createInterfaceTypeSymbol(modifierAst, identifierAst, namespaceScope);
//        return namespaceScope;
//    }
//
//    @Test
//    public void defineInterface_OneExtends_SetScopeForIdentifierAndParentTypeCreateInterfaceSymbolAndDefineIt() {
//        ITSPHPAst extendsAst = mock(ITSPHPAst.class);
//        when(extendsAst.getChildCount()).thenReturn(1);
//        ITSPHPAst parentType = addParentType(extendsAst, 0);
//
//        INamespaceScope namespaceScope = defineInterface(extendsAst);
//
//        verify(parentType).setScope(namespaceScope);
//    }
//
//    @Test
//    public void defineInterface_MultipleExtends_SetScopeForIdentifierAndParentTypesCreateInterfaceSymbolAndDefineIt
// () {
//        ITSPHPAst extendsAst = mock(ITSPHPAst.class);
//        when(extendsAst.getChildCount()).thenReturn(2);
//        ITSPHPAst parentType = addParentType(extendsAst, 0);
//        ITSPHPAst parentType2 = addParentType(extendsAst, 1);
//
//        INamespaceScope namespaceScope = defineInterface(extendsAst);
//
//        verify(parentType).setScope(namespaceScope);
//        verify(parentType2).setScope(namespaceScope);
//    }
//
//    private ITSPHPAst addParentType(ITSPHPAst identifierList, int index) {
//        ITSPHPAst parentType = mock(ITSPHPAst.class);
//        when(identifierList.getChild(index)).thenReturn(parentType);
//        return parentType;
//    }
//
//    @Test
//    public void defineClass_NoExtendsNoImplements_SetScopeForIdentifierAndCreateClassSymbolAndDefineIt() {
//        defineClass(mock(ITSPHPAst.class), mock(ITSPHPAst.class));
//    }
//
//    private INamespaceScope defineClass(ITSPHPAst extendsAst, ITSPHPAst implementsAst) {
//        INamespaceScope namespaceScope = mock(INamespaceScope.class);
//        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
//        ITSPHPAst identifierAst = mock(ITSPHPAst.class);
//
//        IClassTypeSymbol classTypeSymbol = mock(IClassTypeSymbol.class);
//        when(symbolFactory.createClassTypeSymbol(modifierAst, identifierAst, namespaceScope))
//                .thenReturn(classTypeSymbol);
//
//        IDefinitionPhaseController controller = createDefinitionPhaseController();
//        IClassTypeSymbol classSymbol = controller.defineClass(namespaceScope, modifierAst, identifierAst,
//                extendsAst, implementsAst);
//
//        assertThat(classSymbol, is(classTypeSymbol));
//        verifyScopeSymbolAndDefine(namespaceScope, identifierAst, classTypeSymbol);
//        verify(symbolFactory).createClassTypeSymbol(modifierAst, identifierAst, namespaceScope);
//        return namespaceScope;
//    }
//
//    @Test
//    public void defineClass_OneExtendsNoImplements_SetScopeForIdentifierAndCreateClassSymbolAndDefineIt() {
//        ITSPHPAst extendsAst = mock(ITSPHPAst.class);
//        when(extendsAst.getChildCount()).thenReturn(1);
//        ITSPHPAst parentType = addParentType(extendsAst, 0);
//
//        INamespaceScope namespaceScope = defineClass(extendsAst, mock(ITSPHPAst.class));
//
//        verify(parentType).setScope(namespaceScope);
//    }
//
//    @Test
//    public void defineClass_MultipleExtendsNoImplements_SetScopeForIdentifierAndCreateClassSymbolAndDefineIt() {
//        ITSPHPAst extendsAst = mock(ITSPHPAst.class);
//        when(extendsAst.getChildCount()).thenReturn(2);
//        ITSPHPAst parentType = addParentType(extendsAst, 0);
//        ITSPHPAst parentType2 = addParentType(extendsAst, 1);
//
//        INamespaceScope namespaceScope = defineClass(extendsAst, mock(ITSPHPAst.class));
//
//        verify(parentType).setScope(namespaceScope);
//        verify(parentType2).setScope(namespaceScope);
//    }
//
//    @Test
//    public void defineClass_NoExtendsOneImplements_SetScopeForIdentifierAndCreateClassSymbolAndDefineIt() {
//        ITSPHPAst implementsAst = mock(ITSPHPAst.class);
//        when(implementsAst.getChildCount()).thenReturn(1);
//        ITSPHPAst parentType = addParentType(implementsAst, 0);
//
//        INamespaceScope namespaceScope = defineClass(mock(ITSPHPAst.class), implementsAst);
//
//        verify(parentType).setScope(namespaceScope);
//    }
//
//
//    @Test
//    public void defineClass_NoExtendsMultipleImplements_SetScopeForIdentifierAndCreateClassSymbolAndDefineIt() {
//        ITSPHPAst implementsAst = mock(ITSPHPAst.class);
//        when(implementsAst.getChildCount()).thenReturn(2);
//        ITSPHPAst parentType = addParentType(implementsAst, 0);
//        ITSPHPAst parentType2 = addParentType(implementsAst, 1);
//
//        INamespaceScope namespaceScope = defineClass(mock(ITSPHPAst.class), implementsAst);
//
//        verify(parentType).setScope(namespaceScope);
//        verify(parentType2).setScope(namespaceScope);
//    }
//
//
//    @Test
//    public void defineClass_MultipleExtendsMultipleImplements_SetScopeForIdentifierAndCreateClassSymbolAndDefineIt() {
//        ITSPHPAst extendsAst = mock(ITSPHPAst.class);
//        when(extendsAst.getChildCount()).thenReturn(2);
//        ITSPHPAst parentType = addParentType(extendsAst, 0);
//        ITSPHPAst parentType2 = addParentType(extendsAst, 1);
//
//        ITSPHPAst implementsAst = mock(ITSPHPAst.class);
//        when(implementsAst.getChildCount()).thenReturn(2);
//        ITSPHPAst implParentType = addParentType(implementsAst, 0);
//        ITSPHPAst implParentType2 = addParentType(implementsAst, 1);
//
//        INamespaceScope namespaceScope = defineClass(extendsAst, implementsAst);
//
//        verify(parentType).setScope(namespaceScope);
//        verify(parentType2).setScope(namespaceScope);
//        verify(implParentType).setScope(namespaceScope);
//        verify(implParentType2).setScope(namespaceScope);
//    }
//
//    @Test
//    public void defineMethod_Standard_SetScopeForIdentifierAndReturnTypeAndCreateMethodSymbolAndDefineIt() {
//        INamespaceScope namespaceScope = mock(INamespaceScope.class);
//        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
//        ITSPHPAst returnTypeModifierAst = mock(ITSPHPAst.class);
//        ITSPHPAst returnTypeAst = mock(ITSPHPAst.class);
//        ITSPHPAst identifierAst = mock(ITSPHPAst.class);
//
//        IMethodSymbol expectedMethodSymbol = mock(IMethodSymbol.class);
//        when(symbolFactory.createMethodSymbol(modifierAst, returnTypeModifierAst, identifierAst, namespaceScope))
//                .thenReturn(expectedMethodSymbol);
//
//        IDefinitionPhaseController controller = createDefinitionPhaseController();
//        IMethodSymbol methodSymbol = controller.defineMethod(namespaceScope, modifierAst, returnTypeModifierAst,
//                returnTypeAst, identifierAst);
//
//        assertThat(methodSymbol, is(expectedMethodSymbol));
//        verify(returnTypeAst).setScope(namespaceScope);
//        verifyScopeSymbolAndDefine(namespaceScope, identifierAst, expectedMethodSymbol);
//        verify(symbolFactory).createMethodSymbol(modifierAst, returnTypeModifierAst, identifierAst, namespaceScope);
//    }

    //TODO rstoll TINS-162 definition phase - scopes
//    @Test
//    public void defineConditionalScope_Standard_CallScopeFactory() {
//        IScope parentScope = mock(IScope.class);
//        IConditionalScope conditionalScope = mock(IConditionalScope.class);
//        when(scopeFactory.createConditionalScope(parentScope)).thenReturn(conditionalScope);
//
//        IDefinitionPhaseController controller = createDefinitionPhaseController();
//        IConditionalScope scope = controller.defineConditionalScope(parentScope);
//
//        assertThat(scope, is(conditionalScope));
//        verify(scopeFactory).createConditionalScope(parentScope);
//        //shouldn't have any additional interaction with scopeFactory than creating the global default namespace
//        verify(scopeFactory).createGlobalNamespaceScope("\\");
//        verifyNoMoreInteractions(scopeFactory);
//    }

    private IDefinitionPhaseController createDefinitionPhaseController() {
        return new DefinitionPhaseController(symbolFactory, scopeFactory);
    }

    private void verifyScopeSymbolAndDefine(IScope scope, ITSPHPAst identifierAst, ISymbol symbol) {
        verify(identifierAst).setSymbol(symbol);
        verify(identifierAst).setScope(scope);
        verify(scope).define(symbol);
    }

    private void initScopeFactoryForGlobalDefaultNamespace() {
        globalDefaultScope = mock(IGlobalNamespaceScope.class);
        when(scopeFactory.createGlobalNamespaceScope("\\")).thenReturn(globalDefaultScope);
    }

    private NamespaceAndGlobalPair initDefineNamespace(String name) {
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        IGlobalNamespaceScope globalScope = mock(IGlobalNamespaceScope.class);
        when(scopeFactory.createNamespaceScope(name, globalScope)).thenReturn(namespaceScope);
        when(scopeFactory.createGlobalNamespaceScope(name)).thenReturn(globalScope);

        return new NamespaceAndGlobalPair(globalScope, namespaceScope);
    }

    private class NamespaceAndGlobalPair
    {
        public IGlobalNamespaceScope globalNamespaceScope;
        public INamespaceScope namespaceScope;

        private NamespaceAndGlobalPair(IGlobalNamespaceScope globalNamespaceScope, INamespaceScope namespaceScope) {
            this.globalNamespaceScope = globalNamespaceScope;
            this.namespaceScope = namespaceScope;
        }
    }
}
