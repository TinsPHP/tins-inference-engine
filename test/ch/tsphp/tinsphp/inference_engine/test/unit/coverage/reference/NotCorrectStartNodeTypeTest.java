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

import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Else;
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
//                {"accessModifier", Else},
                {"actualParameters", Else},
                //requires parameters - see NotCorrectStartNodeTypeForRulesWithParamsTest
                //{"allTypesOrUnknown", Else},
                {"array", Else},
                {"arrayKeyValue", Else},
                {"assignOperator", Else},
                {"atom", Else},
                {"binaryOperatorExcludingAssign", Else},
                {"block", Else},
                {"blockConditional", Else},
                {"breakContinue", Else},
                {"caseLabels", Else},
                {"catchBlocks", Else},
                //TODO TINS-210 - reference phase - class definitions
//                {"classBody", Else},
//                {"classBodyDefinition", Else},
                // TINS-220 - reference phase - double definition check fields
//                {"fieldDefinition", Else},
                //TODO TINS-210 - reference phase - class definitions
//                {"classStaticAccess", Else},
                {"compilationUnit", Else},
                //requires parameters - see NotCorrectStartNodeTypeForRulesWithParamsTest
//                {"constDeclaration", Else},
                {"constDefinitionList", Else},
                //TODO TINS-221 - reference phase - double definition check methods
//                {"constructDefinition", Else},
                {"definition", Else},
                {"doWhileLoop", Else},
                {"exit", Else},
                {"expression", Else},
                {"expressionList", Else},
                {"foreachLoop", Else},
                {"forLoop", Else},
                {"functionCall", Else},
                {"functionDefinition", Else},
                {"ifCondition", Else},
                {"instruction", Else},
                {"instructions", Else},
                //TODO TINS-211 - reference phase - interface definitions
//                {"interfaceBody", Else},
//                {"interfaceBodyDefinition", Else},
//                {"interfaceDefinition", Else},

                //TODO TINS-161 inference OOP
//                {"methodCall", Else},
//                {"methodCallee", Else},
//                {"methodCallStatic", Else},
//                {"methodDefinition", Else},
//                {"methodModifier", Else},
                {"namespace", Else},
                {"namespaceBody", Else},
                {"operator", Else},
                {"parameterDeclaration", Else},
                {"parameterDeclarationList", Else},
                {"postFixExpression", Else},
                {"primitiveAtomWithConstant", Else},
                //requires parameters - see NotCorrectStartNodeTypeForRulesWithParamsTest
                //{"returnTypesOrUnknown", Else},
                {"statement", Else},
                //TODO TINS-217 reference phase - class constant access
//                {"staticAccessor", Else},
                {"switchCondition", Else},
                {"switchContents", Else},
                //TODO TINS-223 - reference phase - resolve this and self
//                {"thisVariable", Else},
                {"tryCatch", Else},
                {"unaryOperator", Else},
                {"unaryPrimitiveAtom", Else},
                {"useDeclaration", Else},
                {"useDefinitionList", Else},

                {"variable", Else},
                {"whileLoop", Else},
        });
    }
}

