/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration;

import ch.tsphp.common.IConstraint;
import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.constraints.ParentInclusionConstraint;
import ch.tsphp.tinsphp.inference_engine.constraints.RefParentInclusionConstraint;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeHelper;
import ch.tsphp.tinsphp.inference_engine.test.ActWithTimeout;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ATest;
import ch.tsphp.tinsphp.symbols.ModifierHelper;
import ch.tsphp.tinsphp.symbols.SymbolFactory;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConstraintSolverTest extends ATest
{
    private static ITypeSymbol mixedTypeSymbol;
    private static ITypeSymbol arrayTypeSymbol;
    private static ITypeSymbol scalarTypeSymbol;
    private static ITypeSymbol stringTypeSymbol;
    private static ITypeSymbol numTypeSymbol;
    private static ITypeSymbol floatTypeSymbol;
    private static ITypeSymbol intTypeSymbol;
    private static ITypeSymbol nothingTypeSymbol;

    private static ITypeSymbol interface1TypeSymbol;
    private static ITypeSymbol interface2TypeSymbol;
    private static ITypeSymbol fooTypeSymbol;


    @BeforeClass
    public static void init() {
        mixedTypeSymbol = mock(ITypeSymbol.class);

        arrayTypeSymbol = mock(ITypeSymbol.class);
        when(arrayTypeSymbol.getParentTypeSymbols()).thenReturn(set(mixedTypeSymbol));
        when(arrayTypeSymbol.getAbsoluteName()).thenReturn("array");

        scalarTypeSymbol = mock(ITypeSymbol.class);
        when(scalarTypeSymbol.getParentTypeSymbols()).thenReturn(set(mixedTypeSymbol));
        when(scalarTypeSymbol.getAbsoluteName()).thenReturn("scalar");

        stringTypeSymbol = mock(ITypeSymbol.class);
        when(stringTypeSymbol.getParentTypeSymbols()).thenReturn(set(scalarTypeSymbol));
        when(stringTypeSymbol.getAbsoluteName()).thenReturn("string");

        numTypeSymbol = mock(ITypeSymbol.class);
        when(numTypeSymbol.getParentTypeSymbols()).thenReturn(set(scalarTypeSymbol));
        when(numTypeSymbol.getAbsoluteName()).thenReturn("num");

        floatTypeSymbol = mock(ITypeSymbol.class);
        when(floatTypeSymbol.getParentTypeSymbols()).thenReturn(set(numTypeSymbol));
        when(floatTypeSymbol.getAbsoluteName()).thenReturn("float");

        intTypeSymbol = mock(ITypeSymbol.class);
        when(intTypeSymbol.getParentTypeSymbols()).thenReturn(set(numTypeSymbol));
        when(intTypeSymbol.getAbsoluteName()).thenReturn("int");

        nothingTypeSymbol = mock(ITypeSymbol.class);
        when(nothingTypeSymbol.getAbsoluteName()).thenReturn("nothing");


        interface1TypeSymbol = mock(ITypeSymbol.class);
        when(interface1TypeSymbol.getParentTypeSymbols()).thenReturn(set(mixedTypeSymbol));
        when(interface1TypeSymbol.getAbsoluteName()).thenReturn("IA");

        interface2TypeSymbol = mock(ITypeSymbol.class);
        when(interface2TypeSymbol.getParentTypeSymbols()).thenReturn(set(mixedTypeSymbol));
        when(interface2TypeSymbol.getAbsoluteName()).thenReturn("IB");

        fooTypeSymbol = mock(ITypeSymbol.class);
        when(fooTypeSymbol.getParentTypeSymbols()).thenReturn(set(interface1TypeSymbol, interface2TypeSymbol));
        when(fooTypeSymbol.getAbsoluteName()).thenReturn("Foo");
    }

    private static HashSet<ITypeSymbol> set(ITypeSymbol... symbols) {
        return new HashSet<>(Arrays.asList(symbols));
    }

    @Test
    public void resolveConstraints_Int_UnionContainsOnlyInt() {
        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("a", Arrays.asList(parent(intTypeSymbol)));
        IScope scope = createScopeWithConstraints(map);

        IConstraintSolver solver = createConstraintSolver();
        Map<String, IUnionTypeSymbol> result = solver.resolveConstraints(scope);

        assertThat(result.size(), is(1));
        assertThat(result, Matchers.hasKey("a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("int"));
    }

    @Test
    public void resolveConstraints_IntAndFloat_UnionContainsIntAndFloat() {
        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("a", Arrays.asList(parent(intTypeSymbol), parent(floatTypeSymbol)));
        IScope scope = createScopeWithConstraints(map);

        IConstraintSolver solver = createConstraintSolver();
        Map<String, IUnionTypeSymbol> result = solver.resolveConstraints(scope);

        assertThat(result.size(), is(1));
        assertThat(result, Matchers.hasKey("a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("int", "float"));
    }

    @Test
    public void resolveConstraints_IntAndNum_UnionContainsOnlyNum() {
        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("a", Arrays.asList(parent(intTypeSymbol), parent(numTypeSymbol)));
        IScope scope = createScopeWithConstraints(map);

        IConstraintSolver solver = createConstraintSolver();
        Map<String, IUnionTypeSymbol> result = solver.resolveConstraints(scope);

        assertThat(result.size(), is(1));
        assertThat(result, Matchers.hasKey("a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("num"));
    }

    @Test
    public void resolveConstraints_NumAndIntAndScalar_UnionContainsOnlyScalar() {
        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("a", Arrays.asList(parent(numTypeSymbol), parent(intTypeSymbol), parent(scalarTypeSymbol)));
        IScope scope = createScopeWithConstraints(map);

        IConstraintSolver solver = createConstraintSolver();
        Map<String, IUnionTypeSymbol> result = solver.resolveConstraints(scope);

        assertThat(result.size(), is(1));
        assertThat(result, Matchers.hasKey("a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("scalar"));
    }

    @Test
    public void resolveConstraints_RefConstraint_UnionContainsAllTypesOfRef() {
        Map<String, List<IConstraint>> refMap = new HashMap<>();
        refMap.put("b", Arrays.asList(parent(intTypeSymbol), parent(fooTypeSymbol), parent(arrayTypeSymbol)));
        IScope refScope = createScopeWithConstraints(refMap);

        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("a", Arrays.asList(refParent("b", refScope)));
        IScope scope = createScopeWithConstraints(map);

        //act
        IConstraintSolver solver = createConstraintSolver();
        Map<String, IUnionTypeSymbol> result = solver.resolveConstraints(scope);

        assertThat(result.size(), is(1));
        assertThat(result, Matchers.hasKey("a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("int", "Foo", "array"));
    }

    @Test
    public void resolveConstraints_RefConstraintInclIntAndInt_UnionContainsAllTypesOfRefAndIntOnlyOnce() {
        Map<String, List<IConstraint>> refMap = new HashMap<>();
        refMap.put("b", Arrays.asList(parent(intTypeSymbol), parent(fooTypeSymbol), parent(arrayTypeSymbol)));
        IScope refScope = createScopeWithConstraints(refMap);

        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("a", Arrays.asList(refParent("b", refScope), parent(intTypeSymbol)));
        IScope scope = createScopeWithConstraints(map);

        //act
        IConstraintSolver solver = createConstraintSolver();
        Map<String, IUnionTypeSymbol> result = solver.resolveConstraints(scope);

        assertThat(result.size(), is(1));
        assertThat(result, Matchers.hasKey("a"));
        Map<String, ITypeSymbol> typesInUnion = result.get("a").getTypeSymbols();
        assertThat(typesInUnion.keySet(), containsInAnyOrder("int", "Foo", "array"));
    }

    @Test
    public void resolveConstraints_CircleRefConstraint_UnionContainsAllTypesOfRefDoesTerminate()
            throws ExecutionException, InterruptedException {
        Map<String, List<IConstraint>> refMap = new HashMap<>();
        List<IConstraint> bConstraints = new ArrayList<>();
        bConstraints.addAll(Arrays.asList(parent(intTypeSymbol), parent(fooTypeSymbol), parent(arrayTypeSymbol)));
        refMap.put("b", bConstraints);
        IScope refScope = createScopeWithConstraints(refMap);

        Map<String, List<IConstraint>> map = new HashMap<>();
        map.put("a", Arrays.asList(refParent("b", refScope), parent(intTypeSymbol)));
        final IScope scope = createScopeWithConstraints(map);

        //a points to b and b points to a
        bConstraints.add(refParent("a", scope));

        try {
            //act
            Map<String, IUnionTypeSymbol> result = ActWithTimeout.exec(new Callable<Map<String, IUnionTypeSymbol>>()
            {
                public Map<String, IUnionTypeSymbol> call() {
                    IConstraintSolver solver = createConstraintSolver();
                    return solver.resolveConstraints(scope);
                }
            }, 2, TimeUnit.SECONDS);

            //assert
            assertThat(result.size(), is(1));
            assertThat(result, Matchers.hasKey("a"));
            Map<String, ITypeSymbol> typesInUnion = result.get("a").getTypeSymbols();
            assertThat(typesInUnion.keySet(), containsInAnyOrder("int", "Foo", "array"));
        } catch (TimeoutException e) {
            fail("Did not terminate after 2 seconds, most probably endless loop");
        }
    }

    private IConstraint refParent(String refVariableName, IScope refScope) {
        return new RefParentInclusionConstraint(refVariableName, refScope);
    }

    private IConstraint parent(ITypeSymbol typeSymbol) {
        return new ParentInclusionConstraint(typeSymbol);
    }

    private IScope createScopeWithConstraints(final Map<String, List<IConstraint>> map) {
        IScope scope = mock(IScope.class);
        when(scope.getConstraints()).thenReturn(map);
        when(scope.getConstraintsForVariable(anyString())).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String variableName = (String) invocationOnMock.getArguments()[0];
                return map.get(variableName);
            }
        });
        return scope;
    }

    private IConstraintSolver createConstraintSolver() {
        return createConstraintSolver(nothingTypeSymbol, new SymbolFactory(new ScopeHelper(), new ModifierHelper()));
    }

    protected IConstraintSolver createConstraintSolver(
            ITypeSymbol theNothingTypeSymbol, ISymbolFactory theSymbolFactory) {
        return new ConstraintSolver(theNothingTypeSymbol, theSymbolFactory);
    }
}
