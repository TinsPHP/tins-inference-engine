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
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IIntersectionConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.symbols.IOverloadSymbol;
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

import java.util.List;

import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingMatcher.varBinding;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingMatcher.withVariableBindings;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConstraintSolverTest
{
    private static IOverloadResolver overloadResolver;
    private static ISymbolFactory symbolFactory;
    private static ICore core;

    @BeforeClass
    public static void init() {
        overloadResolver = new OverloadResolver();
        symbolFactory = new SymbolFactory(new ScopeHelper(), new ModifierHelper(), overloadResolver);
        core = new Core(symbolFactory, overloadResolver, new AstHelper(new TSPHPAstAdaptor()));
    }

    @Test
    public void solveConstraints_Addition_HasThreeOverloads() {
        //corresponds to: function foo($x, $y){ return $x + $y; }
        IConstraintCollection collection = mock(IConstraintCollection.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable $y = var("$y");
        when(collection.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, $y), core.getOperators().get(TokenTypes.Plus))
        ));


        IConstraintSolver solver = createConstraintSolver();
        List<IBinding> bindings = solver.solveConstraints(collection);


        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", asList("@T1"), asList("num")),
                varBinding("$y", "T1", asList("@T1"), asList("num")),
                varBinding("rtn", "T1", asList("@T1"), asList("num"))
        )));
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T2", null, asList("bool")),
                varBinding("$y", "T3", null, asList("bool")),
                varBinding("rtn", "T1", asList("int"), null)
        )));
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T2", null, asList("array")),
                varBinding("$y", "T3", null, asList("array")),
                varBinding("rtn", "T1", asList("array"), null)
        )));
        assertThat(bindings, hasSize(3));
    }

    @Test
    public void solveConstraints_PartialAdditionWithInt_HasOneOverload() {
        //corresponds to: function foo($x){ return $x + 1; }
        IConstraintCollection collection = mock(IConstraintCollection.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable e1 = var("e1");
        when(e1.getType()).thenReturn(core.getPrimitiveTypes().get(PrimitiveTypeNames.INT));
        when(collection.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, e1), core.getOperators().get(TokenTypes.Plus))
        ));

        IConstraintSolver solver = createConstraintSolver();
        List<IBinding> bindings = solver.solveConstraints(collection);

        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", asList("int"), asList("num")),
                varBinding("e1", "T1", asList("int"), asList("num")),
                varBinding("rtn", "T1", asList("int"), asList("num"))
        )));
        assertThat(bindings, hasSize(1));
    }

    @Test
    public void solveConstraints_PartialAdditionWithFloat_HasOneOverload() {
        //corresponds to: function foo($x){ return $x + 1.4; }
        IConstraintCollection collection = mock(IConstraintCollection.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable e1 = var("e1");
        when(e1.getType()).thenReturn(core.getPrimitiveTypes().get(PrimitiveTypeNames.FLOAT));
        when(collection.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, e1), core.getOperators().get(TokenTypes.Plus))
        ));


        IConstraintSolver solver = createConstraintSolver();
        List<IBinding> bindings = solver.solveConstraints(collection);


        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", asList("float"), asList("num")),
                varBinding("e1", "T1", asList("float"), asList("num")),
                varBinding("rtn", "T1", asList("float"), asList("num"))
        )));
        assertThat(bindings, hasSize(1));
    }

    @Test
    public void solveConstraints_PartialAdditionWithNum_HasOneOverload() {
        //corresponds to: function foo($x){ return $x + 1.4 + 1; }
        IConstraintCollection collection = mock(IConstraintCollection.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable e1 = var("e1");
        when(e1.getType()).thenReturn(core.getPrimitiveTypes().get(PrimitiveTypeNames.NUM));
        when(collection.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, e1), core.getOperators().get(TokenTypes.Plus))
        ));

        IConstraintSolver solver = createConstraintSolver();
        List<IBinding> bindings = solver.solveConstraints(collection);

        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", asList("num"), asList("num")),
                varBinding("e1", "T1", asList("num"), asList("num")),
                varBinding("rtn", "T1", asList("num"), asList("num"))
        )));
        assertThat(bindings, hasSize(1));
    }

    @Test
    public void solveConstraints_PartialAdditionWithArray_HasOneOverload() {
        //corresponds to: function foo($x){ return $x + []; }
        IConstraintCollection collection = mock(IConstraintCollection.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable e1 = var("e1");
        when(e1.getType()).thenReturn(core.getPrimitiveTypes().get(PrimitiveTypeNames.ARRAY));
        when(collection.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, e1), core.getOperators().get(TokenTypes.Plus))
        ));

        IConstraintSolver solver = createConstraintSolver();
        List<IBinding> bindings = solver.solveConstraints(collection);

        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T2", null, asList("array")),
                varBinding("e1", "T3", asList("array"), asList("array")),
                varBinding("rtn", "T1", asList("array"), null)
        )));
        assertThat(bindings, hasSize(1));
    }

    @Test
    public void solveConstraints_PartialAdditionWithBool_HasOneOverload() {
        //corresponds to: function foo($x){ return $x + true; }
        IConstraintCollection collection = mock(IConstraintCollection.class);
        IVariable rtn = var("rtn");
        IVariable $x = var("$x");
        IVariable e1 = var("e1");
        when(e1.getType()).thenReturn(core.getPrimitiveTypes().get(PrimitiveTypeNames.BOOL));
        when(collection.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList($x, e1), core.getOperators().get(TokenTypes.Plus))
        ));

        IConstraintSolver solver = createConstraintSolver();
        List<IBinding> bindings = solver.solveConstraints(collection);

        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T2", null, asList("bool")),
                varBinding("e1", "T3", asList("bool"), asList("bool")),
                varBinding("rtn", "T1", asList("int"), null)
        )));
        assertThat(bindings, hasSize(1));
    }

    @Test
    public void solveConstraints_ThreePlus_HasThreeOverloads() {
        //corresponds to: function foo($x, $y, $z){ return $x + $y + $z; }
        IConstraintCollection collection = mock(IConstraintCollection.class);
        IVariable rtn = var("rtn"); //e1 + $z
        IVariable $x = var("$x");
        IVariable $y = var("$y");
        IVariable $z = var("$z");
        IVariable e1 = var("e1"); //$x + $y
        when(collection.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList(e1, $z), core.getOperators().get(TokenTypes.Plus)),
                intersect(e1, asList($x, $y), core.getOperators().get(TokenTypes.Plus))
        ));


        IConstraintSolver solver = createConstraintSolver();
        List<IBinding> bindings = solver.solveConstraints(collection);


        List<String> num = asList("num");
        List<String> t1 = asList("@T1");
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", t1, num),
                varBinding("$y", "T1", t1, num),
                varBinding("$z", "T1", t1, num),
                varBinding("e1", "T1", t1, num),
                varBinding("rtn", "T1", t1, num)
        )));
        List<String> intAndT1 = asList("int", "@T1");
        List<String> bool = asList("bool");
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T4", null, bool),
                varBinding("$y", "T5", null, bool),
                varBinding("$z", "T1", intAndT1, num),
                varBinding("e1", "T1", intAndT1, num),
                varBinding("rtn", "T1", intAndT1, num)
        )));
        List<String> array = asList("array");
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T4", null, array),
                varBinding("$y", "T5", null, array),
                varBinding("$z", "T3", null, array),
                varBinding("e1", "T2", array, array),
                varBinding("rtn", "T1", array, null)
        )));
    }

    @Test
    public void solveConstraints_MultiplePlusMinusAndMultiple_HasThreeOverloads() {
        //corresponds to: function foo($x, $y, $a, $b){ return $a * ($x + $y) - $a * $b; }
        IConstraintCollection collection = mock(IConstraintCollection.class);
        IVariable rtn = var("rtn"); //e3 - e2
        IVariable $x = var("$x");
        IVariable $y = var("$y");
        IVariable $a = var("$a");
        IVariable $b = var("$b");
        IVariable e1 = var("e1"); //$x + $y
        IVariable e2 = var("e2"); //$a * $b
        IVariable e3 = var("e3"); //$a * e1
        when(collection.getLowerBoundConstraints()).thenReturn(asList(
                intersect(rtn, asList(e3, e2), core.getOperators().get(TokenTypes.Minus)),
                intersect(e3, asList($a, e1), core.getOperators().get(TokenTypes.Multiply)),
                intersect(e1, asList($x, $y), core.getOperators().get(TokenTypes.Plus)),
                intersect(e2, asList($a, $b), core.getOperators().get(TokenTypes.Multiply))
        ));


        IConstraintSolver solver = createConstraintSolver();
        List<IBinding> bindings = solver.solveConstraints(collection);


        List<String> num = asList("num");
        List<String> t1 = asList("@T1");
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T1", t1, num),
                varBinding("$y", "T1", t1, num),
                varBinding("$a", "T1", t1, num),
                varBinding("$b", "T1", t1, num),
                varBinding("e1", "T1", t1, num),
                varBinding("e2", "T1", t1, num),
                varBinding("e3", "T1", t1, num),
                varBinding("rtn", "T1", t1, num)
        )));
        List<String> intAndT1 = asList("int", "@T1");
        List<String> bool = asList("bool");
        assertThat(bindings, hasItem(withVariableBindings(
                varBinding("$x", "T6", null, bool),
                varBinding("$y", "T7", null, bool),
                varBinding("$a", "T1", intAndT1, num),
                varBinding("$b", "T1", intAndT1, num),
                varBinding("e1", "T1", intAndT1, num),
                varBinding("e2", "T1", intAndT1, num),
                varBinding("e3", "T1", intAndT1, num),
                varBinding("rtn", "T1", intAndT1, num)
        )));
    }

