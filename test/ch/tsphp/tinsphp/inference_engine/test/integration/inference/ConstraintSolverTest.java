/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.common.AstHelper;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.config.ICoreInitialiser;
import ch.tsphp.tinsphp.common.config.ISymbolsInitialiser;
import ch.tsphp.tinsphp.common.gen.TokenTypes;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.TypeVariableReference;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IConvertibleTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.PrimitiveTypeNames;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.core.config.HardCodedCoreInitialiser;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.issues.HardCodedIssueMessageProvider;
import ch.tsphp.tinsphp.inference_engine.issues.InferenceIssueReporter;
import ch.tsphp.tinsphp.symbols.config.HardCodedSymbolsInitialiser;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.tsphp.tinsphp.common.TinsPHPConstants.RETURN_VARIABLE_NAME;
import static ch.tsphp.tinsphp.core.StandardConstraintAndVariables.T_LHS;
import static ch.tsphp.tinsphp.core.StandardConstraintAndVariables.T_RETURN;
import static ch.tsphp.tinsphp.core.StandardConstraintAndVariables.T_RHS;
import static ch.tsphp.tinsphp.core.StandardConstraintAndVariables.VAR_LHS;
import static ch.tsphp.tinsphp.core.StandardConstraintAndVariables.VAR_RHS;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.OverloadBindingsMatcher.varBinding;
import static ch.tsphp.tinsphp.inference_engine.test.integration.testutils.OverloadBindingsMatcher.withVariableBindings;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConstraintSolverTest
{

    @Test
    public void solveMethod_XPlusYPlusZ_NumXAsNumXAsT4ReturnT4WhereT4SubNum() {
        ISymbolsInitialiser symbolsInitialiser = new HardCodedSymbolsInitialiser();
        ISymbolFactory symbolFactory = symbolsInitialiser.getSymbolFactory();
        ITypeHelper typeHelper = symbolsInitialiser.getTypeHelper();
        InferenceIssueReporter issueReporter = new InferenceIssueReporter(new HardCodedIssueMessageProvider());
        AstHelper astHelper = new AstHelper(new TSPHPAstAdaptor());
        ICoreInitialiser coreInitialiser = new HardCodedCoreInitialiser(astHelper, symbolsInitialiser);
        ICore core = coreInitialiser.getCore();
        Map<String, ITypeSymbol> primitiveTypes = core.getPrimitiveTypes();
        Map<Integer, IMinimalMethodSymbol> operators = core.getOperators();

        //  function foo($x, $y, $z){ return $x + $y + $z; }
        // where:
        //   T x {as T} -> T \ T <: num
        // was applied for e1 = $x + $y and
        //   {as T} x {as T} -> T \ T <: num
        // was applied for  e2 = e1 + $z

        //$x + $y
        //T x {as T} -> T \ T <: num
        IVariable e1 = symbolFactory.createVariable("e1");
        IVariableSymbol $x = mock(IVariableSymbol.class);
        when($x.getAbsoluteName()).thenReturn("$x");
        when($x.getName()).thenReturn("$x");
        IVariableSymbol $y = mock(IVariableSymbol.class);
        when($y.getAbsoluteName()).thenReturn("$y");
        when($y.getName()).thenReturn("$y");
        IOverloadBindings overloadBindings = symbolFactory.createOverloadBindings();
        String t1 = "T1";
        overloadBindings.addVariable(VAR_LHS, reference(t1));
        overloadBindings.addVariable(VAR_RHS, reference("T2"));
        overloadBindings.addVariable(RETURN_VARIABLE_NAME, reference(t1));
        //bind convertible type to Tlhs
        IConvertibleTypeSymbol asT1 = symbolFactory.createConvertibleTypeSymbol();
        overloadBindings.bind(asT1, Arrays.asList(t1));
        overloadBindings.addUpperTypeBound(t1, primitiveTypes.get(PrimitiveTypeNames.NUM));
        overloadBindings.addUpperTypeBound("T2", asT1);
        IVariable lhs = symbolFactory.createVariable(VAR_LHS);
        IVariable rhs = symbolFactory.createVariable(VAR_RHS);
        List<IVariable> binaryParameterIds = Arrays.asList(lhs, rhs);
        IFunctionType function = symbolFactory.createFunctionType("+", overloadBindings, binaryParameterIds);
        Set<String> set = new HashSet<>();
        set.add(t1);
        function.manuallySimplified(set, 0, true);
        IMinimalMethodSymbol minimalMethodSymbol1 = symbolFactory.createMinimalMethodSymbol("+");
        minimalMethodSymbol1.addOverload(function);
        IConstraint constraint1 = symbolFactory.createConstraint(
                mock(ITSPHPAst.class), e1, asList((IVariable) $x, $y), minimalMethodSymbol1);

        //e1 + $z
        //{as T} x {as T} -> T \ T <: num
        IVariableSymbol $z = mock(IVariableSymbol.class);
        when($z.getAbsoluteName()).thenReturn("$z");
        when($z.getName()).thenReturn("$z");
        IVariable e2 = symbolFactory.createVariable("e2");
        overloadBindings = symbolFactory.createOverloadBindings();
        overloadBindings.addVariable(VAR_LHS, reference(T_LHS));
        overloadBindings.addVariable(VAR_RHS, reference(T_RHS));
        overloadBindings.addVariable(RETURN_VARIABLE_NAME, reference(T_RETURN));
        //bind convertible type to T2
        IConvertibleTypeSymbol asTReturn = symbolFactory.createConvertibleTypeSymbol();
        overloadBindings.bind(asTReturn, Arrays.asList(T_RETURN));
        overloadBindings.addUpperTypeBound(T_LHS, asTReturn);
        overloadBindings.addUpperTypeBound(T_RHS, asTReturn);
        overloadBindings.addUpperTypeBound(T_RETURN, primitiveTypes.get(PrimitiveTypeNames.NUM));
        function = symbolFactory.createFunctionType("+", overloadBindings, binaryParameterIds);
        set = new HashSet<>();
        set.add(T_RETURN);
        function.manuallySimplified(set, 0, true);
        IMinimalMethodSymbol minimalMethodSymbol2 = symbolFactory.createMinimalMethodSymbol("+");
        minimalMethodSymbol2.addOverload(function);
        IConstraint constraint2
                = symbolFactory.createConstraint(mock(ITSPHPAst.class), e2, asList(e1, $z), minimalMethodSymbol2);

        //return e2
        //Tlhs x Trhs -> Tlhs \ Trhs <: Tlhs
        IVariable rtn = symbolFactory.createVariable(RETURN_VARIABLE_NAME);
        IConstraint constraint3 = symbolFactory.createConstraint(
                mock(ITSPHPAst.class), rtn, asList(rtn, e2), operators.get(TokenTypes.Assign));

        List<IConstraint> constraints = asList(constraint1, constraint2, constraint3);
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        when(methodSymbol.getConstraints()).thenReturn(constraints);
        when(methodSymbol.getParameters()).thenReturn(asList($x, $y, $z));
        when(methodSymbol.getAbsoluteName()).thenReturn("foo");

        IConstraintSolver constraintSolver = new ConstraintSolver(symbolFactory, typeHelper, issueReporter);
        constraintSolver.solveConstraints(asList(methodSymbol), mock(IGlobalNamespaceScope.class));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IOverloadBindings> bindingsList = captor.getValue();
        IOverloadBindings bindings = bindingsList.get(0);

        assertThat(bindings, withVariableBindings(
                varBinding("$x", "T1", null, asList("(float | int)", "@T2"), false),
                varBinding("$y", "V3", asList("{as T1}"), asList("{as T1}"), true),
                varBinding("$z", "V5", asList("{as T2}"), asList("{as T2}"), true),
                varBinding("e1", "T1", null, asList("(float | int)", "@T2"), false),
                varBinding("e2", "T2", asList("@T1"), asList("(float | int)"), false),
                varBinding("rtn", "T2", asList("@T1"), asList("(float | int)"), false)
        ));
        assertThat(bindingsList.size(), is(1));
    }

    @Test
    public void solveMethod_XPlusYPlusZ_AsTXAsTXasTReturnTWhereTSubNum() {
        ISymbolsInitialiser symbolsInitialiser = new HardCodedSymbolsInitialiser();
        ISymbolFactory symbolFactory = symbolsInitialiser.getSymbolFactory();
        ITypeHelper typeHelper = symbolsInitialiser.getTypeHelper();
        InferenceIssueReporter issueReporter = new InferenceIssueReporter(new HardCodedIssueMessageProvider());
        AstHelper astHelper = new AstHelper(new TSPHPAstAdaptor());
        ICoreInitialiser coreInitialiser = new HardCodedCoreInitialiser(astHelper, symbolsInitialiser);
        ICore core = coreInitialiser.getCore();
        Map<String, ITypeSymbol> primitiveTypes = core.getPrimitiveTypes();
        Map<Integer, IMinimalMethodSymbol> operators = core.getOperators();

        //  function foo($x, $y, $z){ return $x + $y + $z; }
        // where:
        //   {as T} x {as T} -> T \ T <: num
        // was applied for e1 = $x + $y and
        //   {as T} x {as T} -> T \ T <: num
        // should be applied for e2 = e1 + $z

        //$x + $y
        //{as T} x {as T} -> T \ T <: num
        IVariable e1 = symbolFactory.createVariable("e1");
        IVariableSymbol $x = mock(IVariableSymbol.class);
        when($x.getAbsoluteName()).thenReturn("$x");
        when($x.getName()).thenReturn("$x");
        IVariableSymbol $y = mock(IVariableSymbol.class);
        when($y.getAbsoluteName()).thenReturn("$y");
        when($y.getName()).thenReturn("$y");

        IOverloadBindings overloadBindings = symbolFactory.createOverloadBindings();
        overloadBindings.addVariable(VAR_LHS, reference(T_LHS));
        overloadBindings.addVariable(VAR_RHS, reference(T_RHS));
        overloadBindings.addVariable(RETURN_VARIABLE_NAME, reference(T_RETURN));
        //bind convertible type to T2
        IConvertibleTypeSymbol asTReturn = symbolFactory.createConvertibleTypeSymbol();
        overloadBindings.bind(asTReturn, Arrays.asList(T_RETURN));
        overloadBindings.addUpperTypeBound(T_LHS, asTReturn);
        overloadBindings.addUpperTypeBound(T_RHS, asTReturn);
        overloadBindings.addUpperTypeBound(T_RETURN, primitiveTypes.get(PrimitiveTypeNames.NUM));

        IVariable lhs = symbolFactory.createVariable(VAR_LHS);
        IVariable rhs = symbolFactory.createVariable(VAR_RHS);
        List<IVariable> binaryParameterIds = Arrays.asList(lhs, rhs);
        IFunctionType function = symbolFactory.createFunctionType("+", overloadBindings, binaryParameterIds);
        Set<String> set = new HashSet<>();
        set.add(T_RETURN);
        function.manuallySimplified(set, 0, true);
        IMinimalMethodSymbol minimalMethodSymbol2 = symbolFactory.createMinimalMethodSymbol("+");
        minimalMethodSymbol2.addOverload(function);
        IConstraint constraint1 = symbolFactory.createConstraint(
                mock(ITSPHPAst.class), e1, asList((IVariable) $x, $y), minimalMethodSymbol2);

        //e1 + $z
        //{as T} x {as T} -> T \ T <: num
        IVariableSymbol $z = mock(IVariableSymbol.class);
        when($z.getAbsoluteName()).thenReturn("$z");
        when($z.getName()).thenReturn("$z");
        IVariable e2 = symbolFactory.createVariable("e2");
        IConstraint constraint2 = symbolFactory.createConstraint(
                mock(ITSPHPAst.class), e2, asList(e1, $z), operators.get(TokenTypes.Plus));

        //return e2
        //Tlhs x Trhs -> Tlhs \ Trhs <: Tlhs
        IVariable rtn = symbolFactory.createVariable(RETURN_VARIABLE_NAME);
        IConstraint constraint3 = symbolFactory.createConstraint(
                mock(ITSPHPAst.class), rtn, asList(rtn, e2), operators.get(TokenTypes.Assign));

        List<IConstraint> constraints = asList(constraint1, constraint2, constraint3);
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        when(methodSymbol.getConstraints()).thenReturn(constraints);
        when(methodSymbol.getParameters()).thenReturn(asList($x, $y, $z));
        when(methodSymbol.getAbsoluteName()).thenReturn("foo");

        IConstraintSolver constraintSolver = new ConstraintSolver(symbolFactory, typeHelper, issueReporter);
        constraintSolver.solveConstraints(asList(methodSymbol), mock(IGlobalNamespaceScope.class));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IOverloadBindings> bindingsList = captor.getValue();

        assertThat(bindingsList, hasItem(withVariableBindings(
                varBinding("$x", "V2", asList("{as int}"), asList("{as int}"), true),
                varBinding("$y", "V3", asList("{as int}"), asList("{as int}"), true),
                varBinding("$z", "V5", asList("int"), asList("int"), true),
                varBinding("e1", "V1", asList("int"), asList("int"), true),
                varBinding("e2", "V4", asList("int"), asList("int"), true),
                varBinding("rtn", "V6", asList("int"), asList("int"), true)
        )));
        assertThat(bindingsList, hasItem(withVariableBindings(
                varBinding("$x", "V2", asList("{as float}"), asList("{as float}"), true),
                varBinding("$y", "V3", asList("{as float}"), asList("{as float}"), true),
                varBinding("$z", "V5", asList("float"), asList("float"), true),
                varBinding("e1", "V1", asList("float"), asList("float"), true),
                varBinding("e2", "V4", asList("float"), asList("float"), true),
                varBinding("rtn", "V6", asList("float"), asList("float"), true)
        )));
        assertThat(bindingsList, hasItem(withVariableBindings(
                varBinding("$x", "V2", asList("{as float}"), asList("{as float}"), true),
                varBinding("$y", "V3", asList("{as float}"), asList("{as float}"), true),
                varBinding("$z", "V5", asList("{as (float | int)}"), asList("{as (float | int)}"), true),
                varBinding("e1", "V1", asList("float"), asList("float"), true),
                varBinding("e2", "V4", asList("float"), asList("float"), true),
                varBinding("rtn", "V6", asList("float"), asList("float"), true)
        )));
        assertThat(bindingsList, hasItem(withVariableBindings(
                varBinding("$x", "V2", asList("{as (float | int)}"), asList("{as (float | int)}"), true),
                varBinding("$y", "V3", asList("{as (float | int)}"), asList("{as (float | int)}"), true),
                varBinding("$z", "V5", asList("float"), asList("float"), true),
                varBinding("e1", "V1", asList("int", "float"), asList("(float | int)"), true),
                varBinding("e2", "V4", asList("float"), asList("float"), true),
                varBinding("rtn", "V6", asList("float"), asList("float"), true)
        )));
        assertThat(bindingsList, hasItem(withVariableBindings(
                varBinding("$x", "V2", asList("{as T2}"), asList("{as T2}"), true),
                varBinding("$y", "V3", asList("{as T2}"), asList("{as T2}"), true),
                varBinding("$z", "V5", asList("{as T1}"), asList("{as T1}"), true),
                varBinding("e1", "T2", null, asList("(float | int)", "@T1"), false),
                varBinding("e2", "T1", asList("@T2"), asList("(float | int)"), false),
                varBinding("rtn", "T1", asList("@T2"), asList("(float | int)"), false)
        )));

        assertThat(bindingsList.size(), is(5));
    }

    @Test
    public void solveMethod_PlusArrayCombinedWithPlusAsNum_HasHelperVariable() {
        ISymbolsInitialiser symbolsInitialiser = new HardCodedSymbolsInitialiser();
        ISymbolFactory symbolFactory = symbolsInitialiser.getSymbolFactory();
        ITypeHelper typeHelper = symbolsInitialiser.getTypeHelper();
        InferenceIssueReporter issueReporter = new InferenceIssueReporter(new HardCodedIssueMessageProvider());
        AstHelper astHelper = new AstHelper(new TSPHPAstAdaptor());
        ICoreInitialiser coreInitialiser = new HardCodedCoreInitialiser(astHelper, symbolsInitialiser);
        ICore core = coreInitialiser.getCore();
        Map<String, ITypeSymbol> primitiveTypes = core.getPrimitiveTypes();
        Map<Integer, IMinimalMethodSymbol> operators = core.getOperators();

        //  function foo($y, $z){ if (1 < 2) { return $y + $z; } return $y - $z; }
        // where:
        //   array x array -> array
        // was applied for e1 = $x + $z and
        //   {as T} x {as T} -> T \ T <: num
        // was applied for  e2 = $y - $z

        //$y + $z
        //array x array -> array
        IVariable e1 = symbolFactory.createVariable("e1");
        IVariableSymbol $y = mock(IVariableSymbol.class);
        when($y.getAbsoluteName()).thenReturn("$y");
        when($y.getName()).thenReturn("$y");
        IVariableSymbol $z = mock(IVariableSymbol.class);
        when($z.getAbsoluteName()).thenReturn("$z");
        when($z.getName()).thenReturn("$z");
        IOverloadBindings overloadBindings = symbolFactory.createOverloadBindings();
        overloadBindings.addVariable(VAR_LHS, reference("Tlhs"));
        overloadBindings.addVariable(VAR_RHS, reference("Trhs"));
        overloadBindings.addVariable(RETURN_VARIABLE_NAME, reference("Treturn"));
        overloadBindings.addUpperTypeBound("Tlhs", primitiveTypes.get(PrimitiveTypeNames.ARRAY));
        overloadBindings.addUpperTypeBound("Trhs", primitiveTypes.get(PrimitiveTypeNames.ARRAY));
        overloadBindings.addLowerTypeBound("Treturn", primitiveTypes.get(PrimitiveTypeNames.ARRAY));
        IVariable lhs = symbolFactory.createVariable(VAR_LHS);
        IVariable rhs = symbolFactory.createVariable(VAR_RHS);
        List<IVariable> binaryParameterIds = Arrays.asList(lhs, rhs);
        IFunctionType function = symbolFactory.createFunctionType("+", overloadBindings, binaryParameterIds);
        Set<String> set = new HashSet<>();
        set.add("T");
        function.manuallySimplified(set, 0, false);
        IMinimalMethodSymbol minimalMethodSymbol1 = symbolFactory.createMinimalMethodSymbol("+");
        minimalMethodSymbol1.addOverload(function);
        IConstraint constraint1 = symbolFactory.createConstraint(
                mock(ITSPHPAst.class), e1, asList((IVariable) $y, $z), minimalMethodSymbol1);

        //$y + $z
        //{as T} x {as T} -> T \ T <: num
        IVariable e2 = symbolFactory.createVariable("e2");
        overloadBindings = symbolFactory.createOverloadBindings();
        overloadBindings.addVariable(VAR_LHS, reference(T_LHS));
        overloadBindings.addVariable(VAR_RHS, reference(T_RHS));
        overloadBindings.addVariable(RETURN_VARIABLE_NAME, reference(T_RETURN));
        //bind convertible type to T2
        IConvertibleTypeSymbol asTReturn = symbolFactory.createConvertibleTypeSymbol();
        overloadBindings.bind(asTReturn, Arrays.asList(T_RETURN));
        overloadBindings.addUpperTypeBound(T_LHS, asTReturn);
        overloadBindings.addUpperTypeBound(T_RHS, asTReturn);
        overloadBindings.addUpperTypeBound(T_RETURN, primitiveTypes.get(PrimitiveTypeNames.NUM));
        function = symbolFactory.createFunctionType("+", overloadBindings, binaryParameterIds);
        set = new HashSet<>();
        set.add("T");
        function.manuallySimplified(set, 0, true);
        IMinimalMethodSymbol minimalMethodSymbol2 = symbolFactory.createMinimalMethodSymbol("+");
        minimalMethodSymbol2.addOverload(function);
        IConstraint constraint2 = symbolFactory.createConstraint(
                mock(ITSPHPAst.class), e2, asList((IVariable) $y, $z), minimalMethodSymbol2);

        //Tlhs x Trhs -> Tlhs \ Trhs <: Tlhs
        IVariable rtn = symbolFactory.createVariable(RETURN_VARIABLE_NAME);
        //return e1
        IMinimalMethodSymbol assignFunction = operators.get(TokenTypes.Assign);
        IConstraint constraint3 = symbolFactory.createConstraint(
                mock(ITSPHPAst.class), rtn, asList(rtn, e1), assignFunction);
        //return e2
        IConstraint constraint4
                = symbolFactory.createConstraint(mock(ITSPHPAst.class), rtn, asList(rtn, e2), assignFunction);

        List<IConstraint> constraints = asList(constraint1, constraint2, constraint3, constraint4);
        IMethodSymbol methodSymbol = mock(IMethodSymbol.class);
        when(methodSymbol.getConstraints()).thenReturn(constraints);
        when(methodSymbol.getParameters()).thenReturn(asList($y, $z));
        when(methodSymbol.getAbsoluteName()).thenReturn("foo");

        IConstraintSolver constraintSolver = new ConstraintSolver(symbolFactory, typeHelper, issueReporter);
        constraintSolver.solveConstraints(asList(methodSymbol), mock(IGlobalNamespaceScope.class));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(methodSymbol).setBindings(captor.capture());
        List<IOverloadBindings> bindingsList = captor.getValue();
        IOverloadBindings bindings = bindingsList.get(0);

        assertThat(bindings, withVariableBindings(
                varBinding("$y", "V2", asList("(array & {as T})"), asList("array", "{as T}"), true),
                varBinding("$z", "V3", asList("(array & {as T})"), asList("array", "{as T}"), true),
                varBinding("e1", "V1", asList("array"), asList("array"), true),
                varBinding("e2", "T", null, asList("(float | int)", "@V5"), false),
                varBinding("rtn", "V5", asList("array", "@T"), null, false)
        ));
        assertThat(bindingsList.size(), is(1));
    }

    protected TypeVariableReference reference(String name) {
        return new TypeVariableReference(name);
    }
}
