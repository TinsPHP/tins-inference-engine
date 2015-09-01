/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.inference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.AReferenceTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AppliedOverloadTest.class,
        ConstraintSolverTest.class,
        ConstraintErrorTest.class,
        ConstraintSolverTest.class,
        FunctionCallTest.class,
        FunctionDefinitionBindingTest.class,
        FunctionDefinitionMultipleOverloadTest.class,
        FunctionDefinitionOverloadRecursiveTest.class,
        FunctionDefinitionOverloadTest.class,
        FunctionDefinitionWithImplicitReturnOverloadTest.class,
        OperatorFromGreaterThanTest.class,
        OperatorUpToGreaterThanTest.class,
        ProblematicExpressionTest.class,
        SoftTypingGlobalScopeTest.class,
        VariableDeclarationTest.class
})
public class ParallelTest
{
    @BeforeClass
    public static void init() {
        AReferenceTest.numberOfThreads = 4;
    }

    @AfterClass
    public static void tearDown() {
        AReferenceTest.numberOfThreads = 1;
    }
}
