/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.error;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.exceptions.DefinitionException;
import ch.tsphp.common.exceptions.ReferenceException;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.issues.IIssueLogger;
import ch.tsphp.tinsphp.common.issues.IssueReporterHelper;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumSet;

public class InferenceIssueReporter implements IInferenceIssueReporter
{
    private final Collection<IIssueLogger> issueLoggers = new ArrayDeque<>();
    private EnumSet<EIssueSeverity> foundIssues = EnumSet.noneOf(EIssueSeverity.class);

    @Override
    public void registerIssueLogger(IIssueLogger issueLogger) {
        issueLoggers.add(issueLogger);
    }

    @Override
    public boolean hasFound(EnumSet<EIssueSeverity> severities) {
        return IssueReporterHelper.hasFound(foundIssues, severities);
    }

    @Override
    public void reset() {
        foundIssues = EnumSet.noneOf(EIssueSeverity.class);
    }

    private void reportError(TSPHPException exception, EIssueSeverity severity) {
        foundIssues.add(severity);
        for (IIssueLogger logger : issueLoggers) {
            logger.log(exception, severity);
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
        reportError(ex, EIssueSeverity.FatalError);
        return ex;
    }

    @Override
    public DefinitionException aliasForwardReference(ITSPHPAst typeAst, ITSPHPAst useDefinition) {
        //TODO rstoll TINS-174 inference engine and error reporting
        DefinitionException ex = new DefinitionException("aliasForwardReference", typeAst, useDefinition);
        reportError(ex, EIssueSeverity.Error);
        return ex;
    }

    @Override
    public DefinitionException forwardReference(ITSPHPAst definitionAst, ITSPHPAst identifier) {
        //TODO rstoll TINS-174 inference engine and error reporting
        DefinitionException ex = new DefinitionException("forwardReference", definitionAst, identifier);
        reportError(ex, EIssueSeverity.Error);
        return ex;
    }

    @Override
    public DefinitionException variablePartiallyInitialised(ITSPHPAst definitionAst, ITSPHPAst variableId) {
        //TODO rstoll TINS-174 inference engine and error reporting
        DefinitionException ex = new DefinitionException("variablePartiallyInitialised", definitionAst, variableId);
        reportError(ex, EIssueSeverity.Error);
        return ex;
    }

    @Override
    public DefinitionException variableNotInitialised(ITSPHPAst definitionAst, ITSPHPAst variableId) {
        //TODO rstoll TINS-174 inference engine and error reporting
        DefinitionException ex = new DefinitionException("variableNotInitialised", definitionAst, variableId);
        reportError(ex, EIssueSeverity.Error);
        return ex;
    }

    @Override
    public void partialReturnFromFunction(ITSPHPAst identifier) {
        //TODO rstoll TINS-174 inference engine and error reporting
        ReferenceException ex = new ReferenceException("partialReturnFromFunction", identifier);
        reportError(ex, EIssueSeverity.Warning);
    }

    @Override
    public void noReturnFromFunction(ITSPHPAst identifier) {
        //TODO rstoll TINS-174 inference engine and error reporting
        ReferenceException ex = new ReferenceException("noReturnFromFunction", identifier);
        reportError(ex, EIssueSeverity.Notice);
    }

    @Override
    public ReferenceException notDefined(ITSPHPAst identifier) {
        //TODO rstoll TINS-174 inference engine and error reporting
        ReferenceException ex = new ReferenceException("notDefined", identifier);
        reportError(ex, EIssueSeverity.FatalError);
        return ex;
    }

    @Override
    public ReferenceException unknownType(ITSPHPAst typeAst) {
        //TODO rstoll TINS-174 inference engine and error reporting
        ReferenceException ex = new ReferenceException("unknownType", typeAst);
        reportError(ex, EIssueSeverity.FatalError);
        return ex;
    }
}
