/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class NamespaceTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.definition;

import ch.tsphp.common.IScope;
import ch.tsphp.tinsphp.common.config.ISymbolsInitialiser;
import ch.tsphp.tinsphp.common.scopes.IScopeFactory;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestConditionalScopeFactory;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.TestSymbolsInitialiser;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.ADefinitionTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ConditionalTest extends ADefinitionTest
{
    private String namespaces;

    public ConditionalTest(String testString, String theNamespaces) {
        super(testString);
        namespaces = theNamespaces;
    }

    @Override
    protected ISymbolsInitialiser createSymbolsInitialiser() {
        return new ConditionalSymbolsInitialiser();
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
        Assert.assertEquals(testString + " failed.", namespaces, getNamespacesAsString());
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        String deflt = "\\.\\.";
        String b = "\\b\\.\\b\\.";
        String ab = "\\a\\b\\.\\a\\b\\.";
        String foreach = "foreach([1] as $v){$a=1;}";
        String tryCatch = "try{}catch(Exception $e){}";

        return Arrays.asList(new Object[][]{
                {"if(true){}", deflt + " " + deflt + "cScope."},
                {"if(true){}else{}", deflt + " " + deflt + "cScope. " + deflt + "cScope."},
                {
                        "if(true){if($b){}}else{}",
                        deflt + " " + deflt + "cScope. " + deflt + "cScope.cScope. " + deflt + "cScope."
                },
                {"switch($a){case 1:}", deflt + " " + deflt + "cScope."},
                {"for(;;){}", deflt + " " + deflt + "cScope."},
                {"while(true){}", deflt + " " + deflt + "cScope."},
                {foreach, deflt + " " + deflt + "cScope. " + deflt + "cScope.cScope."},
                {tryCatch, deflt + " " + deflt + "cScope. " + deflt + "cScope. " + deflt + "cScope.cScope."},

                //not in default namespace
                {"namespace b;if(true){}", b + " " + b + "cScope."},
                {"namespace b;if(true){}else{}", b + " " + b + "cScope. " + b + "cScope."},
                {
                        "namespace b;if(true){if($b){}}else{}",
                        b + " " + b + "cScope. " + b + "cScope.cScope. " + b + "cScope."
                },
                {"namespace b;switch($a){case 1: case 2:}", b + " " + b + "cScope."},
                {"namespace b;for(;;){}", b + " " + b + "cScope."},
                {"namespace b;while(true){}", b + " " + b + "cScope."},
                {"namespace b;" + foreach, b + " " + b + "cScope. " + b + "cScope.cScope."},
                {"namespace b;" + tryCatch, b + " " + b + "cScope. " + b + "cScope. " + b + "cScope.cScope."},

                //in sub namespace
                {"namespace a\\b;if(true){}", ab + " " + ab + "cScope."},
                {"namespace a\\b;if(true){}else{}", ab + " " + ab + "cScope. " + ab + "cScope."},
                {
                        "namespace a\\b;if(true){if($b){}}else{}",
                        ab + " " + ab + "cScope. " + ab + "cScope.cScope. " + ab + "cScope."
                },
                {"namespace a\\b;switch($a){case 1: default:}", ab + " " + ab + "cScope."},
                {"namespace a\\b;for(;;){}", ab + " " + ab + "cScope."},
                {"namespace a\\b;while(true){}", ab + " " + ab + "cScope."},
                {"namespace a\\b;" + foreach, ab + " " + ab + "cScope. " + ab + "cScope.cScope."},
                {"namespace a\\b;" + tryCatch, ab + " " + ab + "cScope. " + ab + "cScope. " + ab + "cScope.cScope."},

                //multiple namespace
                {
                        "namespace{if(true){}} namespace b{if(true);} namespace a\\b{if($a<1){$a=1;}}",
                        deflt + " " + deflt + "cScope. "
                                + b + " " + b + "cScope. "
                                + ab + " " + ab + "cScope."
                },
                {
                        "namespace{switch($a){case 1:}} namespace b{switch($a){case 1:$a=1;}} "
                                + "namespace a\\b{switch($a){case 1:break; default: case 2:}}",
                        deflt + " " + deflt + "cScope. "
                                + b + " " + b + "cScope. "
                                + ab + " " + ab + "cScope. " + ab + "cScope."
                },
                {
                        "namespace{for(;;){}} namespace b{for(;;){}} namespace a\\b{for($i=0;;);}",
                        deflt + " " + deflt + "cScope. "
                                + b + " " + b + "cScope. "
                                + ab + " " + ab + "cScope."
                },
                {
                        "namespace{while(true){}} namespace b{while(true);} namespace a\\b{while(true){$a=1;}}",
                        deflt + " " + deflt + "cScope. "
                                + b + " " + b + "cScope. "
                                + ab + " " + ab + "cScope."
                },
                {
                        "namespace{" + foreach + "} namespace b{" + foreach + "} namespace a\\b{" + foreach + "}",
                        deflt + " " + deflt + "cScope. " + deflt + "cScope.cScope. "
                                + b + " " + b + "cScope. " + b + "cScope.cScope. "
                                + ab + " " + ab + "cScope. " + ab + "cScope.cScope."
                },
                {
                        "namespace{" + tryCatch + "} namespace b{" + tryCatch + "} namespace a\\b{" + tryCatch + "}",
                        deflt + " " + deflt + "cScope. " + deflt + "cScope. " + deflt + "cScope.cScope. "
                                + b + " " + b + "cScope. " + b + "cScope. " + b + "cScope.cScope. "
                                + ab + " " + ab + "cScope. " + ab + "cScope. " + ab + "cScope.cScope."
                },
                {
                        "namespace b{" + foreach + "} namespace b{" + foreach + "}",
                        b + " " + b + "cScope. " + b + "cScope.cScope. "
                                + b + " " + b + "cScope. " + b + "cScope.cScope."
                },
                {
                        "namespace b{" + tryCatch + "} namespace b{" + tryCatch + "}",
                        b + " " + b + "cScope. " + b + "cScope. " + b + "cScope.cScope. "
                                + b + " " + b + "cScope. " + b + "cScope. " + b + "cScope.cScope."
                },
                //multiple catch blocks
                {
                        "try{}catch(ErrorException $e){}catch(Exception $e2){}",
                        deflt + " " + deflt + "cScope. " + deflt + "cScope. " + deflt + "cScope.cScope. "
                                + deflt + "cScope. " + deflt + "cScope.cScope."
                },
                {
                        "namespace b{try{}catch(Dummy $e3){}catch(ErrorException $e){}catch(Exception $e2){}}",
                        b + " " + b + "cScope. " + b + "cScope. " + b + "cScope.cScope. "
                                + b + "cScope. " + b + "cScope.cScope. "
                                + b + "cScope. " + b + "cScope.cScope."
                },
        });
    }

    private String getNamespacesAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        boolean isNotFirst = false;
        TestConditionalScopeFactory scopeFactory = (TestConditionalScopeFactory) symbolsInitialiser.getScopeFactory();
        for (IScope scope : scopeFactory.scopes) {
            if (isNotFirst) {
                stringBuilder.append(" ");
            }
            isNotFirst = true;
            stringBuilder.append(ScopeTestHelper.getEnclosingScopeNames(scope));

        }
        return stringBuilder.toString();
    }

    private class ConditionalSymbolsInitialiser extends TestSymbolsInitialiser
    {
        private TestConditionalScopeFactory factory;

        public ConditionalSymbolsInitialiser() {
            factory = new TestConditionalScopeFactory(getScopeHelper());
        }

        @Override
        public IScopeFactory getScopeFactory() {
            return factory;
        }
    }
}