//    @Test
//    public void solveConstraints_Division_HasTwoOverloads() {
//        //corresponds to: function foo($x, $y){ return $x / $y; }
//        IConstraintCollection collection = mock(IConstraintCollection.class);
//        IVariable rtn = var("rtn");
//        IVariable $x = var("$x");
//        IVariable $y = var("$y");
//        when(collection.getLowerBoundConstraints()).thenReturn(asList(
//                intersect(rtn, asList($x, $y), core.getOperators().get(TokenTypes.Divide))
//        ));
//
//
//        IConstraintSolver solver = createConstraintSolver();
//        List<IBinding> bindings = solver.solveConstraints(collection);
//
//
//        assertThat(bindings, hasItem(withVariableBindings(
//                vars("$x", "$y", "rtn"),
//                typeVars("T2", "T3", "T1"),
//                lowerConstraints(asList("bool"), asList("bool"), asList("{int V false}")),
//                upperConstraints(asList("bool"), asList("bool"), asList("{int V false}"))
//        )));
//        assertThat(bindings, hasItem(withVariableBindings(
//                vars("$x", "$y", "rtn"),
//                typeVars("T2", "T2", "T1"),
//                lowerConstraints(asList("float"), asList("float"), asList("@T2", "false")),
//                upperConstraints(asList("num"), asList("num"), null)
//        )));
//    }

    private IVariable var(String name) {
        IVariable mock = mock(IVariable.class);
        when(mock.getAbsoluteName()).thenReturn(name);
        return mock;
    }

    private IIntersectionConstraint intersect(
            IVariable returnVariable, List<IVariable> arguments, IOverloadSymbol overloads) {
        return new IntersectionConstraint(returnVariable, arguments, overloads.getOverloads());
    }

    private IConstraintSolver createConstraintSolver() {
        return createConstraintSolver(symbolFactory, overloadResolver);
    }

    private IConstraintSolver createConstraintSolver(
            ISymbolFactory theSymbolFactory, IOverloadResolver theOverloadResolver) {
        return new ConstraintSolver(theSymbolFactory, theOverloadResolver);
    }
}

