/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.IConstraint;
import ch.tsphp.common.symbols.IUnionTypeSymbol;

import java.util.List;
import java.util.Set;

public class ConstraintSolverDto
{

    public ScopeVariableDto startVariable;
    public ScopeVariableDto currentVariable;
    public Set<String> visitedVariables;
    public Set<String> revisitVariables;
    public List<IConstraint> constraints;
    public IUnionTypeSymbol unionTypeSymbol;
    public boolean notInIterativeMode = true;
    public boolean hasUnionChanged;
    public boolean hasNotCircularReference = true;
    public ScopeVariableDto circularRefVariable;

    public ConstraintSolverDto(
            ConstraintSolverDto dto,
            ScopeVariableDto theCurrentVariable,
            List<IConstraint> theConstraints,
            IUnionTypeSymbol theUnionTypeSymbol) {
        this(
                dto.startVariable,
                dto.visitedVariables,
                dto.revisitVariables,
                theCurrentVariable,
                theConstraints,
                theUnionTypeSymbol
        );
        notInIterativeMode = dto.notInIterativeMode;
        hasUnionChanged = dto.hasUnionChanged;
    }

    public ConstraintSolverDto(
            ScopeVariableDto theStartVariable,
            Set<String> theVisitedVariables,
            Set<String> theVariablesToRevisit,
            ScopeVariableDto theCurrentVariable,
            List<IConstraint> theConstraints,
            IUnionTypeSymbol theUnionTypeSymbol) {
        startVariable = theStartVariable;
        currentVariable = theCurrentVariable;
        visitedVariables = theVisitedVariables;
        revisitVariables = theVariablesToRevisit;
        constraints = theConstraints;
        unionTypeSymbol = theUnionTypeSymbol;
    }
}
