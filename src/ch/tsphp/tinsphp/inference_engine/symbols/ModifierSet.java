/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ModifierSet from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.symbols;


import ch.tsphp.common.symbols.modifiers.IModifierSet;
import ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker;

import java.util.HashSet;

public class ModifierSet extends HashSet<Integer> implements IModifierSet
{
    @Override
    public boolean isAbstract() {
        return contains(TinsPHPDefinitionWalker.Abstract);
    }

    @Override
    public boolean isFinal() {
        return contains(TinsPHPDefinitionWalker.Final);
    }

    public boolean isStatic() {
        return contains(TinsPHPDefinitionWalker.Static);
    }

    @Override
    public boolean isPublic() {
        return contains(TinsPHPDefinitionWalker.Public);
    }

    @Override
    public boolean isProtected() {
        return contains(TinsPHPDefinitionWalker.Protected);
    }

    @Override
    public boolean isPrivate() {
        return contains(TinsPHPDefinitionWalker.Private);
    }

    public boolean isAlwaysCasting() {
        return contains(TinsPHPDefinitionWalker.Cast);
    }

    public boolean isFalseable() {
        return contains(TinsPHPDefinitionWalker.LogicNot);
    }

    public boolean isNullable() {
        return contains(TinsPHPDefinitionWalker.QuestionMark);
    }
}
