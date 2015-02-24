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
public class ResolveUseTest extends AReferenceTypeScopeTest
{


    public ResolveUseTest(String testString, TypeScopeTestStruct[] theTestStructs) {
        super(testString, theTestStructs);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                //TODO TINS-161 inference OOP
                //use from default namespace
//                {"namespace t{use \\A;} namespace{class A{}}", typeStruct("A", "\\t\\", "A", "\\", 0, 1, 0, 0, 1)}
                {
                        "namespace a\\b{const a = 1;} namespace t{use \\a\\b; b\\a;}",
                        typeStruct("b\\a#", "\\a\\b\\.\\a\\b\\.", null, null, 1, 1, 1, 0)
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
