/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.symbols;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.modifiers.IModifierSet;
import ch.tsphp.tinsphp.inference_engine.symbols.ASymbolWithModifier;
import ch.tsphp.tinsphp.inference_engine.symbols.ModifierSet;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.mock;

public class ASymbolWithModifierTest
{
    class DummySymbolWithModifier extends ASymbolWithModifier
    {

        public DummySymbolWithModifier(ITSPHPAst definitionAst, IModifierSet theModifiers, String name) {
            super(definitionAst, theModifiers, name);
        }
    }

    @Test
    public void getModifiers_NothingDefined_ReturnEmptyModifierSet() {
        //no arrange necessary

        ASymbolWithModifier symbolWithModifier = createSymbolWithModifier();
        IModifierSet result = symbolWithModifier.getModifiers();

        assertThat(result.size(), is(0));
    }

    @Test
    public void getModifiers_OneDefined_ReturnModifierSetWithModifier() {
        int modifier = 12;

        ASymbolWithModifier symbolWithModifier = createSymbolWithModifier();
        symbolWithModifier.addModifier(modifier);
        IModifierSet result = symbolWithModifier.getModifiers();

        assertThat(result, containsInAnyOrder(modifier));
    }

    @Test
    public void getModifiers_TwoDefined_ReturnModifierSetWithTwoModifier() {
        int modifier1 = 12;
        int modifier2 = 34;

        ASymbolWithModifier symbolWithModifier = createSymbolWithModifier();
        symbolWithModifier.addModifier(modifier1);
        symbolWithModifier.addModifier(modifier2);
        IModifierSet result = symbolWithModifier.getModifiers();

        assertThat(result, containsInAnyOrder(modifier1, modifier2));
    }

    @Test
    public void removeModifiers_RemoveNotDefined_ReturnsFalse() {
        //no arrange necessary

        ASymbolWithModifier symbolWithModifier = createSymbolWithModifier();
        boolean result = symbolWithModifier.removeModifier(12);

        assertThat(result, is(false));
    }

    @Test
    public void removeModifiers_Defined_ReturnsSetWithoutModifier() {
        int modifier1 = 12;
        int modifier2 = 34;

        ASymbolWithModifier symbolWithModifier = createSymbolWithModifier();
        symbolWithModifier.addModifier(modifier1);
        symbolWithModifier.addModifier(modifier2);
        boolean result = symbolWithModifier.removeModifier(modifier1);
        IModifierSet set = symbolWithModifier.getModifiers();

        assertThat(result, is(true));
        assertThat(set, containsInAnyOrder(modifier2));
    }

    @Test
    public void setModifiers_Standard_ReturnSameSet() {
        IModifierSet set = new ModifierSet();
        set.add(12);

        ASymbolWithModifier symbolWithModifier = createSymbolWithModifier();
        symbolWithModifier.setModifiers(set);
        IModifierSet result = symbolWithModifier.getModifiers();

        assertThat(result, is(set));
    }

    protected ASymbolWithModifier createSymbolWithModifier() {
        return createSymbolWithModifier(mock(ITSPHPAst.class), new ModifierSet(), "foo");
    }

    protected ASymbolWithModifier createSymbolWithModifier(ITSPHPAst definitionAst, IModifierSet modifiers,
            String name) {
        return new DummySymbolWithModifier(definitionAst, modifiers, name);
    }
}
