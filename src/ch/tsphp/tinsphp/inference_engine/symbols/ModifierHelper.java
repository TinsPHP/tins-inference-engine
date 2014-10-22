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

package ch.tsphp.tinsphp.inference_engine.symbols;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.modifiers.IModifierSet;

import java.util.List;

public class ModifierHelper implements IModifierHelper
{

    public ModifierHelper() {
    }

    public IModifierSet getModifiers(ITSPHPAst modifierAst) {
        IModifierSet modifiers = new ModifierSet();

        List<ITSPHPAst> children = modifierAst.getChildren();
        if (children != null && !children.isEmpty()) {
            for (ITSPHPAst child : children) {
                modifiers.add(child.getType());
            }
        }
        return modifiers;
    }
}
