/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ITSPHPErrorAst;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.common.symbols.modifiers.IModifierSet;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.IVariableDeclarationCreator;
import ch.tsphp.tinsphp.common.checking.AlreadyDefinedAsTypeResultDto;
import ch.tsphp.tinsphp.common.checking.DoubleDefinitionCheckResultDto;
import ch.tsphp.tinsphp.common.checking.ForwardReferenceCheckResultDto;
import ch.tsphp.tinsphp.common.checking.ISymbolCheckController;
import ch.tsphp.tinsphp.common.checking.VariableInitialisedResultDto;
import ch.tsphp.tinsphp.common.gen.TokenTypes;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCreator;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.resolving.ISymbolResolverController;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IAliasSymbol;
import ch.tsphp.tinsphp.common.symbols.IAliasTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.PrimitiveTypeNames;
import ch.tsphp.tinsphp.common.symbols.erroneous.IErroneousTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.erroneous.IErroneousVariableSymbol;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.common.utils.TypeHelperDto;
import ch.tsphp.tinsphp.inference_engine.ReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.constraints.solvers.IConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import ch.tsphp.tinsphp.symbols.ModifierSet;
import ch.tsphp.tinsphp.symbols.UnionTypeSymbol;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ReferencePhaseControllerTest
{

    @Test
    public void resolveConstant_Standard_DelegatesToSymbolResolver() {
        ITSPHPAst ast = createAst("Dummy");
        ISymbolResolverController symbolResolverController = mock(ISymbolResolverController.class);

        IReferencePhaseController controller = createController(symbolResolverController);
        controller.resolveConstant(ast);

        verify(symbolResolverController).resolveConstantLikeIdentifier(ast);
    }

    @Test
    public void resolveConstant_SymbolResolverFindsSymbol_ReturnsSymbol() {
        ITSPHPAst ast = createAst("Dummy");
        ISymbolResolverController symbolResolverController = mock(ISymbolResolverController.class);
        IMinimalVariableSymbol symbol = mock(IMinimalVariableSymbol.class);
        when(symbolResolverController.resolveConstantLikeIdentifier(ast)).thenReturn(symbol);

        IReferencePhaseController controller = createController(symbolResolverController);
        IMinimalVariableSymbol result = controller.resolveConstant(ast);

        assertThat(result, is(symbol));
    }

    @Test
    public void resolveConstant_SymbolResolverDoesNotFindSymbol_ReturnsErroneousVariableSymbol() {
        String aliasName = "Dummy";
        ITSPHPAst ast = createAst(aliasName);
        ISymbolFactory symbolFactory = createSymbolFactoryMock();
        IErroneousVariableSymbol erroneousVariableSymbol = mock(IErroneousVariableSymbol.class);
        when(symbolFactory.createErroneousVariableSymbol(eq(ast), any(TSPHPException.class)))
                .thenReturn(erroneousVariableSymbol);

        IReferencePhaseController controller = createController(symbolFactory);
        IMinimalVariableSymbol result = controller.resolveConstant(ast);

        verify(symbolFactory).createErroneousVariableSymbol(eq(ast), any(TSPHPException.class));
        assertThat(result, is((IMinimalVariableSymbol) erroneousVariableSymbol));
    }

    @Test
    public void resolveVariable_Standard_DelegatesToSymbolResolver() {
        ITSPHPAst ast = createAst("Dummy");
        ISymbolResolverController symbolResolverController = mock(ISymbolResolverController.class);

        IReferencePhaseController controller = createController(symbolResolverController);
        controller.resolveVariable(ast);

        verify(symbolResolverController).resolveVariableLikeIdentifier(ast);
    }

    @Test
    public void resolveVariable_SymbolResolverFindsSymbol_ReturnsSymbol() {
        ITSPHPAst ast = createAst("Dummy");
        ISymbolResolverController symbolResolverController = mock(ISymbolResolverController.class);
        IMinimalVariableSymbol symbol = mock(IMinimalVariableSymbol.class);
        when(symbolResolverController.resolveVariableLikeIdentifier(ast)).thenReturn(symbol);

        IReferencePhaseController controller = createController(symbolResolverController);
        IMinimalVariableSymbol result = controller.resolveVariable(ast);

        assertThat(result, is(symbol));
    }

    @Test
    public void
    resolveVariable_SymbolResolverDoesNotFindSymbol_DelegatesToVariableDeclarationCreatorAndReturnsSymbol() {
        String aliasName = "Dummy";
        ITSPHPAst ast = createAst(aliasName);
        ISymbolResolverController symbolResolverController = mock(ISymbolResolverController.class);
        IVariableDeclarationCreator variableDeclarationCreator = mock(IVariableDeclarationCreator.class);
        IVariableSymbol symbol = mock(IVariableSymbol.class);
        when(variableDeclarationCreator.create(ast)).thenReturn(symbol);

        IReferencePhaseController controller = createController(symbolResolverController, variableDeclarationCreator);
        IMinimalVariableSymbol result = controller.resolveVariable(ast);

        verify(symbolResolverController).resolveVariableLikeIdentifier(ast);
        verify(variableDeclarationCreator).create(ast);
        assertThat(result, is((IMinimalVariableSymbol) symbol));
    }

    @Test
    public void resolvePrimitiveType_NonExistingName_ReturnsErroneousTypeSymbol() {
        String name = "nonExisting";
        ITSPHPAst ast = createAst(name);
        Map<String, ITypeSymbol> primitiveTypes = new HashMap<>();
        ISymbolFactory symbolFactory = createSymbolFactoryMock();
        IErroneousTypeSymbol erroneousTypeSymbol = mock(IErroneousTypeSymbol.class);
        when(symbolFactory.createErroneousTypeSymbol(any(ITSPHPAst.class), any(TSPHPException.class)))
                .thenReturn(erroneousTypeSymbol);
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);

        //act
        IReferencePhaseController controller = createController(symbolFactory, issueReporter, primitiveTypes);
        ITypeSymbol result = controller.resolvePrimitiveType(ast, null);

        verify(symbolFactory).createErroneousTypeSymbol(eq(ast), any(TSPHPException.class));
        verify(issueReporter).unknownType(ast);
        assertThat(result, is((ITypeSymbol) erroneousTypeSymbol));
    }

    @Test
    public void resolvePrimitiveType_FalseableType_ReturnsUnionType() {
        String name = "dummy";
        ITSPHPAst ast = createAst(name);
        Map<String, ITypeSymbol> primitiveTypes = new HashMap<>();
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(typeSymbol.getAbsoluteName()).thenReturn("\\a\\" + name);
        primitiveTypes.put("dummy", typeSymbol);
        ITypeSymbol falseTypeTypeSymbol = mock(ITypeSymbol.class);
        when(falseTypeTypeSymbol.getAbsoluteName()).thenReturn(PrimitiveTypeNames.FALSE_TYPE);
        primitiveTypes.put(PrimitiveTypeNames.FALSE_TYPE, falseTypeTypeSymbol);
        ISymbolFactory symbolFactory = createSymbolFactoryMock();
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        IModifierHelper modifierHelper = mock(IModifierHelper.class);
        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
        IModifierSet modifiers = new ModifierSet();
        modifiers.add(TokenTypes.LogicNot);
        when(modifierHelper.getModifiers(modifierAst)).thenReturn(modifiers);

        IReferencePhaseController controller = createController(
                symbolFactory, issueReporter, modifierHelper, primitiveTypes);
        IUnionTypeSymbol result = (IUnionTypeSymbol) controller.resolvePrimitiveType(ast, modifierAst);

        assertThat(result.getTypeSymbols(), allOf(
                hasEntry("\\a\\dummy", typeSymbol),
                hasEntry(PrimitiveTypeNames.FALSE_TYPE, falseTypeTypeSymbol)
        ));
        assertThat(result.getTypeSymbols().size(), is(2));
    }

    @Test
    public void resolvePrimitiveType_NullableType_ReturnsUnionType() {
        String name = "dummy";
        ITSPHPAst ast = createAst(name);
        Map<String, ITypeSymbol> primitiveTypes = new HashMap<>();
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(typeSymbol.getAbsoluteName()).thenReturn("\\a\\dummy");
        primitiveTypes.put("dummy", typeSymbol);
        ITypeSymbol nullTypeTypeSymbol = mock(ITypeSymbol.class);
        when(nullTypeTypeSymbol.getAbsoluteName()).thenReturn(PrimitiveTypeNames.NULL_TYPE);
        primitiveTypes.put(PrimitiveTypeNames.NULL_TYPE, nullTypeTypeSymbol);
        ISymbolFactory symbolFactory = createSymbolFactoryMock();
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        IModifierHelper modifierHelper = mock(IModifierHelper.class);
        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
        IModifierSet modifiers = new ModifierSet();
        modifiers.add(TokenTypes.QuestionMark);
        when(modifierHelper.getModifiers(modifierAst)).thenReturn(modifiers);


        IReferencePhaseController controller = createController(
                symbolFactory, issueReporter, modifierHelper, primitiveTypes);
        IUnionTypeSymbol result = (IUnionTypeSymbol) controller.resolvePrimitiveType(ast, modifierAst);

        assertThat(result.getTypeSymbols(), allOf(
                hasEntry("\\a\\dummy", typeSymbol),
                hasEntry(PrimitiveTypeNames.NULL_TYPE, nullTypeTypeSymbol)
        ));
        assertThat(result.getTypeSymbols().size(), is(2));
    }

    @Test
    public void resolvePrimitiveType_FalseableNullableType_ReturnsUnionType() {
        String name = "dummy";
        ITSPHPAst ast = createAst(name);
        Map<String, ITypeSymbol> primitiveTypes = new HashMap<>();
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(typeSymbol.getAbsoluteName()).thenReturn("\\a\\" + name);
        primitiveTypes.put("dummy", typeSymbol);
        ITypeSymbol nullTypeTypeSymbol = mock(ITypeSymbol.class);
        when(nullTypeTypeSymbol.getAbsoluteName()).thenReturn(PrimitiveTypeNames.NULL_TYPE);
        primitiveTypes.put(PrimitiveTypeNames.NULL_TYPE, nullTypeTypeSymbol);
        ITypeSymbol falseTypeTypeSymbol = mock(ITypeSymbol.class);
        when(falseTypeTypeSymbol.getAbsoluteName()).thenReturn(PrimitiveTypeNames.FALSE_TYPE);
        primitiveTypes.put(PrimitiveTypeNames.FALSE_TYPE, falseTypeTypeSymbol);
        ISymbolFactory symbolFactory = createSymbolFactoryMock();

        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        IModifierHelper modifierHelper = mock(IModifierHelper.class);
        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
        IModifierSet modifiers = new ModifierSet();
        modifiers.add(TokenTypes.LogicNot);
        modifiers.add(TokenTypes.QuestionMark);
        when(modifierHelper.getModifiers(modifierAst)).thenReturn(modifiers);


        IReferencePhaseController controller = createController(
                symbolFactory, issueReporter, modifierHelper, primitiveTypes);
        IUnionTypeSymbol result = (IUnionTypeSymbol) controller.resolvePrimitiveType(ast, modifierAst);

        assertThat(result.getTypeSymbols(), allOf(
                hasEntry("\\a\\dummy", typeSymbol),
                hasEntry(PrimitiveTypeNames.FALSE_TYPE, falseTypeTypeSymbol),
                hasEntry(PrimitiveTypeNames.NULL_TYPE, nullTypeTypeSymbol)
        ));
        assertThat(result.getTypeSymbols().size(), is(3));
    }

    @Test
    public void resolvePrimitiveType_Bool_ReturnsBoolTypeSymbol() {
        String name = PrimitiveTypeNames.BOOL;
        ITSPHPAst ast = createAst(name);
        Map<String, ITypeSymbol> primitiveTypes = new HashMap<>();
        ITypeSymbol boolTypeSymbol = mock(ITypeSymbol.class);
        when(boolTypeSymbol.getName()).thenReturn(name);
        when(boolTypeSymbol.getAbsoluteName()).thenReturn(name);
        primitiveTypes.put(PrimitiveTypeNames.BOOL, boolTypeSymbol);
        ISymbolFactory symbolFactory = createSymbolFactoryMock();
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        IModifierHelper modifierHelper = mock(IModifierHelper.class);
        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
        IModifierSet modifiers = new ModifierSet();
        when(modifierHelper.getModifiers(modifierAst)).thenReturn(modifiers);

        IReferencePhaseController controller = createController(
                symbolFactory, issueReporter, modifierHelper, primitiveTypes);
        ITypeSymbol result = controller.resolvePrimitiveType(ast, modifierAst);

        assertThat(result, is(boolTypeSymbol));
    }

    @Test
    public void resolvePrimitiveType_Int_ReturnsIntTypeSymbol() {
        String name = PrimitiveTypeNames.INT;
        ITSPHPAst ast = createAst(name);
        Map<String, ITypeSymbol> primitiveTypes = new HashMap<>();
        ITypeSymbol intTypeSymbol = mock(ITypeSymbol.class);
        when(intTypeSymbol.getName()).thenReturn(name);
        when(intTypeSymbol.getAbsoluteName()).thenReturn(name);
        primitiveTypes.put(PrimitiveTypeNames.INT, intTypeSymbol);
        ISymbolFactory symbolFactory = createSymbolFactoryMock();
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        IModifierHelper modifierHelper = mock(IModifierHelper.class);
        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
        IModifierSet modifiers = new ModifierSet();
        when(modifierHelper.getModifiers(modifierAst)).thenReturn(modifiers);

        IReferencePhaseController controller = createController(
                symbolFactory, issueReporter, modifierHelper, primitiveTypes);
        ITypeSymbol result = controller.resolvePrimitiveType(ast, modifierAst);

        assertThat(result, is(intTypeSymbol));
    }

    @Test
    public void resolvePrimitiveType_Float_ReturnsFloatTypeSymbol() {
        String name = PrimitiveTypeNames.FLOAT;
        ITSPHPAst ast = createAst(name);
        Map<String, ITypeSymbol> primitiveTypes = new HashMap<>();
        ITypeSymbol floatTypeSymbol = mock(ITypeSymbol.class);
        when(floatTypeSymbol.getName()).thenReturn(name);
        when(floatTypeSymbol.getAbsoluteName()).thenReturn(name);
        primitiveTypes.put(PrimitiveTypeNames.FLOAT, floatTypeSymbol);
        ISymbolFactory symbolFactory = createSymbolFactoryMock();
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        IModifierHelper modifierHelper = mock(IModifierHelper.class);
        ITSPHPAst modifierAst = mock(ITSPHPAst.class);
        IModifierSet modifiers = new ModifierSet();
        when(modifierHelper.getModifiers(modifierAst)).thenReturn(modifiers);

        IReferencePhaseController controller = createController(
                symbolFactory, issueReporter, modifierHelper, primitiveTypes);
        ITypeSymbol result = controller.resolvePrimitiveType(ast, modifierAst);

        assertThat(result, is(floatTypeSymbol));
    }

    @Test
    public void resolvePrimitiveLiteral_NonExisting_ReturnsNull() {
        ITSPHPAst ast = mock(ITSPHPAst.class);

        IReferencePhaseController controller = createController();
        ITypeSymbol result = controller.resolvePrimitiveLiteral(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolvePrimitiveLiteral_Null_ReturnsNullTypeSymbol() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getType()).thenReturn(TokenTypes.Null);
        Map<String, ITypeSymbol> types = new HashMap<>();
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        types.put(PrimitiveTypeNames.NULL_TYPE, typeSymbol);

        ISymbolFactory symbolFactory = mock(ISymbolFactory.class);
        IUnionTypeSymbol unionTypeSymbol = mock(IUnionTypeSymbol.class);
        when(symbolFactory.createUnionTypeSymbol()).thenReturn(unionTypeSymbol);

        //act
        IReferencePhaseController controller = createController(symbolFactory, types);
        ITypeSymbol result = controller.resolvePrimitiveLiteral(ast);

        assertThat(result, is(typeSymbol));
    }

    @Test
    public void resolvePrimitiveLiteral_False_ReturnsFalseTypeSymbol() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getType()).thenReturn(TokenTypes.False);
        when(ast.getText()).thenReturn("false");
        Map<String, ITypeSymbol> types = new HashMap<>();
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        types.put(PrimitiveTypeNames.FALSE_TYPE, typeSymbol);

        ISymbolFactory symbolFactory = mock(ISymbolFactory.class);
        IUnionTypeSymbol unionTypeSymbol = mock(IUnionTypeSymbol.class);
        when(symbolFactory.createUnionTypeSymbol()).thenReturn(unionTypeSymbol);

        //act
        IReferencePhaseController controller = createController(symbolFactory, types);
        ITypeSymbol result = controller.resolvePrimitiveLiteral(ast);

        assertThat(result, is(typeSymbol));
    }

    @Test
    public void resolvePrimitiveLiteral_True_ReturnsTrueTypeSymbol() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getType()).thenReturn(TokenTypes.True);
        when(ast.getText()).thenReturn("true");
        Map<String, ITypeSymbol> types = new HashMap<>();
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        types.put(PrimitiveTypeNames.TRUE_TYPE, typeSymbol);

        ISymbolFactory symbolFactory = mock(ISymbolFactory.class);
        IUnionTypeSymbol unionTypeSymbol = mock(IUnionTypeSymbol.class);
        when(symbolFactory.createUnionTypeSymbol()).thenReturn(unionTypeSymbol);

        //act
        IReferencePhaseController controller = createController(symbolFactory, types);
        ITypeSymbol result = controller.resolvePrimitiveLiteral(ast);

        assertThat(result, is(typeSymbol));
    }

    @Test
    public void resolvePrimitiveLiteral_Int_ReturnsIntTypeSymbol() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getType()).thenReturn(TokenTypes.Int);
        Map<String, ITypeSymbol> types = new HashMap<>();
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        types.put(PrimitiveTypeNames.INT, typeSymbol);

        ISymbolFactory symbolFactory = mock(ISymbolFactory.class);
        IUnionTypeSymbol unionTypeSymbol = mock(IUnionTypeSymbol.class);
        when(symbolFactory.createUnionTypeSymbol()).thenReturn(unionTypeSymbol);

        //act
        IReferencePhaseController controller = createController(symbolFactory, types);
        ITypeSymbol result = controller.resolvePrimitiveLiteral(ast);

        assertThat(result, is(typeSymbol));
    }

    @Test
    public void resolvePrimitiveLiteral_Float_ReturnsFloatTypeSymbol() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getType()).thenReturn(TokenTypes.Float);
        Map<String, ITypeSymbol> types = new HashMap<>();
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        types.put(PrimitiveTypeNames.FLOAT, typeSymbol);

        ISymbolFactory symbolFactory = mock(ISymbolFactory.class);
        IUnionTypeSymbol unionTypeSymbol = mock(IUnionTypeSymbol.class);
        when(symbolFactory.createUnionTypeSymbol()).thenReturn(unionTypeSymbol);

        //act
        IReferencePhaseController controller = createController(symbolFactory, types);
        ITypeSymbol result = controller.resolvePrimitiveLiteral(ast);

        assertThat(result, is(typeSymbol));
    }

    @Test
    public void resolvePrimitiveLiteral_String_ReturnsStringTypeSymbol() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getType()).thenReturn(TokenTypes.String);
        Map<String, ITypeSymbol> types = new HashMap<>();
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        types.put(PrimitiveTypeNames.STRING, typeSymbol);

        ISymbolFactory symbolFactory = mock(ISymbolFactory.class);
        IUnionTypeSymbol unionTypeSymbol = mock(IUnionTypeSymbol.class);
        when(symbolFactory.createUnionTypeSymbol()).thenReturn(unionTypeSymbol);

        //act
        IReferencePhaseController controller = createController(symbolFactory, types);
        ITypeSymbol result = controller.resolvePrimitiveLiteral(ast);

        assertThat(result, is(typeSymbol));
    }

    @Test
    public void resolvePrimitiveLiteral_Array_ReturnsArrayTypeSymbol() {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getType()).thenReturn(TokenTypes.TypeArray);
        Map<String, ITypeSymbol> types = new HashMap<>();
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        types.put(PrimitiveTypeNames.ARRAY, typeSymbol);

        ISymbolFactory symbolFactory = mock(ISymbolFactory.class);
        IUnionTypeSymbol unionTypeSymbol = mock(IUnionTypeSymbol.class);
        when(symbolFactory.createUnionTypeSymbol()).thenReturn(unionTypeSymbol);

        //act
        IReferencePhaseController controller = createController(symbolFactory, types);
        ITypeSymbol result = controller.resolvePrimitiveLiteral(ast);

        assertThat(result, is(typeSymbol));
    }

    @Test
    public void createErroneousTypeSymbol1_Standard_DelegatesToSymbolFactory() {
        ITSPHPErrorAst ast = mock(ITSPHPErrorAst.class);
        TSPHPException exception = new TSPHPException();
        when(ast.getException()).thenReturn(exception);
        ISymbolFactory symbolFactory = createSymbolFactoryMock();

        IReferencePhaseController controller = createController(symbolFactory);
        controller.createErroneousTypeSymbol(ast);

        verify(symbolFactory).createErroneousTypeSymbol(ast, exception);
    }

    @Test
    public void createErroneousTypeSymbol2_Standard_DelegatesToSymbolFactory() {
        ITSPHPErrorAst ast = mock(ITSPHPErrorAst.class);
        RecognitionException exception = new RecognitionException();
        ISymbolFactory symbolFactory = createSymbolFactoryMock();

        IReferencePhaseController controller = createController(symbolFactory);
        controller.createErroneousTypeSymbol(ast, exception);

        ArgumentCaptor<TSPHPException> captor = ArgumentCaptor.forClass(TSPHPException.class);
        verify(symbolFactory).createErroneousTypeSymbol(eq(ast), captor.capture());
        assertThat(captor.getValue().getCause(), is((Throwable) exception));
    }

    @Test
    public void resolveUseType_IsNotAbsoluteIdentifier_ChangeToAbsolute() {
        ITSPHPAst ast = createAst("Dummy");
        ITSPHPAst alias = mock(ITSPHPAst.class);

        IReferencePhaseController controller = createController();
        controller.resolveUseType(ast, alias);

        verify(ast).setText("\\Dummy");
    }

    @Test
    public void resolveUseType_Standard_DelegatesToTypeSymbolResolver() {
        ITSPHPAst ast = createAst("Dummy");
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ISymbolResolverController symbolResolverController = mock(ISymbolResolverController.class);

        IReferencePhaseController controller = createController(symbolResolverController);
        controller.resolveUseType(ast, alias);

        verify(symbolResolverController).resolveIdentifierFromItsNamespaceScope(ast);
    }

    @Test
    public void resolveUseType_SymbolResolverFindsType_ReturnsType() {
        ITSPHPAst ast = createAst("Dummy");
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ISymbolResolverController symbolResolverController = mock(ISymbolResolverController.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbolResolverController.resolveIdentifierFromItsNamespaceScope(ast)).thenReturn(typeSymbol);

        IReferencePhaseController controller = createController(symbolResolverController);
        ITypeSymbol result = controller.resolveUseType(ast, alias);

        assertThat(result, is(typeSymbol));
    }

    @Test
    public void resolveUseType_TypeSymbolResolverDoesNotFindType_ReturnsAliasTypeSymbol() {
        String aliasName = "Dummy";
        ITSPHPAst ast = createAst(aliasName);
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ISymbolFactory symbolFactory = createSymbolFactoryMock();
        IAliasTypeSymbol aliasTypeSymbol = mock(IAliasTypeSymbol.class);
        when(symbolFactory.createAliasTypeSymbol(ast, aliasName)).thenReturn(aliasTypeSymbol);

        IReferencePhaseController controller = createController(symbolFactory);
        ITypeSymbol result = controller.resolveUseType(ast, alias);

        verify(symbolFactory).createAliasTypeSymbol(ast, aliasName);
        assertThat(result, is((ITypeSymbol) aliasTypeSymbol));
    }

    @Test
    public void
    checkUseDefinition_IsDefinedTwice_DoesNotCheckForAlreadyDefinedTypeAndReturnsFalseAndReportsDoubleDefinition() {
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        ITSPHPAst alias = mock(ITSPHPAst.class);
        IAliasSymbol aliasSymbol = mock(IAliasSymbol.class);
        when(alias.getSymbol()).thenReturn(aliasSymbol);
        ISymbolCheckController symbolCheckController = mock(ISymbolCheckController.class);
        ISymbol symbol = mock(ISymbol.class);
        when(symbolCheckController.isNotUseDoubleDefinition(alias))
                .thenReturn(new DoubleDefinitionCheckResultDto(false, symbol));

        //act
        IReferencePhaseController controller = createController(issueReporter, symbolCheckController);
        boolean result = controller.checkUseDefinition(alias);

        verify(symbolCheckController).isNotUseDoubleDefinition(alias);
        verifyNoMoreInteractions(symbolCheckController);
        verify(issueReporter).alreadyDefined(symbol, aliasSymbol);
        verifyNoMoreInteractions(issueReporter);
        assertThat(result, is(false));
    }

    @Test
    public void checkUseDefinition_IsDefinedOnlyOnceAndIsNotAlreadyDefinedAsType_ReturnsTrueAndNoException() {
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ISymbolCheckController symbolCheckController = mock(ISymbolCheckController.class);
        when(symbolCheckController.isNotUseDoubleDefinition(alias))
                .thenReturn(new DoubleDefinitionCheckResultDto(true, null));
        when(symbolCheckController.isNotAlreadyDefinedAsType(alias))
                .thenReturn(new AlreadyDefinedAsTypeResultDto(true, null));

        //act
        IReferencePhaseController controller = createController(issueReporter, symbolCheckController);
        boolean result = controller.checkUseDefinition(alias);

        verify(symbolCheckController).isNotUseDoubleDefinition(alias);
        verify(symbolCheckController).isNotAlreadyDefinedAsType(alias);
        verifyZeroInteractions(issueReporter);
        assertThat(result, is(true));
    }


    @Test
    public void
    checkUseDefinition_IsDefinedOnlyOnceAndIsAlreadyDefinedAsType_ReturnsFalseAndReportsDetermineAlreadyDefinedDefinition() {
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        ITSPHPAst alias = mock(ITSPHPAst.class);
        IAliasTypeSymbol aliasTypeSymbol = mock(IAliasTypeSymbol.class);
        when(alias.getSymbol()).thenReturn(aliasTypeSymbol);
        ISymbolCheckController symbolCheckController = mock(ISymbolCheckController.class);
        when(symbolCheckController.isNotUseDoubleDefinition(alias))
                .thenReturn(new DoubleDefinitionCheckResultDto(true, null));
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbolCheckController.isNotAlreadyDefinedAsType(alias))
                .thenReturn(new AlreadyDefinedAsTypeResultDto(false, typeSymbol));

        //act
        IReferencePhaseController controller = createController(issueReporter, symbolCheckController);
        boolean result = controller.checkUseDefinition(alias);

        verify(symbolCheckController).isNotUseDoubleDefinition(alias);
        verify(symbolCheckController).isNotAlreadyDefinedAsType(alias);
        verify(issueReporter).determineAlreadyDefined(aliasTypeSymbol, typeSymbol);
        assertThat(result, is(false));
    }

    @Test
    public void checkIsNotForwardReference_IsDefinedEarlier_ReturnsTrue() {
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbolCheckController symbolCheckController = mock(ISymbolCheckController.class);
        when(symbolCheckController.isNotForwardReference(ast))
                .thenReturn(new ForwardReferenceCheckResultDto(true, null));

        IReferencePhaseController controller = createController(issueReporter, symbolCheckController);
        boolean result = controller.checkIsNotForwardReference(ast);

        verify(symbolCheckController).isNotForwardReference(ast);
        verifyZeroInteractions(issueReporter);
        assertThat(result, is(true));
    }

    @Test
    public void checkIsNotForwardReference_IsDefinedLaterOwn_ReturnsFalseAndReportsForwardUsage() {
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbolCheckController symbolCheckController = mock(ISymbolCheckController.class);
        ITSPHPAst definitionAst = mock(ITSPHPAst.class);
        when(symbolCheckController.isNotForwardReference(ast))
                .thenReturn(new ForwardReferenceCheckResultDto(false, definitionAst));

        IReferencePhaseController controller = createController(issueReporter, symbolCheckController);
        boolean result = controller.checkIsNotForwardReference(ast);

        verify(symbolCheckController).isNotForwardReference(ast);
        verify(issueReporter).forwardReference(definitionAst, ast);
        assertThat(result, is(false));
    }

    @Test
    public void checkIsVariableInitialised_Standard_DelegatesToSymbolCheckController() {
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbolCheckController symbolCheckController = mock(ISymbolCheckController.class);
        when(symbolCheckController.isVariableInitialised(ast))
                .thenReturn(new VariableInitialisedResultDto(true, false));

        IReferencePhaseController controller = createController(issueReporter, symbolCheckController);
        controller.checkIsVariableInitialised(ast);

        verify(symbolCheckController).isVariableInitialised(ast);
    }

    @Test
    public void checkIsVariableInitialised_IsFullyInitialised_ReturnsTrueNoIssueReported() {
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbolCheckController symbolCheckController = mock(ISymbolCheckController.class);
        when(symbolCheckController.isVariableInitialised(ast))
                .thenReturn(new VariableInitialisedResultDto(true, false));

        IReferencePhaseController controller = createController(issueReporter, symbolCheckController);
        boolean result = controller.checkIsVariableInitialised(ast);

        verifyZeroInteractions(issueReporter);
        assertThat(result, is(true));
    }

    @Test
    public void checkIsVariableInitialised_IsPartiallyInitialised_ReturnsFalseReportsPartialInitialisation() {
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(ast.getSymbol()).thenReturn(symbol);
        ITSPHPAst definitionAst = mock(ITSPHPAst.class);
        when(symbol.getDefinitionAst()).thenReturn(definitionAst);
        ISymbolCheckController symbolCheckController = mock(ISymbolCheckController.class);
        when(symbolCheckController.isVariableInitialised(ast))
                .thenReturn(new VariableInitialisedResultDto(false, true));

        IReferencePhaseController controller = createController(issueReporter, symbolCheckController);
        boolean result = controller.checkIsVariableInitialised(ast);

        verify(issueReporter).variablePartiallyInitialised(definitionAst, ast);
        assertThat(result, is(false));
    }

    @Test
    public void checkIsVariableInitialised_IsNotInitialised_ReturnsFalseReportsUninitialisedVariable() {
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        ITSPHPAst ast = mock(ITSPHPAst.class);
        ISymbol symbol = mock(ISymbol.class);
        when(ast.getSymbol()).thenReturn(symbol);
        ITSPHPAst definitionAst = mock(ITSPHPAst.class);
        when(symbol.getDefinitionAst()).thenReturn(definitionAst);
        ISymbolCheckController symbolCheckController = mock(ISymbolCheckController.class);
        when(symbolCheckController.isVariableInitialised(ast))
                .thenReturn(new VariableInitialisedResultDto(false, false));

        IReferencePhaseController controller = createController(issueReporter, symbolCheckController);
        boolean result = controller.checkIsVariableInitialised(ast);

        verify(issueReporter).variableNotInitialised(definitionAst, ast);
        assertThat(result, is(false));
    }

    @Test
    public void transferInitialisedSymbolsFromGlobalDefault_IsDefaultNamespace_NothingHappens() {
        ITSPHPAst namespaceAst = mock(ITSPHPAst.class);
        when(namespaceAst.getText()).thenReturn("\\");
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);

        IReferencePhaseController controller = createController(globalNamespaceScope);
        controller.transferInitialisedSymbolsFromGlobalDefault(namespaceAst);

        verifyZeroInteractions(globalNamespaceScope);
    }

    @Test
    public void transferInitialisedSymbolsFromGlobalDefault_NoSymbolsDefined_NothingTransferred() {
        ITSPHPAst namespaceAst = mock(ITSPHPAst.class);
        when(namespaceAst.getText()).thenReturn("\\a\\");
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        IScope scope = mock(IScope.class);
        when(namespaceAst.getScope()).thenReturn(scope);
        Map<String, Boolean> symbols = spy(new HashMap<String, Boolean>());
        when(scope.getInitialisedSymbols()).thenReturn(symbols);
        Map<String, Boolean> globalSymbols = new HashMap<>();
        when(globalNamespaceScope.getInitialisedSymbols()).thenReturn(globalSymbols);

        IReferencePhaseController controller = createController(globalNamespaceScope);
        controller.transferInitialisedSymbolsFromGlobalDefault(namespaceAst);

        verify(globalNamespaceScope).getInitialisedSymbols();
        verify(scope).getInitialisedSymbols();
        assertThat(symbols.size(), is(0));
        verify(symbols, times(0)).containsKey(anyString());
    }

    @Test
    public void transferInitialisedSymbolsFromGlobalDefault_TwoDefined_TwoTransferred() {
        ITSPHPAst namespaceAst = mock(ITSPHPAst.class);
        when(namespaceAst.getText()).thenReturn("\\a\\");
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        IScope scope = mock(IScope.class);
        when(namespaceAst.getScope()).thenReturn(scope);
        Map<String, Boolean> symbols = spy(new HashMap<String, Boolean>());
        symbols.put("z", true);
        when(scope.getInitialisedSymbols()).thenReturn(symbols);
        Map<String, Boolean> globalSymbols = new HashMap<>();
        globalSymbols.put("a", true);
        globalSymbols.put("b", false);
        when(globalNamespaceScope.getInitialisedSymbols()).thenReturn(globalSymbols);

        IReferencePhaseController controller = createController(globalNamespaceScope);
        controller.transferInitialisedSymbolsFromGlobalDefault(namespaceAst);

        verify(globalNamespaceScope).getInitialisedSymbols();
        verify(scope).getInitialisedSymbols();
        assertThat(symbols, allOf(
                hasEntry("z", true),
                hasEntry("a", true),
                hasEntry("b", false)
        ));
        assertThat(symbols.size(), is(3));
        verify(symbols, times(2)).containsKey(anyString());
    }

    @Test
    public void
    transferInitialisedSymbolsFromGlobalDefault_TwoDefinedOneAlreadyInScopeButNotInitialised_TwoTransferred() {
        ITSPHPAst namespaceAst = mock(ITSPHPAst.class);
        when(namespaceAst.getText()).thenReturn("\\a\\");
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        IScope scope = mock(IScope.class);
        when(namespaceAst.getScope()).thenReturn(scope);
        Map<String, Boolean> symbols = spy(new HashMap<String, Boolean>());
        symbols.put("z", true);
        symbols.put("a", false);
        when(scope.getInitialisedSymbols()).thenReturn(symbols);
        Map<String, Boolean> globalSymbols = new HashMap<>();
        globalSymbols.put("a", true);
        globalSymbols.put("b", false);
        when(globalNamespaceScope.getInitialisedSymbols()).thenReturn(globalSymbols);

        IReferencePhaseController controller = createController(globalNamespaceScope);
        controller.transferInitialisedSymbolsFromGlobalDefault(namespaceAst);

        verify(globalNamespaceScope).getInitialisedSymbols();
        verify(scope).getInitialisedSymbols();
        assertThat(symbols, allOf(
                hasEntry("z", true),
                hasEntry("a", true),
                hasEntry("b", false)
        ));
        assertThat(symbols.size(), is(3));
        verify(symbols, times(2)).containsKey(anyString());
        verify(symbols).put("z", true);
        verify(symbols).put("a", false);
        verify(symbols).put("a", true);
        verify(symbols).put("b", false);
    }

    @Test
    public void transferInitialisedSymbolsFromGlobalDefault_TwoDefinedOneAlreadyInScopeAndInitialised_OneTransferred() {
        ITSPHPAst namespaceAst = mock(ITSPHPAst.class);
        when(namespaceAst.getText()).thenReturn("\\a\\");
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        IScope scope = mock(IScope.class);
        when(namespaceAst.getScope()).thenReturn(scope);
        Map<String, Boolean> symbols = spy(new HashMap<String, Boolean>());
        symbols.put("z", true);
        symbols.put("a", true);
        when(scope.getInitialisedSymbols()).thenReturn(symbols);
        Map<String, Boolean> globalSymbols = new HashMap<>();
        globalSymbols.put("a", true);
        globalSymbols.put("b", false);
        when(globalNamespaceScope.getInitialisedSymbols()).thenReturn(globalSymbols);

        IReferencePhaseController controller = createController(globalNamespaceScope);
        controller.transferInitialisedSymbolsFromGlobalDefault(namespaceAst);

        verify(globalNamespaceScope).getInitialisedSymbols();
        verify(scope).getInitialisedSymbols();
        assertThat(symbols, allOf(
                hasEntry("z", true),
                hasEntry("a", true),
                hasEntry("b", false)
        ));
        assertThat(symbols.size(), is(3));
        verify(symbols, times(2)).containsKey(anyString());
        verify(symbols, times(1)).get(anyString());
        verify(symbols).put("z", true);
        verify(symbols, times(1)).put("a", true);
        verify(symbols).put("b", false);
    }

    @Test
    public void transferInitialisedSymbolsToGlobalDefault_IsDefaultNamespace_NothingHappens() {
        ITSPHPAst namespaceAst = mock(ITSPHPAst.class);
        when(namespaceAst.getText()).thenReturn("\\");
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);

        IReferencePhaseController controller = createController(globalNamespaceScope);
        controller.transferInitialisedSymbolsToGlobalDefault(namespaceAst);

        verifyZeroInteractions(globalNamespaceScope);
    }

    @Test
    public void transferInitialisedSymbolsToGlobalDefault_NoSymbolsDefined_NothingTransferred() {
        ITSPHPAst namespaceAst = mock(ITSPHPAst.class);
        when(namespaceAst.getText()).thenReturn("\\a\\");
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        IScope scope = mock(IScope.class);
        when(namespaceAst.getScope()).thenReturn(scope);
        Map<String, Boolean> symbols = new HashMap<>();
        when(scope.getInitialisedSymbols()).thenReturn(symbols);
        Map<String, Boolean> globalSymbols = spy(new HashMap<String, Boolean>());
        when(globalNamespaceScope.getInitialisedSymbols()).thenReturn(globalSymbols);

        IReferencePhaseController controller = createController(globalNamespaceScope);
        controller.transferInitialisedSymbolsToGlobalDefault(namespaceAst);

        verify(globalNamespaceScope).getInitialisedSymbols();
        verify(scope).getInitialisedSymbols();
        assertThat(globalSymbols.size(), is(0));
        verify(globalSymbols, times(0)).containsKey(anyString());
    }

    @Test
    public void transferInitialisedSymbolsToGlobalDefault_TwoDefined_TwoTransferred() {
        ITSPHPAst namespaceAst = mock(ITSPHPAst.class);
        when(namespaceAst.getText()).thenReturn("\\a\\");
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        IScope scope = mock(IScope.class);
        when(namespaceAst.getScope()).thenReturn(scope);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", false);
        when(scope.getInitialisedSymbols()).thenReturn(symbols);
        Map<String, Boolean> globalSymbols = spy(new HashMap<String, Boolean>());
        globalSymbols.put("z", true);
        when(globalNamespaceScope.getInitialisedSymbols()).thenReturn(globalSymbols);

        IReferencePhaseController controller = createController(globalNamespaceScope);
        controller.transferInitialisedSymbolsToGlobalDefault(namespaceAst);

        verify(globalNamespaceScope).getInitialisedSymbols();
        verify(scope).getInitialisedSymbols();
        assertThat(globalSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", true),
                hasEntry("b", false)
        ));
        assertThat(globalSymbols.size(), is(3));
        verify(globalSymbols, times(2)).containsKey(anyString());
    }

    @Test
    public void
    transferInitialisedSymbolsToGlobalDefault_TwoDefinedOneAlreadyInScopeButNotInitialised_TwoTransferred() {
        ITSPHPAst namespaceAst = mock(ITSPHPAst.class);
        when(namespaceAst.getText()).thenReturn("\\a\\");
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        IScope scope = mock(IScope.class);
        when(namespaceAst.getScope()).thenReturn(scope);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", false);
        when(scope.getInitialisedSymbols()).thenReturn(symbols);
        Map<String, Boolean> globalSymbols = spy(new HashMap<String, Boolean>());
        globalSymbols.put("z", true);
        globalSymbols.put("a", false);
        when(globalNamespaceScope.getInitialisedSymbols()).thenReturn(globalSymbols);


        IReferencePhaseController controller = createController(globalNamespaceScope);
        controller.transferInitialisedSymbolsToGlobalDefault(namespaceAst);

        verify(globalNamespaceScope).getInitialisedSymbols();
        verify(scope).getInitialisedSymbols();
        assertThat(globalSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", true),
                hasEntry("b", false)
        ));
        assertThat(globalSymbols.size(), is(3));
        verify(globalSymbols, times(2)).containsKey(anyString());
        verify(globalSymbols).put("z", true);
        verify(globalSymbols).put("a", false);
        verify(globalSymbols).put("a", true);
        verify(globalSymbols).put("b", false);
    }

    @Test
    public void transferInitialisedSymbolsToGlobalDefault_TwoDefinedOneAlreadyInScopeAndInitialised_OneTransferred() {
        ITSPHPAst namespaceAst = mock(ITSPHPAst.class);
        when(namespaceAst.getText()).thenReturn("\\a\\");
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);
        IScope scope = mock(IScope.class);
        when(namespaceAst.getScope()).thenReturn(scope);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", false);
        when(scope.getInitialisedSymbols()).thenReturn(symbols);
        Map<String, Boolean> globalSymbols = spy(new HashMap<String, Boolean>());
        globalSymbols.put("z", true);
        globalSymbols.put("a", true);
        when(globalNamespaceScope.getInitialisedSymbols()).thenReturn(globalSymbols);

        IReferencePhaseController controller = createController(globalNamespaceScope);
        controller.transferInitialisedSymbolsToGlobalDefault(namespaceAst);

        verify(globalNamespaceScope).getInitialisedSymbols();
        verify(scope).getInitialisedSymbols();
        assertThat(globalSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", true),
                hasEntry("b", false)
        ));
        assertThat(globalSymbols.size(), is(3));
        verify(globalSymbols, times(2)).containsKey(anyString());
        verify(globalSymbols, times(1)).get(anyString());
        verify(globalSymbols).put("z", true);
        verify(globalSymbols).put("a", true);
        verify(globalSymbols).put("b", false);
    }

    @Test
    public void sendUpInitialisedSymbols_NoSymbolsDefined_NothingTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbols(blockConditional);

        assertThat(enclosingSymbols.size(), is(0));
        verify(enclosingSymbols, times(0)).containsKey(anyString());
    }

    @Test
    public void sendUpInitialisedSymbols_TwoDefined_TwoTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", false);
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbols(blockConditional);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols, times(2)).containsKey(anyString());
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols).put("b", false);
    }

    @Test
    public void sendUpInitialisedSymbols_TwoDefinedOneAlreadyInScopeButNotInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        enclosingSymbols.put("b", false);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", false);
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbols(blockConditional);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols, times(2)).containsKey(anyString());
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols, times(1)).put("b", false);
    }

    @Test
    public void sendUpInitialisedSymbols_TwoDefinedOneAlreadyInScopeAndInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        enclosingSymbols.put("b", false);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", true);
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbols(blockConditional);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols, times(2)).containsKey(anyString());
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols, times(1)).put("b", false);
    }


    @Test
    public void sendUpInitialisedSymbolsAfterIf_TwoSymbolsDefinedButOnlyOneInBoth_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst ifBlock = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", true);
        ITSPHPAst elseBlock = createBlockConditional(symbols2, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterIf(ifBlock, elseBlock);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", true),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", true);
        verify(enclosingSymbols).put("b", false);
    }

    @Test
    public void sendUpInitialisedSymbolsAfterIf_TwoSymbolsDefinedButOneOnlyPartialInOneBlock_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst ifBlock = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", false);
        symbols2.put("b", true);
        ITSPHPAst elseBlock = createBlockConditional(symbols2, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterIf(ifBlock, elseBlock);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", true)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols).put("b", true);
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterIf_ThreeBlocksTwoSymbolsDefinedButOneInEnclosingScopeFullyInitialised_OneTransferred
            () {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("a", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst ifBlock = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", true);
        symbols2.put("b", true);
        ITSPHPAst elseBlock = createBlockConditional(symbols2, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterIf(ifBlock, elseBlock);

        assertThat(enclosingSymbols, allOf(
                hasEntry("a", true),
                hasEntry("b", true)
        ));
        assertThat(enclosingSymbols.size(), is(2));
        verify(enclosingSymbols, times(1)).put("a", true);
        verify(enclosingSymbols).put("b", true);
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterIf_TwoSymbolsDefinedButOneInEnclosingScopePartiallyInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("a", false);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst ifBlock = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", true);
        symbols2.put("b", true);
        ITSPHPAst elseBlock = createBlockConditional(symbols2, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterIf(ifBlock, elseBlock);

        assertThat(enclosingSymbols, allOf(
                hasEntry("a", true),
                hasEntry("b", true)
        ));
        assertThat(enclosingSymbols.size(), is(2));
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols).put("a", true);
        verify(enclosingSymbols).put("b", true);
    }

    @Test
    public void sendUpInitialisedSymbolsAfterIf_OnlyIfNoSymbolsDefined_NothingTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterIf(blockConditional, null);

        assertThat(enclosingSymbols.size(), is(0));
        verify(enclosingSymbols, times(0)).containsKey(anyString());
    }

    @Test
    public void sendUpInitialisedSymbolsAfterIf_OnlyIfTwoDefined_TwoTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", false);
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);
        Map<String, Boolean> result = new HashMap<>();
        result.put("z", true);
        result.put("a", false);
        result.put("b", false);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterIf(blockConditional, null);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols, times(2)).containsKey(anyString());
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols).put("b", false);
    }

    @Test
    public void sendUpInitialisedSymbolsAfterIf_OnlyIfTwoDefinedOneAlreadyInScopeButNotInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        enclosingSymbols.put("b", false);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", false);
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterIf(blockConditional, null);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols, times(2)).containsKey(anyString());
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols, times(1)).put("b", false);
    }

    @Test
    public void sendUpInitialisedSymbolsAfterIf_OnlyIfTwoDefinedOneAlreadyInScopeAndInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        enclosingSymbols.put("b", false);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", true);
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterIf(blockConditional, null);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols, times(2)).containsKey(anyString());
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols, times(1)).put("b", false);
    }

    @Test
    public void sendUpInitialisedSymbolsAfterSwitch_ThreeBlocksTwoSymbolsDefinedButOnlyOneInAll_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst blockConditional1 = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", true);
        symbols2.put("b", true);
        ITSPHPAst blockConditional2 = createBlockConditional(symbols2, enclosingScope);
        Map<String, Boolean> symbols3 = new HashMap<>();
        symbols3.put("a", true);
        ITSPHPAst blockConditional3 = createBlockConditional(symbols3, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterSwitch(
                Arrays.asList(blockConditional1, blockConditional2, blockConditional3), true);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", true),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", true);
        verify(enclosingSymbols).put("b", false);
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterSwitch_ThreeBlocksTwoSymbolsDefinedButOneOnlyPartialInOneBlock_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst blockConditional1 = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", false);
        symbols2.put("b", true);
        ITSPHPAst blockConditional2 = createBlockConditional(symbols2, enclosingScope);
        Map<String, Boolean> symbols3 = new HashMap<>();
        symbols3.put("a", true);
        symbols3.put("b", true);
        ITSPHPAst blockConditional3 = createBlockConditional(symbols3, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterSwitch(
                Arrays.asList(blockConditional1, blockConditional2, blockConditional3), true);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", true)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols).put("b", true);
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterSwitch_ThreeBlocksTwoSymbolsDefinedButOneInEnclosingScopeFullyInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("a", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst blockConditional1 = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", true);
        symbols2.put("b", true);
        ITSPHPAst blockConditional2 = createBlockConditional(symbols2, enclosingScope);
        Map<String, Boolean> symbols3 = new HashMap<>();
        symbols3.put("a", true);
        symbols3.put("b", true);
        ITSPHPAst blockConditional3 = createBlockConditional(symbols3, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterSwitch(
                Arrays.asList(blockConditional1, blockConditional2, blockConditional3), true);

        assertThat(enclosingSymbols, allOf(
                hasEntry("a", true),
                hasEntry("b", true)
        ));
        assertThat(enclosingSymbols.size(), is(2));
        verify(enclosingSymbols, times(1)).put("a", true);
        verify(enclosingSymbols).put("b", true);
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterSwitch_ThreeBlocksTwoSymbolsDefinedButOneInEnclosingScopePartiallyInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("a", false);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst blockConditional1 = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", true);
        symbols2.put("b", true);
        ITSPHPAst blockConditional2 = createBlockConditional(symbols2, enclosingScope);
        Map<String, Boolean> symbols3 = new HashMap<>();
        symbols3.put("a", true);
        symbols3.put("b", true);
        ITSPHPAst blockConditional3 = createBlockConditional(symbols3, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterSwitch(
                Arrays.asList(blockConditional1, blockConditional2, blockConditional3), true);

        assertThat(enclosingSymbols, allOf(
                hasEntry("a", true),
                hasEntry("b", true)
        ));
        assertThat(enclosingSymbols.size(), is(2));
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols).put("a", true);
        verify(enclosingSymbols).put("b", true);
    }

    @Test
    public void sendUpInitialisedSymbolsAfterSwitch_HasNoDefaultNoSymbolsDefined_NothingTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterSwitch(Arrays.asList(blockConditional), false);

        assertThat(enclosingSymbols.size(), is(0));
        verify(enclosingSymbols, times(0)).containsKey(anyString());
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterSwitch_HasNoDefaultTwoDefinedInDifferentCases_TwoTransferredBothPartially() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        ITSPHPAst blockConditional1 = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("b", true);
        ITSPHPAst blockConditional2 = createBlockConditional(symbols2, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterSwitch(
                Arrays.asList(blockConditional1, blockConditional2), false);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols).put("b", false);
    }

    @Test
    public void sendUpInitialisedSymbolsAfterSwitch_HasNoDefaultTwoDefined_TwoTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", false);
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterSwitch(Arrays.asList(blockConditional), false);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols, times(2)).containsKey(anyString());
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols).put("b", false);
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterSwitch_HasNoDefaultTwoDefinedOneAlreadyInScopeButNotInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        enclosingSymbols.put("b", false);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", false);
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterSwitch(Arrays.asList(blockConditional), false);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols, times(2)).containsKey(anyString());
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols, times(1)).put("b", false);
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterSwitch_HasNoDefaultTwoDefinedOneAlreadyInScopeAndInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        enclosingSymbols.put("b", false);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        symbols.put("a", true);
        symbols.put("b", true);
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterSwitch(Arrays.asList(blockConditional), false);

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols, times(2)).containsKey(anyString());
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols, times(1)).put("b", false);
    }

    @Test
    public void sendUpInitialisedSymbolsAfterTryCatch_NoBlock_NothingTransferred() {
        //no arrange necessary
        List<ITSPHPAst> list = mock(List.class);
        when(list.size()).thenReturn(0);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterTryCatch(list);

        verify(list).size();
        //no further assert possible, but at least the test for size was done
    }

    @Test
    public void sendUpInitialisedSymbolsAfterTryCatch_OneBlockNothingDefined_NothingTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols = new HashMap<>();
        ITSPHPAst blockConditional = createBlockConditional(symbols, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterTryCatch(Arrays.asList(blockConditional));

        assertThat(enclosingSymbols.size(), is(0));
        verify(enclosingSymbols, times(0)).containsKey(anyString());
    }

    @Test
    public void sendUpInitialisedSymbolsAfterTryCatch_ThreeBlocksTwoSymbolsDefinedButOnlyOneInAll_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst blockConditional1 = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", true);
        symbols2.put("b", true);
        ITSPHPAst blockConditional2 = createBlockConditional(symbols2, enclosingScope);
        Map<String, Boolean> symbols3 = new HashMap<>();
        symbols3.put("a", true);
        ITSPHPAst blockConditional3 = createBlockConditional(symbols3, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterTryCatch(
                Arrays.asList(blockConditional1, blockConditional2, blockConditional3));

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", true),
                hasEntry("b", false)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", true);
        verify(enclosingSymbols).put("b", false);
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterTryCatch_ThreeBlocksTwoSymbolsDefinedButOneOnlyPartialInOneBlock_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("z", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst blockConditional1 = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", false);
        symbols2.put("b", true);
        ITSPHPAst blockConditional2 = createBlockConditional(symbols2, enclosingScope);
        Map<String, Boolean> symbols3 = new HashMap<>();
        symbols3.put("a", true);
        symbols3.put("b", true);
        ITSPHPAst blockConditional3 = createBlockConditional(symbols3, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterTryCatch(
                Arrays.asList(blockConditional1, blockConditional2, blockConditional3));

        assertThat(enclosingSymbols, allOf(
                hasEntry("z", true),
                hasEntry("a", false),
                hasEntry("b", true)
        ));
        assertThat(enclosingSymbols.size(), is(3));
        verify(enclosingSymbols).put("z", true);
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols).put("b", true);
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterTryCatch_ThreeBlocksTwoSymbolsDefinedButOneInEnclosingScopeFullyInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("a", true);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst blockConditional1 = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", true);
        symbols2.put("b", true);
        ITSPHPAst blockConditional2 = createBlockConditional(symbols2, enclosingScope);
        Map<String, Boolean> symbols3 = new HashMap<>();
        symbols3.put("a", true);
        symbols3.put("b", true);
        ITSPHPAst blockConditional3 = createBlockConditional(symbols3, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterTryCatch(
                Arrays.asList(blockConditional1, blockConditional2, blockConditional3));

        assertThat(enclosingSymbols, allOf(
                hasEntry("a", true),
                hasEntry("b", true)
        ));
        assertThat(enclosingSymbols.size(), is(2));
        verify(enclosingSymbols, times(1)).put("a", true);
        verify(enclosingSymbols).put("b", true);
    }

    @Test
    public void
    sendUpInitialisedSymbolsAfterTryCatch_ThreeBlocksTwoSymbolsDefinedButOneInEnclosingScopePartiallyInitialised_OneTransferred() {
        Map<String, Boolean> enclosingSymbols = spy(new HashMap<String, Boolean>());
        enclosingSymbols.put("a", false);
        IScope enclosingScope = createScope(enclosingSymbols);
        Map<String, Boolean> symbols1 = new HashMap<>();
        symbols1.put("a", true);
        symbols1.put("b", true);
        ITSPHPAst blockConditional1 = createBlockConditional(symbols1, enclosingScope);
        Map<String, Boolean> symbols2 = new HashMap<>();
        symbols2.put("a", true);
        symbols2.put("b", true);
        ITSPHPAst blockConditional2 = createBlockConditional(symbols2, enclosingScope);
        Map<String, Boolean> symbols3 = new HashMap<>();
        symbols3.put("a", true);
        symbols3.put("b", true);
        ITSPHPAst blockConditional3 = createBlockConditional(symbols3, enclosingScope);

        IReferencePhaseController controller = createController();
        controller.sendUpInitialisedSymbolsAfterTryCatch(
                Arrays.asList(blockConditional1, blockConditional2, blockConditional3));

        assertThat(enclosingSymbols, allOf(
                hasEntry("a", true),
                hasEntry("b", true)
        ));
        assertThat(enclosingSymbols.size(), is(2));
        verify(enclosingSymbols).put("a", false);
        verify(enclosingSymbols).put("a", true);
        verify(enclosingSymbols).put("b", true);
    }

    @Test
    public void
    addImplicitReturnStatementIfRequired_IsReturningAndHasAtLeastOneReturnOrThrow_NothingAddedIssueReporterNotCalled() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);

        IReferencePhaseController controller = createController(issueReporter);
        controller.addImplicitReturnStatementIfRequired(true, true, identifier, block);

        verifyZeroInteractions(issueReporter);
        verifyZeroInteractions(identifier);
    }

    /**
     * Should never happen, but if isReturn is true then hasAtLeastOneReturnOrThrow should simply be ignored
     */
    @Test
    public void addImplicitReturnStatementIfRequired_IsReturningHasNoReturnOThrow_NothingAddedIssueReporterNotCalled() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);

        IReferencePhaseController controller = createController(issueReporter);
        controller.addImplicitReturnStatementIfRequired(true, false, identifier, block);

        verifyZeroInteractions(issueReporter);
        verifyZeroInteractions(identifier);
    }

    /**
     * Should never happen, but if isReturn is true then hasAtLeastOneReturnOrThrow should simply be ignored
     */
    @Test
    public void
    addImplicitReturnStatementIfRequired_IsNotReturningAndHasAtLeastOneReturnOrThrow_ReturnAddedAndPartiallyCalled() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        when(identifier.getSymbol()).thenReturn(methodSymbol);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst returnAst = mock(ITSPHPAst.class);
        when(astModificationHelper.createNullLiteral()).thenReturn(mock(ITSPHPAst.class));
        when(astModificationHelper.createReturnStatement(any(ITSPHPAst.class))).thenReturn(returnAst);

        IReferencePhaseController controller = createController(issueReporter, astModificationHelper);
        controller.addImplicitReturnStatementIfRequired(false, true, identifier, block);

        verify(issueReporter).partialReturnFromFunction(identifier);
        verify(block).addChild(returnAst);
    }

    /**
     * Should never happen, but if isReturn is true then hasAtLeastOneReturnOrThrow should simply be ignored
     */
    @Test
    public void addImplicitReturnStatementIfRequired_IsNotReturningHasNoReturnOThrow_ReturnAddedAndNoReturnCalled() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        when(identifier.getSymbol()).thenReturn(methodSymbol);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst returnAst = mock(ITSPHPAst.class);
        when(astModificationHelper.createNullLiteral()).thenReturn(mock(ITSPHPAst.class));
        when(astModificationHelper.createReturnStatement(any(ITSPHPAst.class))).thenReturn(returnAst);

        IReferencePhaseController controller = createController(issueReporter, astModificationHelper);
        controller.addImplicitReturnStatementIfRequired(false, false, identifier, block);

        verify(issueReporter).noReturnFromFunction(identifier);
        verify(block).addChild(returnAst);
    }

    /**
     * Should never happen, but if isReturn is true then hasAtLeastOneReturnOrThrow should simply be ignored
     */
    @Test
    public void
    addImplicitReturnStatementIfRequired_IsNotReturningAndHasAtLeastOneReturnOrThrow_ReturnStatementHasScopeAndConstraintCreated() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        when(identifier.getSymbol()).thenReturn(methodSymbol);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst returnAst = mock(ITSPHPAst.class);
        ITSPHPAst nullLiteral = mock(ITSPHPAst.class);
        when(astModificationHelper.createNullLiteral()).thenReturn(nullLiteral);
        when(astModificationHelper.createReturnStatement(any(ITSPHPAst.class))).thenReturn(returnAst);
        IConstraintCreator constraintCreator = mock(IConstraintCreator.class);

        IReferencePhaseController controller = createController(issueReporter, astModificationHelper,
                constraintCreator);
        controller.addImplicitReturnStatementIfRequired(false, true, identifier, block);

        verify(returnAst).setScope(methodSymbol);
        verify(constraintCreator).createRefConstraint(methodSymbol, returnAst, nullLiteral);
    }

    /**
     * Should never happen, but if isReturn is true then hasAtLeastOneReturnOrThrow should simply be ignored
     */
    @Test
    public void
    addImplicitReturnStatementIfRequired_IsNotReturningAndHasNoReturnOrThrow_ReturnStatementHasScopeAndConstraintCreated() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        when(identifier.getSymbol()).thenReturn(methodSymbol);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IInferenceIssueReporter issueReporter = mock(IInferenceIssueReporter.class);
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst returnAst = mock(ITSPHPAst.class);
        ITSPHPAst nullLiteral = mock(ITSPHPAst.class);
        when(astModificationHelper.createNullLiteral()).thenReturn(nullLiteral);
        when(astModificationHelper.createReturnStatement(any(ITSPHPAst.class))).thenReturn(returnAst);
        IConstraintCreator constraintCreator = mock(IConstraintCreator.class);

        IReferencePhaseController controller = createController(issueReporter, astModificationHelper,
                constraintCreator);
        controller.addImplicitReturnStatementIfRequired(false, false, identifier, block);

        verify(returnAst).setScope(methodSymbol);
        verify(constraintCreator).createRefConstraint(methodSymbol, returnAst, nullLiteral);
    }

    private ITSPHPAst createAst(String name) {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getText()).thenReturn(name);
        return ast;
    }

    private ITSPHPAst createBlockConditional(Map<String, Boolean> symbols, IScope enclosingScope) {
        ITSPHPAst blockConditional = mock(ITSPHPAst.class);
        IScope scope = createScope(symbols);
        when(blockConditional.getScope()).thenReturn(scope);
        when(scope.getEnclosingScope()).thenReturn(enclosingScope);
        return blockConditional;
    }

    private IScope createScope(Map<String, Boolean> symbols) {
        IScope scope = mock(IScope.class);
        when(scope.getInitialisedSymbols()).thenReturn(symbols);
        return scope;
    }

    private IReferencePhaseController createController() {
        return createController(mock(IInferenceIssueReporter.class));
    }

    private IReferencePhaseController createController(IInferenceIssueReporter issueReporter) {
        return createController(issueReporter, mock(IScopeHelper.class));
    }

    private IReferencePhaseController createController(
            IInferenceIssueReporter issueReporter, IScopeHelper scopeHelper) {
        return createController(issueReporter, mock(IAstModificationHelper.class), scopeHelper);
    }

    private IReferencePhaseController createController(
            IInferenceIssueReporter issueReporter, IAstModificationHelper astModificationHelper) {
        return createController(
                issueReporter, astModificationHelper, mock(IScopeHelper.class));
    }

    private IReferencePhaseController createController(
            IInferenceIssueReporter issueReporter,
            IAstModificationHelper astModificationHelper,
            IConstraintCreator constraintCreator) {
        ICore core = mock(ICore.class);
        when(core.getPrimitiveTypes()).thenReturn(new HashMap<String, ITypeSymbol>());
        return createController(
                createSymbolFactoryMock(),
                issueReporter,
                astModificationHelper,
                mock(ISymbolResolverController.class),
                mock(ISymbolCheckController.class),
                mock(IVariableDeclarationCreator.class),
                mock(IScopeHelper.class),
                mock(IModifierHelper.class),
                constraintCreator,
                mock(IConstraintSolver.class),
                core,
                mock(IGlobalNamespaceScope.class)
        );

    }

    private IReferencePhaseController createController(
            IInferenceIssueReporter issueReporter,
            IAstModificationHelper astModificationHelper,
            IScopeHelper scopeHelper) {
        ICore core = mock(ICore.class);
        when(core.getPrimitiveTypes()).thenReturn(new HashMap<String, ITypeSymbol>());
        return createController(
                createSymbolFactoryMock(),
                issueReporter,
                astModificationHelper,
                mock(ISymbolResolverController.class),
                mock(ISymbolCheckController.class),
                mock(IVariableDeclarationCreator.class),
                scopeHelper,
                mock(IModifierHelper.class),
                mock(IConstraintCreator.class),
                mock(IConstraintSolver.class),
                core,
                mock(IGlobalNamespaceScope.class)
        );
    }

    private IReferencePhaseController createController(
            IInferenceIssueReporter issueReporter,
            ISymbolCheckController symbolCheckController) {
        ICore core = mock(ICore.class);
        when(core.getPrimitiveTypes()).thenReturn(new HashMap<String, ITypeSymbol>());
        return createController(
                createSymbolFactoryMock(),
                issueReporter,
                mock(IAstModificationHelper.class),
                mock(ISymbolResolverController.class),
                symbolCheckController,
                mock(IVariableDeclarationCreator.class),
                mock(IScopeHelper.class),
                mock(IModifierHelper.class),
                mock(IConstraintCreator.class),
                mock(IConstraintSolver.class),
                core,
                mock(IGlobalNamespaceScope.class)
        );

    }

    private IReferencePhaseController createController(ISymbolFactory symbolFactory) {
        return createController(
                symbolFactory,
                mock(IInferenceIssueReporter.class),
                new HashMap<String, ITypeSymbol>()
        );
    }

    private IReferencePhaseController createController(
            ISymbolFactory symbolFactory, Map<String, ITypeSymbol> primitiveTypes) {
        return createController(
                symbolFactory,
                mock(IInferenceIssueReporter.class),
                primitiveTypes
        );
    }

    private ISymbolFactory createSymbolFactoryMock() {
        ISymbolFactory symbolFactory = mock(ISymbolFactory.class);
        final ITypeHelper typeHelper = mock(ITypeHelper.class);
        TypeHelperDto dto = new TypeHelperDto(mock(ITypeSymbol.class), mock(ITypeSymbol.class), false);
        when(typeHelper.isFirstSameOrParentTypeOfSecond(any(ITypeSymbol.class), any(ITypeSymbol.class), eq(false)))
                .thenReturn(dto);
        when(typeHelper.isFirstSameOrSubTypeOfSecond(any(ITypeSymbol.class), any(ITypeSymbol.class), eq(false)))
                .thenReturn(dto);
        when(symbolFactory.createUnionTypeSymbol()).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return new UnionTypeSymbol(typeHelper);
            }
        });
        return symbolFactory;
    }

    private IReferencePhaseController createController(
            ISymbolFactory symbolFactory,
            IInferenceIssueReporter issueReporter,
            Map<String, ITypeSymbol> primitiveTypes) {
        return createController(
                symbolFactory,
                issueReporter,
                mock(IModifierHelper.class),
                primitiveTypes
        );
    }

    private IReferencePhaseController createController(
            ISymbolFactory symbolFactory,
            IInferenceIssueReporter issueReporter,
            IModifierHelper modifierHelper, Map<String, ITypeSymbol> primitiveTypes) {
        ICore core = mock(ICore.class);
        when(core.getPrimitiveTypes()).thenReturn(primitiveTypes);
        return createController(
                symbolFactory,
                issueReporter,
                mock(IAstModificationHelper.class),
                mock(ISymbolResolverController.class),
                mock(ISymbolCheckController.class),
                mock(IVariableDeclarationCreator.class),
                mock(IScopeHelper.class),
                modifierHelper,
                mock(IConstraintCreator.class),
                mock(IConstraintSolver.class),
                core,
                mock(IGlobalNamespaceScope.class)
        );
    }


    private IReferencePhaseController createController(IGlobalNamespaceScope globalNamespaceScope) {
        ICore core = mock(ICore.class);
        when(core.getPrimitiveTypes()).thenReturn(new HashMap<String, ITypeSymbol>());
        return createController(
                createSymbolFactoryMock(),
                mock(IInferenceIssueReporter.class),
                mock(IAstModificationHelper.class),
                mock(ISymbolResolverController.class),
                mock(ISymbolCheckController.class),
                mock(IVariableDeclarationCreator.class),
                mock(IScopeHelper.class),
                mock(IModifierHelper.class),
                mock(IConstraintCreator.class),
                mock(IConstraintSolver.class),
                core,
                globalNamespaceScope
        );
    }

    private IReferencePhaseController createController(ISymbolResolverController symbolResolverController) {
        return createController(symbolResolverController, mock(IVariableDeclarationCreator.class));
    }

    private IReferencePhaseController createController(
            ISymbolResolverController symbolResolverController,
            IVariableDeclarationCreator variableDeclarationCreator) {
        ICore core = mock(ICore.class);
        when(core.getPrimitiveTypes()).thenReturn(new HashMap<String, ITypeSymbol>());
        return createController(
                createSymbolFactoryMock(),
                mock(IInferenceIssueReporter.class),
                mock(IAstModificationHelper.class),
                symbolResolverController,
                mock(ISymbolCheckController.class),
                variableDeclarationCreator,
                mock(IScopeHelper.class),
                mock(IModifierHelper.class),
                mock(IConstraintCreator.class),
                mock(IConstraintSolver.class),
                core,
                mock(IGlobalNamespaceScope.class)
        );
    }

    protected IReferencePhaseController createController(
            ISymbolFactory symbolFactory,
            IInferenceIssueReporter inferenceErrorReporter,
            IAstModificationHelper astModificationHelper,
            ISymbolResolverController symbolResolverControllerController,
            ISymbolCheckController theSymbolCheckController,
            IVariableDeclarationCreator theVariableDeclarationCreator,
            IScopeHelper scopeHelper,
            IModifierHelper modifierHelper,
            IConstraintCreator constraintCreator,
            IConstraintSolver constraintSolver,
            ICore theCore,
            IGlobalNamespaceScope globalDefaultNamespace) {
        return new ReferencePhaseController(
                symbolFactory,
                inferenceErrorReporter,
                astModificationHelper,
                symbolResolverControllerController,
                theSymbolCheckController,
                theVariableDeclarationCreator,
                scopeHelper,
                modifierHelper,
                constraintCreator,
                constraintSolver,
                theCore,
                globalDefaultNamespace
        );
    }
}
