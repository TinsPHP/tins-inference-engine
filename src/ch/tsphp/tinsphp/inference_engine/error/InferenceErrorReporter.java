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
    public DefinitionException alreadyDefined(ISymbol existingSymbol, ISymbol newSymbol) {
        //TODO rstoll TINS-174 inference engine and error reporting
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public DefinitionException definedInOuterScope(ISymbol firstDefinition, ISymbol symbolToCheck) {
        //TODO rstoll TINS-174 inference engine and error reporting
        throw new UnsupportedOperationException("not yet implemented");
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
}
