/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.OverloadDto;
import ch.tsphp.tinsphp.inference_engine.test.ActWithTimeout;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.AConstraintSolverTest;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class IntersectionConstraintSolverTest extends AConstraintSolverTest
{

    @Test
    public void solveConstraintsOfScope_AdditionWithIntAndInt$b_$aAnd$bAreInt() {
        // corresponds:
        // $b = 1;
        // $a = $b + 1;

        ITypeVariableSymbol $b = typeVar("$b", type(intType));
        IConstraint intersection = createPartialAdditionWithInt($b);
        ITypeVariableSymbol $a = typeVar("$a", intersection);
        ITypeVariableCollection scope = createTypeVariableCollection($b, $a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithFloatAndFloat$b_$aAnd$bAreFloat() {
        // corresponds:
        // $b = 1.2;
        // $a = $b + 1.2;

        ITypeVariableSymbol $b = typeVar("$b", type(floatType));
        IConstraint intersection = createPartialAdditionWithFloat($b);
        ITypeVariableSymbol $a = typeVar("$a", intersection);
        ITypeVariableCollection scope = createTypeVariableCollection($b, $a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("float"));
        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithFloatAndFloat$bFromOtherScope_$aAnd$bAreFloat() {
        // corresponds:
        // $b = 1.2;
        // --- different scope
        // $a = $b + 1.2;

        ITypeVariableSymbol $b = typeVar("$b", type(floatType));
        IConstraint intersection = createPartialAdditionWithFloat($b);
        ITypeVariableSymbol $a = typeVar("$a", intersection);
        ITypeVariableCollection scope = createTypeVariableCollection($a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("float"));
        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }

    @Test
    public void solveConstraintsOfScope_ErroneousFuncCallWhichExpectsFooAndFloat$bGiven_$aIsEmptyAnd$bIsFloat() {
        // corresponds:
        // function foo(Foo $f){}
        // $b = 1.2;
        // erroneous expression, will result in a fatal error. Hence $a will be empty
        // $a = foo($b);

        ITypeVariableSymbol $b = typeVar("$b", type(floatType));
        IConstraint intersection = intersect(asList($b), asList(
                new OverloadDto(asList(asList(type(fooType))), arrayType)
        ));
        ITypeVariableSymbol $a = typeVar("$a", intersection);
        ITypeVariableCollection scope = createTypeVariableCollection($a, $b);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().size(), is(0));
        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }

    @Test
    public void solveConstraintsOfScope_ErroneousFuncCallWhichExpectsFooInt$aGiven_$aIsIntAnd$bIsEmpty() {
        // corresponds:
        // function foo(Foo $f){}
        // $a = 1;
        // erroneous expression, will result in a fatal error. Hence $b will be empty
        // $b = foo($a);

        ITypeVariableSymbol $a = typeVar("$a", type(intType));
        IConstraint intersection = intersect(asList($a), asList(
                new OverloadDto(asList(asList(type(fooType))), arrayType)
        ));
        ITypeVariableSymbol $b = typeVar("$b", intersection);
        ITypeVariableCollection scope = createTypeVariableCollection($a, $b);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().size(), is(0));
    }

    @Test
    public void
    solveConstraintsOfScope_AdditionWithIntAndEmpty$aDueToErroneousCallWithFloat$b_$aIsEmptyAnd$bIsFloatAnd$cIsInt() {
        // corresponds:
        // function foo(Foo $f){}
        // $b = 1.2;
        // erroneous expression, will result in a fatal error. Hence $a will be empty
        // $a = foo($b);
        // $c = 1 + $a;

        ITypeVariableSymbol $b = typeVar("$b", type(floatType));
        IConstraint intersectionA = intersect(asList($b), asList(
                new OverloadDto(asList(asList(type(fooType))), arrayType)
        ));
        ITypeVariableSymbol $a = typeVar("$a", intersectionA);
        IConstraint intersectionC = createPartialAdditionWithInt($a);
        ITypeVariableSymbol $c = typeVar("$c", intersectionC);
        ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().size(), is(0));
        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("float"));
        assertThat($c.getType().isReadyForEval(), is(true));
        assertThat($c.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithInt$bAndFloat$c_$aIsNumAnd$bIsIntAnd$cIsFloat() {
        // corresponds:
        // $b = 1;
        // $c = 1.2;
        // $a = $b + $c;

        ITypeVariableSymbol $b = typeVar("$b", type(intType));
        ITypeVariableSymbol $c = typeVar("$c", type(floatType));
        IConstraint intersection = createAdditionIntersection($b, $c);
        ITypeVariableSymbol $a = typeVar("$a", intersection);
        ITypeVariableCollection scope = createTypeVariableCollection($b, $c, $a);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
        assertThat($c.getType().isReadyForEval(), is(true));
        assertThat($c.getType().getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }

    @Test
    public void solveConstraintsOfScope_MultipleOverloadsAndArray$b_$aIsFooAnd$bIsArray() {
        // corresponds:
        // $b = [1];
        // $a = foo($b);  //where foo has overloads as bellow

        ITypeVariableSymbol $b = typeVar("$b", type(arrayType));
        IConstraint intersection = intersect(asList($b), asList(
                new OverloadDto(asList(asList(type(intType))), intType),
                new OverloadDto(asList(asList(type(floatType))), floatType),
                new OverloadDto(asList(asList(type(arrayType))), fooType),
                new OverloadDto(asList(asList(type(fooType))), arrayType)
        ));
        ITypeVariableSymbol $a = typeVar("$a", intersection);
        ITypeVariableCollection scope = createTypeVariableCollection($a, $b);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("Foo"));
        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("array"));
    }

    @Test
    public void solveConstraintsOfScope_MultipleOverloadsAndArray$a_$aIsArrayAnd$bIsFooy() {
        // corresponds:
        // $a = [1];
        // $b = foo($a);  //where foo has overloads as bellow

        ITypeVariableSymbol $a = typeVar("$a", type(arrayType));
        IConstraint intersection = intersect(asList($a), asList(
                new OverloadDto(asList(asList(type(intType))), intType),
                new OverloadDto(asList(asList(type(floatType))), floatType),
                new OverloadDto(asList(asList(type(arrayType))), fooType),
                new OverloadDto(asList(asList(type(fooType))), arrayType)
        ));
        ITypeVariableSymbol $b = typeVar("$b", intersection);
        ITypeVariableCollection scope = createTypeVariableCollection($a, $b);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat($a.getType().isReadyForEval(), is(true));
        assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("array"));
        assertThat($b.getType().isReadyForEval(), is(true));
        assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("Foo"));
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithIntAndSelfRefInt$a_$aIsInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1;
        // $a = $a + 1;

        ITypeVariableSymbol $a = typeVar("$a");
        IConstraint intersection = createPartialAdditionWithInt($a);
        when($a.getConstraints()).thenReturn(list(intersection, type(intType)));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));

        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithInt$bAndSelfRefInt$a_$aAnd$bAreInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1;
        // $a = $b + $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b", type(intType));
        IConstraint intersection = createAdditionIntersection($b, $a);
        when($a.getConstraints()).thenReturn(list(intersection, type(intType)));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithSelfRefInt$aAndInt$b_$aAnd$bAreInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1;
        // $a = $a + $b;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b", type(intType));
        IConstraint intersection = createAdditionIntersection($a, $b);
        when($a.getConstraints()).thenReturn(list(intersection, type(intType)));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithFloat$bAndSelfRefInt$a_$aIsNumAnd$bIsFloat()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.5;
        // $a = $b + $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b", type(floatType));
        IConstraint intersection = createAdditionIntersection($b, $a);
        when($a.getConstraints()).thenReturn(list(intersection, type(intType)));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("float"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithInt$bAndSelfRefFloat$a_$aIsNumAnd$bIsInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1.5; $b = 1;
        // $a = $b + $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b", type(intType));
        IConstraint intersection = createAdditionIntersection($b, $a);
        when($a.getConstraints()).thenReturn(list(intersection, type(floatType)));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithIntAndInt$bWithRefToInt$a_$aAnd$bAreInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1;
        // $b = $a;
        // $a = $b + 1;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        IConstraint intersection = createPartialAdditionWithInt($b);
        when($a.getConstraints()).thenReturn(list(type(intType), intersection));
        when($b.getConstraints()).thenReturn(list(type(intType), $a));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithIntAndInt$aWithRefToInt$b_$aAnd$bAreInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1;
        // $a = $b;
        // $b = $a + 1;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        IConstraint intersection = createPartialAdditionWithInt($a);
        when($a.getConstraints()).thenReturn(list(type(intType), $b));
        when($b.getConstraints()).thenReturn(list(type(intType), intersection));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithIntAndFloat$bWithRefToInt$a_$aAnd$bAreNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $b = $a;
        // $a = $b + 1;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        IConstraint intersection = createPartialAdditionWithInt($b);
        when($a.getConstraints()).thenReturn(list(type(intType), intersection));
        when($b.getConstraints()).thenReturn(list(type(floatType), $a));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithIntAndInt$aWithRefToFloat$b_$aAnd$bAreNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $a = $b;
        // $b = $a + 1;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        IConstraint intersection = createPartialAdditionWithInt($a);
        when($a.getConstraints()).thenReturn(list(type(intType), $b));
        when($b.getConstraints()).thenReturn(list(type(floatType), intersection));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithArrayAndFloat$bWithRefToInt$a_$aIsIntAnd$bIsIntAndFloat()
            throws ExecutionException, InterruptedException {
        // notice, $a will be int since there does not exist an overload for float x array
        // the resurrection phase would add the necessary error

        // corresponds:
        // $a = 1; $b = 1.2;
        // $b = $a;
        // $a = $b + [];

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        IConstraint intersection = intersect(asList($b), asList(
                new OverloadDto(asList(asList(type(arrayType))), arrayType)
        ));
        when($a.getConstraints()).thenReturn(list(type(intType), intersection));
        when($b.getConstraints()).thenReturn(list(type(floatType), $a));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithArrayAndInt$aWithRefToFloat$b_$aAnd$bAreIntAndFloat()
            throws ExecutionException, InterruptedException {
        // notice, $b will be int since there does not exist an overload for float x array
        // the resurrection phase would add the necessary error

        // corresponds:
        // $a = 1; $b = 1.2;
        // $a = $b;
        // $b = $a + [];

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        when($a.getConstraints()).thenReturn(list(type(intType), $b));
        IConstraint intersection = intersect(asList($a), asList(
                new OverloadDto(asList(asList(type(arrayType))), arrayType)
        ));
        when($b.getConstraints()).thenReturn(list(type(floatType), intersection));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("float"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_$cWithRefTo$aAnd$aAdditionWithIntAndFloat$bWithRefToInt$a_$aAnd$bAnd$cAreNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $b = $a;
        // $a = $b + 1;
        // $c = $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        IConstraint intersection = createPartialAdditionWithInt($b);
        when($a.getConstraints()).thenReturn(list(type(intType), intersection));
        when($b.getConstraints()).thenReturn(list(type(floatType), $a));
        when($c.getConstraints()).thenReturn(list($a));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($c.getType().isReadyForEval(), is(true));
            assertThat($c.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_$cWithRefTo$aAnd$bAdditionWithIntAndInt$aWithRefToFloat$b_$aAnd$bAnd$cAreNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $a = $b;
        // $b = $a + 1;
        // $c = $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        when($a.getConstraints()).thenReturn(list(type(intType), $b));
        IConstraint intersection = createPartialAdditionWithInt($a);
        when($b.getConstraints()).thenReturn(list(type(floatType), intersection));
        when($c.getConstraints()).thenReturn(list($a));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($c.getType().isReadyForEval(), is(true));
            assertThat($c.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_IntersectionCircleWith$aAddIntAndFloat$bAnd$bAddIntAndInt$a_$aAnd$bAreNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $a = 1 + $b;
        // $b = 2 + $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        IConstraint intersectionA = createPartialAdditionWithInt($b);
        when($a.getConstraints()).thenReturn(list(type(intType), intersectionA));
        IConstraint intersectionB = createPartialAdditionWithInt($a);
        when($b.getConstraints()).thenReturn(list(type(floatType), intersectionB));

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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));

        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_IntersectionCircleWith$aAddIntAndInt$bAnd$bAddIntAndFloat$a_$aAnd$bAreNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1.5; $b = 1;
        // $b = 2 + $a;
        // $a = 1 + $b;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        IConstraint intersectionA = createPartialAdditionWithInt($b);
        when($a.getConstraints()).thenReturn(list(type(floatType), intersectionA));
        IConstraint intersectionB = createPartialAdditionWithInt($a);
        when($b.getConstraints()).thenReturn(list(type(intType), intersectionB));

        final ITypeVariableCollection scope = createTypeVariableCollection($b, $a);

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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($b.getType().isReadyForEval(), is(true));
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));

        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithTwiceSelfRefIntAndFloat$a_$aIsNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1;
        // $a = $a + $a;
        // $a = 1.2;

        ITypeVariableSymbol $a = typeVar("$a");
        IConstraint intersection = createAdditionIntersection($a, $a);
        when($a.getConstraints()).thenReturn(list(type(intType), intersection, type(floatType)));
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
            assertThat($a.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));

        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }
}
