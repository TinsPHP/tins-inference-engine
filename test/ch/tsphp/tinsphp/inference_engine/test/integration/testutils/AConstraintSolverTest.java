/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.symbols.IFunctionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbolWithRef;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.constraints.IntersectionConstraint;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeHelper;
import ch.tsphp.tinsphp.symbols.ConstantFunctionTypeSymbol;
import ch.tsphp.tinsphp.symbols.ModifierHelper;
import ch.tsphp.tinsphp.symbols.SymbolFactory;
import ch.tsphp.tinsphp.symbols.UnionTypeSymbol;
import ch.tsphp.tinsphp.symbols.constraints.TypeConstraint;
import ch.tsphp.tinsphp.symbols.utils.OverloadResolver;
import org.junit.BeforeClass;
import org.junit.Ignore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public abstract class AConstraintSolverTest
{
    //TODO reset to 2, 2000 just for debugging
    protected static int TIMEOUT = 2;

    //Warning! start code duplication - same as in OverloadResolverPromotionLevelTest from the symbols component
    protected static ITypeSymbol mixedType;
    protected static ITypeSymbol arrayType;
    protected static ITypeSymbol scalarType;
    protected static ITypeSymbol stringType;
    protected static ITypeSymbol numType;
    protected static ITypeSymbol floatType;
    protected static ITypeSymbol intType;
    protected static ITypeSymbol boolType;
    protected static ITypeSymbol nothingType;

    protected static ITypeSymbol interfaceAType;
    protected static ITypeSymbol interfaceSubAType;
    protected static ITypeSymbol interfaceBType;
    protected static ITypeSymbol fooType;
    //Warning! end code duplication - same as in OverloadResolverPromotionLevelTest from the symbols component

    protected static IOverloadResolver overloadResolver = new OverloadResolver();

    @BeforeClass
    public static void init() {
        //Warning! start code duplication - same as in OverloadResolverPromotionLevelTest from the symbols component
        mixedType = mock(ITypeSymbol.class);

        arrayType = mock(ITypeSymbol.class);
        when(arrayType.getParentTypeSymbols()).thenReturn(set(mixedType));
        when(arrayType.getAbsoluteName()).thenReturn("array");

        scalarType = mock(ITypeSymbol.class);
        when(scalarType.getParentTypeSymbols()).thenReturn(set(mixedType));
        when(scalarType.getAbsoluteName()).thenReturn("scalar");

        stringType = mock(ITypeSymbol.class);
        when(stringType.getParentTypeSymbols()).thenReturn(set(scalarType));
        when(stringType.getAbsoluteName()).thenReturn("string");

        numType = mock(ITypeSymbol.class);
        when(numType.getParentTypeSymbols()).thenReturn(set(scalarType));
        when(numType.getAbsoluteName()).thenReturn("num");

        floatType = mock(ITypeSymbol.class);
        when(floatType.getParentTypeSymbols()).thenReturn(set(numType));
        when(floatType.getAbsoluteName()).thenReturn("float");

        intType = mock(ITypeSymbol.class);
        when(intType.getParentTypeSymbols()).thenReturn(set(numType));
        when(intType.getAbsoluteName()).thenReturn("int");

        boolType = mock(ITypeSymbol.class);
        when(boolType.getParentTypeSymbols()).thenReturn(set(scalarType));
        when(boolType.getAbsoluteName()).thenReturn("bool");

        nothingType = mock(ITypeSymbol.class);
        when(nothingType.getAbsoluteName()).thenReturn("nothing");

        interfaceAType = mock(ITypeSymbol.class);
        when(interfaceAType.getParentTypeSymbols()).thenReturn(set(mixedType));
        when(interfaceAType.getAbsoluteName()).thenReturn("IA");

        interfaceSubAType = mock(ITypeSymbol.class);
        when(interfaceSubAType.getParentTypeSymbols()).thenReturn(set(interfaceAType));
        when(interfaceSubAType.getAbsoluteName()).thenReturn("ISubA");

        interfaceBType = mock(ITypeSymbol.class);
        when(interfaceBType.getParentTypeSymbols()).thenReturn(set(mixedType));
        when(interfaceBType.getAbsoluteName()).thenReturn("IB");

        fooType = mock(ITypeSymbol.class);
        when(fooType.getParentTypeSymbols()).thenReturn(set(interfaceSubAType, interfaceBType));
        when(fooType.getAbsoluteName()).thenReturn("Foo");
        //Warning! end code duplication - same as in OverloadResolverPromotionLevelTest from the inference component
    }

    protected static HashSet<ITypeSymbol> set(ITypeSymbol... symbols) {
        return new HashSet<>(asList(symbols));
    }

    protected ITypeVariableSymbolWithRef typeVarWithRef(String id, ITypeSymbol... types) {
        ITypeVariableSymbolWithRef typeVariableSymbol = mock(ITypeVariableSymbolWithRef.class);
        when(typeVariableSymbol.getAbsoluteName()).thenReturn(id);
        IUnionTypeSymbol unionTypeSymbol = new UnionTypeSymbol(overloadResolver);
        for (ITypeSymbol type : types) {
            unionTypeSymbol.addTypeSymbol(type);
        }
        unionTypeSymbol.seal();
        when(typeVariableSymbol.getType()).thenReturn(unionTypeSymbol);
        return typeVariableSymbol;
    }


    protected ITypeVariableSymbol typeVar(String id, ITypeSymbol... types) {
        ITypeVariableSymbol typeVariableSymbol = mock(ITypeVariableSymbol.class);
        when(typeVariableSymbol.getAbsoluteName()).thenReturn(id);
        IUnionTypeSymbol unionTypeSymbol = new UnionTypeSymbol(overloadResolver);
        for (ITypeSymbol type : types) {
            unionTypeSymbol.addTypeSymbol(type);
        }
        unionTypeSymbol.seal();
        when(typeVariableSymbol.getType()).thenReturn(unionTypeSymbol);
        return typeVariableSymbol;
    }

    protected ITypeVariableSymbol typeVar(String id, IConstraint constraints) {
        ITypeVariableSymbol typeVariableSymbol = mock(ITypeVariableSymbol.class);
        when(typeVariableSymbol.getAbsoluteName()).thenReturn(id);
        when(typeVariableSymbol.getConstraint()).thenReturn(constraints);
        IUnionTypeSymbol unionTypeSymbol = new UnionTypeSymbol(overloadResolver);
        when(typeVariableSymbol.getType()).thenReturn(unionTypeSymbol);
        return typeVariableSymbol;
    }

    protected List<IConstraint> list(IConstraint... constraints) {
        List<IConstraint> list = new ArrayList<>();
        Collections.addAll(list, constraints);
        return list;
    }

    protected <T> List<T> list(T... objects) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, objects);
        return list;
    }

    protected IConstraint intersect(List<ITypeVariableSymbol> variables, List<IFunctionTypeSymbol> overloads) {
        return new IntersectionConstraint(variables, overloads);
    }

