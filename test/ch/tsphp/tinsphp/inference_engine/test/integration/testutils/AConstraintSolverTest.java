/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.common.IConstraint;
import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.constraints.IntersectionConstraint;
import ch.tsphp.tinsphp.inference_engine.constraints.OverloadDto;
import ch.tsphp.tinsphp.inference_engine.constraints.RefConstraint;
import ch.tsphp.tinsphp.inference_engine.constraints.TypeConstraint;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeHelper;
import ch.tsphp.tinsphp.symbols.ModifierHelper;
import ch.tsphp.tinsphp.symbols.SymbolFactory;
import ch.tsphp.tinsphp.symbols.utils.OverloadResolver;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public abstract class AConstraintSolverTest
{
    //TODO reset to 2, 2000 just for debugging
    protected static int TIMEOUT = 2000;

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

    protected <T> List<T> list(T... objects) {
        List<T> list = new ArrayList<>();
        for (T obj : objects) {
            list.add(obj);
        }
        return list;
    }

    protected IConstraint intersect(List<RefConstraint> variables, List<OverloadDto> overloads) {
        return new IntersectionConstraint(variables, overloads);
    }

    protected IConstraint iRef(String refVariableName, IScope refScope) {
        return ref(refVariableName, refScope);
    }

    protected RefConstraint ref(String refVariableName, IScope refScope) {
        return new RefConstraint(refScope, refVariableName);
    }

    protected IConstraint type(ITypeSymbol typeSymbol) {
        return new TypeConstraint(typeSymbol);
    }

    protected IScope createScopeWithConstraints(final Map<String, List<IConstraint>> map) {
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


    protected Map<String, IUnionTypeSymbol> createResolvingResult(IScope scope) {
        final Map<String, IUnionTypeSymbol> map = new HashMap<>();
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                map.put((String) args[0], (IUnionTypeSymbol) args[1]);
                return null;
            }
        }).when(scope).setResultOfConstraintSolving(anyString(), any(IUnionTypeSymbol.class));
        when(scope.getResultOfConstraintSolving(anyString())).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return map.get((String) invocationOnMock.getArguments()[0]);
            }
        });
        return map;
    }

    protected IConstraint createPartialAdditionWithInt(String variableId, IScope scope) {
        return intersect(asList(ref(variableId, scope)), asList(
                new OverloadDto(asList(asList(type(intType))), intType),
                new OverloadDto(asList(asList(type(numType))), numType)
        ));
    }

    protected IConstraint createPartialAdditionWithFloat(String variableId, IScope scope) {
        return intersect(asList(ref(variableId, scope)), asList(
                new OverloadDto(asList(asList(type(floatType))), floatType),
                new OverloadDto(asList(asList(type(numType))), numType)
        ));
    }

    protected IConstraint createAdditionIntersection(
            String variableLhs, IScope scopeLhs, String variableRhs, IScope scopeRhs) {
        return intersect(asList(ref(variableLhs, scopeLhs), ref(variableRhs, scopeRhs)),
                asList(
                        new OverloadDto(asList(asList(type(boolType)), asList(type(boolType))), intType),
                        new OverloadDto(asList(asList(type(intType)), asList(type(intType))), intType),
                        new OverloadDto(asList(asList(type(floatType)), asList(type(floatType))), floatType),
                        new OverloadDto(asList(asList(type(numType)), asList(type(numType))), numType),
                        new OverloadDto(asList(asList(type(arrayType)), asList(type(arrayType))), arrayType)
                ));
    }

    protected IConstraintSolver createConstraintSolver() {
        IOverloadResolver overloadResolver = new OverloadResolver();
        return createConstraintSolver(nothingType,
                new SymbolFactory(new ScopeHelper(), new ModifierHelper(), overloadResolver), overloadResolver);
    }

    protected IConstraintSolver createConstraintSolver(
            ITypeSymbol theNothingTypeSymbol, ISymbolFactory theSymbolFactory, IOverloadResolver overloadResolver) {
        return new ConstraintSolver(theNothingTypeSymbol, theSymbolFactory, overloadResolver);
    }
}
