/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.constraints;

import ch.tsphp.common.IConstraint;
import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.ITypeSymbol;
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class RefConstraintSolverTest extends AConstraintSolverTest
{

    @Test
    public void solveConstraintsOfScope_InSameScope_UnionContainsAllTypesOfRefAndOwn() {
        // corresponds:
        // $b = 1; $b = 1.2; $b = []; $a = 1;
        // $a = $b;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        map.put("$b", list(type(intType), type(fooType), type(arrayType)));
        map.put("$a", list(ref("$b", scope), type(intType)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
    }

    @Test
    public void solveConstraintsOfScope_InSameScope_RefIsAlsoSolved() {
        // corresponds:
        // $b = 1; $b = 1.2; $b = []; $a = 1;
        // $a = $b;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        map.put("$b", list(type(intType), type(fooType), type(arrayType)));
        map.put("$a", list(iRef("$b", scope)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);


        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
    }

    @Test
    public void solveConstraintsOfScope_InOtherScope_UnionContainsAllTypesOfRefAndOwn() {
        // corresponds:
        // $b = 1; $b = 1.2; $b = [];
        // --- in different scope
        // $a = 1;
        // $a = $b;

        Map<String, List<IConstraint>> refMap = new HashMap<>();
        refMap.put("$b", list(type(intType), type(fooType), type(arrayType)));
        IScope refScope = createScopeWithConstraints(refMap);
        //necessary in order that refScope also saves results (is used by the algorithm)
        createResolvingResult(refScope);

        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("$a", list(ref("$b", refScope), type(intType), type(floatType)));
        IScope scope = createScopeWithConstraints(map);
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        //act
        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);


        assertThat(result.size(), is(1));
        assertThat(result, hasKey("$a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("$a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("int", "float", "Foo", "array"));
    }

    @Test
    public void solveConstraintsOfScope_InOtherScope_RefIsSolved() {
        // corresponds:
        // $b = 1; $b = 1.2; $b = [];
        // --- in different scope
        // $a = 1;
        // $a = $b;

        Map<String, List<IConstraint>> refMap = new HashMap<>();
        refMap.put("$b", list(type(intType), type(fooType), type(arrayType)));
        IScope refScope = createScopeWithConstraints(refMap);
        Map<String, IUnionTypeSymbol> result = createResolvingResult(refScope);

        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("$a", list(ref("$b", refScope), type(intType), type(floatType)));
        IScope scope = createScopeWithConstraints(map);
        //necessary in order that refScope also saves results (is used by the algorithm)
        createResolvingResult(scope);

        //act
        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(1));
        assertThat(result.get("$b").isReadyForEval(), is(true));
    }

    @Test
    public void solveConstraintsOfScope_CircleInOwnScope_UnionContainsAllTypesOfRefAndTerminates()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $b = 1; $b = 1.2; $b = []; $a = 1;
        // $b = $a;
        // $a = $b;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        map.put("$a", list(ref("$b", scope), type(intType)));
        map.put("$b", list(type(intType), type(fooType), type(arrayType), ref("$a", scope)));
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
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
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

        Map<String, List<IConstraint>> refMap = new HashMap<>();
        IScope refScope = createScopeWithConstraints(refMap);
        //necessary in order that refScope also saves results (is used by the algorithm)
        createResolvingResult(refScope);

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        map.put("$a", list(ref("$b", refScope), type(intType)));
        refMap.put("$b", list(type(intType), type(fooType), type(arrayType), ref("$a", scope)));

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
            assertThat(result, not(hasKey("$b")));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int", "Foo", "array"));
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

        Map<String, List<IConstraint>> refMap = new HashMap<>();
        IScope refScope = createScopeWithConstraints(refMap);
        Map<String, IUnionTypeSymbol> refResult = createResolvingResult(refScope);

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        //necessary in order that scope also saves results (is used by the algorithm)
        createResolvingResult(scope);

        map.put("$a", list(ref("$b", refScope), type(intType)));
        refMap.put("$b", list(type(intType), type(fooType), type(arrayType), ref("$a", scope)));

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
            assertThat(refResult.size(), is(1));
            assertThat(refResult, hasKey("$b"));
            assertThat(refResult, not(hasKey("$a")));
            assertThat(refResult.get("$b").isReadyForEval(), is(false));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        map.put("$a", list(ref("$b", scope), type(arrayType)));
        map.put("$b", list(ref("$c", scope), type(intType)));
        map.put("$c", list(ref("$a", scope), type(floatType)));
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
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        map.put("$a", list(ref("$b", scope), type(arrayType)));
        map.put("$b", list(ref("$c", scope), type(intType)));
        map.put("$c", list(ref("$b", scope), type(floatType)));
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
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int", "float"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("int", "float"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        map.put("$a", list(ref("$b", scope), type(arrayType)));
        map.put("$b", list(ref("$c", scope), ref("$a", scope), type(intType)));
        map.put("$c", list(ref("$d", scope), type(floatType)));
        map.put("$d", list(iRef("$b", scope), ref("$c", scope)));
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
            assertThat(result.size(), is(4));
            assertThat(result, hasKey("$a"));
            assertThat(result, hasKey("$b"));
            assertThat(result, hasKey("$c"));
            assertThat(result, hasKey("$d"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
            assertThat(result.get("$d").getTypeSymbols().keySet(), containsInAnyOrder("int", "float", "array"));
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

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        map.put("$a", list(ref("$a", scope), type(arrayType)));
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
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("array"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }
}
