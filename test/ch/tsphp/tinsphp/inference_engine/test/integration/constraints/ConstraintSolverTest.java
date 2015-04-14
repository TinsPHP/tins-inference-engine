/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.constraints;


import ch.tsphp.common.AstHelper;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.inference.constraints.IBinding;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IIntersectionConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.core.Core;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeHelper;
import ch.tsphp.tinsphp.symbols.ModifierHelper;
import ch.tsphp.tinsphp.symbols.PrimitiveTypeNames;
import ch.tsphp.tinsphp.symbols.SymbolFactory;
import ch.tsphp.tinsphp.symbols.constraints.IntersectionConstraint;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;
import ch.tsphp.tinsphp.symbols.utils.OverloadResolver;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingsMatcher.varBinding;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingsMatcher.withVariableBindings;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class ConstraintSolverTest
{
    private static IOverloadResolver overloadResolver;
    private static ICore core;

    @BeforeClass
    public static void init() {
        overloadResolver = new OverloadResolver();
        ISymbolFactory symbolFactory = new SymbolFactory(new ScopeHelper(), new ModifierHelper(), overloadResolver);
        core = new Core(symbolFactory, overloadResolver, new AstHelper(new TSPHPAstAdaptor()));
    }

    @Test
    public void solveConstraints_Addition_HasThreeOverloads() {
        //corresponds to: function foo($x, $y){ return $x + $y; }
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable $y = var("$y");
        when(methodSymbol.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, $y), core.getOperators().get(TokenTypes.Plus))
        ));

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(asList(methodSymbol));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IBinding> bindings = captor.getValue();
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", asList("@T1"), asList("num"), false),
                varBinding("$y", "T1", asList("@T1"), asList("num"), false),
                varBinding("rtn", "T1", asList("@T1"), asList("num"), false)
        )));
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T2", null, asList("bool"), false),
                varBinding("$y", "T3", null, asList("bool"), false),
                varBinding("rtn", "T1", asList("int"), null, true)
        )));
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T2", null, asList("array"), false),
                varBinding("$y", "T3", null, asList("array"), false),
                varBinding("rtn", "T1", asList("array"), null, true)
        )));
        assertThat(bindings, hasSize(3));
    }

    @Test
    public void solveConstraints_PartialAdditionWithInt_HasOneOverload() {
        //corresponds to: function foo($x){ return $x + 1; }
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable e1 = var("1@1|0");
        when(e1.getType()).thenReturn(core.getPrimitiveTypes().get(PrimitiveTypeNames.INT));
        when(methodSymbol.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, e1), core.getOperators().get(TokenTypes.Plus))
        ));

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(asList(methodSymbol));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IBinding> bindings = captor.getValue();
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", asList("int"), asList("num"), false),
                varBinding("1@1|0", "T1", asList("int"), asList("num"), true),
                varBinding("rtn", "T1", asList("int"), asList("num"), false)
        )));
        assertThat(bindings, hasSize(1));
    }

    @Test
    public void solveConstraints_PartialAdditionWithFloat_HasOneOverload() {
        //corresponds to: function foo($x){ return $x + 1.4; }
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable e1 = var("1.4@1|0");
        when(e1.getType()).thenReturn(core.getPrimitiveTypes().get(PrimitiveTypeNames.FLOAT));
        when(methodSymbol.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, e1), core.getOperators().get(TokenTypes.Plus))
        ));


        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(asList(methodSymbol));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IBinding> bindings = captor.getValue();
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", asList("float"), asList("num"), false),
                varBinding("1.4@1|0", "T1", asList("float"), asList("num"), true),
                varBinding("rtn", "T1", asList("float"), asList("num"), false)
        )));
        assertThat(bindings, hasSize(1));
    }

    @Test
    public void solveConstraints_PartialAdditionWithNum_HasOneOverload() {
        //corresponds to: function foo($x){ return $x + 1.4 + 1; }
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable e1 = var("e1");
        when(e1.getType()).thenReturn(core.getPrimitiveTypes().get(PrimitiveTypeNames.NUM));
        when(methodSymbol.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, e1), core.getOperators().get(TokenTypes.Plus))
        ));

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(asList(methodSymbol));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IBinding> bindings = captor.getValue();
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", asList("num"), asList("num"), false),
                varBinding("e1", "T1", asList("num"), asList("num"), true),
                varBinding("rtn", "T1", asList("num"), asList("num"), false)
        )));
        assertThat(bindings, hasSize(1));
    }

    @Test
    public void solveConstraints_PartialAdditionWithArray_HasOneOverload() {
        //corresponds to: function foo($x){ return $x + []; }
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable e1 = var("[]@1|0");
        when(e1.getType()).thenReturn(core.getPrimitiveTypes().get(PrimitiveTypeNames.ARRAY));
        when(methodSymbol.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, e1), core.getOperators().get(TokenTypes.Plus))
        ));

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(asList(methodSymbol));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IBinding> bindings = captor.getValue();
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T2", null, asList("array"), false),
                varBinding("[]@1|0", "T3", asList("array"), asList("array"), true),
                varBinding("rtn", "T1", asList("array"), null, true)
        )));
        assertThat(bindings, hasSize(1));
    }

    @Test
    public void solveConstraints_PartialAdditionWithBool_HasOneOverload() {
        //corresponds to: function foo($x){ return $x + true; }
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable e1 = var("true@1|0");
        when(e1.getType()).thenReturn(core.getPrimitiveTypes().get(PrimitiveTypeNames.BOOL));
        when(methodSymbol.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, e1), core.getOperators().get(TokenTypes.Plus))
        ));

        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(asList(methodSymbol));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IBinding> bindings = captor.getValue();
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T2", null, asList("bool"), false),
                varBinding("true@1|0", "T3", asList("bool"), asList("bool"), true),
                varBinding("rtn", "T1", asList("int"), null, true)
        )));
        assertThat(bindings, hasSize(1));
    }

    @Test
    public void solveConstraints_ThreePlus_HasThreeOverloads() {
        //corresponds to: function foo($x, $y, $z){ return $x + $y + $z; }
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        IVariable rtn = var("rtn"); //e1 + $z
        IVariable $x = var("$x");
        IVariable $y = var("$y");
        IVariable $z = var("$z");
        IVariable e1 = var("e1"); //$x + $y
        when(methodSymbol.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList(e1, $z), core.getOperators().get(TokenTypes.Plus)),
                intersect(e1, asList($x, $y), core.getOperators().get(TokenTypes.Plus))
        ));


        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(asList(methodSymbol));


        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IBinding> bindings = captor.getValue();
        List<String> num = asList("num");
        List<String> t1 = asList("@T1");
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", t1, num, false),
                varBinding("$y", "T1", t1, num, false),
                varBinding("$z", "T1", t1, num, false),
                varBinding("e1", "T1", t1, num, false),
                varBinding("rtn", "T1", t1, num, false)
        )));
        List<String> intAndT1 = asList("int", "@T1");
        List<String> bool = asList("bool");
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T4", null, bool, false),
                varBinding("$y", "T5", null, bool, false),
                varBinding("$z", "T1", intAndT1, num, false),
                varBinding("e1", "T1", intAndT1, num, true),
                varBinding("rtn", "T1", intAndT1, num, false)
        )));
        List<String> array = asList("array");
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T4", null, array, false),
                varBinding("$y", "T5", null, array, false),
                varBinding("$z", "T3", null, array, false),
                varBinding("e1", "T2", array, array, true),
                varBinding("rtn", "T1", array, null, true)
        )));
    }

    @Test
    public void solveConstraints_MultiplePlusMinusAndMultiple_HasThreeOverloads() {
        //corresponds to: function foo($x, $y, $a, $b){ return $a * ($x + $y) - $a * $b; }
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        IVariable rtn = var("rtn"); //e3 - e2
        IVariable $x = var("$x");
        IVariable $y = var("$y");
        IVariable $a = var("$a");
        IVariable $b = var("$b");
        IVariable e1 = var("e1"); //$x + $y
        IVariable e2 = var("e2"); //$a * $b
        IVariable e3 = var("e3"); //$a * e1
        when(methodSymbol.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList(e3, e2), core.getOperators().get(TokenTypes.Minus)),
                intersect(e3, asList($a, e1), core.getOperators().get(TokenTypes.Multiply)),
                intersect(e1, asList($x, $y), core.getOperators().get(TokenTypes.Plus)),
                intersect(e2, asList($a, $b), core.getOperators().get(TokenTypes.Multiply))
        ));


        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(asList(methodSymbol));


        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IBinding> bindings = captor.getValue();
        List<String> num = asList("num");
        List<String> t1 = asList("@T1");
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", t1, num, false),
                varBinding("$y", "T1", t1, num, false),
                varBinding("$a", "T1", t1, num, false),
                varBinding("$b", "T1", t1, num, false),
                varBinding("e1", "T1", t1, num, false),
                varBinding("e2", "T1", t1, num, false),
                varBinding("e3", "T1", t1, num, false),
                varBinding("rtn", "T1", t1, num, false)
        )));
        List<String> intAndT1 = asList("int", "@T1");
        List<String> bool = asList("bool");
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T6", null, bool, false),
                varBinding("$y", "T7", null, bool, false),
                varBinding("$a", "T1", intAndT1, num, false),
                varBinding("$b", "T1", intAndT1, num, false),
                varBinding("e1", "T1", intAndT1, num, true),
                varBinding("e2", "T1", intAndT1, num, false),
                varBinding("e3", "T1", intAndT1, num, false),
                varBinding("rtn", "T1", intAndT1, num, false)
        )));
    }

    @Test
    public void solveConstraints_Division_HasTwoOverloads() {
        //corresponds to: function foo($x, $y){ return $x / $y; }
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable $y = var("$y");
        when(methodSymbol.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, $y), core.getOperators().get(TokenTypes.Divide))
        ));


        IConstraintSolver solver = createConstraintSolver();
        solver.solveConstraints(asList(methodSymbol));


        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IBinding> bindings = captor.getValue();
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T2", null, asList("bool"), false),
                varBinding("$y", "T3", null, asList("bool"), false),
                varBinding("rtn", "T1", asList("(int | false)"), null, true)
        )));
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T2", asList("float"), asList("num"), false),
                varBinding("$y", "T2", asList("float"), asList("num"), false),
                varBinding("rtn", "T1", asList("@T2", "false"), null, false)
        )));
        assertThat(bindings, hasSize(2));
    }

    private IVariable var(String name) {
        IVariable mock = mock(IVariable.class);
        when(mock.getAbsoluteName()).thenReturn(name);
        return mock;
    }

    private IIntersectionConstraint intersect(
            IVariable returnVariable, List<IVariable> arguments, IMinimalMethodSymbol overloads) {
        return new IntersectionConstraint(returnVariable, arguments, overloads);
    }

    private IConstraintSolver createConstraintSolver() {
        return createConstraintSolver(overloadResolver);
    }

    private IConstraintSolver createConstraintSolver(IOverloadResolver theOverloadResolver) {
        return new ConstraintSolver(theOverloadResolver);
    }
}

