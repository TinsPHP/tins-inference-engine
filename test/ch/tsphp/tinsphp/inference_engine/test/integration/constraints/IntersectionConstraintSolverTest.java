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

public class IntersectionConstraintSolverTest extends AConstraintSolverTest
{

    @Test
    public void resolveConstraints_OneOverloadAndFits_UnionContainsCorrespondingType() {
        // corresponds:
        // $b = 1;
        // $a = $b + 1;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = asList(
                entry(asList(refType("$b", scope, varAst(intType))), intType)
        );
        map.put("$a", asList(intersect(overloads)));
        map.put("$b", asList(type(intType)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("int"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int"));
    }

    @Test
    public void resolveConstraints_MultipleOverloadsAndFloatFits_$aIsFloat() {
        // corresponds:
        // $b = 1.2;
        // $a = $b + 1.2;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = asList(
                entry(asList(refType("$b", scope, varAst(intType))), intType),
                entry(asList(refType("$b", scope, varAst(floatType))), floatType),
                entry(asList(refType("$b", scope, varAst(numType))), numType),
                entry(asList(refType("$b", scope, varAst(numType, true))), numType),
                entry(asList(refType("$b", scope, varAst(arrayType))), arrayType)
        );
        map.put("$a", asList(intersect(overloads)));
        map.put("$b", asList(type(floatType)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("float"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }

    @Test
    public void resolveConstraints_MultipleOverloadsAndArrayFits_$aIsFloat() {
        // corresponds:
        // $b = [1];
        // $a = $b + [2, 3];

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = asList(
                entry(asList(refType("$b", scope, varAst(intType))), intType),
                entry(asList(refType("$b", scope, varAst(floatType))), floatType),
                entry(asList(refType("$b", scope, varAst(numType))), numType),
                entry(asList(refType("$b", scope, varAst(numType, true))), numType),
                entry(asList(refType("$b", scope, varAst(arrayType))), arrayType)
        );
        map.put("$b", asList(type(arrayType)));
        map.put("$a", asList(intersect(overloads)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat(result.size(), is(2));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("array"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("array"));
    }

    @Test
    public void resolveConstraints_MultipleOverloadsMultipleParametersAndNumFits_$aIsNum() {
        // corresponds:
        // $b = 1;
        // $c = 1.2;
        // $a = $b + $c;

        Map<String, List<IConstraint>> map = new HashMap<>();
        IScope scope = createScopeWithConstraints(map);
        List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads = asList(
                entry(asList(refType("$b", scope, intType), refType("$c", scope, intType)), intType),
                entry(asList(refType("$b", scope, floatType), refType("$c", scope, floatType)), floatType),
                entry(asList(refType("$b", scope, numType), refType("$c", scope, numType)), numType),
                entry(
                        asList(
                                refType("$b", scope, varAst(numType, true)),
                                refType("$c", scope, varAst(numType, true))
                        ), numType),
                entry(asList(refType("$b", scope, arrayType), refType("$c", scope, arrayType)), arrayType)
        );
        map.put("$b", asList(type(intType)));
        map.put("$c", asList(type(floatType)));
        map.put("$a", asList(intersect(overloads)));
        Map<String, IUnionTypeSymbol> result = createResolvingResult(scope);

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(scope);

        assertThat(result.size(), is(3));
        assertThat(result, hasKey("$a"));
        assertThat(result, hasKey("$b"));
        assertThat(result, hasKey("$c"));
        assertThat(result.get("$a").getTypeSymbols().keySet(), containsInAnyOrder("num"));
        assertThat(result.get("$b").getTypeSymbols().keySet(), containsInAnyOrder("int"));
        assertThat(result.get("$c").getTypeSymbols().keySet(), containsInAnyOrder("float"));
    }
}
