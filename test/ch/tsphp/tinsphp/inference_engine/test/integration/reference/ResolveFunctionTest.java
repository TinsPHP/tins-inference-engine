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
public class ResolveFunctionTest extends AReferenceTypeScopeTest
{

    public ResolveFunctionTest(String testString, TypeScopeTestStruct[] testStructs) {
        super(testString, testStructs);
    }

    @Test
    public void test() throws RecognitionException {
        runTest();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                //conditionals
                {"function foo(){return;} foo();", structDefault("", 1, 1, 0, 0)},
                {"function foo(){return;}{ foo();}", structDefault("", 1, 1, 0, 0)},
                {"function foo(){return;}if(foo()==1){}", structDefault("", 1, 1, 0, 0, 0)},
                {"function foo(){return;}if(true){ foo();}", structDefault("", 1, 1, 1, 0, 0, 0)},
                {"function foo(){return;}if(true){}else{ foo();}", structDefault("", 1, 1, 2, 0, 0, 0)},
                {"function foo(){return;}if(true){ if(true){ foo();}}", structDefault("", 1, 1, 1, 0, 1, 0, 0, 0)},
                {"function foo(){return;} switch(foo()){case 1: foo();break;}", structDefault("", 1, 1, 0, 0)},
                {"function foo(){return;} $b=0; switch($b){case 1: foo();break;}", structDefault("", 1, 2, 2, 0, 0, 0)},
                {
                        "function foo(){return;} $b=0; switch($b){case 1:{foo();}break;}",
                        structDefault("", 1, 2, 2, 0, 0, 0)
                },
                {
                        "function foo(){return;} $b=0; switch($b){default:{foo();}break;}",
                        structDefault("", 1, 2, 2, 0, 0, 0)
                },
                {"function foo(){return;} for($a=foo();;){}", structDefault("", 1, 1, 0, 0, 1, 0)},
                {"function foo(){return;} for(;foo()==1;){}", structDefault("", 1, 1, 1, 0, 0, 0)},
                {"function foo(){return;} $a=0;for(;;$a+=foo()){}", structDefault("", 1, 2, 2, 0, 1, 0)},
                {"function foo(){return;} for(;;){foo();}", structDefault("", 1, 1, 3, 0, 0, 0)},
                {"function foo(){return;} foreach([1] as $v){foo();}", structDefault("", 1, 1, 2, 0, 0, 0)},
                {"function foo(){return;} while(foo()==1){}", structDefault("", 1, 1, 0, 0, 0)},
                {"function foo(){return;} while(true)foo();", structDefault("", 1, 1, 1, 0, 0, 0)},
                {"function foo(){return;} while(true){foo();}", structDefault("", 1, 1, 1, 0, 0, 0)},
                {"function foo(){return;} do ; while(foo()==1);", structDefault("", 1, 1, 1, 0, 0)},
                {"function foo(){return;} do foo(); while(true);", structDefault("", 1, 1, 0, 0, 0, 0)},
                {"function foo(){return;} try{foo();}catch(\\Exception $ex){}", structDefault("", 1, 1, 0, 0, 0, 0)},
                {"function foo(){return;} try{}catch(\\Exception $ex){foo();}", structDefault("", 1, 1, 1, 2, 0, 0, 0)},
                //in expression (ok foo(); is also an expression but at the top of the AST)
                {
                        "function foo(){return;} !(1+foo()-foo()/foo()*foo() && foo()) || foo();",
                        new TypeScopeTestStruct[]{
                                new TypeScopeTestStruct("foo()", "\\.\\.", Arrays.asList(1, 1, 0, 1, 0), null, "\\."),
                                new TypeScopeTestStruct(
                                        "foo()", "\\.\\.", Arrays.asList(1, 1, 0, 0, 0, 1, 0), null, "\\."),
                                new TypeScopeTestStruct(
                                        "foo()", "\\.\\.", Arrays.asList(1, 1, 0, 0, 0, 0, 0, 1, 0), null, "\\."),
                                new TypeScopeTestStruct(
                                        "foo()", "\\.\\.", Arrays.asList(1, 1, 0, 0, 0, 0, 1, 1, 0), null, "\\."),
                                new TypeScopeTestStruct(
                                        "foo()", "\\.\\.", Arrays.asList(1, 1, 0, 0, 0, 0, 1, 0, 0, 0), null, "\\."),
                                new TypeScopeTestStruct(
                                        "foo()", "\\.\\.", Arrays.asList(1, 1, 0, 0, 0, 0, 1, 0, 1, 0), null, "\\.")
                        }
                },
                //functions are global
                {"function foo(){return;} function bar(){foo(); return null;}", structDefault("", 1, 1, 4, 0, 0, 0)},
                //TODO TINS-161 inference OOP
//                {
//                        "function foo(){return;} class a{ function foo(){foo(); return null;}}",
//                        structDefault("", 1, 1, 4, 0, 4, 0, 0, 0)
//                },
                //same namespace
                {"namespace{function foo(){return;}} namespace{foo();}", structDefault("", 1, 1, 0, 0, 0)},
                {"namespace a{function foo(){return;}} namespace a{foo();}", struct("", "\\a\\.\\a\\.", 1, 1, 0, 0, 0)},
                {
                        "namespace b\\c{function foo(){return;}} namespace b\\c{foo();}",
                        struct("", "\\b\\c\\.\\b\\c\\.", 1, 1, 0, 0, 0)
                },
                {
                        "namespace d\\e\\f{function foo(){return;}} namespace d\\e\\f{foo();}",
                        struct("", "\\d\\e\\f\\.\\d\\e\\f\\.", 1, 1, 0, 0, 0)
                },
                //different namespace absolute
                {
                        "namespace a{ function foo(){return;}} namespace x{\\a\\foo();}",
                        struct("\\a\\", "\\a\\.\\a\\.", 1, 1, 0, 0, 0)
                },
                //different namespace relative - defaut is like absolute
                {
                        "namespace a{ function foo(){return;}} namespace{a\\foo();}",
                        struct("a\\", "\\a\\.\\a\\.", 1, 1, 0, 0, 0)
                },
                //different namespace relative
                {
                        "namespace a\\b{ function foo(){return;}} namespace a{ b\\foo();}",
                        struct("b\\", "\\a\\b\\.\\a\\b\\.", 1, 1, 0, 0, 0)
                },
                //using an alias
                {
                        "namespace a\\b{ function foo(){return;} } namespace x{ use a\\b as b; b\\foo();}",
                        struct("b\\", "\\a\\b\\.\\a\\b\\.", 1, 1, 1, 0, 0)
                },
                //const have a fallback mechanism to default scope
                {"namespace{ function foo(){return;}} namespace a{foo();}", structDefault("", 1, 1, 0, 0, 0)},
                {"namespace{ function foo(){return;}} namespace a\\b{foo();}", structDefault("", 1, 1, 0, 0, 0)},
                {"namespace{ function foo(){return;}} namespace a\\b\\c{foo();}", structDefault("", 1, 1, 0, 0, 0)},
                {
                        "namespace{ function foo(){return;}} namespace a{function foo(){foo(); return null;}}",
                        structDefault("", 1, 1, 0, 4, 0, 0, 0)
                },
                //TODO TINS-161 inference OOP
//                {
//                        "namespace{ function foo(){return;}} "
//                                + "namespace a{class a{ function foo(){foo(); return null;}}}",
//                        structDefault("", 1, 1, 0, 4, 0, 4, 0, 0)
//                }
        });
    }

    private static TypeScopeTestStruct[] structDefault(String prefix, Integer... accessToScope) {
        return struct(prefix, "\\.\\.", accessToScope);
    }

    private static TypeScopeTestStruct[] struct(String prefix, String scope, Integer... accessToScope) {
        return new TypeScopeTestStruct[]{
                new TypeScopeTestStruct(prefix + "foo()", scope, Arrays.asList(accessToScope), null, "\\.")
        };
    }
}
