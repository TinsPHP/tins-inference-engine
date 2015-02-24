/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class VariableInitialisationTest from the TSPHP project.
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
public class VariableInitialisationTest extends AVerifyTimesReferenceTest
{

    private static List<Object[]> collection;

    public VariableInitialisationTest(String testString, int howManyTimes) {
        super(testString, howManyTimes);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Override
    protected void verifyTimes() {
        verify(referencePhaseController, times(howManyTimes)).checkIsVariableInitialised(any(ITSPHPAst.class));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        collection = new ArrayList<>();

        //global
        addVariations("", "");
        addVariations("namespace a;", "");
        addVariations("namespace a\\b\\z{", "}");

        //functions
        addVariations("function foo(){", "return;}");
        addVariations("namespace a;function foo(){", "return;}");
        addVariations("namespace a\\b\\z{function foo(){", "return;}}");

        //TODO rstoll TINS-161 inference OOP
        //methods
//        addVariations("class a{ function void foo(){", "return;}}");
//        addVariations("namespace a; class a{ function void foo(){", "return;}}");
//        addVariations("namespace a\\b\\z{ class a{ function void foo(){", "return;}}}");

        collection.addAll(Arrays.asList(new Object[][]{
                //same namespace
                {"namespace{$a=1;} namespace{$a;} namespace{$a;}", 3},
                {"namespace a{$a=1;} namespace a{$a;} namespace a{$a;}", 3},
                {"namespace b\\c{$a=1;} namespace b\\c{$a;} namespace b\\c{$a;}", 3},
                {"namespace d\\e\\f{$a=1;} namespace d\\e\\f{$a;} namespace d\\e\\f{$a;}", 3},
                //different namespaces - no error since PHP namespace variables are implicitly global by default
                {"namespace{$a=1;} namespace a{$a;} namespace b\\c{$a;}", 3}
        }));

        return collection;
    }

    private static void addVariations(String prefix, String appendix) {
        collection.addAll(Arrays.asList(new Object[][]{
                {prefix + "$a=1; $a;" + appendix, 2},
                {prefix + "$a=1; $a; $a;" + appendix, 3},
                {prefix + "$a=1; { $a; $a;}" + appendix, 3},
                {prefix + "$a=1; if($a==1){}" + appendix, 2},
                {prefix + "$a=1; if(true){ $a;}" + appendix, 2},
                {prefix + "$a=1; if(true){}else{ $a;}" + appendix, 2},
                {prefix + "$a=1; if(true){ if(true){ $a;}}" + appendix, 2},
                {prefix + "$a=1; $b=1; switch($a == $b){case 1: $a;break;}" + appendix, 5 /*$b 2x as well*/},
                {prefix + "$a=1; $b=2; switch($b){case 1: $a;break;}" + appendix, 4 /*$b 2x as well*/},
                {prefix + "$a=1; $b=3; switch($b){case 1:{$a;}break;}" + appendix, 4 /*$b 2x as well*/},
                {prefix + "$a=1; $b=4; switch($b){default:{$a;}break;}" + appendix, 4 /*$b 2x as well*/},
                {prefix + "$a=1; for($a;;){}" + appendix, 2},
                {prefix + "$a=1; for(;$a==1;){}" + appendix, 2},
                {prefix + "$a=1; for(;;++$a){}" + appendix, 2},
                {prefix + "$a=1; for(;;){$a;}" + appendix, 2},
                {prefix + "for($a=0; $a < 10; ){}" + appendix, 2},
                {prefix + "for($a=0; ; ++$a){}" + appendix, 2},
                {prefix + "for($a=0; ;){$a;}" + appendix, 2},
                {prefix + "for(;$a=0,$a<10;){}" + appendix, 2},
                {prefix + "for(;$a=0,true; ++$a){}" + appendix, 2},
                {prefix + "for(;$a=0,true; ){$a+1;}" + appendix, 2},
                {prefix + "$a=2; foreach([1] as $v){$a-=1;}" + appendix, 2},
                {prefix + "$a=2; while($a==1){}" + appendix, 2},
                {prefix + "$a=2; while(true)$a-1;" + appendix, 2},
                {prefix + "$a=2; while(true){$a/1;}" + appendix, 2},
                {prefix + "$a=2; do ; while($a==1);" + appendix, 2},
                {prefix + "$a=2; do $a; while(true);" + appendix, 2},
                {prefix + "$a=2; try{$a*2;}catch(\\Exception $ex){}" + appendix, 2},
                {prefix + "$a=2; try{}catch(\\Exception $ex){$a+=1;}" + appendix, 2},
                //in expression (ok $a; is also an expression but at the top of the AST)
                {prefix + "$a=1;  !(1+$a-$a/$a*$a && $a) || $a;" + appendix, 7},
                //definition in for header is not in a conditional scope and thus accessible from outer scope
                {prefix + "for($a=0;;){} $a;" + appendix, 2},
                //definition in an catch header is not an conditional scope and thus accessible from outer scope
                {prefix + "try{}catch(\\Exception $e){} $e=1;" + appendix, 1},
                //do while does not create a conditional scope
                {prefix + "do{$a=1;} while(true); $a;" + appendix, 2},
                {prefix + "do{$a=1; if(true){$a;} }while(true); $a;" + appendix, 3},
                //implicit initialisations
                {prefix + "try{}catch(\\Exception $e){ $e;}" + appendix, 1},
                {prefix + "foreach([1] as $v){$v;}" + appendix, 1},
                {prefix + "foreach([1] as $k => $v){$k; $v;}" + appendix, 2}
        }));
    }
}
