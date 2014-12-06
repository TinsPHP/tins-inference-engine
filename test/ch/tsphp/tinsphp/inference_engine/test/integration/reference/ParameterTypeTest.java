/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceTypeScopeTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.TypeScopeTestStruct;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;


@RunWith(Parameterized.class)
public class ParameterTypeTest extends AReferenceTypeScopeTest
{


    public ParameterTypeTest(String testString, TypeScopeTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                {
                        "function foo(array $a){return;}",
                        typeStruct("$a", "\\.\\.foo().", "array", "", 1, 0, 3, 0, 1)
                },
                {
                        "function foo(Exception $a){return;}",
                        typeStruct("$a", "\\.\\.foo().", "Exception", "", 1, 0, 3, 0, 1)
                },
                {
                        "function foo(ErrorException $a){return;}",
                        typeStruct("$a", "\\.\\.foo().", "ErrorException", "", 1, 0, 3, 0, 1)
                },
                {
                        "namespace a; function foo(array $a){return;}",
                        typeStruct("$a", "\\a\\.\\a\\.foo().", "array", "", 1, 0, 3, 0, 1)
                },
                {
                        "namespace a; function foo(\\Exception $a){return;}",
                        typeStruct("$a", "\\a\\.\\a\\.foo().", "Exception", "", 1, 0, 3, 0, 1)
                },
                {
                        "namespace a; function foo(\\ErrorException $a){return;}",
                        typeStruct("$a", "\\a\\.\\a\\.foo().", "ErrorException", "", 1, 0, 3, 0, 1)
                }
        });
    }

    private static TypeScopeTestStruct[] typeStruct(
            String astText, String symbolScope, String typeText, String typeScope, Integer... astAccessOrder) {
        return new TypeScopeTestStruct[]{
                new TypeScopeTestStruct(astText, symbolScope, Arrays.asList(astAccessOrder), typeText, typeScope)
        };

    }
}
