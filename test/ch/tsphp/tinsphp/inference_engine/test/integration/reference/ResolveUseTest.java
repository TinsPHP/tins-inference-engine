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

    @Override
    protected void verifyReferences() {
        //nothing to check, should just not cause an error
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                //TODO TINS-161 inference OOP
                //use from default namespace
//                {"namespace t{use \\A;} namespace{class A{}}", typeStruct("A", "\\t\\", "A", "\\", 0, 1, 0, 0, 1)}
                //TODO rstoll TINS-213 reference phase - resolve constants
//                {
//                        "namespace t{use \\a\\b; a\\a;} namespace a\\b{const a = 1;}",
//                        typeStruct("A", "\\t\\", "A", "\\", 0, 1, 0, 0, 1)
//                }
        });
    }

    private static TypeScopeTestStruct[] typeStruct(
            String astText, String astScope, String typeText, String typeScope, Integer... astAccessOrder) {
        return new TypeScopeTestStruct[]{
                new TypeScopeTestStruct(astText, astScope, Arrays.asList(astAccessOrder), typeText, typeScope)
        };

    }
}
