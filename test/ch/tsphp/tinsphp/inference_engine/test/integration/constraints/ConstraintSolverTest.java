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

import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingMatcher.isBinding;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingMatcher.lowerConstraints;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingMatcher.typeVars;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingMatcher.upperConstraints;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.BindingMatcher.vars;
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


        assertThat(bindings, hasItem(isBinding(
                vars("$x", "$y", "rtn"),
                typeVars("T1", "T1", "T1"),
                lowerConstraints(null, null, null),
                upperConstraints(asList("num"), asList("num"), asList("num"))
        )));
        assertThat(bindings, hasItem(isBinding(
                vars("$x", "$y", "rtn"),
                typeVars("T2", "T3", "T1"),
                lowerConstraints(asList("bool"), asList("bool"), asList("int")),
                upperConstraints(asList("bool"), asList("bool"), asList("int"))
        )));
        assertThat(bindings, hasItem(isBinding(
                vars("$x", "$y", "rtn"),
                typeVars("T2", "T3", "T1"),
                lowerConstraints(asList("array"), asList("array"), asList("array")),
                upperConstraints(asList("array"), asList("array"), asList("array"))
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


        assertThat(bindings, hasItem(isBinding(
                vars("$x", "e1", "rtn"),
                typeVars("T1", "T1", "T1"),
                lowerConstraints(asList("int"), asList("int"), asList("int")),
                upperConstraints(asList("num"), asList("num"), asList("num"))
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


        assertThat(bindings, hasItem(isBinding(
                vars("$x", "e1", "rtn"),
                typeVars("T1", "T1", "T1"),
                lowerConstraints(asList("float"), asList("float"), asList("float")),
                upperConstraints(asList("num"), asList("num"), asList("num"))
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


        assertThat(bindings, hasItem(isBinding(
                vars("$x", "e1", "rtn"),
                typeVars("T1", "T1", "T1"),
                lowerConstraints(asList("num"), asList("num"), asList("num")),
                upperConstraints(asList("num"), asList("num"), asList("num"))
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


        assertThat(bindings, hasItem(isBinding(
                vars("$x", "e1", "rtn"),
                typeVars("T2", "T3", "T1"),
                lowerConstraints(asList("array"), asList("array"), asList("array")),
                upperConstraints(asList("array"), asList("array"), asList("array"))
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


        assertThat(bindings, hasItem(isBinding(
                vars("$x", "e1", "rtn"),
                typeVars("T2", "T3", "T1"),
                lowerConstraints(asList("bool"), asList("bool"), asList("int")),
                upperConstraints(asList("bool"), asList("bool"), asList("int"))
        )));
        assertThat(bindings, hasSize(1));
    }

//    @Test
//    public void solveConstraints_MultiplePlusMinusAndMultiple_HasThreeOverloads() {
//        //corresponds to: function foo($x, $y, $a, $b){ return $a * ($x + $y) - $a * $b; }
//        IConstraintCollection collection = mock(IConstraintCollection.class);
//        IVariable rtn = var("rtn");
//        IVariable $x = var("$x");
//        IVariable $y = var("$y");
//        IVariable $a = var("$a");
//        IVariable $b = var("$b");
//        IVariable e1 = var("e1"); //$x + $y
//        IVariable e2 = var("e2"); //$a * $b
//        IVariable e3 = var("e3"); //e1 - e2
//        when(collection.getLowerBoundConstraints()).thenReturn(asList(
//                intersect(e1, asList($x, $y), core.getOperators().get(TokenTypes.Plus)),
//                intersect(e2, asList($a, $b), core.getOperators().get(TokenTypes.Multiply)),
//                intersect(e3, asList(e1, e2), core.getOperators().get(TokenTypes.Minus)),
//                intersect(rtn, asList($a, e3), core.getOperators().get(TokenTypes.Multiply))
//        ));
//
//
//        IConstraintSolver solver = createConstraintSolver();
//        List<IBinding> bindings = solver.solveConstraints(collection);
//
//
//        List<String> num = asList("num");
//        assertThat(bindings, hasItem(isBinding(
//                vars("$x", "$y", "$a", "$b", "e1", "e2", "e3", "rtn"),
//                typeVars("T1", "T1", "T1", "T1", "T1", "T1", "T1", "T1"),
//                lowerConstraints(null, null, null, null, null, null, null, null),
//                upperConstraints(num, num, num, num, num, num, num, num)
//        )));
//
//    }

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
//        assertThat(bindings, hasItem(isBinding(
//                vars("$x", "$y", "rtn"),
//                typeVars("T2", "T3", "T1"),
//                lowerConstraints(asList("bool"), asList("bool"), asList("{int V false}")),
//                upperConstraints(asList("bool"), asList("bool"), asList("{int V false}"))
//        )));
//        assertThat(bindings, hasItem(isBinding(
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

