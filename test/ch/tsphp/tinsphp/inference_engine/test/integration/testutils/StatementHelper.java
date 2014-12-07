/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatementHelper
{

    private StatementHelper() {
    }

    public static List<Object[]> getStatements(String prefix, String appendix) {
        List<Object[]> collection = new ArrayList<>();
        collection.addAll(InstructionHelper.getInstructions(prefix, appendix));
        collection.addAll(Arrays.asList(new Object[][]{
                {prefix + "use \\A;" + appendix},
                {prefix + "function foo(){return;}" + appendix},
                {prefix + "const a = 1;" + appendix},
        }));

        return collection;
    }
}
