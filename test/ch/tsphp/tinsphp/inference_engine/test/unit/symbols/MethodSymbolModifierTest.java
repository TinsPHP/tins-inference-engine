/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.symbols;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.modifiers.IModifierSet;
import ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.inference_engine.symbols.MethodSymbol;
import ch.tsphp.tinsphp.inference_engine.symbols.ModifierSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class MethodSymbolModifierTest
{
    private String methodName;
    private int modifierType;

    public MethodSymbolModifierTest(String theMethodName, int theModifierType) {
        methodName = theMethodName;
        modifierType = theModifierType;
    }

    @Test
    public void is_ReturnsTrue() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        IModifierSet set = createModifierSet();
        set.add(modifierType);

        IMethodSymbol methodSymbol = createMethodSymbol(set);
        boolean result = (boolean) methodSymbol.getClass().getMethod(methodName).invoke(methodSymbol);

        //the following three modifiers are return type modifiers and thus the expected result is false for those
        //modifiers since no return modifier was defined
        boolean is = !methodName.equals("isAlwaysCasting")
                && !methodName.equals("isFalseable")
                && !methodName.equals("isNullable");

        assertEquals(methodName + " failed.", is, result);
    }

    @Test
    public void isNot_ReturnsFalse() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        IModifierSet set = createModifierSet();

        IMethodSymbol methodSymbol = createMethodSymbol(set);
        boolean result = (boolean) methodSymbol.getClass().getMethod(methodName).invoke(methodSymbol);

        assertFalse(methodName + " failed.", result);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                {"isAbstract", TinsPHPDefinitionWalker.Abstract},
                {"isFinal", TinsPHPDefinitionWalker.Final},
                {"isStatic", TinsPHPDefinitionWalker.Static},
                {"isPublic", TinsPHPDefinitionWalker.Public},
                {"isProtected", TinsPHPDefinitionWalker.Protected},
                {"isPrivate", TinsPHPDefinitionWalker.Private},
                {"isAlwaysCasting", TinsPHPDefinitionWalker.Cast},
                {"isFalseable", TinsPHPDefinitionWalker.LogicNot},
                {"isNullable", TinsPHPDefinitionWalker.QuestionMark},
        });
    }

    protected IModifierSet createModifierSet() {
        return new ModifierSet();
    }

    protected IMethodSymbol createMethodSymbol(IModifierSet set) {
        return new MethodSymbol(mock(IScopeHelper.class), mock(ITSPHPAst.class), set, mock(IModifierSet.class), "foo",
                mock(IScope.class));
    }
}
