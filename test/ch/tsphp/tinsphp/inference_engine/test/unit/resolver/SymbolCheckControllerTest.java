/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.resolver;

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
import ch.tsphp.tinsphp.inference_engine.resolver.SymbolCheckController;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SymbolCheckControllerTest
{

    @Test
    public void isNotForwardReference_IsErroneousSymbol_ReturnsTrue() {
        IErroneousSymbol symbol = mock(IErroneousSymbol.class);

        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getSymbol()).thenReturn(symbol);

        ISymbolCheckController controller = createController();
        ForwardReferenceCheckResultDto result = controller.isNotForwardReference(ast);

        assertThat(result.isNotForwardReference, is(true));
        assertThat(result.definitionAst, is(nullValue()));
    }

    @Test
    public void isNotForwardReference_IsDefinedEarlier_ReturnsTrue() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(ast.getSymbol()).thenReturn(symbol);
        ITSPHPAst definitionAst = mock(ITSPHPAst.class);
        when(symbol.getDefinitionAst()).thenReturn(definitionAst);
        when(definitionAst.isDefinedEarlierThan(ast)).thenReturn(true);

        ISymbolCheckController controller = createController();
        ForwardReferenceCheckResultDto result = controller.isNotForwardReference(ast);

        verify(definitionAst).isDefinedEarlierThan(ast);
        assertThat(result.isNotForwardReference, is(true));
        assertThat(result.definitionAst, is(definitionAst));
    }

    @Test
    public void isNotForwardReference_IsDefinedLaterOwn_ReturnsFalse() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(ast.getSymbol()).thenReturn(symbol);
        ITSPHPAst definitionAst = mock(ITSPHPAst.class);
        when(symbol.getDefinitionAst()).thenReturn(definitionAst);
        when(definitionAst.isDefinedEarlierThan(ast)).thenReturn(false);

        ISymbolCheckController controller = createController();
        ForwardReferenceCheckResultDto result = controller.isNotForwardReference(ast);

        verify(definitionAst).isDefinedEarlierThan(ast);
        assertThat(result.isNotForwardReference, is(false));
        assertThat(result.definitionAst, is(definitionAst));
    }

    @Test
    public void isNotDoubleDefinition_IsFirstFoundByCore_ReturnsTrueAndSymbolFromCore() {
        ISymbol firstSymbol = mock(ISymbol.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getSymbol()).thenReturn(firstSymbol);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        List<ISymbolResolver> additionalSymbolResolvers = new ArrayList<>(Arrays.asList(
                coreSymbolResolver,
                additionalSymbolResolver1,
                additionalSymbolResolver2));
        when(coreSymbolResolver.resolveIdentifierFromItsScope(ast)).thenReturn(firstSymbol);

        ISymbolCheckController controller
                = createController(userSymbolResolver, additionalSymbolResolvers);
        DoubleDefinitionCheckResultDto result = controller.isNotDoubleDefinition(ast);

        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verifyZeroInteractions(additionalSymbolResolver1);
        verifyZeroInteractions(additionalSymbolResolver2);
        verifyZeroInteractions(userSymbolResolver);
        assertThat(result.isNotDoubleDefinition, is(true));
        assertThat(result.existingSymbol, is(firstSymbol));
    }

    @Test
    public void isNotDoubleDefinition_IsNotFirstFoundByCore_ReturnsFalseAndSymbolFromCore() {
        ISymbol firstSymbol = mock(ISymbol.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(ast.getSymbol()).thenReturn(symbol);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        List<ISymbolResolver> additionalSymbolResolvers = new ArrayList<>(Arrays.asList(
                coreSymbolResolver,
                additionalSymbolResolver1,
                additionalSymbolResolver2));
        when(coreSymbolResolver.resolveIdentifierFromItsScope(ast)).thenReturn(firstSymbol);

        ISymbolCheckController controller
                = createController(userSymbolResolver, additionalSymbolResolvers);
        DoubleDefinitionCheckResultDto result = controller.isNotDoubleDefinition(ast);

        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verifyZeroInteractions(additionalSymbolResolver1);
        verifyZeroInteractions(additionalSymbolResolver2);
        verifyZeroInteractions(userSymbolResolver);
        assertThat(result.isNotDoubleDefinition, is(false));
        assertThat(result.existingSymbol, is(firstSymbol));
    }

    @Test
    public void isNotDoubleDefinition_IsFirstFoundByAdditional1_ReturnsTrueAndSymbolFromAdditional1() {
        ISymbol firstSymbol = mock(ISymbol.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getSymbol()).thenReturn(firstSymbol);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        List<ISymbolResolver> additionalSymbolResolvers = new ArrayList<>(Arrays.asList(
                coreSymbolResolver,
                additionalSymbolResolver1,
                additionalSymbolResolver2));
        when(additionalSymbolResolver1.resolveIdentifierFromItsScope(ast)).thenReturn(firstSymbol);

        ISymbolCheckController controller
                = createController(userSymbolResolver, additionalSymbolResolvers);
        DoubleDefinitionCheckResultDto result = controller.isNotDoubleDefinition(ast);

        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsScope(ast);
        verifyZeroInteractions(additionalSymbolResolver2);
        verifyZeroInteractions(userSymbolResolver);
        assertThat(result.isNotDoubleDefinition, is(true));
        assertThat(result.existingSymbol, is(firstSymbol));
    }

    @Test
    public void isNotDoubleDefinition_IsNotFirstFoundByAdditional1_ReturnsFalseAndSymbolFromAdditional1() {
        ISymbol firstSymbol = mock(ISymbol.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(ast.getSymbol()).thenReturn(symbol);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        List<ISymbolResolver> additionalSymbolResolvers = new ArrayList<>(Arrays.asList(
                coreSymbolResolver,
                additionalSymbolResolver1,
                additionalSymbolResolver2));
        when(additionalSymbolResolver1.resolveIdentifierFromItsScope(ast)).thenReturn(firstSymbol);

        ISymbolCheckController controller
                = createController(userSymbolResolver, additionalSymbolResolvers);
        DoubleDefinitionCheckResultDto result = controller.isNotDoubleDefinition(ast);

        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsScope(ast);
        verifyZeroInteractions(additionalSymbolResolver2);
        verifyZeroInteractions(userSymbolResolver);
        assertThat(result.isNotDoubleDefinition, is(false));
        assertThat(result.existingSymbol, is(firstSymbol));
    }

    @Test
    public void isNotDoubleDefinition_IsFirstFoundByAdditional2_ReturnsTrueAndSymbolFromAdditional2() {
        ISymbol firstSymbol = mock(ISymbol.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getSymbol()).thenReturn(firstSymbol);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        List<ISymbolResolver> additionalSymbolResolvers = new ArrayList<>(Arrays.asList(
                coreSymbolResolver,
                additionalSymbolResolver1,
                additionalSymbolResolver2));
        when(additionalSymbolResolver2.resolveIdentifierFromItsScope(ast)).thenReturn(firstSymbol);

        ISymbolCheckController controller
                = createController(userSymbolResolver, additionalSymbolResolvers);
        DoubleDefinitionCheckResultDto result = controller.isNotDoubleDefinition(ast);

        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver2).resolveIdentifierFromItsScope(ast);
        verifyZeroInteractions(userSymbolResolver);
        assertThat(result.isNotDoubleDefinition, is(true));
        assertThat(result.existingSymbol, is(firstSymbol));
    }

    @Test
    public void isNotDoubleDefinition_IsNotFirstFoundByAdditional2_ReturnsFalseAndSymbolFromAdditional2() {
        ISymbol firstSymbol = mock(ISymbol.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(ast.getSymbol()).thenReturn(symbol);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        List<ISymbolResolver> additionalSymbolResolvers = new ArrayList<>(Arrays.asList(
                coreSymbolResolver,
                additionalSymbolResolver1,
                additionalSymbolResolver2));
        when(additionalSymbolResolver2.resolveIdentifierFromItsScope(ast)).thenReturn(firstSymbol);

        ISymbolCheckController controller
                = createController(userSymbolResolver, additionalSymbolResolvers);
        DoubleDefinitionCheckResultDto result = controller.isNotDoubleDefinition(ast);

        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver2).resolveIdentifierFromItsScope(ast);
        verifyZeroInteractions(userSymbolResolver);
        assertThat(result.isNotDoubleDefinition, is(false));
        assertThat(result.existingSymbol, is(firstSymbol));
    }

    @Test
    public void isNotDoubleDefinition_IsFirstFoundByUser_ReturnsTrueAndSymbolFromUser() {
        ISymbol firstSymbol = mock(ISymbol.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getSymbol()).thenReturn(firstSymbol);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        List<ISymbolResolver> additionalSymbolResolvers = new ArrayList<>(Arrays.asList(
                coreSymbolResolver,
                additionalSymbolResolver1,
                additionalSymbolResolver2));
        when(userSymbolResolver.resolveIdentifierFromItsScope(ast)).thenReturn(firstSymbol);

        ISymbolCheckController controller
                = createController(userSymbolResolver, additionalSymbolResolvers);
        DoubleDefinitionCheckResultDto result = controller.isNotDoubleDefinition(ast);

        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver2).resolveIdentifierFromItsScope(ast);
        verify(userSymbolResolver).resolveIdentifierFromItsScope(ast);
        assertThat(result.isNotDoubleDefinition, is(true));
        assertThat(result.existingSymbol, is(firstSymbol));
    }

    @Test
    public void isNotDoubleDefinition_IsNotFirstFoundByUser_ReturnsFalseAndSymbolFromUser() {
        ISymbol firstSymbol = mock(ISymbol.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(ast.getSymbol()).thenReturn(symbol);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        List<ISymbolResolver> additionalSymbolResolvers = new ArrayList<>(Arrays.asList(
                coreSymbolResolver,
                additionalSymbolResolver1,
                additionalSymbolResolver2));
        when(userSymbolResolver.resolveIdentifierFromItsScope(ast)).thenReturn(firstSymbol);

        ISymbolCheckController controller
                = createController(userSymbolResolver, additionalSymbolResolvers);
        DoubleDefinitionCheckResultDto result = controller.isNotDoubleDefinition(ast);

        verify(coreSymbolResolver).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsScope(ast);
        verify(additionalSymbolResolver2).resolveIdentifierFromItsScope(ast);
        verify(userSymbolResolver).resolveIdentifierFromItsScope(ast);
        assertThat(result.isNotDoubleDefinition, is(false));
        assertThat(result.existingSymbol, is(firstSymbol));
    }

    @Test
    public void isNotUseDoubleDefinition_NotFirst_ReturnsFalse() {
        ISymbol firstSymbol = mock(ISymbol.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        String alias = "alias";
        when(ast.getText()).thenReturn(alias);
        ISymbol symbol = mock(ISymbol.class);
        when(ast.getSymbol()).thenReturn(symbol);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(symbol.getDefinitionScope()).thenReturn(namespaceScope);
        when(namespaceScope.getCaseInsensitiveFirstUseSymbol(alias)).thenReturn(firstSymbol);

        ISymbolCheckController controller = createController();
        DoubleDefinitionCheckResultDto result = controller.isNotUseDoubleDefinition(ast);

        assertThat(result.isNotDoubleDefinition, is(false));
        assertThat(result.existingSymbol, is(firstSymbol));
    }

    @Test
    public void isNotUseDoubleDefinition_IsFirst_ReturnsTrue() {
        ISymbol firstSymbol = mock(ISymbol.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        String alias = "alias";
        when(ast.getText()).thenReturn(alias);
        when(ast.getSymbol()).thenReturn(firstSymbol);
        INamespaceScope namespaceScope = mock(INamespaceScope.class);
        when(firstSymbol.getDefinitionScope()).thenReturn(namespaceScope);
        when(namespaceScope.getCaseInsensitiveFirstUseSymbol(alias)).thenReturn(firstSymbol);

        ISymbolCheckController controller = createController();
        DoubleDefinitionCheckResultDto result = controller.isNotUseDoubleDefinition(ast);

        assertThat(result.isNotDoubleDefinition, is(true));
        assertThat(result.existingSymbol, is(firstSymbol));
    }

    @Test
    public void isNotAlreadyDefinedAsType_Standard_UsesIsNotDoubleDefinitionDelegatesToAllAndReturnNull() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver coreSymbolResolver = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver1 = mock(ISymbolResolver.class);
        ISymbolResolver additionalSymbolResolver2 = mock(ISymbolResolver.class);
        List<ISymbolResolver> additionalSymbolResolvers = new ArrayList<>(Arrays.asList(
                coreSymbolResolver,
                additionalSymbolResolver1,
                additionalSymbolResolver2));

        ISymbolCheckController controller
                = createController(userSymbolResolver, additionalSymbolResolvers);
        AlreadyDefinedAsTypeResultDto result = controller.isNotAlreadyDefinedAsType(ast);

        verify(coreSymbolResolver).resolveIdentifierFromItsScopeCaseInsensitive(ast);
        verify(additionalSymbolResolver1).resolveIdentifierFromItsScopeCaseInsensitive(ast);
        verify(additionalSymbolResolver2).resolveIdentifierFromItsScopeCaseInsensitive(ast);
        verify(userSymbolResolver).resolveIdentifierFromItsScopeCaseInsensitive(ast);
        assertThat(result.isNotAlreadyDefinedAsType, is(true));
        assertThat(result.typeSymbol, is(nullValue()));
    }

    @Test
    public void isNotAlreadyDefinedAsType_NoTypeFound_ReturnsTrueAndNull() {
        //no arrange necessary

        ISymbolCheckController controller = createController();
        AlreadyDefinedAsTypeResultDto result = controller.isNotAlreadyDefinedAsType(mock(ITSPHPAst.class));

        assertThat(result.isNotAlreadyDefinedAsType, is(true));
        assertThat(result.typeSymbol, is(nullValue()));
    }

    @Test
    public void isNotAlreadyDefinedAsType_TypeFoundAndIsDefinedLaterOnInSameNamespace_ReturnsFalseAndType() {
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsScopeCaseInsensitive(alias)).thenReturn(typeSymbol);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(typeSymbol.getDefinitionAst()).thenReturn(ast);
        when(alias.isDefinedEarlierThan(ast)).thenReturn(true);
        IScope scope = mock(IScope.class);
        when(alias.getScope()).thenReturn(scope);
        when(typeSymbol.getDefinitionScope()).thenReturn(scope);

        ISymbolCheckController controller = createController(userSymbolResolver);
        AlreadyDefinedAsTypeResultDto result = controller.isNotAlreadyDefinedAsType(alias);

        verify(alias).isDefinedEarlierThan(ast);
        assertThat(result.isNotAlreadyDefinedAsType, is(false));
        assertThat(result.typeSymbol, is(typeSymbol));
    }

    @Test
    public void isNotAlreadyDefinedAsType_TypeFoundAndIsDefinedLaterOnInDifferentNamespace_ReturnsTrueAndType() {
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsScopeCaseInsensitive(alias)).thenReturn(typeSymbol);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(typeSymbol.getDefinitionAst()).thenReturn(ast);
        when(alias.isDefinedEarlierThan(ast)).thenReturn(true);
        IScope aliasScope = mock(IScope.class);
        when(alias.getScope()).thenReturn(aliasScope);
        IScope typeSymbolScope = mock(IScope.class);
        when(typeSymbol.getDefinitionScope()).thenReturn(typeSymbolScope);

        ISymbolCheckController controller = createController(userSymbolResolver);
        AlreadyDefinedAsTypeResultDto result = controller.isNotAlreadyDefinedAsType(alias);

        verify(alias).isDefinedEarlierThan(ast);
        assertThat(result.isNotAlreadyDefinedAsType, is(true));
        assertThat(result.typeSymbol, is(typeSymbol));
    }

    @Test
    public void isNotAlreadyDefinedAsType_TypeFoundIsDefinedEarlierAndInDifferentScope_ReturnsFalseAndType() {
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsScopeCaseInsensitive(alias)).thenReturn(typeSymbol);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(typeSymbol.getDefinitionAst()).thenReturn(ast);
        when(alias.isDefinedEarlierThan(ast)).thenReturn(false);
        IScope aliasScope = mock(IScope.class);
        when(alias.getScope()).thenReturn(aliasScope);
        IScope typeSymbolScope = mock(IScope.class);
        when(typeSymbol.getDefinitionScope()).thenReturn(typeSymbolScope);

        ISymbolCheckController controller = createController(userSymbolResolver);
        AlreadyDefinedAsTypeResultDto result = controller.isNotAlreadyDefinedAsType(alias);

        verify(alias).isDefinedEarlierThan(ast);
        verify(alias).getScope();
        assertThat(result.isNotAlreadyDefinedAsType, is(false));
        assertThat(result.typeSymbol, is(typeSymbol));
    }

    @Test
    public void isNotAlreadyDefinedAsType_TypeFoundIsDefinedEarlierAndInSameScope_ReturnsFalseAndType() {
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ISymbolResolver userSymbolResolver = mock(ISymbolResolver.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(userSymbolResolver.resolveIdentifierFromItsScopeCaseInsensitive(alias)).thenReturn(typeSymbol);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(typeSymbol.getDefinitionAst()).thenReturn(ast);
        when(alias.isDefinedEarlierThan(ast)).thenReturn(false);
        IScope scope = mock(IScope.class);
        when(alias.getScope()).thenReturn(scope);
        when(typeSymbol.getDefinitionScope()).thenReturn(scope);

        ISymbolCheckController controller = createController(userSymbolResolver);
        AlreadyDefinedAsTypeResultDto result = controller.isNotAlreadyDefinedAsType(alias);

        verify(alias).isDefinedEarlierThan(ast);
        verify(alias).getScope();
        assertThat(result.isNotAlreadyDefinedAsType, is(false));
        assertThat(result.typeSymbol, is(typeSymbol));
    }

    @Test
    public void isVariableInitialised_IsErroneousSymbol_ReturnsTrueAndFalse() {
        ITSPHPAst variableId = mock(ITSPHPAst.class);
        IErroneousVariableSymbol symbol = mock(IErroneousVariableSymbol.class);
        when(variableId.getSymbol()).thenReturn(symbol);

        ISymbolCheckController controller = createController();
        VariableInitialisedResultDto result = controller.isVariableInitialised(variableId);

        assertThat(result.isFullyInitialised, is(true));
        assertThat(result.isPartiallyInitialised, is(false));
    }

    @Test
    public void isVariableInitialised_IsFullyInitialised_ReturnsTrueAndFalse() {
        ITSPHPAst variableId = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(variableId.getSymbol()).thenReturn(symbol);
        IScope scope = mock(IScope.class);
        when(variableId.getScope()).thenReturn(scope);
        when(scope.isFullyInitialised(symbol)).thenReturn(true);

        ISymbolCheckController controller = createController();
        VariableInitialisedResultDto result = controller.isVariableInitialised(variableId);

        verify(scope).isFullyInitialised(symbol);
        verifyNoMoreInteractions(scope);
        assertThat(result.isFullyInitialised, is(true));
        assertThat(result.isPartiallyInitialised, is(false));
    }

    @Test
    public void isVariableInitialised_IsPartiallyInitialised_ReturnsTrueAndFalse() {
        ITSPHPAst variableId = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(variableId.getSymbol()).thenReturn(symbol);
        IScope scope = mock(IScope.class);
        when(variableId.getScope()).thenReturn(scope);
        when(scope.isPartiallyInitialised(symbol)).thenReturn(true);
        ITSPHPAst parent = mock(ITSPHPAst.class);
        when(variableId.getParent()).thenReturn(parent);
        when(parent.getType()).thenReturn(TokenTypes.Plus);

        ISymbolCheckController controller = createController();
        VariableInitialisedResultDto result = controller.isVariableInitialised(variableId);

        verify(scope).isFullyInitialised(symbol);
        verify(scope).isPartiallyInitialised(symbol);
        assertThat(result.isFullyInitialised, is(false));
        assertThat(result.isPartiallyInitialised, is(true));
    }

    @Test
    public void isVariableInitialised_IsLeftHandSideOfAssignment_ReturnsTrueAndFalse() {
        ITSPHPAst variableId = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(variableId.getSymbol()).thenReturn(symbol);
        IScope scope = mock(IScope.class);
        when(variableId.getScope()).thenReturn(scope);
        when(scope.isFullyInitialised(symbol)).thenReturn(false);
        ITSPHPAst parent = mock(ITSPHPAst.class);
        when(variableId.getParent()).thenReturn(parent);
        when(parent.getType()).thenReturn(TokenTypes.Assign);
        when(parent.getChild(0)).thenReturn(variableId);

        ISymbolCheckController controller = createController();
        VariableInitialisedResultDto result = controller.isVariableInitialised(variableId);

        verify(scope).isFullyInitialised(symbol);
        verifyNoMoreInteractions(scope);
        assertThat(result.isFullyInitialised, is(true));
        assertThat(result.isPartiallyInitialised, is(false));
    }

    @Test
    public void isVariableInitialised_IsRightHandSideOfAssignmentAndPartiallyInitialised_ReturnsFalseAndTrue() {
        ITSPHPAst variableId = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(variableId.getSymbol()).thenReturn(symbol);
        IScope scope = mock(IScope.class);
        when(variableId.getScope()).thenReturn(scope);
        when(scope.isFullyInitialised(symbol)).thenReturn(false);
        ITSPHPAst parent = mock(ITSPHPAst.class);
        when(variableId.getParent()).thenReturn(parent);
        when(parent.getType()).thenReturn(TokenTypes.Assign);
        when(parent.getChild(0)).thenReturn(mock(ITSPHPAst.class));
        when(scope.isPartiallyInitialised(symbol)).thenReturn(true);

        ISymbolCheckController controller = createController();
        VariableInitialisedResultDto result = controller.isVariableInitialised(variableId);

        verify(scope).isFullyInitialised(symbol);
        verify(scope).isPartiallyInitialised(symbol);
        assertThat(result.isFullyInitialised, is(false));
        assertThat(result.isPartiallyInitialised, is(true));
    }

    @Test
    public void isVariableInitialised_IsRightHandSideOfAssignmentAndNotInitialised_ReturnsFalseAndFalse() {
        ITSPHPAst variableId = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(variableId.getSymbol()).thenReturn(symbol);
        IScope scope = mock(IScope.class);
        when(variableId.getScope()).thenReturn(scope);
        when(scope.isFullyInitialised(symbol)).thenReturn(false);
        ITSPHPAst parent = mock(ITSPHPAst.class);
        when(variableId.getParent()).thenReturn(parent);
        when(parent.getType()).thenReturn(TokenTypes.Assign);
        when(parent.getChild(0)).thenReturn(mock(ITSPHPAst.class));
        when(scope.isPartiallyInitialised(symbol)).thenReturn(false);

        ISymbolCheckController controller = createController();
        VariableInitialisedResultDto result = controller.isVariableInitialised(variableId);

        verify(scope).isFullyInitialised(symbol);
        verify(scope).isPartiallyInitialised(symbol);
        assertThat(result.isFullyInitialised, is(false));
        assertThat(result.isPartiallyInitialised, is(false));
    }

    private ISymbolCheckController createController() {
        return createController(mock(ISymbolResolver.class));
    }

    private ISymbolCheckController createController(ISymbolResolver userSymbolResolver) {
        return createController(userSymbolResolver, new ArrayList<ISymbolResolver>());
    }

    protected ISymbolCheckController createController(
            ISymbolResolver theUserSymbolResolver,
            List<ISymbolResolver> additionalSymbolResolvers) {
        return new SymbolCheckController(theUserSymbolResolver, additionalSymbolResolvers);
    }
}
