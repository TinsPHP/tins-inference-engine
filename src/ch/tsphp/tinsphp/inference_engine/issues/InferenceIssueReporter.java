/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.issues;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.exceptions.DefinitionException;
import ch.tsphp.common.exceptions.ReferenceException;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.issues.DefinitionIssueDto;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.issues.IIssueLogger;
import ch.tsphp.tinsphp.common.issues.IIssueMessageProvider;
import ch.tsphp.tinsphp.common.issues.IssueReporterHelper;
import ch.tsphp.tinsphp.common.issues.ReferenceIssueDto;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumSet;

public class InferenceIssueReporter implements IInferenceIssueReporter
{
    private final IIssueMessageProvider messageProvider;

    private final Collection<IIssueLogger> issueLoggers = new ArrayDeque<>();
    private EnumSet<EIssueSeverity> foundIssues = EnumSet.noneOf(EIssueSeverity.class);


    public InferenceIssueReporter(IIssueMessageProvider theMessageProvier) {
        messageProvider = theMessageProvier;
    }

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

    @Override
    public DefinitionException determineAlreadyDefined(ISymbol symbol1, ISymbol symbol2) {
        return symbol1.getDefinitionAst().isDefinedEarlierThan(symbol2.getDefinitionAst())
                ? alreadyDefined(symbol1, symbol2)
                : alreadyDefined(symbol2, symbol1);
    }

    @Override
    public DefinitionException alreadyDefined(ISymbol existingSymbol, ISymbol newSymbol) {
        return addAndGetDefinitionException("alreadyDefined", EIssueSeverity.FatalError,
                existingSymbol.getDefinitionAst(), newSymbol.getDefinitionAst());
    }

    @Override
    public DefinitionException aliasForwardReference(ITSPHPAst typeAst, ITSPHPAst useDefinition) {
        return addAndGetDefinitionException("aliasForwardReference", EIssueSeverity.Error, typeAst, useDefinition);
    }

    @Override
    public DefinitionException forwardReference(ITSPHPAst definitionAst, ITSPHPAst identifier) {
        return addAndGetDefinitionException("forwardReference", EIssueSeverity.Error, definitionAst, identifier);
    }

    @Override
    public DefinitionException variablePartiallyInitialised(ITSPHPAst definitionAst, ITSPHPAst variableId) {
        return addAndGetDefinitionException(
                "variablePartiallyInitialised", EIssueSeverity.Error, definitionAst, variableId);
    }

    @Override
    public DefinitionException variableNotInitialised(ITSPHPAst definitionAst, ITSPHPAst variableId) {
        return addAndGetDefinitionException("variableNotInitialised", EIssueSeverity.Error, definitionAst, variableId);
    }

    private DefinitionException addAndGetDefinitionException(String identifier, EIssueSeverity severity,
            ITSPHPAst existingDefinition, ITSPHPAst newDefinition) {

        String errorMessage = messageProvider.getDefinitionErrorMessage(
                identifier,
                new DefinitionIssueDto(
                        existingDefinition.getText(),
                        existingDefinition.getLine(),
                        existingDefinition.getCharPositionInLine(),
                        newDefinition.getText(),
                        newDefinition.getLine(),
                        newDefinition.getCharPositionInLine())
        );

        DefinitionException exception = new DefinitionException(errorMessage, existingDefinition, newDefinition);
        reportIssue(exception, severity);
        return exception;
    }

    private void reportIssue(TSPHPException exception, EIssueSeverity severity) {
        foundIssues.add(severity);
        for (IIssueLogger logger : issueLoggers) {
            logger.log(exception, severity);
        }
    }

    @Override
    public ReferenceException notDefined(ITSPHPAst ast) {
        return addAndGetReferenceException("notDefined", EIssueSeverity.FatalError, ast);
    }

    @Override
    public ReferenceException unknownType(ITSPHPAst typeAst) {
        return addAndGetReferenceException("unknownType", EIssueSeverity.FatalError, typeAst);
    }

    @Override
    public void partialReturnFromFunction(ITSPHPAst identifier) {
        addAndGetReferenceException("partialReturnFromFunction", EIssueSeverity.Warning, identifier);
    }

    @Override
    public void noReturnFromFunction(ITSPHPAst identifier) {
        addAndGetReferenceException("noReturnFromFunction", EIssueSeverity.Notice, identifier);
    }

    private ReferenceException addAndGetReferenceException(
            String identifier, EIssueSeverity severity, ITSPHPAst definition) {

        String errorMessage = messageProvider.getReferenceErrorMessage(
                identifier,
                new ReferenceIssueDto(
                        definition.getText(),
                        definition.getLine(),
                        definition.getCharPositionInLine())
        );

        ReferenceException exception = new ReferenceException(errorMessage, definition);
        reportIssue(exception, severity);
        return exception;
    }
}
