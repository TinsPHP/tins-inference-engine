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

    //    public IScope currentScope;
//    public String variableId;
    public Set<String> visitedVariables;
    public List<IConstraint> constraints;
    public IUnionTypeSymbol unionTypeSymbol;

    public ConstraintSolverDto(
//            IScope theCurrentScope,
//            String theVariableId,
            Set<String> theVisitedVariables,
            List<IConstraint> theConstraints,
            IUnionTypeSymbol theUnionTypeSymbol) {
//        currentScope = theCurrentScope;
//        variableId = theVariableId;
        visitedVariables = theVisitedVariables;
        constraints = theConstraints;
        unionTypeSymbol = theUnionTypeSymbol;
    }

}
