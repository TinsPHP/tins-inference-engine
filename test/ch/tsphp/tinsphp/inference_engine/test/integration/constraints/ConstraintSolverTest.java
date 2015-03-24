/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.constraints;


import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.symbols.IFunctionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbolWithRef;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.AConstraintSolverTest;
import ch.tsphp.tinsphp.symbols.PolymorphicFunctionTypeSymbol;
import ch.tsphp.tinsphp.symbols.constraints.TypeConstraint;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConstraintSolverTest extends AConstraintSolverTest
{

    @Test
    public void solveConstraints_RefConstraint_UnionContainsTypesOfRef() {
        //corresponds:
        //predefined $b = 1 V 1.2; V [];
        // ? $a = $b;

        ITypeVariableSymbol $b = typeVar("$b", intType, floatType, arrayType);
        ITypeVariableSymbol $a = typeVar("$a", $b);
        ITypeVariableCollection scope = createTypeVariableCollection($a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        ArgumentCaptor<ITypeSymbol> captor = ArgumentCaptor.forClass(ITypeSymbol.class);
        verify($a).setType(captor.capture());
        IUnionTypeSymbol unionTypeSymbol = (IUnionTypeSymbol) captor.getValue();
        assertThat(unionTypeSymbol.isReadyForEval(), is(true));
        assertThat(unionTypeSymbol.getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
    }

//    @Test
//    public void solveConstraints_IntersectionConstraint_UnionContainsTypesOfRef() {
//        //corresponds (= is a function as well with by-ref semantic):
//        //predefined $b = 1 V 1.2; V [];
//        // $a = $b;
//
//        IOverloadResolver overloadResolver = new OverloadResolver();
//        ISymbolFactory symbolFactory = createSymbolFactory(overloadResolver);
//        IConstraintSolver solver = createConstraintSolver(symbolFactory, overloadResolver);
//
//        ITypeVariableSymbol $a = typeVar("$a");
//        ITypeVariableSymbol $b = typeVar("$b", intType, floatType, arrayType);
//        new IntersectionConstraint(asList($a, $b), getAssignmentOverloads(symbolFactory, solver));
//        ITypeVariableSymbol e1 = typeVar("@e1", );
//        ITypeVariableCollection scope = createTypeVariableCollection(e1);
//
//
//        solver.solveConstraints(scope);
//
//        assertThat($a.getType().isReadyForEval(), is(true));
//        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
//    }

    public List<IFunctionTypeSymbol> getAssignmentOverloads(ISymbolFactory symbolFactory, IConstraintSolver solver) {
        Deque<ITypeVariableSymbol> typeVariables = new ArrayDeque<>();
        ITypeVariableSymbolWithRef lhs = typeVarWithRef("$lhs");
        ITypeVariableSymbolWithRef rhs = typeVarWithRef("$rhs");
        ITypeVariableSymbolWithRef rtn = typeVarWithRef("return");
        when(lhs.getConstraint()).thenReturn(rhs);
        when(rtn.getConstraint()).thenReturn(lhs);
        typeVariables.add(lhs);
        typeVariables.add(rtn);
        IFunctionTypeSymbol function = new PolymorphicFunctionTypeSymbol(
                "=",
                Arrays.asList(lhs, rhs),
                mixedType,
                rtn,
                typeVariables,
                symbolFactory,
                solver);
        function.addInputConstraint("$lhs", new TypeConstraint(mixedType));
        function.addInputConstraint("$rhs", new TypeConstraint(mixedType));
        function.addOutputConstraint("$lhs", rhs);

        List<IFunctionTypeSymbol> list = new ArrayList<>(1);
        list.add(function);
        return list;
    }
}
