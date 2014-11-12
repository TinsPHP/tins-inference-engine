/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.common.symbols.IAliasTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ISymbolResolver;
import ch.tsphp.tinsphp.common.symbols.ITypeSymbolResolver;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.erroneous.IErroneousVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.IReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.ReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ReferencePhaseControllerTest
{
    @Test
    public void resolveConstant_Standard_DelegatesToSymbolResolver() {
        ITSPHPAst ast = createAst("Dummy");
        ISymbolResolver symbolResolver = mock(ISymbolResolver.class);

        IReferencePhaseController controller = createController(symbolResolver);
        controller.resolveConstant(ast);

        verify(symbolResolver).resolveConstantLikeIdentifier(ast);
    }

    @Test
    public void resolveConstant_SymbolResolverFindsSymbol_ReturnsSymbol() {
        ITSPHPAst ast = createAst("Dummy");
        ISymbolResolver symbolResolver = mock(ISymbolResolver.class);
        IVariableSymbol symbol = mock(IVariableSymbol.class);
        when(symbolResolver.resolveConstantLikeIdentifier(ast)).thenReturn(symbol);

        IReferencePhaseController controller = createController(symbolResolver);
        IVariableSymbol result = controller.resolveConstant(ast);

        assertThat(result, is(symbol));
    }

    @Test
    public void resolveConstant_SymbolResolverDoesNotFindSymbol_ReturnsErroneousVariableSymbol() {
        String aliasName = "Dummy";
        ITSPHPAst ast = createAst(aliasName);
        ISymbolFactory symbolFactory = mock(ISymbolFactory.class);
        IErroneousVariableSymbol erroneousVariableSymbol = mock(IErroneousVariableSymbol.class);
        when(symbolFactory.createErroneousVariableSymbol(eq(ast), any(TSPHPException.class)))
                .thenReturn(erroneousVariableSymbol);

        IReferencePhaseController controller = createController(symbolFactory);
        IVariableSymbol result = controller.resolveConstant(ast);

        verify(symbolFactory).createErroneousVariableSymbol(eq(ast), any(TSPHPException.class));
        assertThat(result, is((IVariableSymbol) erroneousVariableSymbol));
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
    public void resolveUserType_Standard_DelegatesToTypeSymbolResolver() {
        ITSPHPAst ast = createAst("Dummy");
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ITypeSymbolResolver typeSymbolResolver = mock(ITypeSymbolResolver.class);

        IReferencePhaseController controller = createController(typeSymbolResolver);
        controller.resolveUseType(ast, alias);

        verify(typeSymbolResolver).resolveTypeFor(ast);
    }

    @Test
    public void resolveUserType_TypeSymbolResolverFindsType_ReturnsType() {
        ITSPHPAst ast = createAst("Dummy");
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ITypeSymbolResolver typeSymbolResolver = mock(ITypeSymbolResolver.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(typeSymbolResolver.resolveTypeFor(ast)).thenReturn(typeSymbol);

        IReferencePhaseController controller = createController(typeSymbolResolver);
        ITypeSymbol result = controller.resolveUseType(ast, alias);

        assertThat(result, is(typeSymbol));
    }

    @Test
    public void resolveUserType_TypeSymbolResolverDoesNotFindType_ReturnsAliasTypeSymbol() {
        String aliasName = "Dummy";
        ITSPHPAst ast = createAst(aliasName);
        ITSPHPAst alias = mock(ITSPHPAst.class);
        ISymbolFactory symbolFactory = mock(ISymbolFactory.class);
        IAliasTypeSymbol aliasTypeSymbol = mock(IAliasTypeSymbol.class);
        when(symbolFactory.createAliasTypeSymbol(ast, aliasName)).thenReturn(aliasTypeSymbol);

        IReferencePhaseController controller = createController(symbolFactory);
        ITypeSymbol result = controller.resolveUseType(ast, alias);

        verify(symbolFactory).createAliasTypeSymbol(ast, aliasName);
        assertThat(result, is((ITypeSymbol) aliasTypeSymbol));
    }

    @Test
    public void
    addImplicitReturnStatementIfRequired_IsReturningAndHasAtLeastOneReturnOrThrow_NothingAddedIssueReporterNotCalled() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IInferenceErrorReporter issueReporter = mock(IInferenceErrorReporter.class);

        IReferencePhaseController controller = createController(issueReporter);
        controller.addImplicitReturnStatementIfRequired(true, true, identifier, block);

        verifyNoMoreInteractions(issueReporter);
        verifyNoMoreInteractions(identifier);
    }

    /**
     * Should never happen, but if isReturn is true then hasAtLeastOneReturnOrThrow should simply be ignored
     */
    @Test
    public void addImplicitReturnStatementIfRequired_IsReturningHasNoReturnOThrow_NothingAddedIssueReporterNotCalled() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IInferenceErrorReporter issueReporter = mock(IInferenceErrorReporter.class);

        IReferencePhaseController controller = createController(issueReporter);
        controller.addImplicitReturnStatementIfRequired(true, false, identifier, block);

        verifyNoMoreInteractions(issueReporter);
        verifyNoMoreInteractions(identifier);
    }

    /**
     * Should never happen, but if isReturn is true then hasAtLeastOneReturnOrThrow should simply be ignored
     */
    @Test
    public void
    addImplicitReturnStatementIfRequired_IsNotReturningAndHasAtLeastOneReturnOrThrow_ReturnAddedAndPartiallyCalled() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IInferenceErrorReporter issueReporter = mock(IInferenceErrorReporter.class);
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst returnAst = mock(ITSPHPAst.class);
        when(astModificationHelper.getNullReturnStatement()).thenReturn(returnAst);

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
        ITSPHPAst block = mock(ITSPHPAst.class);
        IInferenceErrorReporter issueReporter = mock(IInferenceErrorReporter.class);
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst returnAst = mock(ITSPHPAst.class);
        when(astModificationHelper.getNullReturnStatement()).thenReturn(returnAst);

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
    addImplicitReturnStatementIfRequired_IsNotReturningAndHasAtLeastOneReturnOrThrow_ReturnStatementHasScopeAndEvalType() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IScope scope = mock(IScope.class);
        when(block.getScope()).thenReturn(scope);
        IInferenceErrorReporter issueReporter = mock(IInferenceErrorReporter.class);
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst returnAst = mock(ITSPHPAst.class);
        when(astModificationHelper.getNullReturnStatement()).thenReturn(returnAst);


        IReferencePhaseController controller = createController(issueReporter, astModificationHelper);
        controller.addImplicitReturnStatementIfRequired(false, true, identifier, block);

        verify(returnAst).setScope(scope);
        verify(returnAst).setEvalType(any(ITypeSymbol.class));
    }

    /**
     * Should never happen, but if isReturn is true then hasAtLeastOneReturnOrThrow should simply be ignored
     */
    @Test
    public void
    addImplicitReturnStatementIfRequired_IsNotReturningAndHasNoReturnOrThrow_ReturnStatementHasScopeAndEvalType() {
        ITSPHPAst identifier = mock(ITSPHPAst.class);
        ITSPHPAst block = mock(ITSPHPAst.class);
        IScope scope = mock(IScope.class);
        when(block.getScope()).thenReturn(scope);
        IInferenceErrorReporter issueReporter = mock(IInferenceErrorReporter.class);
        IAstModificationHelper astModificationHelper = mock(IAstModificationHelper.class);
        ITSPHPAst returnAst = mock(ITSPHPAst.class);
        when(astModificationHelper.getNullReturnStatement()).thenReturn(returnAst);


        IReferencePhaseController controller = createController(issueReporter, astModificationHelper);
        controller.addImplicitReturnStatementIfRequired(false, false, identifier, block);

        verify(returnAst).setScope(scope);
        verify(returnAst).setEvalType(any(ITypeSymbol.class));
    }

    private ITSPHPAst createAst(String name) {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getText()).thenReturn(name);
        return ast;
    }

    private IReferencePhaseController createController() {
        return createController(mock(IInferenceErrorReporter.class));
    }

    private IReferencePhaseController createController(IInferenceErrorReporter inferenceErrorReporter) {
        return createController(inferenceErrorReporter, mock(IAstModificationHelper.class));
    }

    private IReferencePhaseController createController(ISymbolResolver symbolResolver) {
        return createController(
                mock(ISymbolFactory.class),
                mock(IInferenceErrorReporter.class),
                mock(IAstModificationHelper.class),
                symbolResolver,
                mock(ITypeSymbolResolver.class),
                mock(IScopeHelper.class),
                mock(ICore.class),
                mock(IModifierHelper.class),
                mock(IGlobalNamespaceScope.class)
        );
    }

    private IReferencePhaseController createController(ITypeSymbolResolver typeSymbolResolver) {
        return createController(
                mock(ISymbolFactory.class),
                mock(IInferenceErrorReporter.class),
                mock(IAstModificationHelper.class),
                mock(ISymbolResolver.class),
                typeSymbolResolver,
                mock(IScopeHelper.class),
                mock(ICore.class),
                mock(IModifierHelper.class),
                mock(IGlobalNamespaceScope.class)
        );
    }

    private IReferencePhaseController createController(ISymbolFactory symbolFactory) {
        return createController(
                symbolFactory,
                mock(IInferenceErrorReporter.class),
                mock(IAstModificationHelper.class),
                mock(ISymbolResolver.class),
                mock(ITypeSymbolResolver.class),
                mock(IScopeHelper.class),
                mock(ICore.class),
                mock(IModifierHelper.class),
                mock(IGlobalNamespaceScope.class)
        );
    }

    private IReferencePhaseController createController(
            IInferenceErrorReporter inferenceErrorReporter, IAstModificationHelper astModificationHelper) {
        return createController(
                mock(ISymbolFactory.class),
                inferenceErrorReporter,
                astModificationHelper,
                mock(ISymbolResolver.class),
                mock(ITypeSymbolResolver.class),
                mock(IScopeHelper.class),
                mock(ICore.class),
                mock(IModifierHelper.class),
                mock(IGlobalNamespaceScope.class)
        );
    }

    protected IReferencePhaseController createController(
            ISymbolFactory symbolFactory,
            IInferenceErrorReporter inferenceErrorReporter,
            IAstModificationHelper astModificationHelper,
            ISymbolResolver symbolResolver,
            ITypeSymbolResolver typeSymbolResolver,
            IScopeHelper scopeHelper,
            ICore core,
            IModifierHelper modifierHelper,
            IGlobalNamespaceScope globalDefaultNamespace) {
        return new ReferencePhaseController(
                symbolFactory,
                inferenceErrorReporter,
                astModificationHelper,
                symbolResolver,
                typeSymbolResolver,
                scopeHelper,
                core,
                modifierHelper,
                globalDefaultNamespace
        );
    }
}