//    protected IConstraint iRef(String refVariableName, IScope refScope) {
//        return ref(refVariableName, refScope);
//    }
//
//    protected RefConstraint ref(String refTypeVariableId, ITypeVariableSymbol refTypeVariableId) {
//        return new RefConstraint(refTypeVariableId, refTypeVariableId);
//    }

    protected IConstraint type(ITypeSymbol typeSymbol) {
        return new TypeConstraint(typeSymbol);
    }

    protected ITypeVariableCollection createTypeVariableCollection(ITypeVariableSymbol... typeVariables) {
        Deque<ITypeVariableSymbol> deque = new ArrayDeque<>();
        for (ITypeVariableSymbol typeVariableSymbol : typeVariables) {
            deque.addLast(typeVariableSymbol);
        }
        ITypeVariableCollection scope = mock(ITypeVariableCollection.class);
        when(scope.getTypeVariables()).thenReturn(deque);
        return scope;
    }

    protected IConstraint createPartialAdditionWithInt(ITypeVariableSymbol typeVariableSymbol) {
        return intersect(asList(typeVariableSymbol), asList(
                funcUnary(intType, intType),
                funcUnary(numType, numType)
        ));
    }

    protected IFunctionTypeSymbol funcUnary(ITypeSymbol parameterType, ITypeSymbol returnType) {
        IFunctionTypeSymbol function = new ConstantFunctionTypeSymbol("+", asList("$lhs"), mixedType, returnType);
        function.addInputConstraint("$lhs", type(parameterType));
        return function;
    }

    protected IFunctionTypeSymbol funcBinary(ITypeSymbol lhsType, ITypeSymbol rhsType, ITypeSymbol returnType) {
        IFunctionTypeSymbol function = new ConstantFunctionTypeSymbol(
                "+", asList("$lhs", "$rhs"), mixedType, returnType);
        function.addInputConstraint("$lhs", type(lhsType));
        function.addInputConstraint("$rhs", type(rhsType));
        return function;
    }

    protected IConstraint createPartialAdditionWithFloat(ITypeVariableSymbol typeVariableSymbol) {
        return intersect(asList(typeVariableSymbol), asList(
                funcUnary(floatType, floatType),
                funcUnary(numType, numType)
        ));
    }

    protected IConstraint createAdditionIntersection(ITypeVariableSymbol lhs, ITypeVariableSymbol rhs) {
        return intersect(asList(lhs, rhs), asList(
                funcBinary(boolType, boolType, intType),
                funcBinary(intType, intType, intType),
                funcBinary(floatType, floatType, floatType),
                funcBinary(numType, numType, numType),
                funcBinary(arrayType, arrayType, arrayType)
        ));
    }

    protected IConstraintSolver createConstraintSolver() {
        IOverloadResolver overloadResolver = new OverloadResolver();
        return createConstraintSolver(
                createSymbolFactory(overloadResolver), overloadResolver);
    }

    protected SymbolFactory createSymbolFactory(IOverloadResolver overloadResolver) {
        return new SymbolFactory(new ScopeHelper(), new ModifierHelper(), overloadResolver);
    }

    protected IConstraintSolver createConstraintSolver(ISymbolFactory theSymbolFactory,
            IOverloadResolver overloadResolver) {
        return new ConstraintSolver(theSymbolFactory, overloadResolver);
    }
}
