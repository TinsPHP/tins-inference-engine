/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintSolver;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConstraintSolverTest
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

        scalarTypeSymbol = mock(ITypeSymbol.class);
        when(scalarTypeSymbol.getParentTypeSymbols()).thenReturn(set(mixedTypeSymbol));

        stringTypeSymbol = mock(ITypeSymbol.class);
        when(stringTypeSymbol.getParentTypeSymbols()).thenReturn(set(scalarTypeSymbol));

        numTypeSymbol = mock(ITypeSymbol.class);
        when(numTypeSymbol.getParentTypeSymbols()).thenReturn(set(scalarTypeSymbol));

        floatTypeSymbol = mock(ITypeSymbol.class);
        when(floatTypeSymbol.getParentTypeSymbols()).thenReturn(set(numTypeSymbol));

        intTypeSymbol = mock(ITypeSymbol.class);
        when(intTypeSymbol.getParentTypeSymbols()).thenReturn(set(numTypeSymbol));

        nothingTypeSymbol = mock(ITypeSymbol.class);


        interface1TypeSymbol = mock(ITypeSymbol.class);
        when(interface1TypeSymbol.getParentTypeSymbols()).thenReturn(set(mixedTypeSymbol));

        interface2TypeSymbol = mock(ITypeSymbol.class);
        when(interface2TypeSymbol.getParentTypeSymbols()).thenReturn(set(mixedTypeSymbol));

        fooTypeSymbol = mock(ITypeSymbol.class);
        when(fooTypeSymbol.getParentTypeSymbols()).thenReturn(set(interface1TypeSymbol, interface2TypeSymbol));
    }

    private static HashSet<ITypeSymbol> set(ITypeSymbol... symbols) {
        return new HashSet<>(Arrays.asList(symbols));
    }

    @Test
    public void getPromotionLevelFromTo_IntToInt_Returns0() {
        //no arrange necessary

        IConstraintSolver solver = createConstraintSolver();
        int result = solver.getPromotionLevelFromTo(intTypeSymbol, intTypeSymbol);

        assertThat(result, is(0));
    }

    @Test
    public void getPromotionLevelFromTo_IntToNum_Returns1() {
        //no arrange necessary

        IConstraintSolver solver = createConstraintSolver();
        int result = solver.getPromotionLevelFromTo(intTypeSymbol, numTypeSymbol);

        assertThat(result, is(1));
    }

    @Test
    public void getPromotionLevelFromTo_IntToString_ReturnsMinus1() {
        //no arrange necessary

        IConstraintSolver solver = createConstraintSolver();
        int result = solver.getPromotionLevelFromTo(intTypeSymbol, stringTypeSymbol);

        assertThat(result, is(-1));
    }

    @Test
    public void getPromotionLevelFromTo_IntToScalar_Returns2() {
        //no arrange necessary

        IConstraintSolver solver = createConstraintSolver();
        int result = solver.getPromotionLevelFromTo(intTypeSymbol, scalarTypeSymbol);

        assertThat(result, is(2));
    }

    @Test
    public void getPromotionLevelFromTo_IntToMixed_Returns3() {
        //no arrange necessary

        IConstraintSolver solver = createConstraintSolver();
        int result = solver.getPromotionLevelFromTo(intTypeSymbol, mixedTypeSymbol);

        assertThat(result, is(3));
    }

    @Test
    public void getPromotionLevelFromTo_NumToInt_ReturnsMinus1() {
        //no arrange necessary

        IConstraintSolver solver = createConstraintSolver();
        int result = solver.getPromotionLevelFromTo(numTypeSymbol, intTypeSymbol);

        assertThat(result, is(-1));
    }

    @Test
    public void getPromotionLevelFromTo_FooToInterface1_Returns1() {
        //no arrange necessary

        IConstraintSolver solver = createConstraintSolver();
        int result = solver.getPromotionLevelFromTo(fooTypeSymbol, interface1TypeSymbol);

        assertThat(result, is(1));
    }

    @Test
    public void getPromotionLevelFromTo_FooToInterface2_Returns1() {
        //no arrange necessary

        IConstraintSolver solver = createConstraintSolver();
        int result = solver.getPromotionLevelFromTo(fooTypeSymbol, interface2TypeSymbol);

        assertThat(result, is(1));
    }

    @Test
    public void getPromotionLevelFromTo_FooToMixed_Returns2() {
        //no arrange necessary

        IConstraintSolver solver = createConstraintSolver();
        int result = solver.getPromotionLevelFromTo(fooTypeSymbol, mixedTypeSymbol);

        assertThat(result, is(2));
    }

    private IConstraintSolver createConstraintSolver() {
        return createConstraintSolver(nothingTypeSymbol);
    }

    protected IConstraintSolver createConstraintSolver(ITypeSymbol theNothingTypeSymbol) {
        return new ConstraintSolver(theNothingTypeSymbol);
    }
}
