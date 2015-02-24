/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.IConstraint;
import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstraintSolver implements IConstraintSolver
{
    private ITypeSymbol nothingTypeSymbol;
    private ISymbolFactory symbolFactory;

    public ConstraintSolver(ITypeSymbol theNothingTypeSymbol, ISymbolFactory theSymbolFactory) {
        nothingTypeSymbol = theNothingTypeSymbol;
        symbolFactory = theSymbolFactory;
    }


    @Override
    public Map<String, IUnionTypeSymbol> resolveConstraints(IScope currentScope) {
        Map<String, IUnionTypeSymbol> types = new HashMap<>();
        for (Map.Entry<String, List<IConstraint>> constraintsEntry : currentScope.getConstraints().entrySet()) {
            ConstraintSolverDto dto = new ConstraintSolverDto(
                    currentScope,
                    constraintsEntry.getKey(),
                    constraintsEntry.getValue(),
                    symbolFactory.createUnionTypeSymbol(new HashMap<String, ITypeSymbol>()));

            IUnionTypeSymbol unionTypeSymbol = resolveConstraint(dto);
            unionTypeSymbol.seal();
            types.put(constraintsEntry.getKey(), unionTypeSymbol);
        }
        return types;
    }

    private class ConstraintSolverDto
    {
        public IScope currentScope;
        public String variableName;
        public List<IConstraint> constraints;
        public IUnionTypeSymbol unionTypeSymbol;

        public ConstraintSolverDto(
                IScope theCurrentScope,
                String theVariableName,
                List<IConstraint> theConstraints,
                IUnionTypeSymbol theUnionTypeSymbol) {
            currentScope = theCurrentScope;
            variableName = theVariableName;
            constraints = theConstraints;
            unionTypeSymbol = theUnionTypeSymbol;
        }
    }

    private IUnionTypeSymbol resolveConstraint(ConstraintSolverDto dto) {
        for (IConstraint constraint : dto.constraints) {
            if (constraint instanceof ParentInclusionConstraint) {
                dto.unionTypeSymbol.addTypeSymbol(((ParentInclusionConstraint) constraint).getSubType());
            } else if (constraint instanceof RefParentInclusionConstraint) {
                resolveReferenceConstraint(dto, (RefParentInclusionConstraint) constraint);
            }
        }
        return dto.unionTypeSymbol;
    }

    private void resolveReferenceConstraint(ConstraintSolverDto dto, RefParentInclusionConstraint refConstraint) {
        IScope scope = refConstraint.getRefScope();
        String refVariableId = refConstraint.getRefVariableName();
        if (dto.currentScope != scope || !dto.variableName.equals(refVariableId)) {
            List<IConstraint> refConstraints = scope.getConstraintsForVariable(refVariableId);
            ConstraintSolverDto newDto = new ConstraintSolverDto(
                    dto.currentScope,
                    dto.variableName,
                    refConstraints,
                    dto.unionTypeSymbol);

            resolveConstraint(newDto);
        }
    }

    /**
     * Return how many promotions have to be applied to the actualType to reach the formalType whereby -1 is returned in
     * the case where formalType is not the actualType or one of its parent types.
     */
    public int getPromotionLevelFromTo(ITypeSymbol actualParameterType, ITypeSymbol formalParameterType) {
        int count = 0;
        if (actualParameterType != formalParameterType) {
            count = -1;
            Set<ITypeSymbol> parentTypes = actualParameterType.getParentTypeSymbols();
            for (ITypeSymbol parentType : parentTypes) {
                if (parentType != null) {
                    int tmp = getPromotionLevelFromTo(parentType, formalParameterType);
                    if (tmp != -1) {
                        count += tmp + 2;
                        break;
                    }
                }
            }
        }
        return count;
    }


    public boolean isSameOrSubType(int promotionLevel) {
        return promotionLevel != -1;
    }
}
