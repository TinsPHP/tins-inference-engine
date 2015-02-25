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
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.AConstraintSolverTest;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;

public class TypeConstraintSolverTest extends AConstraintSolverTest
{

    @Test
    public void resolveConstraints_Int_UnionContainsOnlyInt() {
        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("$a", asList(type(intType)));
        IScope scope = createScopeWithConstraints(map);
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(1));
        assertThat(result, hasKey("$a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("$a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("int"));
    }

    @Test
    public void resolveConstraints_IntAndFloat_UnionContainsIntAndFloat() {
        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("$a", asList(type(intType), type(floatType)));
        IScope scope = createScopeWithConstraints(map);
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(1));
        assertThat(result, hasKey("$a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("$a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("int", "float"));
    }

    @Test
    public void resolveConstraints_IntAndNum_UnionContainsOnlyNum() {
        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("$a", asList(type(intType), type(numType)));
        IScope scope = createScopeWithConstraints(map);
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(1));
        assertThat(result, hasKey("$a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("$a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("num"));
    }

    @Test
    public void resolveConstraints_NumAndIntAndScalar_UnionContainsOnlyScalar() {
        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("$a", asList(type(numType), type(intType), type(scalarType)));
        IScope scope = createScopeWithConstraints(map);
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraintsOfScope(scope);

        assertThat(result.size(), is(1));
        assertThat(result, hasKey("$a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("$a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("scalar"));
    }
}
