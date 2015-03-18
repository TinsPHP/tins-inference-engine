/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.AConstraintSolverTest;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

public class TypeConstraintSolverTest extends AConstraintSolverTest
{

    @Test
    public void solveConstraintsOfScope_Int_UnionContainsOnlyInt() {
        ITypeVariableSymbol $a = typeVar("$a", type(intType));
        ITypeVariableCollection scope = createTypeVariableCollection($a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
    }

    @Test
    public void solveConstraintsOfScope_IntAndFloat_UnionContainsIntAndFloat() {
        ITypeVariableSymbol $a = typeVar("$a", type(intType), type(floatType));
        ITypeVariableCollection scope = createTypeVariableCollection($a);


        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float"));
    }

    @Test
    public void solveConstraintsOfScope_IntAndNum_UnionContainsOnlyNum() {
        ITypeVariableSymbol $a = typeVar("$a", type(intType), type(numType));
        ITypeVariableCollection scope = createTypeVariableCollection($a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
    }

    @Test
    public void solveConstraintsOfScope_NumAndIntAndScalar_UnionContainsOnlyScalar() {
        ITypeVariableSymbol $a = typeVar("$a", type(numType), type(intType), type(scalarType));
        ITypeVariableCollection scope = createTypeVariableCollection($a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("scalar"));
    }
}
