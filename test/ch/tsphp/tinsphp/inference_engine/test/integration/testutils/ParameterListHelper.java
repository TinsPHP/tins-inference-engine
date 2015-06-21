/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ParameterListHelper from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ParameterListHelper
{

    private static String prefix;
    private static String appendix;
    private static String prefixExpected;
    private static String scopeName;
    private static boolean isDefinitionPhase;

    private ParameterListHelper() {
    }

    public static Collection<Object[]> getTestStrings(final String thePrefix, final String theAppendix,
            final String thePrefixExpected, final String theScopeName,
            final boolean isItDefinitionPhase) {

        prefix = thePrefix;
        appendix = theAppendix;
        prefixExpected = thePrefixExpected;
        scopeName = theScopeName;
        isDefinitionPhase = isItDefinitionPhase;

        //check all types
        final List<Object[]> collection = new ArrayList<>();

        collection.add(new Object[]{
                prefix + " $a" + appendix,
                prefixExpected + scopeName + "? "
                        + scopeName + "$a" + (isDefinitionPhase ? "" : "?")
        });
        String[] types = TestTypeHelper.getClassInterfaceTypes();
        for (String type : types) {
            collection.add(new Object[]{
                    prefix + type + " $a" + appendix,
                    prefixExpected + scopeName + type + " "
                            + scopeName + "$a" + (isDefinitionPhase ? "" : type)
            });
            collection.add(new Object[]{
                    prefix + type + " $a=1" + appendix,
                    prefixExpected + scopeName + type + " "
                            + scopeName + "$a" + (isDefinitionPhase ? "" : type)
            });
        }
        collection.addAll(getVariations("A", "A", ""));
        collection.addAll(getVariations("", "?", ""));

        collection.addAll(getVariationsForOptional());

        return collection;
    }

    private static Collection<Object[]> getVariations(String type, String typeExpected, String typeModifierExpected) {


        String dynPrefix = scopeName + typeExpected + " " + scopeName;
        String dynAppendix = (isDefinitionPhase ? "" : typeExpected) + typeModifierExpected;

        String paramStat1 = "$x";
        String paramStat2 = "$y";
        String paramStat1Expected = scopeName + "? " + scopeName + "$x" + (isDefinitionPhase ? "" : "?");
        String paramStat2Expected = scopeName + "? " + scopeName + "$y" + (isDefinitionPhase ? "" : "?");

        return Arrays.asList(new Object[][]{
                {
                        prefix + type + " $a" + appendix,
                        prefixExpected + dynPrefix + "$a" + dynAppendix
                },
                {
                        prefix + type + " $a" + "," + paramStat1 + appendix,
                        prefixExpected
                                + dynPrefix + "$a" + dynAppendix + " "
                                + paramStat1Expected
                },
                {
                        prefix + paramStat1 + "," + type + " $a" + appendix,
                        prefixExpected
                                + paramStat1Expected + " "
                                + dynPrefix + "$a" + dynAppendix
                },
                {
                        prefix + type + " $a" + ", " + paramStat1 + ", " + paramStat2 + appendix,
                        prefixExpected
                                + dynPrefix + "$a" + dynAppendix + " "
                                + paramStat1Expected + " "
                                + paramStat2Expected
                },
                {
                        prefix + type + " $a" + ", " + type + " $b" + ", " + paramStat1 + appendix,
                        prefixExpected
                                + dynPrefix + "$a" + dynAppendix + " "
                                + dynPrefix + "$b" + dynAppendix + " "
                                + paramStat1Expected
                },
                {
                        prefix + type + " $a" + ", " + paramStat1 + "," + type + " $b" + "" + appendix,
                        prefixExpected
                                + dynPrefix + "$a" + dynAppendix + " "
                                + paramStat1Expected + " "
                                + dynPrefix + "$b" + dynAppendix
                },
                {
                        prefix + paramStat1 + "," + type + " $a" + ", " + paramStat2 + appendix,
                        prefixExpected
                                + paramStat1Expected + " "
                                + dynPrefix + "$a" + dynAppendix + " "
                                + paramStat2Expected
                },
                {
                        prefix + paramStat1 + "," + type + " $a" + ", " + type + " $b" + "" + appendix,
                        prefixExpected
                                + paramStat1Expected + " "
                                + dynPrefix + "$a" + dynAppendix + " "
                                + dynPrefix + "$b" + dynAppendix
                },
                {
                        prefix + type + " $a, " + type + " $b , " + type + " $c" + appendix,
                        prefixExpected
                                + dynPrefix + "$a" + dynAppendix + " "
                                + dynPrefix + "$b" + dynAppendix + " "
                                + dynPrefix + "$c" + dynAppendix
                }
        });
    }

    private static Collection<Object[]> getVariationsForOptional() {

        String typeExpected = isDefinitionPhase ? "" : "A";

        String a = prefixExpected + scopeName + "A " + scopeName + "$a" + typeExpected;
        String b = scopeName + "A " + scopeName + "$b" + typeExpected;
        String c = scopeName + "A " + scopeName + "$c" + typeExpected;
        String d = scopeName + "A " + scopeName + "$d" + typeExpected;

        List<Object[]> collection = new ArrayList<>();
        collection.addAll(Arrays.asList(new Object[][]{
                //optional parameter
                {
                        prefix + "A $a, A $b='hallo'" + appendix,
                        a + " " + b
                },
                {
                        prefix + "A $a=null, A $b, A $c=+1" + appendix,
                        a + " "
                                + b + " "
                                + c
                },
                {
                        prefix + "A $a,A $b, A $c=-10, A $d=2.0" + appendix,
                        a + " "
                                + b + " "
                                + c + " "
                                + d
                },
                {
                        prefix + "A $a=null,A $b=true, A $c=E_ALL" + appendix,
                        a + " "
                                + b + " "
                                + c
                },
                {
                        prefix + "A $a, A $b=false, A $c=null" + appendix,
                        a + " "
                                + b + " "
                                + c
                },
                {
                        prefix + "A $a=true, A $b, A $c" + appendix,
                        a + " "
                                + b + " "
                                + c
                },
        }));

//TODO rstoll TINS-161 inference OOP
//        String[] types = TypeHelper.getClassInterfaceTypes();
//
//        for (String type : types) {
//            collection.add(new Object[]{prefix + "$a=" + type + "::a" + appendix, a});
//        }
        return collection;
    }
}