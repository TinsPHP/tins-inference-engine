/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.symbols;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.modifiers.IModifierSet;
import ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.symbols.ModifierSet;
import ch.tsphp.tinsphp.inference_engine.symbols.VariableSymbol;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class VariableSymbolModifierTest
{
    private String methodName;
    private int modifierType;

    public VariableSymbolModifierTest(String theMethodName, int theModifierType) {
        methodName = theMethodName;
        modifierType = theModifierType;
    }

    @Test
    public void is_ReturnsTrue() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        IModifierSet set = createModifierSet();
        set.add(modifierType);

        IVariableSymbol variableSymbol = createVariableSymbol(set);
        boolean result = (boolean) variableSymbol.getClass().getMethod(methodName).invoke(variableSymbol);

        assertTrue(methodName + " failed.", result);
    }

    @Test
    public void isNot_ReturnsFalse() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        IModifierSet set = createModifierSet();

        IVariableSymbol variableSymbol = createVariableSymbol(set);
        boolean result = (boolean) variableSymbol.getClass().getMethod(methodName).invoke(variableSymbol);

        assertFalse(methodName + " failed.", result);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                //not yet supported by PHP
//                {"isFinal", TinsPHPDefinitionWalker.Final},
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

    protected IVariableSymbol createVariableSymbol(IModifierSet set) {
        return new VariableSymbol(mock(ITSPHPAst.class), set, "foo");
    }
}
