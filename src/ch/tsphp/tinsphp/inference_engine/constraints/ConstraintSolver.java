/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;


import ch.tsphp.common.IConstraint;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstraintSolver implements IConstraintSolver
{
    private ITypeSymbol nothingTypeSymbol;

    public ConstraintSolver(ITypeSymbol theNothingTypeSymbol) {
        nothingTypeSymbol = theNothingTypeSymbol;
    }

    @Override
    public Map<String, List<IConstraint>> simplify(String variable, Map<String, List<IConstraint>> constraints) {
        if (!constraints.containsKey(variable)) {
            throw new IllegalArgumentException("the given variable does not exist in the given constraints");
        }

        List<IConstraint> optimised = new ArrayList<>();
        List<IConstraint> old = constraints.get(variable);

        ITypeSymbol mostPreciseType = nothingTypeSymbol;
        for (IConstraint constraint : old) {
            if (constraint instanceof SubInclusionConstraint) {
                SubInclusionConstraint subInclusionConstraint = (SubInclusionConstraint) constraint;
                if (mostPreciseType != nothingTypeSymbol) {

                } else {
//                    mostPreciseType = inclusionConstraintDto.
                }
            }
        }

        return constraints;
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
