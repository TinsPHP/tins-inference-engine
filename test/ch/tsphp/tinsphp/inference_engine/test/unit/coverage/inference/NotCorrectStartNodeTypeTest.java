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

package ch.tsphp.tinsphp.inference_engine.test.unit.coverage.inference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.TestTinsPHPInferenceWalker;
import ch.tsphp.tinsphp.inference_engine.test.unit.testutils.AInferenceWalkerTest;
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
public class NotCorrectStartNodeTypeTest extends AInferenceWalkerTest
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

        TestTinsPHPInferenceWalker walker = spy(createWalker(ast));
        Method method = TestTinsPHPInferenceWalker.class.getMethod(methodName);
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
//                {"actualParameters", Else},
                //requires parameters - see NotCorrectStartNodeTypeForRulesWithParamsTest
                //{"allTypesOrUnknown", Else},
                {"array", Else},
                {"arrayKeyValue", Else},
                //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
//                {"assignOperator", Else},
                {"atom", Else},
                //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
//                {"binaryOperatorExcludingAssign", Else},
//                {"block", Else},
                //TODO  TINS-71 inference procedural - take into account control structures
//                {"blockConditional", Else},
//                {"breakContinue", Else},
//                {"caseLabels", Else},
//                {"catchBlocks", Else},
                //TODO TINS-210 - reference phase - class definitions
//                {"classBody", Else},
//                {"classBodyDefinition", Else},
                // TINS-220 - reference phase - double definition check fields
//                {"fieldDefinition", Else},
                //TODO TINS-210 - reference phase - class definitions
//                {"classStaticAccess", Else},
                {"compilationUnit", Else},
                {"constDeclaration", Else},
                {"constDefinitionList", Else},
                //TODO TINS-221 - reference phase - double definition check methods
//                {"constructDefinition", Else},
                {"definition", Else},
                //TODO  TINS-71 inference procedural - take into account control structures
//                {"doWhileLoop", Else},
                //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
//                {"exit", Else},
                {"expression", Else},
                //TODO  TINS-71 inference procedural - take into account control structures
//                {"expressionList", Else},
//                {"foreachLoop", Else},
//                {"forLoop", Else},
                //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
//                {"functionCall", Else},
//                {"functionDefinition", Else},
                //TODO  TINS-71 inference procedural - take into account control structures
//                {"ifCondition", Else},
                //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
//                {"instruction", Else},
//                {"instructions", Else},
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
                //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
//                {"operator", Else},
//                {"parameterDeclaration", Else},
//                {"parameterDeclarationList", Else},
//                {"postFixExpression", Else},
                {"primitiveAtomWithConstant", Else},
                //requires parameters - see NotCorrectStartNodeTypeForRulesWithParamsTest
                //{"returnTypesOrUnknown", Else},
                {"statement", Else},
                //TODO TINS-217 reference phase - class constant access
//                {"staticAccessor", Else},
                //TODO  TINS-71 inference procedural - take into account control structures
//                {"switchCondition", Else},
//                {"switchContents", Else},
                //TODO TINS-223 - reference phase - resolve this and self
//                {"thisVariable", Else},
                //TODO  TINS-71 inference procedural - take into account control structures
//                {"tryCatch", Else},
                //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
//                {"unaryOperator", Else},
                {"unaryPrimitiveAtom", Else},
                //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
//                {"useDeclaration", Else},
//                {"useDefinitionList", Else},
//                {"variable", Else},
                //TODO  TINS-71 inference procedural - take into account control structures
//                {"whileLoop", Else},
        });
    }
}
