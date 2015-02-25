/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.common.IConstraint;
import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadResolver;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.ConstraintSolver;
import ch.tsphp.tinsphp.inference_engine.constraints.IntersectionConstraint;
import ch.tsphp.tinsphp.inference_engine.constraints.RefConstraint;
import ch.tsphp.tinsphp.inference_engine.constraints.RefTypeConstraint;
import ch.tsphp.tinsphp.inference_engine.constraints.TypeConstraint;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeHelper;
import ch.tsphp.tinsphp.symbols.ModifierHelper;
import ch.tsphp.tinsphp.symbols.SymbolFactory;
import ch.tsphp.tinsphp.symbols.utils.OverloadResolver;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.AbstractMap;
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
    //Warning! start code duplication - same as in OverloadResolverPromotionLevelTest from the inference component
    protected static ITypeSymbol mixedType;
    protected static ITypeSymbol arrayType;
    protected static ITypeSymbol scalarType;
    protected static ITypeSymbol stringType;
    protected static ITypeSymbol numType;
    protected static ITypeSymbol floatType;
    protected static ITypeSymbol intType;
    protected static ITypeSymbol nothingType;

    protected static ITypeSymbol interfaceAType;
    protected static ITypeSymbol interfaceSubAType;
    protected static ITypeSymbol interfaceBType;
    protected static ITypeSymbol fooType;
    //Warning! end code duplication - same as in OverloadResolverPromotionLevelTest from the inference component


    @BeforeClass
    public static void init() {
        //Warning! start code duplication - same as in OverloadResolverPromotionLevelTest from the inference component
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

    protected ITSPHPAst varAst(ITypeSymbol typeSymbol) {
        return varAst(typeSymbol, false);
    }

    protected ITSPHPAst varAst(ITypeSymbol typeSymbol, final boolean isAlwaysCasting) {
        IVariableSymbol variableSymbol = mock(IVariableSymbol.class);
        when(variableSymbol.getType()).thenReturn(typeSymbol);
        when(variableSymbol.isAlwaysCasting()).thenReturn(isAlwaysCasting);

        ITSPHPAst variableAst = mock(ITSPHPAst.class);
        when(variableAst.getSymbol()).thenReturn(variableSymbol);
        return variableAst;
    }

    protected Map.Entry<List<RefTypeConstraint>, ITypeSymbol> entry(
            List<RefTypeConstraint> constraints, ITypeSymbol typeSymbol) {
        return new AbstractMap.SimpleEntry<>(constraints, typeSymbol);
    }

    protected RefTypeConstraint refType(String variableId, IScope scope, ITypeSymbol typeSymbol) {
        return refType(variableId, scope, varAst(typeSymbol));
    }

    protected RefTypeConstraint refType(String variableId, IScope scope, ITSPHPAst variableAst) {
        return new RefTypeConstraint(variableId, scope, variableAst);
    }

    protected IConstraint intersect(List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads) {
        return new IntersectionConstraint(overloads);
    }

    protected IConstraint ref(String refVariableName, IScope refScope) {
        return new RefConstraint(refVariableName, refScope);
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

    protected IConstraintSolver createConstraintSolver() {
        IOverloadResolver overloadResolver = new OverloadResolver();
        return createConstraintSolver(nothingType,
                new SymbolFactory(new ScopeHelper(), new ModifierHelper(), overloadResolver), overloadResolver);
    }

    protected IConstraintSolver createConstraintSolver(
            ITypeSymbol theNothingTypeSymbol, ISymbolFactory theSymbolFactory, IOverloadResolver overloadResolver) {
        return new ConstraintSolver(theNothingTypeSymbol, theSymbolFactory, overloadResolver);
    }


    protected List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> createAdditionOverload(
            String variableLhs, IScope scopeLhs, String variableRhs, IScope scopeRhs) {
        return asList(
                entry(
                        asList(
                                refType(variableLhs, scopeLhs, intType),
                                refType(variableRhs, scopeRhs, intType)
                        ), intType),
                entry(
                        asList(
                                refType(variableLhs, scopeLhs, floatType),
                                refType(variableRhs, scopeRhs, floatType)
                        ), floatType),
                entry(
                        asList(
                                refType(variableLhs, scopeLhs, numType),
                                refType(variableRhs, scopeRhs, numType)
                        ), numType),
                entry(
                        asList(
                                refType(variableLhs, scopeLhs, varAst(numType, true)),
                                refType(variableRhs, scopeRhs, varAst(numType, true))
                        ), numType),
                entry(
                        asList(
                                refType(variableLhs, scopeLhs, arrayType),
                                refType(variableRhs, scopeLhs, arrayType)
                        ), arrayType)
        );
    }
}
