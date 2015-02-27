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
import ch.tsphp.tinsphp.inference_engine.constraints.OverloadDto;
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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class IntersectionConstraintSolverTest extends AConstraintSolverTest
{

    @Test
    public void solveConstraintsOfScope_AdditionWithIntAndInt$b_$aAnd$bAreInt() {
        // corresponds:
        // $b = 1;
        // $a = $b + 1;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createPartialAdditionWithInt("$b", scope);
        map.put("$a", list(intersection));
        map.put("$b", list(type(intType)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int"));
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithFloatAndFloat$b_$aAnd$bAreFloat() {
        // corresponds:
        // $b = 1.2;
        // $a = $b + 1.2;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createPartialAdditionWithFloat("$b", scope);
        map.put("$a", list(intersection));
        map.put("$b", list(type(floatType)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("float"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithFloatAndFloat$bFromOtherScope_$aAnd$bAreFloat() {
        // corresponds:
        // $b = 1.2;
        // --- different scope
        // $a = $b + 1.2;

        Map<String, List<IConstraint>> refMap = new HashMap<>();
        refMap.put("$b", list(type(floatType)));
        IScope refScope = createScopeWithConstraints(refMap);
        Map<String, IUnionTypeSymbol> refResult = createResolvingResult(refScope);

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createPartialAdditionWithFloat("$b", refScope);
        map.put("$a", list(intersection));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(1));
        assertThat(result, hasKey("$a"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("float"));

        assertThat(refResult.size(), is(1));
        assertThat(refResult, hasKey("$b"));
        assertThat(refResult.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }

    @Test
    public void solveConstraintsOfScope_ErroneousFuncCallWhichExpectsFooAndFloat$bGiven_$aIsEmptyAnd$bIsFloat() {
        // corresponds:
        // function foo(Foo $f){}
        // $b = 1.2;
        // erroneous expression, will result in a fatal error. Hence $a will be empty
        // $a = foo($b);

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = intersect(list(ref("$b", scope)), list(
                new OverloadDto(asList(asList(type(fooType))), arrayType)
        ));
        map.put("$a", list(intersection));
        map.put("$b", list(type(floatType)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$a").getTypeSymbols().size(), is(0));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }

    @Test
    public void solveConstraintsOfScope_ErroneousFuncCallWhichExpectsFooInt$aGiven_$aIsIntAnd$bIsEmpty() {
        // corresponds:
        // function foo(Foo $f){}
        // $a = 1;
        // erroneous expression, will result in a fatal error. Hence $b will be empty
        // $b = foo($a);

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = intersect(list(ref("$a", scope)), list(
                new OverloadDto(asList(asList(type(fooType))), arrayType)
        ));
        map.put("$a", list(type(intType)));
        map.put("$b", list(intersection));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
        assertThat(result.get("$b").getTypeSymbols().size(), is(0));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        IConstraint intersectionA = intersect(list(ref("$b", scope)), list(
                new OverloadDto(asList(asList(type(fooType))), arrayType)
        ));
        IConstraint intersectionC = createPartialAdditionWithInt("$a", scope);
        map.put("$a", list(intersectionA));
        map.put("$b", list(type(floatType)));
        map.put("$c", list(intersectionC));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(3));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result, hasKey("$c"));
        assertThat(result.get("$a").getTypeSymbols().size(), is(0));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("float"));
        assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("int"));
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithInt$bAndFloat$c_$aIsNumAnd$bIsIntAnd$cIsFloat() {
        // corresponds:
        // $b = 1;
        // $c = 1.2;
        // $a = $b + $c;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createAdditionIntersection("$b", scope, "$c", scope);
        map.put("$b", list(type(intType)));
        map.put("$c", list(type(floatType)));
        map.put("$a", list(intersection));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(3));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result, hasKey("$c"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int"));
        assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }

    @Test
    public void solveConstraintsOfScope_MultipleOverloadsAndArray$b_$aIsFooAnd$bIsArray() {
        // corresponds:
        // $b = [1];
        // $a = foo($b);  //where foo has overloads as bellow

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = intersect(list(ref("$b", scope)), list(
                new OverloadDto(asList(asList(type(intType))), intType),
                new OverloadDto(asList(asList(type(floatType))), floatType),
                new OverloadDto(asList(asList(type(arrayType))), fooType),
                new OverloadDto(asList(asList(type(fooType))), arrayType)
        ));
        map.put("$a", list(intersection));
        map.put("$b", list(type(arrayType)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("Foo"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("array"));
    }

    @Test
    public void solveConstraintsOfScope_MultipleOverloadsAndArray$a_$aIsArrayAnd$bIsFooy() {
        // corresponds:
        // $a = [1];
        // $b = foo($a);  //where foo has overloads as bellow

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = intersect(list(ref("$a", scope)), list(
                new OverloadDto(asList(asList(type(intType))), intType),
                new OverloadDto(asList(asList(type(floatType))), floatType),
                new OverloadDto(asList(asList(type(arrayType))), fooType),
                new OverloadDto(asList(asList(type(fooType))), arrayType)
        ));
        map.put("$a", list(type(arrayType)));
        map.put("$b", list(intersection));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("array"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("Foo"));
    }

    @Test
    public void solveConstraintsOfScope_AdditionWithIntAndSelfRefInt$a_$aIsInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1;
        // $a = $a + 1;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createPartialAdditionWithInt("$a", scope);
        map.put("$a", list(intersection, type(intType)));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(1));
            assertThat(result, hasKey("$a"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createAdditionIntersection("$b", scope, "$a", scope);
        map.put("$a", list(type(intType), intersection));
        map.put("$b", list(type(intType)));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createAdditionIntersection("$a", scope, "$b", scope);
        map.put("$a", list(intersection, type(intType)));
        map.put("$b", list(type(intType)));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createAdditionIntersection("$b", scope, "$a", scope);
        map.put("$a", list(type(intType), intersection));
        map.put("$b", list(type(floatType)));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("float"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createAdditionIntersection("$b", scope, "$a", scope);
        map.put("$a", list(type(floatType), intersection));
        map.put("$b", list(type(intType)));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createPartialAdditionWithInt("$b", scope);
        map.put("$a", list(type(intType), intersection));
        map.put("$b", list(type(intType), ref("$a", scope)));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createPartialAdditionWithInt("$a", scope);
        map.put("$a", list(type(intType), ref("$b", scope)));
        map.put("$b", list(type(intType), intersection));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createPartialAdditionWithInt("$b", scope);
        map.put("$a", list(type(intType), intersection));
        map.put("$b", list(type(floatType), ref("$a", scope)));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createPartialAdditionWithInt("$a", scope);
        map.put("$a", list(type(intType), ref("$b", scope)));
        map.put("$b", list(type(floatType), intersection));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = intersect(list(ref("$b", scope)), list(
                new OverloadDto(asList(asList(type(arrayType))), arrayType)
        ));
        map.put("$a", list(type(intType), intersection));
        map.put("$b", list(type(floatType), ref("$a", scope)));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int", "float"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = intersect(list(ref("$a", scope)), list(
                new OverloadDto(asList(asList(type(arrayType))), arrayType)
        ));
        map.put("$a", list(type(intType), ref("$b", scope)));
        map.put("$b", list(type(floatType), intersection));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int", "float"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("float"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createPartialAdditionWithInt("$b", scope);
        map.put("$a", list(type(intType), intersection));
        map.put("$b", list(type(floatType), ref("$a", scope)));
        map.put("$c", list(iRef("$a", scope)));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(3));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result, hasKey("$c"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("num"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersection = createPartialAdditionWithInt("$a", scope);
        map.put("$a", list(type(intType), ref("$b", scope)));
        map.put("$b", list(type(floatType), intersection));
        map.put("$c", list(iRef("$a", scope)));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(3));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result, hasKey("$c"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("num"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersectionA = createPartialAdditionWithInt("$b", scope);
        IConstraint intersectionB = createPartialAdditionWithInt("$a", scope);

        map.put("$a", list(type(intType), intersectionA));
        map.put("$b", list(type(floatType), intersectionB));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        IConstraint intersectionA = createPartialAdditionWithInt("$b", scope);
        IConstraint intersectionB = createPartialAdditionWithInt("$a", scope);

        map.put("$a", list(type(floatType), intersectionA));
        map.put("$b", list(type(intType), intersectionB));
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
            }, TIMEOUT, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(2));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("num"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }
}
