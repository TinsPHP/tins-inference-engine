/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class FunctionDoubleDefinitionErrorTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.tinsphp.common.issues.DefinitionIssueDto;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceDefinitionErrorTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class FunctionDoubleDefinitionErrorTest extends AReferenceDefinitionErrorTest
{
    public FunctionDoubleDefinitionErrorTest(String testString, DefinitionIssueDto[] expectedLinesAndPositions) {
        super(testString, expectedLinesAndPositions);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        Collection<Object[]> collection = new ArrayList<>();

        collection.addAll(getNamespaceWithoutBracketVariations("", ""));
        collection.addAll(getNamespaceWithoutBracketVariations("namespace a;", ""));
        collection.addAll(getNamespaceWithoutBracketVariations("namespace a\\b;", ""));

        collection.addAll(getNamespaceBracketVariations("namespace{", "}"));
        collection.addAll(getNamespaceBracketVariations("namespace a{", "}"));
        collection.addAll(getNamespaceBracketVariations("namespace a\\b\\z{", "}"));

        return collection;
    }

    public static Collection<Object[]> getNamespaceWithoutBracketVariations(
            final String prefix, final String appendix) {

        Collection<Object[]> collection = new ArrayList<>();
        DefinitionIssueDto[] errorDto = new DefinitionIssueDto[]{new DefinitionIssueDto("foo()", 2, 1, "foo()", 3, 1)};
        DefinitionIssueDto[] errorDtoTwo = new DefinitionIssueDto[]{
                new DefinitionIssueDto("foo()", 2, 1, "foo()", 3, 1),
                new DefinitionIssueDto("foo()", 2, 1, "foo()", 4, 1)
        };

        collection.addAll(Arrays.asList(new Object[][]{
                {prefix + "function \n foo(){return 1;} function \n foo(){return 1;}" + appendix, errorDto},
                {
                        prefix + "function \n foo(){return 1;}"
                                + "function \n foo(){return 1;}"
                                + "function \n foo(){return 1;}" + appendix,
                        errorDtoTwo
                },
                {
                        prefix + "function \n foO(){return 1;} function \n foo(){return 1;}" + appendix,
                        new DefinitionIssueDto[]{
                                new DefinitionIssueDto("foO()", 2, 1, "foo()", 3, 1)
                        }
                },
                {
                        prefix + "function \n foO(){return 1;} "
                                + "function \n foo(){return 1;}"
                                + "function \n fOO(){return 1;}" + appendix,
                        new DefinitionIssueDto[]{
                                new DefinitionIssueDto("foO()", 2, 1, "foo()", 3, 1),
                                new DefinitionIssueDto("foO()", 2, 1, "fOO()", 4, 1)
                        }
                },
                //parameter name does not matter
                {
                        prefix + "function \n foo($a){return 1;} function \n foo($b){return 1;}" + appendix,
                        errorDto
                },
                {
                        prefix + "function \n foo($a){return 1;}"
                                + "function \n foo($b){return 1;}"
                                + "function \n foo($c){return 1;}" + appendix,
                        errorDtoTwo
                },
                //number of parameters does not matter
                {
                        prefix + "function \n foo($a){return 1;} function \n foo($a, $b){return 1;}" + appendix,
                        errorDto
                },
                {
                        prefix + "function \n foo($a){return 1;}"
                                + "function \n foo($a, $b){return 1;}"
                                + "function \n foo($a, $b, $c){return 1;}" + appendix,
                        errorDtoTwo
                },
        }));

        String[] types = new String[]{"array"};
        for (String type : types) {
            collection.addAll(Arrays.asList(new Object[][]{
                    //PHP does not yet support return type hints
//                    //it does not matter if return values are different
//                    {
//                            prefix + "function " + type + " \n foo(){return 1;} function \n foo(){return 1;}" +
// appendix,
//                            errorDto
//                    },
//                    {
//                            prefix + "function " + type + " \n foo(){return 1;}"
//                                    + "function \n foo(){return 1;}"
//                                    + "function \n foo(){return 1;}" + appendix,
//                            errorDtoTwo
//                    },
                    //parameter type does not matter
                    {
                            prefix + "function \n foo(" + type + " $a){return 1;} "
                                    + "function \n foo(\\Exception $a){return 1;}" + appendix,
                            errorDto
                    },
                    {
                            prefix + "function \n foo(" + type + " $a){return 1;} "
                                    + "function \n foo(\\ErrorException $a){return 1;} "
                                    + "function \n foo(\\Exception $a){return 1;}" + appendix,
                            errorDtoTwo
                    },
            }));
        }
        return collection;
    }

    public static Collection<Object[]> getNamespaceBracketVariations(final String prefix, final String appendix) {
        Collection<Object[]> collection = new ArrayList<>();

        DefinitionIssueDto[] errorDto = new DefinitionIssueDto[]{new DefinitionIssueDto("foo()", 2, 1, "foo()", 3, 1)};
        DefinitionIssueDto[] errorDtoTwo = new DefinitionIssueDto[]{
                new DefinitionIssueDto("foo()", 2, 1, "foo()", 3, 1),
                new DefinitionIssueDto("foo()", 2, 1, "foo()", 4, 1)
        };

        collection.addAll(Arrays.asList(new Object[][]{
                {
                        prefix + "function \n foo(){return 1;}" + appendix + " "
                                + prefix + "function \n foo(){return 1;}" + appendix,
                        errorDto
                },
                {
                        prefix + "function \n foo(){return 1;}" + appendix + " "
                                + prefix + "function \n foo(){return 1;}" + appendix
                                + prefix + "function \n foo(){return 1;}" + appendix,
                        errorDtoTwo
                },
                //case insensitive check
                {
                        prefix + "function \n foO(){return 1;}" + appendix + " "
                                + prefix + "function \n foo(){return 1;}" + appendix,
                        new DefinitionIssueDto[]{
                                new DefinitionIssueDto("foO()", 2, 1, "foo()", 3, 1)
                        }
                },
                {
                        prefix + "function \n foo(){return 1;}" + appendix + " "
                                + prefix + "function \n FOO(){return 1;}" + appendix + " "
                                + prefix + "function \n foO(){return 1;}" + appendix + " "
                                + prefix + "function \n foo(){return 1;}" + appendix,
                        new DefinitionIssueDto[]{
                                new DefinitionIssueDto("foo()", 2, 1, "FOO()", 3, 1),
                                new DefinitionIssueDto("foo()", 2, 1, "foO()", 4, 1),
                                new DefinitionIssueDto("foo()", 2, 1, "foo()", 5, 1)
                        }
                },
                //parameter name does not matter
                {
                        prefix + "function \n foo($a){return 1;}" + appendix + " "
                                + prefix + "function \n foo($b){return 1;}" + appendix,
                        errorDto
                },
                {
                        prefix + "function \n foo($a){return 1;}" + appendix + " "
                                + prefix + "function \n foo($b){return 1;}" + appendix + " "
                                + prefix + "function \n foo($c){return 1;}" + appendix,
                        errorDtoTwo
                },
                //number of parameters does not matter
                {
                        prefix + "function \n foo($a){return 1;}" + appendix + " "
                                + prefix + "function \n foo($a, $b){return 1;}" + appendix,
                        errorDto
                },
                {
                        prefix + "function \n foo($a){return 1;}" + appendix + " "
                                + prefix + "function \n foo($a, $b){return 1;}" + appendix + " "
                                + prefix + "function \n foo($a, $b, $c){return 1;}" + appendix,
                        errorDtoTwo
                },

        }));

        String[] types = new String[]{"array"};
        for (String type : types) {
            collection.addAll(Arrays.asList(new Object[][]{
                    //PHP does not yet support return type hints
//                    //it does not matter if return values are different
//                    {
//                            prefix + "function " + type + " \n foo(){return 1;}" + appendix + " "
//                                    + prefix + "function \n foo(){return 1;}" + appendix,
//                            errorDto
//                    },
//                    {
//                            prefix + "function " + type + " \n foo(){return 1;}" + appendix + " "
//                                    + prefix + "function \n foo(){return 1;}" + appendix + " "
//                                    + prefix + "function \n foo(){return 1;}" + appendix,
//                            errorDtoTwo
//                    },
                    //parameter type does not matter
                    {
                            prefix + "function \n foo(" + type + " $a){return 1;}" + appendix + " "
                                    + prefix + "function \n foo(\\Exception $a){return 1;}" + appendix,
                            errorDto
                    },
                    {
                            prefix + "function \n foo(" + type + " $a){return 1;}" + appendix + " "
                                    + prefix + "function \n foo(\\ErrorException $a){return 1;}" + appendix + " "
                                    + prefix + "function \n foo(\\Exception $a){return 1;}" + appendix,
                            errorDtoTwo
                    },
            }));
        }
        return collection;
    }
}