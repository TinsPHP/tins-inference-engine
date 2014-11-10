/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ISymbolResolver;
import ch.tsphp.tinsphp.common.symbols.ITypeSymbolResolver;
import ch.tsphp.tinsphp.inference_engine.IReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.ReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ReferencePhaseControllerTest
{
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


    private IReferencePhaseController createController(IInferenceErrorReporter inferenceErrorReporter) {
        return createController(inferenceErrorReporter, mock(IAstModificationHelper.class));
    }

    private IReferencePhaseController createController(
            IInferenceErrorReporter inferenceErrorReporter, IAstModificationHelper astModificationHelper) {
        return createController(
                mock(ISymbolFactory.class),
                inferenceErrorReporter,
                astModificationHelper,
                mock(ISymbolResolver.class),
                mock(ITypeSymbolResolver.class),
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
            ICore core,
            IModifierHelper modifierHelper,
            IGlobalNamespaceScope globalDefaultNamespace) {
        return new ReferencePhaseController(
                symbolFactory,
                inferenceErrorReporter,
                astModificationHelper,
                symbolResolver,
                typeSymbolResolver,
                core,
                modifierHelper,
                globalDefaultNamespace
        );

    }
}
