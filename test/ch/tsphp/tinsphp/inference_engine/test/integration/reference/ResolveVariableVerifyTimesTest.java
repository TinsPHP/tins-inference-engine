/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ResolveVariableVerifyTimesTest from the TSPHP project.
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
public class ResolveVariableVerifyTimesTest extends AVerifyTimesReferenceTest
{

    private static List<Object[]> collection;

    public ResolveVariableVerifyTimesTest(String testString, int howManyTimes) {
        super(testString, howManyTimes);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Override
    protected void verifyTimes() {
        verify(referencePhaseController, times(howManyTimes)).resolveVariable(any(ITSPHPAst.class));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        collection = new ArrayList<>();

        //global constants
        addVariations("", "");
        addVariations("namespace{", "}");
        addVariations("namespace a;", "");
        addVariations("namespace a{", "}");
        addVariations("namespace a\\b;", "");
        addVariations("namespace a\\b\\z{", "}");

        //functions
        addVariations("function foo(){", "return;}");
        addVariations("namespace{function foo(){", "return;}}");
        addVariations("namespace a;function foo(){", "return;}");
        addVariations("namespace a{function foo(){", "return;}}");
        addVariations("namespace a\\b;function foo(){", "return;}");
        addVariations("namespace a\\b\\z{function foo(){", "return;}}");

        //methods
        //TODO rstoll TINS-161 inference OOP
//        addVariations("class a{ function foo(){", "return;}}");
//        addVariations("namespace{ class a{ function foo(){", "return;}}}");
//        addVariations("namespace a; class a{ function foo(){", "return;}}");
//        addVariations("namespace a{ class a { function foo(){", "return;}}}");
//        addVariations("namespace a\\b; class a{ function foo(){", "return;}}");
//        addVariations("namespace a\\b\\z{ class a{ function foo(){", "return;}}}");

        collection.addAll(Arrays.asList(new Object[][]{
                //same namespace
                {"namespace{$a;} namespace{$a=1;}", 2},
                {"namespace a{$a;} namespace a{$a=1;}", 2},
                {"namespace b\\c{$a;} namespace b\\c{$a=1;}", 2},
                {"namespace d\\e\\f{$a;} namespace d\\e\\f{$a=1;}", 2},
                //different namespaces - no error since PHP namespace variables are implicitly global by default
                {"namespace{$a;} namespace a{$a;} namespace b\\c{$a;}", 3}
        }));

        return collection;
    }

    private static void addVariations(String prefix, String appendix) {


        collection.addAll(Arrays.asList(new Object[][]{
                {prefix + "$a=0;  $a;" + appendix, 2},
                {prefix + "$a=0; { $a=2;}" + appendix, 2},
                {prefix + "$a=0; if($a==1){}" + appendix, 2},
                {prefix + "$a=0; if(true){ $a=2;}" + appendix, 2},
                {prefix + "$a=0; if(true){}else{ $a=2;}" + appendix, 2},
                {prefix + "$a=0; if(true){ if(true){ $a=2;}}" + appendix, 2},
                {prefix + "$a=0;  $b=0; switch($a = $b){case 1: $a;break;}" + appendix, 5},
                {prefix + "$a=0;  $b=0; switch($b){case 1: $a;break;}" + appendix, 4},
                {prefix + "$a=0;  $b=0; switch($b){case 1:{$a;}break;}" + appendix, 4},
                {prefix + "$a=0;  $b=0; switch($b){default:{$a;}break;}" + appendix, 4},
                {prefix + "$a=0;  for($a=1;;){}" + appendix, 2},
                {prefix + "$a=0;  for(;$a==1;){}" + appendix, 2},
                {prefix + "$a=0;  for(;;++$a){}" + appendix, 2},
                {prefix + "$a=0;  for(;;){$a=1;}" + appendix, 2},
                {prefix + "for($a=0;;){$a=1;}" + appendix, 2},
                {prefix + "foreach([1] as $v){$v=1;}" + appendix, 2},
                {prefix + "$a=1;  foreach([1] as $v){$a=1;}" + appendix, 3},
                {prefix + "$a=1;  while($a==1){}" + appendix, 2},
                {prefix + "$a=1;  while(true)$a=1;" + appendix, 2},
                {prefix + "$a=1;  while(true){$a=1;}" + appendix, 2},
                {prefix + "$a=1;  do ; while($a==1);" + appendix, 2},
                {prefix + "$a=1;  do $a; while(true);" + appendix, 2},
                {prefix + "$a=1;  try{$a=1;}catch(\\Exception $ex){}" + appendix, 3},
                {prefix + "$a=1;  try{}catch(\\Exception $ex){$a=1;}" + appendix, 3},
                //definition in for header is accessible from outer scope in PHP
                {prefix + "for($a=1;;){} $a;" + appendix, 2},
                //definition in an catch header is accessible from outer scope
                {prefix + "try{}catch(\\Exception $e){} $e;" + appendix, 2},
                //do while does not create a conditional scope
                {prefix + "do $a=0; while(true); $a;" + appendix, 2},
                {prefix + "do{ $a=0; if(true){$a;} }while(true);" + appendix, 2},
                //in expressions
                {prefix + "$a=1;  !(1+$a-$a/$a*$a && $a) || $a;" + appendix, 7},
                {prefix + "$a=1; exit($a + $a);" + appendix, 3}
        }));
    }
}
