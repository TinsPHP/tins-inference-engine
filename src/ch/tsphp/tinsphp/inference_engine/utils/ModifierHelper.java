/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ModifierHelper from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.utils;

import ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker;

import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;

public final class ModifierHelper
{
    private ModifierHelper() {
    }

    public static String getModifiersAsString(SortedSet modifiers) {
        String typeModifiers;
        if (modifiers == null || modifiers.size() == 0) {
            typeModifiers = "";
        } else {
            typeModifiers = Arrays.toString(modifiers.toArray());
            typeModifiers = "|" + typeModifiers.substring(1, typeModifiers.length() - 1);
        }
        return typeModifiers;
    }

    public static boolean canBeAccessedFrom(Set<Integer> modifiers, int type) {
        boolean canBeAccessed;
        switch (type) {
            case TinsPHPDefinitionWalker.Public:
                canBeAccessed = modifiers.contains(TinsPHPDefinitionWalker.Public);
                break;
            case TinsPHPDefinitionWalker.Protected:
                canBeAccessed = modifiers.contains(TinsPHPDefinitionWalker.Public)
                        || modifiers.contains(TinsPHPDefinitionWalker.Protected);
                break;
            case TinsPHPDefinitionWalker.Private:
                canBeAccessed = modifiers.contains(TinsPHPDefinitionWalker.Public)
                        || modifiers.contains(TinsPHPDefinitionWalker.Protected)
                        || modifiers.contains(TinsPHPDefinitionWalker.Private);
                break;
            default:
                throw new RuntimeException("Wrong type passed: " + type + " should correspond to "
                        + "TinsPHPDefinitionWalker.Public, TinsPHPDefinitionWalker.Protected or "
                        + "TinsPHPDefinitionWalker.Private");
        }
        return canBeAccessed;
    }
}
