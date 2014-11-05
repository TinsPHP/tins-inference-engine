/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.tinsphp.inference_engine.error.ReferenceErrorDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ReturnCheckHelper
{

    public static Collection<Object[]> getTestStringVariations(String prefix, String appendix) {
        List<Object[]> collection = new ArrayList<>();
        collection.addAll(getTestStringVariations(prefix, appendix, "return 12;"));
        collection.addAll(getTestStringVariations(prefix, appendix, "throw $a;"));
        collection.addAll(Arrays.asList(new Object[][]{
                {prefix + "if(true){return 1;}else{throw $a;}" + appendix},
                {prefix + "if(true){throw $a;}else{return 0;}" + appendix},
        }));
        return collection;
    }

    private static Collection<Object[]> getTestStringVariations(String prefix, String appendix, String statement) {
        return Arrays.asList(new Object[][]{
                {prefix + statement + appendix},
                {prefix + "{" + statement + "}" + appendix},
                {prefix + "if(true){" + statement + "}else{" + statement + "}" + appendix},
                {prefix + "$a; " + statement + appendix},
                {prefix + "$a; {" + statement + "} $b;" + appendix},
                {prefix + "do{" + statement + "}while(true);" + appendix},
                {prefix + "switch(1){default:" + statement + "}" + appendix},
                {prefix + "switch(1){case 1:" + statement + " default:" + statement + "}" + appendix},
                {
                        prefix + "switch(1){case 1:" + statement + "case 2: " + statement
                                + " default:" + statement + "}" + appendix
                },
                {prefix + "switch(1){case 1: default:" + statement + "}" + appendix},
                {prefix + "try{" + statement + "}catch(\\Exception $e){" + statement + "}" + appendix},
                {
                        prefix + "try{" + statement + "}"
                                + "catch(\\ErrorException $e){" + statement + "}"
                                + "catch(\\Exception $e2){" + statement + "}" +
                                appendix
                },
                {prefix + "$a; if(true){ }" + statement + appendix},
                {
                        prefix + "$a; if(true){ } $b;"
                                + "if(true){"
                                + "if(true){}"
                                + "if(true){" + statement + "}else{" + statement + "}"
                                + "}else{ "
                                + "while(true){}" + statement
                                + "}" + appendix
                }
        });
    }

    public static Collection<Object[]> getReferenceErrorPairs(String prefix, String appendix,
            ReferenceErrorDto[] errorDto) {
        List<Object[]> collection = new ArrayList<>();
        collection.addAll(getErrorPairVariations(prefix, appendix, "return 12;", errorDto));
        collection.addAll(getErrorPairVariations(prefix, appendix, "throw $a;", errorDto));
        return collection;
    }

    public static Collection<Object[]> getErrorPairVariations(String prefix, String appendix, String statement,
            ReferenceErrorDto[] errorDto) {
        return Arrays.asList(new Object[][]{
                {prefix + "" + appendix, errorDto},
                {prefix + "$a;" + appendix, errorDto},
                {prefix + "$a; $f;" + appendix, errorDto},
                {prefix + "if(true){" + statement + "}" + appendix, errorDto},
                {prefix + "while(true){" + statement + "}" + appendix, errorDto},
                {prefix + "for(;;){" + statement + "}" + appendix, errorDto},
                {prefix + "foreach([1,2] as $v){" + statement + "}" + appendix, errorDto},
                {prefix + "try{" + statement + "}catch(\\Exception $e){}" + appendix, errorDto},
                {prefix + "try{}catch(\\Exception $e){" + statement + "}" + appendix, errorDto},
                //not all catch blocks return/throw
                {
                        prefix + "try{" + statement + "}"
                                + "catch(\\ErrorException $e){} catch(\\Exception $e2){" + statement + "}" + appendix,
                        errorDto
                },
                {
                        prefix + "try{" + statement + "}"
                                + "catch(\\ErrorException $e){" + statement + "} catch(\\Exception $e2){}" + appendix,
                        errorDto
                },
                //needs default case to be sure that it returns
                {prefix + "switch(1){case 1:" + statement + "}" + appendix, errorDto},
                //break before return/throw statement
                {prefix + "switch(1){case 1: break; " + statement + "}" + appendix, errorDto},
                {prefix + "switch(1){default: break; " + statement + "}" + appendix, errorDto},
                //not all cases return/throw
                {
                        prefix + "switch(1){case 1: break; default: " + statement + "}" +
                                appendix,
                        errorDto
                },
        });
    }

    public static Collection<Object[]> getImplicitReturnAst(
            String prefix, String appendix, String expectedStatement, String expectedAppendix) {
        List<Object[]> collection = new ArrayList<>();
        collection.addAll(getImplicitReturnAstVariations(
                prefix, "return 12;", appendix, expectedStatement, "(return 12)", expectedAppendix));
        collection.addAll(getImplicitReturnAstVariations(
                prefix, "throw $a;", appendix, expectedStatement, "(throw $a)", expectedAppendix));
        return collection;
    }

    public static Collection<Object[]> getImplicitReturnAstVariations(
            String prefix, String statement, String appendix,
            String expectedPrefix, String expectedStatement, String expectedAppendix) {
        String returnNull = "(return null)";
        return Arrays.asList(new Object[][]{
                {prefix + "" + appendix, expectedPrefix + returnNull + expectedAppendix},
                {prefix + "$a;" + appendix, expectedPrefix + "(expr $a) " + returnNull + expectedAppendix},
                {prefix + "$a; $f;" + appendix, expectedPrefix + "(expr $a) (expr $f) " + returnNull +
                        expectedAppendix},
                {
                        prefix + "if(true){" + statement + "}" + appendix,
                        expectedPrefix + "(if true (cBlock " + expectedStatement + ")) " + returnNull + expectedAppendix
                },
                {
                        prefix + "while(true){" + statement + "}" + appendix,
                        expectedPrefix + "(while true (cBlock " + expectedStatement + ")) " + returnNull +
                                expectedAppendix
                },
                {
                        prefix + "for(;;){" + statement + "}" + appendix,
                        expectedPrefix + "(for exprs exprs exprs (cBlock " + expectedStatement + ")) "
                                + returnNull + expectedAppendix
                },
                {
                        prefix + "foreach([1,2] as $v){" + statement + "}" + appendix,
                        expectedPrefix + "(foreach (array 1 2) (vars (type tMod ?) $v) "
                                + "(cBlock " + expectedStatement + ")) "
                                + returnNull + expectedAppendix
                },
                {
                        prefix + "try{" + statement + "}catch(\\Exception $e){}" + appendix,
                        expectedPrefix + "(try (cBlock " + expectedStatement + ") "
                                + "(catch (vars (type tMod \\Exception) $e) cBlock)) "
                                + returnNull + expectedAppendix
                },
                {
                        prefix + "try{}catch(\\Exception $e){" + statement + "}" + appendix,
                        expectedPrefix + "(try cBlock "
                                + "(catch (vars (type tMod \\Exception) $e) (cBlock " + expectedStatement + "))) "
                                + returnNull + expectedAppendix
                },
                //not all catch blocks return/throw
                {
                        prefix + "try{" + statement + "}"
                                + "catch(\\ErrorException $e){} catch(\\Exception $e2){" + statement + "}" + appendix,
                        expectedPrefix + "(try (cBlock " + expectedStatement + ") "
                                + "(catch (vars (type tMod \\ErrorException) $e) cBlock) "
                                + "(catch (vars (type tMod \\Exception) $e2) (cBlock " + expectedStatement + "))) "
                                + returnNull + expectedAppendix
                },
                {
                        prefix + "try{" + statement + "}"
                                + "catch(\\ErrorException $e){" + statement + "} catch(\\Exception $e2){}" + appendix,
                        expectedPrefix + "(try (cBlock " + expectedStatement + ") "
                                + "(catch (vars (type tMod \\ErrorException) $e) (cBlock " + expectedStatement + ")) "
                                + "(catch (vars (type tMod \\Exception) $e2) cBlock)) "
                                + returnNull + expectedAppendix
                },
                //needs default case to be sure that it returns
                {
                        prefix + "switch(1){case 1:" + statement + "}" + appendix,
                        expectedPrefix + "(switch 1 (cases 1) (cBlock " + expectedStatement + ")) "
                                + returnNull + expectedAppendix
                },
                //break before return/throw statement
                {
                        prefix + "switch(1){case 1: break; " + statement + "}" + appendix,
                        expectedPrefix + "(switch 1 (cases 1) (cBlock break " + expectedStatement + ")) "
                                + returnNull + expectedAppendix
                },
                {
                        prefix + "switch(1){default: break; " + statement + "}" + appendix,
                        expectedPrefix + "(switch 1 (cases default) (cBlock break " + expectedStatement + ")) "
                                + returnNull + expectedAppendix
                },
                //not all cases return/throw
                {
                        prefix + "switch(1){case 1: break; default: " + statement + "}" +
                                appendix,
                        expectedPrefix + "(switch 1 (cases 1) (cBlock break) (cases default) "
                                + "(cBlock " + expectedStatement + ")) "
                                + returnNull + expectedAppendix
                },
        });
    }
}
