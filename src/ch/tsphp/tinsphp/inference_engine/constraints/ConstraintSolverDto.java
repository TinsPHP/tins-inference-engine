/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;

import java.util.Deque;
import java.util.Map;
import java.util.Set;

public class ConstraintSolverDto
{

    public ITypeVariableSymbol currentTypeVariable;
    public Map<String, Integer> visitedTypeVariables;
    public Set<String> revisitTypeVariables;
    public IUnionTypeSymbol unionTypeSymbol;
    public boolean notInIterativeMode = true;
    public boolean hasUnionChanged;
    public boolean hasNotCircularReference = true;
    public ITypeVariableSymbol circularRefTypeVariable;
    public Deque<ITypeVariableSymbol> typeVariables;

    public ConstraintSolverDto(
            ConstraintSolverDto dto,
            ITypeVariableSymbol theCurrentTypeVariable,
            IUnionTypeSymbol theUnionTypeSymbol) {
        this(
                dto.typeVariables,
                dto.visitedTypeVariables,
                dto.revisitTypeVariables,
                theCurrentTypeVariable,
                theUnionTypeSymbol
        );
        notInIterativeMode = dto.notInIterativeMode;
        hasUnionChanged = dto.hasUnionChanged;
        circularRefTypeVariable = dto.circularRefTypeVariable;
    }

    public ConstraintSolverDto(
            Deque<ITypeVariableSymbol> theTypeVariables,
            Map<String, Integer> theVisitedTypeVariables,
            Set<String> theTypeVariablesToRevisit,
            ITypeVariableSymbol theCurrentTypeVariable,
            IUnionTypeSymbol theUnionTypeSymbol) {
        typeVariables = theTypeVariables;
        currentTypeVariable = theCurrentTypeVariable;
        visitedTypeVariables = theVisitedTypeVariables;
        revisitTypeVariables = theTypeVariablesToRevisit;
        unionTypeSymbol = theUnionTypeSymbol;
    }
}
