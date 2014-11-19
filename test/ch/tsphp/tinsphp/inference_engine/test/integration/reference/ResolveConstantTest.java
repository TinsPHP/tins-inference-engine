/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ResolveConstantTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
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
public class ResolveConstantTest extends AReferenceTypeScopeTest
{

    public ResolveConstantTest(String testString, TypeScopeTestStruct[] testStructs) {
        super(testString, testStructs);
    }

    @Test
    public void test() throws RecognitionException {
        check();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                //conditionals
                {"const a=1; a;", structDefault("", 1, 1, 0)},
                {"const a=1;{ a;}", structDefault("", 1, 1, 0)},
                {"const a=1;if(a==1){}", structDefault("", 1, 1, 0, 0)},
                {"const a=1;if(true){ a;}", structDefault("", 1, 1, 1, 0, 0)},
                {"const a=1;if(true){}else{ a;}", structDefault("", 1, 1, 2, 0, 0)},
                {"const a=1;if(true){ if(true){ a;}}", structDefault("", 1, 1, 1, 0, 1, 0, 0)},
                {"const a=1; switch(a){case 1: a;break;}", structDefault("", 1, 1, 0)},
                {"const a=1; $b=0; switch($b){case 1: a;break;}", structDefault("", 1, 2, 2, 0, 0)},
                {"const a=1; $b=0; switch($b){case 1:{a;}break;}", structDefault("", 1, 2, 2, 0, 0)},
                {"const a=1; $b=0; switch($b){default:{a;}break;}", structDefault("", 1, 2, 2, 0, 0)},
                {"const a=1; for($a=a;;){}", structDefault("", 1, 1, 0, 0, 1)},
                {"const a=1; for(;a==1;){}", structDefault("", 1, 1, 1, 0, 0)},
                {"const a=1; $a=0;for(;;$a+=a){}", structDefault("", 1, 2, 2, 0, 1)},
                {"const a=1; for(;;){a;}", structDefault("", 1, 1, 3, 0, 0)},
                {"const a=1; foreach([1] as $v){a;}", structDefault("", 1, 1, 2, 0, 0)},
                {"const a=1; while(a==1){}", structDefault("", 1, 1, 0, 0)},
                {"const a=1; while(true)a;", structDefault("", 1, 1, 1, 0, 0)},
                {"const a=1; while(true){a;}", structDefault("", 1, 1, 1, 0, 0)},
                {"const a=1; do ; while(a==1);", structDefault("", 1, 1, 1, 0)},
                {"const a=1; do a; while(true);", structDefault("", 1, 1, 0, 0, 0)},
                {"const a=1; try{a;}catch(\\Exception $ex){}", structDefault("", 1, 1, 0, 0, 0)},
                {"const a=1; try{}catch(\\Exception $ex){a;}", structDefault("", 1, 1, 1, 2, 0, 0)},
                //in expression (ok a; is also an expression but at the top of the AST)
                {
                        "const a=1; !(1+a-a/a*a && a) || a;",
                        new TypeScopeTestStruct[]{
                                new TypeScopeTestStruct("a#", "\\.\\.", Arrays.asList(1, 1, 0, 1), null, "\\."),
                                new TypeScopeTestStruct(
                                        "a#", "\\.\\.", Arrays.asList(1, 1, 0, 0, 0, 1), null, "\\."),
                                new TypeScopeTestStruct(
                                        "a#", "\\.\\.", Arrays.asList(1, 1, 0, 0, 0, 0, 0, 1), null, "\\."),
                                new TypeScopeTestStruct(
                                        "a#", "\\.\\.", Arrays.asList(1, 1, 0, 0, 0, 0, 1, 1), null, "\\."),
                                new TypeScopeTestStruct(
                                        "a#", "\\.\\.", Arrays.asList(1, 1, 0, 0, 0, 0, 1, 0, 0), null, "\\."),
                                new TypeScopeTestStruct(
                                        "a#", "\\.\\.", Arrays.asList(1, 1, 0, 0, 0, 0, 1, 0, 1), null, "\\.")
                        }
                },
                //const are global
                {"const a=1; function foo(){a; return null;}", structDefault("", 1, 1, 4, 0, 0)},
                //TODO TINS-161 inference OOP
//                {"const a=1; class a{ private $a=a;}", structDefault("", 1, 1, 4, 0, 0, 1, 0)},
//                {"const a=1; class a{ function foo(){a; return null;}}", structDefault("", 1, 1, 4, 0, 4, 0, 0)},
                //same namespace
                {"namespace{const a=1;} namespace{a;}", structDefault("", 1, 1, 0, 0)},
                {"namespace a{const a=1;} namespace a{a;}", struct("", "\\a\\.\\a\\.", 1, 1, 0, 0)},
                {
                        "namespace b\\c{const a=1;} namespace b\\c{a;}",
                        struct("", "\\b\\c\\.\\b\\c\\.", 1, 1, 0, 0)
                },
                {
                        "namespace d\\e\\f{const a=1;} namespace d\\e\\f{a;}",
                        struct("", "\\d\\e\\f\\.\\d\\e\\f\\.", 1, 1, 0, 0)
                },
                //different namespace absolute
                {
                        "namespace a{ const a=1;} namespace x{\\a\\a;}",
                        struct("\\a\\", "\\a\\.\\a\\.", 1, 1, 0, 0)
                },
                //different namespace relative - defaut is like absolute
                {"namespace a{ const a=1;} namespace{a\\a;}", struct("a\\", "\\a\\.\\a\\.", 1, 1, 0, 0)},
                //different namespace relative
                {
                        "namespace a\\b{ const a=1;} namespace a{ b\\a;}",
                        struct("b\\", "\\a\\b\\.\\a\\b\\.", 1, 1, 0, 0)
                },
                //using an alias
                {
                        "namespace a\\b{ const a=1; } namespace x{ use a\\b as b; b\\a;}",
                        struct("b\\", "\\a\\b\\.\\a\\b\\.", 1, 1, 1, 0)
                },
                //const have a fallback mechanism to default scope
                {"namespace{ const a=1;} namespace a{a;}", structDefault("", 1, 1, 0, 0)},
                {"namespace{ const a=1;} namespace a\\b{a;}", structDefault("", 1, 1, 0, 0)},
                {"namespace{ const a=1;} namespace a\\b\\c{a;}", structDefault("", 1, 1, 0, 0)},
                {
                        "namespace{ const a=1;} namespace a{function foo(){a; return null;}}",
                        structDefault("", 1, 1, 0, 4, 0, 0)
                },
                //TODO TINS-161 inference OOP
//                {
//                        "namespace{ const a=1;} namespace a{class a{ private $a=a;}}",
//                        structDefault("", 1, 1, 0, 4, 0, 0, 1, 0)
//                },
//                {
//                        "namespace{ const a=1;} namespace a{class a{ function foo(){a; return null;}}}",
//                        structDefault("", 1, 1, 0, 4, 0, 4, 0, 0)
//                }
        });
    }

    private static TypeScopeTestStruct[] structDefault(String prefix, Integer... accessToScope) {
        return struct(prefix, "\\.\\.", accessToScope);
    }

    private static TypeScopeTestStruct[] struct(String prefix, String scope, Integer... accessToScope) {
        return new TypeScopeTestStruct[]{
                new TypeScopeTestStruct(prefix + "a#", scope, Arrays.asList(accessToScope), null, "\\.")
        };
    }
}
