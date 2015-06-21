/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ConstantHelper from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition;

import ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestTypeHelper;
import ch.tsphp.tinsphp.symbols.ModifierHelper;
import ch.tsphp.tinsphp.symbols.ModifierSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ConstantHelper
{

    public static Collection<Object[]> testStrings(String prefix, String appendix, String prefixExpected,
            final String scopeName, boolean isDefinitionPhase) {

        List<Object[]> collection = new ArrayList<>();
        String[] types = TestTypeHelper.getScalarTypes();
        ModifierSet modifiers = new ModifierSet(Arrays.asList(
                TinsPHPDefinitionWalker.Public,
                TinsPHPDefinitionWalker.Static,
                TinsPHPDefinitionWalker.Final));

        String mod = ModifierHelper.getModifiersAsString(modifiers);

        for (String type : types) {
            String typeExpected = isDefinitionPhase ? "" : type;
            collection.addAll(Arrays.asList(new Object[][]{
                    {
                            prefix + "const a=true;" + appendix,
                            prefixExpected + scopeName + "? " + scopeName + "a#" + typeExpected + mod
                    },
                    {
                            prefix + "const a=true, b=false;" + appendix,
                            prefixExpected + scopeName + "? " + scopeName + "a#" + typeExpected + mod + " "
                                    + scopeName + "? " + scopeName + "b#" + typeExpected + mod
                    },
                    {
                            prefix + "const a=1,b=2;" + appendix,
                            prefixExpected + scopeName + "? " + scopeName + "a#" + typeExpected + mod + " "
                                    + scopeName + "? " + scopeName + "b#" + typeExpected + mod
                    },
                    {
                            prefix + "const a=1.0,b=2.0,c=null;" + appendix,
                            prefixExpected + scopeName + "? " + scopeName + "a#" + typeExpected + mod + " "
                                    + scopeName + "? " + scopeName + "b#" + typeExpected + mod + " "
                                    + scopeName + "? " + scopeName + "c#" + typeExpected + mod
                    },
                    {
                            prefix + "const a=1,b=\"2\",c=null,d='2';" + appendix,
                            prefixExpected + scopeName + "? " + scopeName + "a#" + typeExpected + mod + " "
                                    + scopeName + "? " + scopeName + "b#" + typeExpected + mod + " "
                                    + scopeName + "? " + scopeName + "c#" + typeExpected + mod + " "
                                    + scopeName + "? " + scopeName + "d#" + typeExpected + mod
                    }
            }));
        }
        return collection;
    }
}
