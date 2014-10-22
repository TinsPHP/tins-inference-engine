/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ScopeTestHelper from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ScopeTestHelper
{

    public static String getEnclosingScopeNames(IScope scope) {
        StringBuilder stringBuilder = new StringBuilder();
        while (scope != null) {
            stringBuilder.insert(0, ".");
            stringBuilder.insert(0, scope.getScopeName());
            scope = scope.getEnclosingScope();
        }
        return stringBuilder.toString();
    }

    public static Collection<Object[]> testStringsDefaultNamespace() {
        return testStrings("", "", "\\.\\", new Integer[]{1});
    }

    public static Collection<Object[]> testStrings(String prefix, String appendix,
            String fullScopeName, Integer[] accessToScope) {

        fullScopeName += ".";
        List<Object[]> collection = new ArrayList<>();

        String[][] variableIds = new String[][]{
                {"$b", "$b"},
                {"$this", "$this"}
        };

        for (String[] variableId : variableIds) {
            collection.addAll(getVariations(prefix, appendix, variableId[0], variableId[1],
                    fullScopeName, accessToScope));
            collection.addAll(getAccessVariations(prefix, appendix, variableId[0], variableId[1],
                    fullScopeName, accessToScope));
        }

        variableIds = new String[][]{
                {"self::$b", "self"},
                {"parent::$b", "parent"},
                {"foo()", "foo()"},
                {"$a->foo()", "$a"},
                {"$this->foo()", "$this"},
                {"self::foo()", "self"},
                {"parent::foo()", "parent"}
        };

        for (String[] variableId : variableIds) {
            collection.addAll(getVariations(prefix, appendix, variableId[0], variableId[1],
                    fullScopeName, accessToScope, new Integer[]{0}));
            collection.addAll(getAccessVariations(prefix, appendix, variableId[0], variableId[1],
                    fullScopeName, accessToScope, new Integer[]{0}));
        }

        collection.addAll(getVariations(prefix, appendix, "b", "b#",
                fullScopeName, accessToScope));
        collection.addAll(getVariations(prefix, appendix, "self::b", "self",
                fullScopeName, accessToScope, new Integer[]{0}));
        collection.addAll(getVariations(prefix, appendix, "parent::b", "parent",
                fullScopeName, accessToScope, new Integer[]{0}));

        String[] types = TypeHelper.getClassInterfaceTypes();
        for (String type : types) {
            collection.addAll(getVariations(prefix, appendix, type + "::b", type,
                    fullScopeName, accessToScope, new Integer[]{0}));
            collection.addAll(getVariations(prefix, appendix, type + "::$b", type,
                    fullScopeName, accessToScope, new Integer[]{0}));
            collection.addAll(getAccessVariations(prefix, appendix, type + "::$b", type,
                    fullScopeName, accessToScope, new Integer[]{0}));
            collection.addAll(getAccessVariations(prefix, appendix, type + "::foo()", type,
                    fullScopeName, accessToScope, new Integer[]{0}));
        }


        return collection;
    }

    private static Collection<Object[]> getVariations(String prefix, String appendix, String variableId, String astText,
            String fullScopeName, Integer[] accessToScope) {
        return getVariations(prefix, appendix, variableId, astText, fullScopeName, accessToScope, new Integer[]{});
    }

    public static Collection<Object[]> getVariations(String prefix, String appendix, String variableId, String astText,
            String fullScopeName, Integer[] accessToScope, Integer[] stepIn) {
        Integer[] emptyStepIn = new Integer[]{};
        return Arrays.asList(new Object[][]{
                {prefix + "int $a = " + variableId + ";" + appendix, new ScopeTestStruct[]{
                        // vars $a $b
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 1, 0))
                }
                },
                {prefix + "int $a = " + variableId + " + $c;" + appendix, new ScopeTestStruct[]{
                        //vars $a + variableId
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 1, 0, 0)),
                        //vars $a + $c
                        new ScopeTestStruct("$c", fullScopeName,
                                getAstAccessOrder(accessToScope, emptyStepIn, 0, 1, 0, 1))
                }
                },
                {prefix + "int $a = $c + " + variableId + ";" + appendix, new ScopeTestStruct[]{
                        //vars $a + $c
                        new ScopeTestStruct("$c", fullScopeName,
                                getAstAccessOrder(accessToScope, emptyStepIn, 0, 1, 0, 0)),
                        //vars $a + variableId
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 1, 0, 1))
                }
                },
                {prefix + "int $a = 1 + " + variableId + " + $c;" + appendix, new ScopeTestStruct[]{
                        //vars $a + + variableId
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 1, 0, 0, 1)),
                        //vars $a + $c
                        new ScopeTestStruct("$c", fullScopeName,
                                getAstAccessOrder(accessToScope, emptyStepIn, 0, 1, 0, 1))
                }
                },
                {prefix + variableId + " = $a;" + appendix, new ScopeTestStruct[]{
                        //expr = variableId
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 0, 0)),
                        //expr = $a
                        new ScopeTestStruct("$a", fullScopeName,
                                getAstAccessOrder(accessToScope, emptyStepIn, 0, 0, 1))
                }
                },
                //there are no nested local scopes
                {prefix + " { " + variableId + " = $a; } " + appendix, new ScopeTestStruct[]{
                        //expr = variableId
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 0, 0)),
                        //expr = $a
                        new ScopeTestStruct("$a", fullScopeName,
                                getAstAccessOrder(accessToScope, emptyStepIn, 0, 0, 1))
                }
                },
                //there are no nested local scopes, does not matter how many {} we declare
                {prefix + " { { $a = " + variableId + ";} int $a = $c; } " + appendix, new ScopeTestStruct[]{
                        //expr = $a
                        new ScopeTestStruct("$a", fullScopeName,
                                getAstAccessOrder(accessToScope, emptyStepIn, 0, 0, 0)),
                        //expr = variableId
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 0, 1)),
                        //vars $a $c
                        new ScopeTestStruct("$c", fullScopeName,
                                getAstAccessOrder(accessToScope, emptyStepIn, 1, 1, 0))
                }
                }
        });
    }

    private static Collection<Object[]> getAccessVariations(String prefix, String appendix, String variableId,
            String astText, String fullScopeName, Integer[] accessToScope) {
        return getAccessVariations(prefix, appendix, variableId, astText, fullScopeName, accessToScope,
                new Integer[]{});
    }

    private static Collection<Object[]> getAccessVariations(String prefix, String appendix, String variableId,
            String astText, String fullScopeName, Integer[] accessToScope, Integer stepIn[]) {


        return Arrays.asList(new Object[][]{
                {prefix + variableId + "[0];" + appendix, new ScopeTestStruct[]{
                        //expr arrAccess variableId
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 0, 0))
                }
                },
                {prefix + variableId + "[1+1][0];" + appendix, new ScopeTestStruct[]{
                        //arrAccess arrAccess variableId
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 0, 0, 0))
                }
                },
                {prefix + variableId + "->foo();" + appendix, new ScopeTestStruct[]{
                        //smCall/mCall/fCall variableId
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 0, 0))
                }
                },
                {prefix + variableId + "->foo()->bar();" + appendix, new ScopeTestStruct[]{
                        //smCall/mCall/fCall smCall/mCall/fCall variableId
                        new ScopeTestStruct(astText, fullScopeName,
                                getAstAccessOrder(accessToScope, stepIn, 0, 0, 0, 0))
                }
                }
        });
    }

    private static List<Integer> getAstAccessOrder(Integer[] accessToScope, Integer[] stepIn,
            Integer... accessToTestCandidate) {
        List<Integer> accessOrder = new ArrayList<>();
        accessOrder.addAll(Arrays.asList(accessToScope));
        accessOrder.addAll(Arrays.asList(accessToTestCandidate));
        accessOrder.addAll(Arrays.asList(stepIn));
        return accessOrder;
    }

    public static ITSPHPAst getAst(
            ITSPHPAst ast, String testString, ScopeTestStruct testStruct) {

        ITSPHPAst tmp = ast;
        ITSPHPAst tmp2 = ast;
        for (Integer index : testStruct.accessOrderToNode) {
            if (index != null) {
                org.junit.Assert.assertNotNull(
                        testString + " failed.\n"
                                + "Could not reach path to " + testStruct.astText + "\n"
                                + "Last node was " + tmp2.getText(),
                        tmp);
                tmp2 = tmp;
                tmp = tmp.getChild(index);
            }
        }
        return tmp;
    }
}
