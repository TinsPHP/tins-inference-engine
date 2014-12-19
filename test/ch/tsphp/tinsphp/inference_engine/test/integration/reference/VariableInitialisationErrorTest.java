/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class VariableInitialisationErrorTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.inference_engine.error.DefinitionErrorDto;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceDefinitionErrorTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.exceptions.base.MockitoAssertionError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class VariableInitialisationErrorTest extends AReferenceDefinitionErrorTest
{

    private static List<Object[]> collection;

    private IInitialisedVerifier verifier;

    public VariableInitialisationErrorTest(String testString, DefinitionErrorDto[] expectedLinesAndPositions,
            IInitialisedVerifier theVerifier) {
        super(testString, expectedLinesAndPositions);
        verifier = theVerifier;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void test() throws RecognitionException {
        check();

        try {
            verifier.check(inferenceErrorReporter);
        } catch (MockitoAssertionError e) {
            System.err.println(testString + " failed.");
            throw e;
        }
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
//        addVariations("class a{ function foo(){", "return;}}");
//        addVariations("namespace a; class a{ function foo(){", "return;}}");
//        addVariations("namespace a\\b\\z{ class a{ function foo(){", "return;}}}");

        return collection;
    }

    private static void addVariations(String prefix, String appendix) {
        DefinitionErrorDto[] errorDto = new DefinitionErrorDto[]{new DefinitionErrorDto("$a", 2, 1, "$a", 3, 1)};
        DefinitionErrorDto[] twoErrorDto = new DefinitionErrorDto[]{
                new DefinitionErrorDto("$a", 2, 1, "$a", 3, 1),
                new DefinitionErrorDto("$a", 2, 1, "$a", 4, 1)
        };
        collection.addAll(Arrays.asList(new Object[][]{
                {prefix + "\n echo 'hi';\n $a;" + appendix, errorDto, new NotVerifier()},
                {prefix + "\n $b = 1;\n $a;" + appendix, errorDto, new NotVerifier()},
                {prefix + "\n if(true){\n $a;}" + appendix, errorDto, new NotVerifier()},
                {prefix + "\n while(true){\n $a;}" + appendix, errorDto, new NotVerifier()},
                {prefix + "\n switch(1){case 1:\n $a;}" + appendix, errorDto, new NotVerifier()},
                {prefix + "\n switch(1){default:\n $a;}" + appendix, errorDto, new NotVerifier()},
                {prefix + "\n if(true){$a = 1;}\n $a;" + appendix, errorDto, new PartiallyVerifier()},
                {prefix + "\n while(true){$a = 1;}\n $a;" + appendix, errorDto, new PartiallyVerifier()},
                {prefix + "\n for(;;){$a = 1;}\n $a;" + appendix, errorDto, new PartiallyVerifier()},
                {prefix + "\n for(;\n $a < 10; ){}" + appendix, errorDto, new NotVerifier()},
                {
                        prefix + "\n foreach([1] as $v){$a = 1;}\n $a;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                {
                        prefix + "\n try{$a = 1;}catch(\\Exception $e){}\n $a;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                {
                        prefix + "\n try{}catch(\\Exception $e){$a = 1;}\n $a;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                //not all catch blocks initialise
                {
                        prefix + "\n try{$a = 1;}catch(\\ErrorException $e2){}" +
                                "catch(\\Exception $e){$a = 1;}\n $a;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                {
                        prefix + "\n try{$a = 1;}catch(\\ErrorException $e2){$a = 1;}" +
                                "catch(\\Exception $e){}\n $a;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                //needs default case to be sure that it is initialised
                {
                        prefix + "\n switch(1){case 1: $a=1;}\n $a * 96;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                //not all cases initialise
                {
                        prefix + "\n switch(1){case 1: break 1; default: $a = 1;}\n $a * 78;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                //More than one
                {prefix + "\n echo 'hi';\n $a; \n $a; " + appendix, twoErrorDto, new NotVerifier(2)},
                {prefix + "\n $b=1;\n $a; \n $a; " + appendix, twoErrorDto, new NotVerifier(2)},
                {
                        prefix + "\n if(true){while(true) $a=1;\n $a;} "
                                + "if(false){$a = 1;} for(;;){}\n $a; " + appendix,
                        twoErrorDto, new PartiallyVerifier(2)
                },
                {prefix + "\n for(;\n $a < 10;\n $a++){}" + appendix, twoErrorDto, new NotVerifier(2)},
                //break before initialisation -> initialisation is dead code
                {
                        prefix + "\n switch(1){case 1: break 1; $a=1;}\n $a - 25;" + appendix,
                        errorDto, new NotVerifier()
                },
                {
                        prefix + "\n switch(1){default: break;$a=1;} \n $a + 96;" + appendix,
                        errorDto, new NotVerifier()
                },
                {
                        prefix + "\n switch(1){case 1: continue 1; $a=1;}\n $a - 25;" + appendix,
                        errorDto, new NotVerifier()
                },
                {
                        prefix + "\n switch(1){default: continue;$a=1;} \n $a + 96;" + appendix,
                        errorDto, new NotVerifier()
                },
        }));

        addDeadCodeVariations(prefix + "\n ", "return;", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "throw $b;", appendix);
        //dead code after both branches of an if return/throw
        addDeadCodeVariations(prefix + "\n ", "if(true){return;} else{return;} ", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "if(true){throw $b;} else{return;} ", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "if(true){return;} else{throw $b;} ", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "if(true){throw $b;} else{throw $b;} ", appendix);
        //dead code after all cases of switch return throw
        addDeadCodeVariations(prefix + "\n ", "switch(1){default: return;}", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "switch(1){default: throw $b;}", appendix);
        addDeadCodeVariations(prefix + "\n ", "switch(1){case 1: return; default: return;}", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "switch(1){case 1: return; default: throw $b;}", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "switch(1){case 1: throw $b; default: return;}", appendix);
        //dead code after all catches return/throw
        addDeadCodeVariations(prefix + "\n $b=1;", "try{return;}catch(\\Exception $ex){return;}", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "try{throw $b;}catch(\\Exception $ex){return;}", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "try{return;}catch(\\Exception $ex){throw $b;}", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "try{throw $b;}catch(\\Exception $ex){throw $b;}", appendix);
        addDeadCodeVariations(
                prefix + "\n $b=1;",
                "try{throw $b;}catch(\\ErrorException $ex2){throw $b;}catch(\\Exception $ex){throw $b;}",
                appendix);
        //dead code after return/try in do-while loop
        addDeadCodeVariations(prefix + "\n ", "do{return;}while(true);", appendix);
        addDeadCodeVariations(prefix + "\n $b=1;", "do{throw $b;}while(true);", appendix);
    }

    private static void addDeadCodeVariations(String prefix, String statement, String appendix) {
        DefinitionErrorDto[] errorDto = new DefinitionErrorDto[]{new DefinitionErrorDto("$a", 2, 1, "$a", 3, 1)};
        collection.addAll(Arrays.asList(new Object[][]{
                {
                        prefix + "switch(1){default: " + statement + " $a=1;} \n $a + 96;" + appendix,
                        errorDto, new NotVerifier()
                },
                {
                        prefix + "switch(1){case 1: $a=1; default: " + statement + " $a=1;} \n $a + 96;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                {
                        prefix + "switch(1){case 1: " + statement + " $a=1; default: $a=1;} \n $a + 96;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                {
                        prefix + "if(true){ " + statement + " $a=1; } \n $a + 96;" + appendix,
                        errorDto, new NotVerifier()
                },
                {
                        prefix + "if(true){ } else{ " + statement + "$a=1;} \n $a + 96;" + appendix,
                        errorDto, new NotVerifier()
                },
                {
                        prefix + "if(true){ " + statement + " $a=1; } else{ $a=1;} \n $a + 96;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                {
                        prefix + "if(true){ $a=1; } else{ " + statement + " $a=1;} \n $a + 96;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                {
                        prefix + "while(true){" + statement + "$a = 1;}\n $a;" + appendix, errorDto,
                        new NotVerifier()
                },
                {
                        prefix + "for(;;){" + statement + "$a = 1;}\n $a;" + appendix, errorDto,
                        new NotVerifier()
                },
                {
                        prefix + "try{" + statement + "$a = 1;}catch(\\Exception $e){}\n $a;" + appendix,
                        errorDto, new NotVerifier()
                },
                {
                        prefix + "try{}catch(\\Exception $e){" + statement + "$a = 1;}\n $a;" + appendix,
                        errorDto, new NotVerifier()
                },
                {
                        prefix + "try{" + statement + "$a = 1;}catch(\\Exception $e){$a=1;}\n $a;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                {
                        prefix + "try{$a=1;}catch(\\Exception $e){" + statement + "$a = 1;}\n $a;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                //not all catch blocks initialise
                {
                        prefix + "try{$a = 1;}catch(\\ErrorException $e2){" + statement + "$a = 1;}" +
                                "catch(\\Exception $e){$a = 1;}\n $a;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
                {
                        prefix + "try{$a = 1;}catch(\\ErrorException $e2){$a = 1;}" +
                                "catch(\\Exception $e){" + statement + "$a = 1;}\n $a;" + appendix,
                        errorDto, new PartiallyVerifier()
                },
        }));
    }

    @Override
    protected IInferenceIssueReporter createInferenceErrorReporter() {
        return spy(super.createInferenceErrorReporter());
    }

    private static class PartiallyVerifier implements IInitialisedVerifier
    {
        private int times;

        public PartiallyVerifier() {
            this(1);
        }

        public PartiallyVerifier(int howManyTimes) {
            times = howManyTimes;
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        @Override
        public void check(IInferenceIssueReporter inferenceErrorReporter) {
            verify(inferenceErrorReporter, times(times))
                    .variablePartiallyInitialised(any(ITSPHPAst.class), any(ITSPHPAst.class));
        }
    }

    private static class NotVerifier implements IInitialisedVerifier
    {
        private int times;

        public NotVerifier() {
            this(1);
        }

        public NotVerifier(int howManyTimes) {
            times = howManyTimes;
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        @Override
        public void check(IInferenceIssueReporter inferenceErrorReporter) {
            verify(inferenceErrorReporter, times(times))
                    .variableNotInitialised(any(ITSPHPAst.class), any(ITSPHPAst.class));
        }
    }

    private static interface IInitialisedVerifier
    {
        void check(IInferenceIssueReporter inferenceErrorReporter);
    }
}
