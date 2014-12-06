/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.error;

import ch.tsphp.common.IErrorLogger;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.exceptions.DefinitionException;
import ch.tsphp.common.exceptions.ReferenceException;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.common.symbols.ISymbol;

import java.util.ArrayDeque;
import java.util.Collection;

public class InferenceErrorReporter implements IInferenceErrorReporter
{
    private final Collection<IErrorLogger> errorLoggers = new ArrayDeque<>();
    private boolean hasFoundError;

    @Override
    public boolean hasFoundError() {
        return hasFoundError;
    }

    @Override
    public void registerErrorLogger(IErrorLogger errorLogger) {
        errorLoggers.add(errorLogger);
    }

    @Override
    public void reset() {
        hasFoundError = false;
    }

    private void reportError(TSPHPException exception) {
        hasFoundError = true;
        for (IErrorLogger logger : errorLoggers) {
            logger.log(exception);
        }
    }

    @Override
    public DefinitionException determineAlreadyDefined(ISymbol symbol1, ISymbol symbol2) {
        return symbol1.getDefinitionAst().isDefinedEarlierThan(symbol2.getDefinitionAst())
                ? alreadyDefined(symbol1, symbol2)
                : alreadyDefined(symbol2, symbol1);
    }

    @Override
    public DefinitionException alreadyDefined(ISymbol existingSymbol, ISymbol newSymbol) {
        //TODO rstoll TINS-174 inference engine and error reporting
        DefinitionException ex = new DefinitionException(
                "alreadyDefined", existingSymbol.getDefinitionAst(), newSymbol.getDefinitionAst());
        reportError(ex);
        return ex;
    }

    @Override
    public DefinitionException aliasForwardReference(ITSPHPAst typeAst, ITSPHPAst useDefinition) {
        //TODO rstoll TINS-174 inference engine and error reporting
        DefinitionException ex = new DefinitionException("aliasForwardReference", typeAst, useDefinition);
        reportError(ex);
        return ex;
    }

    @Override
    public DefinitionException forwardReference(ITSPHPAst definitionAst, ITSPHPAst identifier) {
        //TODO rstoll TINS-174 inference engine and error reporting
        DefinitionException ex = new DefinitionException("forwardReference", definitionAst, identifier);
        reportError(ex);
        return ex;
    }

    @Override
    public DefinitionException variablePartiallyInitialised(ITSPHPAst definitionAst, ITSPHPAst variableId) {
        //TODO rstoll TINS-174 inference engine and error reporting
        DefinitionException ex = new DefinitionException("variablePartiallyInitialised", definitionAst, variableId);
        reportError(ex);
        return ex;
    }

    @Override
    public DefinitionException variableNotInitialised(ITSPHPAst definitionAst, ITSPHPAst variableId) {
        //TODO rstoll TINS-174 inference engine and error reporting
        DefinitionException ex = new DefinitionException("variableNotInitialised", definitionAst, variableId);
        reportError(ex);
        return ex;
    }

    @Override
    public void partialReturnFromFunction(ITSPHPAst identifier) {
        //TODO rstoll TINS-174 inference engine and error reporting
        reportError(new ReferenceException("partialReturnFromFunction", identifier));
    }

    @Override
    public void noReturnFromFunction(ITSPHPAst identifier) {
        //TODO rstoll TINS-174 inference engine and error reporting
        reportError(new ReferenceException("noReturnFromFunction", identifier));
    }

    @Override
    public ReferenceException notDefined(ITSPHPAst identifier) {
        //TODO rstoll TINS-174 inference engine and error reporting
        ReferenceException ex = new ReferenceException("notDefined", identifier);
        reportError(ex);
        return ex;
    }

    @Override
    public ReferenceException unknownType(ITSPHPAst typeAst) {
        //TODO rstoll TINS-174 inference engine and error reporting
        ReferenceException ex = new ReferenceException("unknownType", typeAst);
        reportError(ex);
        return ex;
    }
}
