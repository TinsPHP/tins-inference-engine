/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ParameterInitialisationTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AVerifyTimesReferenceTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class ParameterInitialisationTest extends AVerifyTimesReferenceTest
{

    private static List<Object[]> collection;

    public ParameterInitialisationTest(String testString, int howManyTimes) {
        super(testString, howManyTimes);
    }

    @Test
    public void test() throws RecognitionException {
        check();

    }

    @Override
    protected void verifyTimes() {
        verify(referencePhaseController, times(howManyTimes)).checkIsVariableInitialised(any(ITSPHPAst.class));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        collection = new ArrayList<>();

        addVariations("", "");
        addVariations("namespace a;", "");
        addVariations("namespace a\\b\\z{", "}");
        //TODO rstoll TINS-161 inference OOP
//        addVariations("class a{", "}");
//        addVariations("namespace a; class b{", "}");
//        addVariations("namespace a\\b\\z{ class m{", "}}");
        return collection;
    }

    public static void addVariations(String prefix, String appendix) {
        String[] types = new String[]{"array"};
        for (String type : types) {
            collection.addAll(Arrays.asList(new Object[][]{
                    {prefix + "function foo(" + type + " $a){ $a; return;}" + appendix, 1},
                    {prefix + "function foo(" + type + " $a, $b){ $a; $b;return;}" + appendix, 2},
                    {prefix + "function foo(" + type + " $a, $b, $c=3){$a;$b;$c;return;}" + appendix, 3},
                    {prefix + "function foo(" + type + " $a,$b, \\Exception $c=3){$a;$b;$c;return;}" + appendix, 3},
                    {prefix + "function foo(" + type + " $a, $b, $c=1, $d=3){$a;$b;$c;$d;return;}" + appendix, 4},
            }));
        }
    }
}
