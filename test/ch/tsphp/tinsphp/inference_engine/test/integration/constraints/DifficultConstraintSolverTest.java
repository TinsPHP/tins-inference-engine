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

public class DifficultConstraintSolverTest extends AConstraintSolverTest
{

    @Test
    public void resolveConstraints_CompetingCircularRefWithRefToBeginning_ShouldGoBackToTheBeginning()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1;
        // $b = 2 + $a;
        // $a = 1 + $b;    //competing circularity $e -> a -> refV b -> c -> a
        // $c = $a;        //competing circularity $e -> a -> refV b -> c -> a
        // $d = $a;
        // $d = $b;
        // $b = $c;
        // $b = $e + 1.2;  //competing circularity $e -> a -> refV b -> refV e  -> should win
        // $e = $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        ITypeVariableSymbol $d = typeVar("$d");
        ITypeVariableSymbol $e = typeVar("$e");
        IConstraint intersectionA = createPartialAdditionWithInt($b);
        IConstraint intersectionB1 = createPartialAdditionWithInt($a);
        IConstraint intersectionB2 = createPartialAdditionWithFloat($e);
        when($a.getConstraints()).thenReturn(list(type(intType), intersectionA));
        when($b.getConstraints()).thenReturn(list(type(intType), intersectionB1, ref($c), intersectionB2));
        when($c.getConstraints()).thenReturn(list(ref($a)));
        when($d.getConstraints()).thenReturn(list(ref($a), ref($b)));
        when($e.getConstraints()).thenReturn(list(ref($a)));
        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c, $d, $e);

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
            assertThat($d.getType().isReadyForEval(), is(true));
            assertThat($d.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($e.getType().isReadyForEval(), is(true));
            assertThat($e.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void resolveConstraints_CompetingCircularRefWithRefToBeginningAndErroneousAddition_SeeComment()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1; $e = [];
        // $b = 2 + $a;
        // $a = 1 + $b;  //competing circularity $e -> a -> refV b -> refV a
        // $c = $a;      //competing circularity $e -> a -> refV b -> c -> a
        // $d = $a;
        // $d = $b;
        // $b = $c;

        // $e is array V int -> erroneous + => $b stays int
        // $b = $e + 1.2; //competing circularity $e -> a -> refV b -> refV e

        // $e = $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        ITypeVariableSymbol $d = typeVar("$d");
        ITypeVariableSymbol $e = typeVar("$e");
        IConstraint intersectionA = createPartialAdditionWithInt($b);
        IConstraint intersectionB1 = createPartialAdditionWithInt($a);
        IConstraint intersectionB2 = createPartialAdditionWithFloat($e);

        when($a.getConstraints()).thenReturn(list(type(intType), intersectionA));
        when($b.getConstraints()).thenReturn(list(type(intType), intersectionB1, ref($c), intersectionB2));
        when($c.getConstraints()).thenReturn(list(ref($a)));
        when($d.getConstraints()).thenReturn(list(ref($a), ref($b)));
        when($e.getConstraints()).thenReturn(list(ref($a), type(arrayType)));

        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c, $d, $e);

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
            assertThat($c.getType().isReadyForEval(), is(true));
            assertThat($c.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat($d.getType().isReadyForEval(), is(true));
            assertThat($d.getType().getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat($e.getType().isReadyForEval(), is(true));
            assertThat($e.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "array"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void resolveConstraints_CompetingCircularRefWithTwoRefToBeginningAndErroneousAddition_SeeComment()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2; $e = [];
        // $b = 2 + $a;

        // $b is array V int V float -> erroneous + => $a stays int
        // $a = 1 + $b;  //competing circularity $e -> a -> refV b -> refV a

        // $c = $e;      //competing circularity $e -> a -> refV b -> c -> e
        // $d = $a;
        // $d = $b;
        // $b = $c;

        // $e is array V int -> erroneous + => $b stays float
        // $b = $e + 1.2; //competing circularity $e -> a -> refV b -> refV e

        // $e = $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        ITypeVariableSymbol $d = typeVar("$d");
        ITypeVariableSymbol $e = typeVar("$e");
        IConstraint intersectionA = createPartialAdditionWithInt($b);
        IConstraint intersectionB1 = createPartialAdditionWithInt($a);
        IConstraint intersectionB2 = createPartialAdditionWithFloat($e);

        when($a.getConstraints()).thenReturn(list(type(intType), intersectionA));
        when($b.getConstraints()).thenReturn(list(type(floatType), intersectionB1, ref($c), intersectionB2));
        when($c.getConstraints()).thenReturn(list(ref($e)));
        when($d.getConstraints()).thenReturn(list(ref($a), ref($b)));
        when($e.getConstraints()).thenReturn(list(ref($a), type(arrayType)));

        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c, $d, $e);

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
            assertThat($b.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat($c.getType().isReadyForEval(), is(true));
            assertThat($c.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "array"));
            assertThat($d.getType().isReadyForEval(), is(true));
            assertThat($d.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat($e.getType().isReadyForEval(), is(true));
            assertThat($e.getType().getTypeSymbols().keySet(), containsInAnyOrder("int", "array"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void resolveConstraints_CompetingCircularRefWithTwoRefToBeginningTwoOtherRefs_SeeComment()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $b = 2 + $a;
        // $a = 1 + $b;  //competing circularity $e -> a -> refV b -> refV a
        // $c = $e;      //competing circularity $e -> a -> refV b -> c -> e
        // $d = $a + 1;  //competing circularity $d -> a  and  d -> b -> refV a
        // $d = $b;
        // $b = $c;
        // $b = $e + 1.2; //competing circularity $e -> a -> refV b -> refV e
        // $e = $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        ITypeVariableSymbol $d = typeVar("$d");
        ITypeVariableSymbol $e = typeVar("$e");
        IConstraint intersectionA = createPartialAdditionWithInt($b);
        IConstraint intersectionB1 = createPartialAdditionWithInt($a);
        IConstraint intersectionB2 = createPartialAdditionWithFloat($e);
        IConstraint intersectionD = createPartialAdditionWithInt($a);

        when($a.getConstraints()).thenReturn(list(type(intType), intersectionA));
        when($b.getConstraints()).thenReturn(list(type(floatType), intersectionB1, ref($c), intersectionB2));
        when($c.getConstraints()).thenReturn(list(ref($e)));
        when($d.getConstraints()).thenReturn(list(ref($a), intersectionD, ref($b)));
        when($e.getConstraints()).thenReturn(list(ref($a)));

        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c, $d, $e);

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
            assertThat($d.getType().isReadyForEval(), is(true));
            assertThat($d.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($e.getType().isReadyForEval(), is(true));
            assertThat($e.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void resolveConstraints_CompetingCircularRefIncludingSelfRef_SeeComment()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $b = 2 + $a;
        // $a = 1 + $b;  //competing circularity $e -> a -> refV b -> refV a
        // $c = $e;      //competing circularity $e -> a -> refV b -> c -> e
        // $d = $a + $b; //competing circularity $d -> a  and  d -> b -> refV a and d -> b -> refV b
        // $d = $b;
        // $b = $c;
        // $b = $c + 1.2;
        // $b = $b;       //self ref
        // $c = 1 + $c;   //self ref in intersection
        // $b = $b + $b;  //double self ref in intersection
        // $e = $a;

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        ITypeVariableSymbol $d = typeVar("$d");
        ITypeVariableSymbol $e = typeVar("$e");

        IConstraint intersectionA = createPartialAdditionWithInt($b);
        IConstraint intersectionBA = createPartialAdditionWithInt($a);
        IConstraint intersectionBC = createPartialAdditionWithFloat($c);
        IConstraint intersectionBBB = createAdditionIntersection($b, $b);
        IConstraint intersectionC = createPartialAdditionWithInt($c);
        IConstraint intersectionD = createAdditionIntersection($a, $b);

        when($a.getConstraints()).thenReturn(list(type(intType), intersectionA));
        when($b.getConstraints()).thenReturn(list(
                type(floatType),
                intersectionBA,
                ref($c),
                intersectionBC,
                ref($b),
                intersectionBBB));
        when($c.getConstraints()).thenReturn(list(ref($e), intersectionC));
        when($d.getConstraints()).thenReturn(list(intersectionD, ref($b)));
        when($e.getConstraints()).thenReturn(list(ref($a)));
        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c, $d, $e);

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
            assertThat($d.getType().isReadyForEval(), is(true));
            assertThat($d.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($e.getType().isReadyForEval(), is(true));
            assertThat($e.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void resolveConstraints_CompetingCircularRefIncludingSelfRef2_SeeComment()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $b = 2 + $a;
        // $a = 1 + $b;  //competing circularity $e -> a -> refV b -> refV a
        // $c = $e;      //competing circularity $e -> a -> refV b -> c -> e
        // $d = $a + $b; //competing circularity $d -> a  and  d -> b -> refV a and d -> b -> refV b
        // $d = $b;
        // $b = $c;
        // $b = $c + 1.2;
        // $b = $b;       //self ref
        // $c = 1 + $c;   //self ref in intersection
        // $b = $b + $b;  //double self ref in intersection
        // $e = $a;
        // $c = $b;  <-- different from above let's see if this one is a problem, $c = 1 + $c is going to be solved
        // iteratively already

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        ITypeVariableSymbol $d = typeVar("$d");
        ITypeVariableSymbol $e = typeVar("$e");

        IConstraint intersectionA = createPartialAdditionWithInt($b);
        IConstraint intersectionBA = createPartialAdditionWithInt($a);
        IConstraint intersectionBC = createPartialAdditionWithFloat($c);
        IConstraint intersectionBBB = createAdditionIntersection($b, $b);
        IConstraint intersectionC = createPartialAdditionWithInt($c);
        IConstraint intersectionD = createAdditionIntersection($a, $b);

        when($a.getConstraints()).thenReturn(list(type(intType), intersectionA));
        when($b.getConstraints()).thenReturn(list(
                type(floatType),
                intersectionBA,
                ref($c),
                intersectionBC,
                ref($b),
                intersectionBBB));
        when($c.getConstraints()).thenReturn(list(ref($e), intersectionC, ref($b)));
        when($d.getConstraints()).thenReturn(list(intersectionD, ref($b)));
        when($e.getConstraints()).thenReturn(list(ref($a)));
        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c, $d, $e);

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
            assertThat($d.getType().isReadyForEval(), is(true));
            assertThat($d.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($e.getType().isReadyForEval(), is(true));
            assertThat($e.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void resolveConstraints_CompetingCircularRefIncludingSelfRef3_SeeComment()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $b = 2 + $a;
        // $a = 1 + $b;  //competing circularity $e -> a -> refV b -> refV a
        // --> different from above, removed ref with beginning // $c = $e;
        // $d = $a + $b; //competing circularity $d -> a  and  d -> b -> refV a and d -> b -> refV b
        // $d = $b;
        // $b = $c;
        // $b = $c + 1.2;
        // $b = $b;       //self ref
        // $c = 1 + $c;   //self ref in intersection
        // $b = $b + $b;  //double self ref in intersection
        // $e = $a;
        // $c = $b;       //let's see if this one is a problem, $c = 1 + $c is going to be solved iteratively already

        ITypeVariableSymbol $a = typeVar("$a");
        ITypeVariableSymbol $b = typeVar("$b");
        ITypeVariableSymbol $c = typeVar("$c");
        ITypeVariableSymbol $d = typeVar("$d");
        ITypeVariableSymbol $e = typeVar("$e");

        IConstraint intersectionA = createPartialAdditionWithInt($b);
        IConstraint intersectionBA = createPartialAdditionWithInt($a);
        IConstraint intersectionBC = createPartialAdditionWithFloat($c);
        IConstraint intersectionBBB = createAdditionIntersection($b, $b);
        IConstraint intersectionC = createPartialAdditionWithInt($c);
        IConstraint intersectionD = createAdditionIntersection($a, $b);

        when($a.getConstraints()).thenReturn(list(type(intType), intersectionA));
        when($b.getConstraints()).thenReturn(list(
                type(floatType),
                intersectionBA,
                ref($c),
                intersectionBC,
                ref($b),
                intersectionBBB));
        when($c.getConstraints()).thenReturn(list(intersectionC, ref($b)));
        when($d.getConstraints()).thenReturn(list(intersectionD, ref($b)));
        when($e.getConstraints()).thenReturn(list(ref($a)));
        final ITypeVariableCollection scope = createTypeVariableCollection($a, $b, $c, $d, $e);

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
            assertThat($d.getType().isReadyForEval(), is(true));
            assertThat($d.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat($e.getType().isReadyForEval(), is(true));
            assertThat($e.getType().getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }
}
