/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.constraints;


import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.test.ActWithTimeout;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.AConstraintSolverTest;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class RefConstraintSolverTest extends AConstraintSolverTest
{

    @Test
    public void solveConstraintsOfScope_InSameScope_UnionContainsAllTypesOfRefAndOwn() {
        // corresponds:
        // $b = 1; $b = 1.2; $b = []; $a = 1;
        // $a = $b;

        ITypeVariableSymbol $b = typeVar("$b", type(intType), type(fooType), type(arrayType));
        ITypeVariableSymbol $a = typeVar("$a", $b, type(intType));
        ITypeVariableCollection scope = createTypeVariableCollection($b, $a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
    }

    @Test
    public void solveConstraintsOfScope_InSameScope_RefIsAlsoSolved() {
        // corresponds:
        // $b = 1; $b = 1.2; $b = []; $a = 1;
        // $a = $b;

        ITypeVariableSymbol $b = typeVar("$b", type(intType), type(fooType), type(arrayType));
        ITypeVariableSymbol $a = typeVar("$a", $b, type(intType));
        ITypeVariableCollection scope = createTypeVariableCollection($b, $a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
    }

    @Test
    public void solveConstraintsOfScope_InOtherScope_UnionContainsAllTypesOfRefAndOwn() {
        // corresponds:
        // $b = 1; $b = 1.2; $b = [];
        // --- in different scope
        // $a = 1;
        // $a = $b;

        ITypeVariableSymbol $b = typeVar("$b", type(intType), type(fooType), type(arrayType));
        ITypeVariableSymbol $a = typeVar("$a", $b, type(intType), type(floatType));
        ITypeVariableCollection scope = createTypeVariableCollection($a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "Foo", "array"));
    }

    @Test
    public void solveConstraintsOfScope_InOtherScope_RefIsSolvedAsWell() {
        // corresponds:
        // $b = 1; $b = 1.2; $b = [];
        // --- in different scope
        // $a = 1;
        // $a = $b;

        ITypeVariableSymbol $b = typeVar("$b", type(intType), type(fooType), type(arrayType));
        ITypeVariableSymbol $a = typeVar("$a", $b, type(intType), type(floatType));
        ITypeVariableCollection scope = createTypeVariableCollection($a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
    }

    @Test
    public void solveConstraintsOfScope_CircleInOwnScope_UnionContainsAllTypesOfRefAndTerminates()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $b = 1; $b = 1.2; $b = []; $a = 1;
        // $b = $a;
        // $a = $b;

        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $a = typeVar("$a");
        when($b.getConstraints()).thenReturn(list(type(intType), type(fooType), type(arrayType), $a));
        when($a.getConstraints()).thenReturn(list($b, type(intType)));
        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraints(scope);
                    return null;
                }
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat($a.getType().isReadyForEval(), is(true));
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_CircleViaOtherScope_UnionContainsAllTypesOfRefAndTerminates()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $b = 1; $b = 1.2; $b = [];
        // $b = $a;
        // ---- in different scope
        // $a = 1;
        // $a = $b;

        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $a = typeVar("$a");
        when($b.getConstraints()).thenReturn(list(type(intType), type(fooType), type(arrayType), $a));
        when($a.getConstraints()).thenReturn(list($b, type(intType)));
        final ITypeVariableCollection scope = createTypeVariableCollection($a);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraints(scope);
                    return null;
                }
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat($a.getType().isReadyForEval(), is(true));
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_CircleViaOtherScope_RefIsNotSolved()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $b = 1; $b = 1.2; $b = [];
        // $b = $a;
        // ---- in different scope
        // $a = 1;
        // $a = $b;

        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $a = typeVar("$a");
        when($b.getConstraints()).thenReturn(list(type(intType), type(fooType), type(arrayType), $a));
        when($a.getConstraints()).thenReturn(list($b, type(intType)));
        final ITypeVariableCollection scope = createTypeVariableCollection($a);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraints(scope);
                    return null;
                }
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat($b.getType().isReadyForEval(), is(false));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_CircleOverThree_UnionContainsAllTypesAndTerminates()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = []; $b = 1; $c = 1.5;
        // $a = $b;
        // $c = $a;
        // $b = $c;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        when($a.getConstraints()).thenReturn(list($b, type(arrayType)));
        when($b.getConstraints()).thenReturn(list($c, type(intType)));
        when($c.getConstraints()).thenReturn(list($a, type(floatType)));
        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraints(scope);
                    return null;
                }
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat($a.getType().isReadyForEval(), is(true));
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat($c.getType().isReadyForEval(), is(true));
            assertThat($c.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_CircleInRef_UnionContainsAllTypesOfRefAndTerminates()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = []; $b = 1; $c = 1.5;
        // $a = $b;
        // $b = $c;
        // $c = $b;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        when($a.getConstraints()).thenReturn(list($b, type(arrayType)));
        when($b.getConstraints()).thenReturn(list($c, type(intType)));
        when($c.getConstraints()).thenReturn(list($b, type(floatType)));
        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraints(scope);
                    return null;
                }
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat($a.getType().isReadyForEval(), is(true));
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float"));
            assertThat($c.getType().isReadyForEval(), is(true));
            assertThat($c.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_MultipleCirclesAlsoInRef_UnionContainsAllTypesOfRefAndTerminates()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = []; $b = 1; $c = 1.5;
        // $a = $b;
        // $b = $c;
        // $c = $d;
        // $d = $b;
        // $d = $c;
        // $b = $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        ITypeVariableSymbol $d = typeVar("$d");
        when($a.getConstraints()).thenReturn(list($b, type(arrayType)));
        when($b.getConstraints()).thenReturn(list($c, $a, type(intType)));
        when($c.getConstraints()).thenReturn(list($d, type(floatType)));
        when($d.getConstraints()).thenReturn(list($b, $c));
        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c, $d);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraints(scope);
                    return null;
                }
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat($a.getType().isReadyForEval(), is(true));
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat($c.getType().isReadyForEval(), is(true));
            assertThat($c.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat($d.getType().isReadyForEval(), is(true));
            assertThat($d.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_SelfRef_DoesTerminate()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = [];
        // $a = $a;

        ITypeVariableSymbol $a = typeVar("$a");
        when($a.getConstraints()).thenReturn(list($a, type(arrayType)));
        final ITypeVariableCollection scope = createTypeVariableCollection($a);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraints(scope);
                    return null;
                }
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat($a.getType().isReadyForEval(), is(true));
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("array"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }
}
