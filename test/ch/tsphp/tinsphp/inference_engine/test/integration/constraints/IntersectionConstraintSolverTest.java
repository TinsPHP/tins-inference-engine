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
import ch.tsphp.tinsphp.inference_engine.constraints.RefTypeConstraint;
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
    public void resolveConstraints_AdditionWithIntAndIntVariable_$aIsInt() {
        // corresponds:
        // $b = 1;
        // $a = $b + 1;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = createPartialAdditionWithInt("$b", scope);
        map.put("$a", asList(intersect(overloads)));
        map.put("$b", asList(type(intType)));
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
    public void resolveConstraints_AdditionWithFloatAndFloatVariable_$aIsFloat() {
        // corresponds:
        // $b = 1.2;
        // $a = $b + 1.2;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = createPartialAdditionWithFloat("$b", scope);
        map.put("$a", asList(intersect(overloads)));
        map.put("$b", asList(type(floatType)));
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
    public void resolveConstraints_AdditionWithFloatAndFloatVariableFromOtherScope_$aIsFloat() {
        // corresponds:
        // $b = 1.2;
        // --- different scope
        // $a = $b + 1.2;

        Map<String, List<IConstraint>> refMap = new HashMap<>();
        refMap.put("$b", asList(type(fooType)));
        IScope refScope = createScopeWithConstraints(refMap);

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = createPartialAdditionWithFloat("$b",
                refScope);
        map.put("$a", asList(intersect(overloads)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(1));
        assertThat(result, hasKey("$a"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }

    @Test
    public void resolveConstraints_AdditionWithIntVariableAndFloatVariable_$aIsNum() {
        // corresponds:
        // $b = 1;
        // $c = 1.2;
        // $a = $b + $c;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads
                = createAdditionOverload("$b", scope, "$c", scope);
        map.put("$b", asList(type(intType)));
        map.put("$c", asList(type(floatType)));
        map.put("$a", asList(intersect(overloads)));
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
    public void resolveConstraints_MultipleOverloadsAndArrayFits_$aIsFloat() {
        // corresponds:
        // $b = [1];
        // $a = foo($b);  //where foo has overloads as bellow

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = asList(
                entry(asList(refType("$b", scope, intType)), intType),
                entry(asList(refType("$b", scope, floatType)), floatType),
                entry(asList(refType("$b", scope, numType)), numType),
                entry(asList(refType("$b", scope, varAst(numType, true))), numType),
                entry(asList(refType("$b", scope, arrayType)), arrayType)
        );
        map.put("$b", asList(type(arrayType)));
        map.put("$a", asList(intersect(overloads)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("array"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("array"));
    }

    @Test
    public void resolveConstraints_AdditionWithIntAndSelfRefWhichIsInt_$aIsInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1;
        // $a = $a + 1;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = asList(
                entry(asList(refType("$a", scope, intType)), intType),
                entry(asList(refType("$b", scope, numType)), numType),
                entry(asList(refType("$b", scope, varAst(numType, true))), numType)
        );
        map.put("$a", asList(intersect(overloads), type(intType)));
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
            }, 2, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(1));
            assertThat(result, hasKey("$a"));
            assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    @Test
    public void resolveConstraints_AdditionWithIntVariableAndSelfRefWhichIsInt_$aIsInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1;
        // $a = $b + $a;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads
                = createAdditionOverload("$b", scope, "$a", scope);
        map.put("$a", asList(type(intType), intersect(overloads)));
        map.put("$b", asList(type(intType)));
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
            }, 2, TimeUnit.SECONDS);

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
    public void resolveConstraints_AdditionWithSelfRefWhichIsIntAndIntVariable_$aIsInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1;
        // $a = $a + $b;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads
                = createAdditionOverload("$a", scope, "$b", scope);
        map.put("$a", asList(intersect(overloads), type(intType)));
        map.put("$b", asList(type(intType)));
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
            }, 2, TimeUnit.SECONDS);

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
    public void resolveConstraints_AdditionWithFloatVariableAndSelfRefWhichIsInt_$aIsNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.5;
        // $a = $b + $a;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads
                = createAdditionOverload("$b", scope, "$a", scope);
        map.put("$a", asList(type(intType), intersect(overloads)));
        map.put("$b", asList(type(floatType)));
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
            }, 2, TimeUnit.SECONDS);

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
    public void resolveConstraints_AdditionWithIntVariableAndSelfRefWhichIsFloat_$aIsNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1.5; $b = 1;
        // $a = $b + $a;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads
                = createAdditionOverload("$b", scope, "$a", scope);
        map.put("$a", asList(type(floatType), intersect(overloads)));
        map.put("$b", asList(type(intType)));
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
            }, 2, TimeUnit.SECONDS);

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
    public void resolveConstraints_AdditionWithIntAndIntVariableWithCircle_$aAnd$bAreInt()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1;
        // $b = $a;
        // $a = $b + 1;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = createPartialAdditionWithInt("$b", scope);
        map.put("$a", asList(type(intType), intersect(overloads)));
        map.put("$b", asList(type(intType), ref("$a", scope)));
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
            }, 2, TimeUnit.SECONDS);

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
    public void resolveConstraints_AdditionWithIntAndFloatVariableWithCircle_$aAnd$bAreNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $b = $a;
        // $a = $b + 1;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = createPartialAdditionWithInt("$b", scope);
        map.put("$a", asList(type(intType), intersect(overloads)));
        map.put("$b", asList(type(floatType), ref("$a", scope)));
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
            }, 2, TimeUnit.SECONDS);

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
    public void resolveConstraints_AdditionWithArrayAndFloatVariableWithCircle_$aIsIntAnd$bIsIntAndFloat()
            throws ExecutionException, InterruptedException {
        // side notice, $a will be int since there does not exist an overload for float x array
        // the resurrection phase would add the necessary error

        // corresponds:
        // $a = 1; $b = 1.2;
        // $b = $a;
        // $a = $b + [];

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = asList(
                entry(asList(refType("$b", scope, arrayType)), arrayType)
        );
        map.put("$a", asList(type(intType), intersect(overloads)));
        map.put("$b", asList(type(floatType), ref("$a", scope)));
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
            }, 2, TimeUnit.SECONDS);

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
    public void resolveConstraints_RefAdditionWithIntAndFloatVariableWithCircle_$aAnd$bAnd$cAreNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $b = $a;
        // $a = $b + 1;
        // $c = $a;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = createPartialAdditionWithInt("$b", scope);
        map.put("$a", asList(type(intType), intersect(overloads)));
        map.put("$b", asList(type(floatType), ref("$a", scope)));
        map.put("$c", asList(ref("$a", scope)));
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
            }, 2, TimeUnit.SECONDS);

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
    public void resolveConstraints_IntersectionCircleWithAddWithIntAndFloatVarAndAddWithIntAndIntVar_$aAnd$bAreNum()
            throws ExecutionException, InterruptedException {
        // corresponds:
        // $a = 1; $b = 1.2;
        // $a = 1 + $b;
        // $b = 2 + $a;

        Map<String, List<IConstraint>> map = new HashMap<>();
        final IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloadA = createPartialAdditionWithInt("$b", scope);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloadB = createPartialAdditionWithInt("$a", scope);

        map.put("$a", asList(type(intType), intersect(overloadA)));
        map.put("$b", asList(type(floatType), intersect(overloadB)));
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
            }, 2, TimeUnit.SECONDS);

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
