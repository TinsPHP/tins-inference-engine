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
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.TinsPHPConstants;
import ch.tsphp.tinsphp.common.gen.TokenTypes;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.issues.DefinitionIssueDto;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.issues.IIssueLogger;
import ch.tsphp.tinsphp.common.issues.IIssueMessageProvider;
import ch.tsphp.tinsphp.common.issues.IssueReporterHelper;
import ch.tsphp.tinsphp.common.issues.ReferenceIssueDto;
import ch.tsphp.tinsphp.common.issues.WrongArgumentTypeIssueDto;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.translation.dtos.OverloadDto;
import ch.tsphp.tinsphp.common.translation.dtos.ParameterDto;
import ch.tsphp.tinsphp.common.translation.dtos.TypeDto;
import ch.tsphp.tinsphp.common.translation.dtos.TypeParameterDto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InferenceIssueReporter implements IInferenceIssueReporter
{
    private final IIssueMessageProvider messageProvider;

    private final Collection<IIssueLogger> issueLoggers = new ArrayDeque<>();
    private EnumSet<EIssueSeverity> foundIssues = EnumSet.noneOf(EIssueSeverity.class);


    public InferenceIssueReporter(IIssueMessageProvider theMessageProvider) {
        messageProvider = theMessageProvider;
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

        String errorMessage = messageProvider.getDefinitionIssueMessage(
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

        String errorMessage = messageProvider.getReferenceIssueMessage(
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

    @Override
    public ReferenceException constraintViolation(IOverloadBindings bindings, IConstraint constraint) {
        ReferenceException exception;
        ITSPHPAst operator = constraint.getOperator();
        switch (operator.getType()) {
            case TokenTypes.FUNCTION_CALL:
                exception = addAndGetWrongArgumentTypeException(
                        "wrongFunctionCall", EIssueSeverity.Error, bindings, constraint, operator.getChild(0));
                break;
            default:
                exception = addAndGetWrongArgumentTypeException(
                        "wrongOperatorUsage", EIssueSeverity.Error, bindings, constraint, operator);
        }
        return exception;
    }

    private ReferenceException addAndGetWrongArgumentTypeException(
            String key, EIssueSeverity severity,
            IOverloadBindings bindings, IConstraint constraint, ITSPHPAst identifier) {

        List<IVariable> arguments = constraint.getArguments();
        int numberOfArguments = arguments.size();
        String[] actualParameterTypes = new String[numberOfArguments];
        for (int i = 0; i < numberOfArguments; ++i) {
            ITypeVariableReference reference = bindings.getTypeVariableReference(arguments.get(i).getAbsoluteName());
            String typeVariable = reference.getTypeVariable();
            actualParameterTypes[i] = bindings.getLowerTypeBounds(typeVariable).getAbsoluteName();
        }

        return addAndGetWrongArgumentTypeException(key, severity, constraint, identifier, actualParameterTypes);
    }

    private ReferenceException addAndGetWrongArgumentTypeException(
            String key, EIssueSeverity severity, IConstraint constraint, ITSPHPAst identifier,
            String[] actualParameterTypes) {

        List<OverloadDto> existingOverloads = new ArrayList<>();
        IMinimalMethodSymbol methodSymbol = constraint.getMethodSymbol();
        for (IFunctionType overload : methodSymbol.getOverloads()) {
            OverloadDto overloadDto = createMethodDto(methodSymbol.getName(), overload);
            existingOverloads.add(overloadDto);
        }

        WrongArgumentTypeIssueDto issueDto = new WrongArgumentTypeIssueDto(
                identifier.getText(), identifier.getLine(), identifier.getCharPositionInLine(),
                actualParameterTypes, existingOverloads);

        String issueMessage = messageProvider.getWrongArgumentTypeIssueMessage(key, issueDto);
        ReferenceException exception = new ReferenceException(issueMessage, identifier);
        reportIssue(exception, severity);
        return exception;
    }

    private OverloadDto createMethodDto(String name, IFunctionType overload) {
        IOverloadBindings bindings = overload.getOverloadBindings();
        List<IVariable> parameters = overload.getParameters();
        int numberOfParameters = parameters.size();

        Set<String> typeVariablesAdded = new HashSet<>(numberOfParameters + 1);
        List<TypeParameterDto> typeParameters = new ArrayList<>(numberOfParameters + 1);
        TypeDto returnType = createTypeDto(
                TinsPHPConstants.RETURN_VARIABLE_NAME, bindings, typeParameters, typeVariablesAdded);

        List<ParameterDto> parameterDtos = new ArrayList<>();
        for (IVariable parameter : parameters) {
            parameterDtos.add(new ParameterDto(
                    createTypeDto(parameter.getAbsoluteName(), bindings, typeParameters, typeVariablesAdded),
                    parameter.getName(),
                    null,
                    null,
                    null
            ));
        }

        if (typeParameters.isEmpty()) {
            typeParameters = null;
        }
        return new OverloadDto(returnType, name, typeParameters, parameterDtos, null);
    }

    private TypeDto createTypeDto(
            String variableId,
            IOverloadBindings bindings,
            List<TypeParameterDto> typeParameters,
            Set<String> typeVariablesAdded) {

        ITypeVariableReference reference = bindings.getTypeVariableReference(variableId);
        TypeDto typeDto = createTypeDto(reference, bindings);
        if (!reference.hasFixedType()) {
            String typeVariable = typeDto.type;
            if (!typeVariablesAdded.contains(typeVariable)) {
                typeVariablesAdded.add(typeVariable);
                List<String> lowerBounds = null;
                if (bindings.hasLowerBounds(typeVariable)) {
                    lowerBounds = new ArrayList<>();
                    if (bindings.hasLowerTypeBounds(typeVariable)) {
                        lowerBounds.addAll(bindings.getLowerTypeBounds(typeVariable).getTypeSymbols().keySet());
                    }
                    if (bindings.hasLowerRefBounds(typeVariable)) {
                        lowerBounds.addAll(bindings.getLowerRefBounds(typeVariable));
                    }
                }
                List<String> upperBounds = null;
                if (bindings.hasUpperTypeBounds(typeVariable)) {
                    upperBounds = new ArrayList<>();
                    upperBounds.addAll(bindings.getUpperTypeBounds(typeVariable).getTypeSymbols().keySet());
                }
                typeParameters.add(new TypeParameterDto(lowerBounds, typeVariable, upperBounds));
            }
        }
        return typeDto;
    }

    private TypeDto createTypeDto(ITypeVariableReference reference, IOverloadBindings bindings) {
        String type;

        String typeVariable = reference.getTypeVariable();
        if (reference.hasFixedType()) {
            ITypeSymbol typeSymbol;
            if (bindings.hasUpperTypeBounds(typeVariable)) {
                typeSymbol = bindings.getUpperTypeBounds(typeVariable);
            } else {
                typeSymbol = bindings.getLowerTypeBounds(typeVariable);
            }
            type = typeSymbol.toString();
        } else {
            type = typeVariable;
        }

        return new TypeDto(null, type, null);
    }
}
