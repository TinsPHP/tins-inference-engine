/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.reference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceEvalTypeScopeTest;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.TypeScopeTestStruct;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;


@RunWith(Parameterized.class)
public class CatchTypeTest extends AReferenceEvalTypeScopeTest
{


    public CatchTypeTest(String testString, TypeScopeTestStruct[] theTestStructs) {
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
                        "try{}catch(Exception $e){}",
                        typeStruct("Exception", "Exception", "", 1, 0, 1, 0)
                },
                {
                        "try{}catch(ErrorException $e){}",
                        typeStruct("ErrorException", "ErrorException", "", 1, 0, 1, 0)
                },
                {
                        "function foo(){try{}catch(Exception $e){} return;}",
                        typeStruct("Exception", "Exception", "", 1, 0, 4, 0, 1, 0)
                },
                {
                        "function foo(){try{}catch(ErrorException $e){} return;}",
                        typeStruct("ErrorException", "ErrorException", "", 1, 0, 4, 0, 1, 0)
                },
                {
                        "namespace a; try{}catch(\\Exception $e){}",
                        typeStruct("\\Exception", "Exception", "", 1, 0, 1, 0)
                },
                {
                        "namespace a; try{}catch(\\ErrorException $e){}",
                        typeStruct("\\ErrorException", "ErrorException", "", 1, 0, 1, 0)
                },
                {
                        "namespace a; function foo(){try{}catch(\\Exception $e){} return;}",
                        typeStruct("\\Exception", "Exception", "", 1, 0, 4, 0, 1, 0)
                },
                {
                        "namespace a; function foo(){try{}catch(\\ErrorException $e){} return;}",
                        typeStruct("\\ErrorException", "ErrorException", "", 1, 0, 4, 0, 1, 0)
                },
        });
    }

    private static TypeScopeTestStruct[] typeStruct(
            String astText, String typeText, String typeScope, Integer... astAccessOrder) {
        return new TypeScopeTestStruct[]{
                new TypeScopeTestStruct(astText, null, Arrays.asList(astAccessOrder), typeText, typeScope)
        };

    }
}
