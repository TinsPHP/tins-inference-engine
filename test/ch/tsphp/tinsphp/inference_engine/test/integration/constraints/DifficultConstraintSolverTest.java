/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.constraints;

import ch.tsphp.common.IConstraint;
import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.test.ActWithTimeout;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.AConstraintSolverTest;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersectionA = createPartialAdditionWithInt("$b", scope);
        IConstraint intersectionB1 = createPartialAdditionWithInt("$a", scope);
        IConstraint intersectionB2 = createPartialAdditionWithFloat("$e", scope);

        map.put("$a", list(type(intType), intersectionA));
        map.put("$b", list(type(intType), intersectionB1, ref("$c", scope), intersectionB2));
        map.put("$c", list(iRef("$a", scope)));
        map.put("$d", list(iRef("$a", scope), ref("$b", scope)));
        map.put("$e", list(iRef("$a", scope)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraintsOfScope(scope);
                    return null;
                }
            }, 2000, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(5));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result, hasKey("$c"));
            assertThat(result, hasKey("$d"));
            assertThat(result, hasKey("$e"));
            //everything must be num due to the addition $e + 1.2
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$d").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$e").getTypeSymbols().keySet(), containsInAnyOrder("num"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersectionA = createPartialAdditionWithInt("$b", scope);
        IConstraint intersectionB1 = createPartialAdditionWithInt("$a", scope);
        IConstraint intersectionB2 = createPartialAdditionWithFloat("$e", scope);

        map.put("$a", list(type(intType), intersectionA));
        map.put("$b", list(type(intType), intersectionB1, ref("$c", scope), intersectionB2));
        map.put("$c", list(iRef("$a", scope)));
        map.put("$d", list(iRef("$a", scope), ref("$b", scope)));
        map.put("$e", list(ref("$a", scope), type(arrayType)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraintsOfScope(scope);
                    return null;
                }
            }, 2000, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(5));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result, hasKey("$c"));
            assertThat(result, hasKey("$d"));
            assertThat(result, hasKey("$e"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat(result.get("$d").getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat(result.get("$e").getTypeSymbols().keySet(), containsInAnyOrder("int", "array"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersectionA = createPartialAdditionWithInt("$b", scope);
        IConstraint intersectionB1 = createPartialAdditionWithInt("$a", scope);
        IConstraint intersectionB2 = createPartialAdditionWithFloat("$e", scope);

        map.put("$a", list(type(intType), intersectionA));
        map.put("$b", list(type(floatType), intersectionB1, ref("$c", scope), intersectionB2));
        map.put("$c", list(iRef("$e", scope)));
        map.put("$d", list(iRef("$a", scope), ref("$b", scope)));
        map.put("$e", list(ref("$a", scope), type(arrayType)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraintsOfScope(scope);
                    return null;
                }
            }, 2000, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(5));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result, hasKey("$c"));
            assertThat(result, hasKey("$d"));
            assertThat(result, hasKey("$e"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("int", "array"));
            assertThat(result.get("$d").getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat(result.get("$e").getTypeSymbols().keySet(), containsInAnyOrder("int", "array"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersectionA = createPartialAdditionWithInt("$b", scope);
        IConstraint intersectionB1 = createPartialAdditionWithInt("$a", scope);
        IConstraint intersectionB2 = createPartialAdditionWithFloat("$e", scope);
        IConstraint intersectionD = createPartialAdditionWithInt("$a", scope);

        map.put("$a", list(type(intType), intersectionA));
        map.put("$b", list(type(floatType), intersectionB1, ref("$c", scope), intersectionB2));
        map.put("$c", list(iRef("$e", scope)));
        map.put("$d", list(iRef("$a", scope), intersectionD, ref("$b", scope)));
        map.put("$e", list(iRef("$a", scope)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraintsOfScope(scope);
                    return null;
                }
            }, 2000, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(5));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result, hasKey("$c"));
            assertThat(result, hasKey("$d"));
            assertThat(result, hasKey("$e"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$d").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$e").getTypeSymbols().keySet(), containsInAnyOrder("num"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersectionA = createPartialAdditionWithInt("$b", scope);
        IConstraint intersectionBA = createPartialAdditionWithInt("$a", scope);
        IConstraint intersectionBC = createPartialAdditionWithFloat("$c", scope);
        IConstraint intersectionBBB = createAdditionIntersection("$b", scope, "$b", scope);
        IConstraint intersectionC = createPartialAdditionWithInt("$c", scope);
        IConstraint intersectionD = createAdditionIntersection("$a", scope, "$b", scope);

        map.put("$a", list(type(intType), intersectionA));
        map.put("$b", list(
                type(floatType),
                intersectionBA,
                ref("$c", scope),
                intersectionBC,
                ref("$b", scope),
                intersectionBBB));
        map.put("$c", list(ref("$e", scope), intersectionC));
        map.put("$d", list(intersectionD, ref("$b", scope)));
        map.put("$e", list(iRef("$a", scope)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraintsOfScope(scope);
                    return null;
                }
            }, 2000, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(5));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result, hasKey("$c"));
            assertThat(result, hasKey("$d"));
            assertThat(result, hasKey("$e"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$d").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$e").getTypeSymbols().keySet(), containsInAnyOrder("num"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersectionA = createPartialAdditionWithInt("$b", scope);
        IConstraint intersectionBA = createPartialAdditionWithInt("$a", scope);
        IConstraint intersectionBC = createPartialAdditionWithFloat("$c", scope);
        IConstraint intersectionBBB = createAdditionIntersection("$b", scope, "$b", scope);
        IConstraint intersectionC = createPartialAdditionWithInt("$c", scope);
        IConstraint intersectionD = createAdditionIntersection("$a", scope, "$b", scope);

        map.put("$a", list(type(intType), intersectionA));
        map.put("$b", list(
                type(floatType),
                intersectionBA,
                ref("$c", scope),
                intersectionBC,
                ref("$b", scope),
                intersectionBBB));
        map.put("$c", list(ref("$e", scope), intersectionC, ref("$b", scope)));
        map.put("$d", list(intersectionD, ref("$b", scope)));
        map.put("$e", list(iRef("$a", scope)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraintsOfScope(scope);
                    return null;
                }
            }, 2000, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(5));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result, hasKey("$c"));
            assertThat(result, hasKey("$d"));
            assertThat(result, hasKey("$e"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$d").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$e").getTypeSymbols().keySet(), containsInAnyOrder("num"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersectionA = createPartialAdditionWithInt("$b", scope);
        IConstraint intersectionBA = createPartialAdditionWithInt("$a", scope);
        IConstraint intersectionBC = createPartialAdditionWithFloat("$c", scope);
        IConstraint intersectionBBB = createAdditionIntersection("$b", scope, "$b", scope);
        IConstraint intersectionC = createPartialAdditionWithInt("$c", scope);
        IConstraint intersectionD = createAdditionIntersection("$a", scope, "$b", scope);

        map.put("$a", list(type(intType), intersectionA));
        map.put("$b", list(
                type(floatType),
                intersectionBA,
                ref("$c", scope),
                intersectionBC,
                ref("$b", scope),
                intersectionBBB));
        map.put("$c", list(intersectionC, ref("$b", scope)));
        map.put("$d", list(intersectionD, ref("$b", scope)));
        map.put("$e", list(iRef("$a", scope)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        try {
            //act
            ActWithTimeout.exec(new Callable<Void>()
            {
                public Void call() {
                    IConstraintSolver solver = createConstraintSolver();
                    solver.solveConstraintsOfScope(scope);
                    return null;
                }
            }, 2000, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(5));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result, hasKey("$c"));
            assertThat(result, hasKey("$d"));
            assertThat(result, hasKey("$e"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$d").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$e").getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }
}
