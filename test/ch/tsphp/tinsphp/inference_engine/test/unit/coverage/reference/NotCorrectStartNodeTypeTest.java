/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class NotCorrectStartNodeTypeForRulesWithParams from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.coverage.reference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.TestTinsPHPReferenceWalker;
import ch.tsphp.tinsphp.inference_engine.test.unit.testutils.AReferenceWalkerTest;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Try;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class NotCorrectStartNodeTypeTest extends AReferenceWalkerTest
{
    private String methodName;
    private int tokenType;

    public NotCorrectStartNodeTypeTest(String theMethodName, int theTokenType) {
        methodName = theMethodName;
        tokenType = theTokenType;
    }

    @Test
    public void withoutBacktracking_reportNoViableAltException()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(tokenType);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        Method method = TestTinsPHPReferenceWalker.class.getMethod(methodName);
        method.invoke(walker);

        try {
            verify(walker).reportError(any(NoViableAltException.class));
        } catch (Exception e) {
            fail(methodName + " failed - verify caused exception:\n" + e.getClass().getName() + e.getMessage());
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
//                {"accessModifier", Try},
                {"actualParameters", Try},
                {"array", Try},
                {"arrayKeyValue", Try},
                {"assignOperator", Try},
                {"atom", Try},
                {"binaryOperatorExcludingAssign", Try},
                {"breakContinue", Try},
                {"caseLabels", Try},
                //TODO TINS-210 - reference phase - class definitions
//                {"classBody", Try},
//                {"classBodyDefinition", Try},
                // TINS-220 - reference phase - double definition check fields
//                {"fieldDefinition", Try},
                //TODO TINS-210 - reference phase - class definitions
//                {"classStaticAccess", Try},
                {"compilationUnit", Try},
                //TODO TINS-214 - reference phase - double definition check constants
//                {"constDefinitionList", Try},
                //TODO TINS-221 - reference phase - double definition check methods
//                {"constructDefinition", Try},
                {"definition", Try},
                {"exit", Try},
                {"expression", Try},
                {"expressionList", Try},
                {"foreachLoop", Try},
                {"forLoop", Try},
                {"functionCall", Try},
                {"functionDefinition", Try},
                // TINS-211 - reference phase - interface definitions
//                {"interfaceBody", Try},
//                {"interfaceBodyDefinition", Try},
//                {"interfaceDefinition", Try},
                //TODO TINS-161 inference OOP
//                {"methodCall", Try},
//                {"methodCallee", Try},
//                {"methodCallStatic", Try},
//                {"methodDefinition", Try},
//                {"methodModifier", Try},
                {"namespace", Try},
                {"namespaceBody", Try},
                {"operator", Try},
                {"parameterDeclaration", Try},
                {"parameterDeclarationList", Try},
                {"postFixExpression", Try},
                {"primitiveAtomWithConstant", Try},
                {"statement", Try},
                //TODO TINS-217 reference phase - class constant access
//                {"staticAccessor", Try},
                //TODO TINS-223 - reference phase - resolve this and self
//                {"thisVariable", Try},
                {"unaryOperator", Try},
                {"unaryPrimitiveAtom", Try},
                {"useDeclaration", Try},
                {"useDefinitionList", Try},

                {"variable", Try},
                {"whileLoop", Try},
        });
    }
}

