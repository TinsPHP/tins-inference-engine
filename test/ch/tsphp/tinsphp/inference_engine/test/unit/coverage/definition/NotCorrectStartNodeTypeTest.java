/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class NotCorrectStartNodeTypeTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.coverage.definition;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition.TestTinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.test.unit.testutils.ADefinitionWalkerTest;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPDefinitionWalker.Try;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class NotCorrectStartNodeTypeTest extends ADefinitionWalkerTest
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

        TestTinsPHPDefinitionWalker walker = spy(createWalker(ast));
        Method method = TestTinsPHPDefinitionWalker.class.getMethod(methodName);
        method.invoke(walker);

        try {
            verify(walker).reportError(any(NoViableAltException.class));
        } catch (Exception e) {
            fail(methodName + " failed - verify caused exception:\n" + e.getClass().getName() + e.getMessage());
        }
    }

    @Test
    public void withBacktracking_stateFailedIsTrue()
            throws RecognitionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ITSPHPAst ast = createAst(tokenType);

        TestTinsPHPDefinitionWalker walker = createWalker(ast);
        walker.setBacktrackingLevel(1);
        Method method = TestTinsPHPDefinitionWalker.class.getMethod(methodName);
        method.invoke(walker);

        assertThat(methodName + " failed - state was false. ", walker.getState().failed, is(true));
        assertThat(methodName + " failed - next node was different. ", treeNodeStream.LA(1), is(tokenType));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(new Object[][]{
                //TODO rstoll TINS-162 definition phase - scopes
//                {"atom", Try},
//                {"blockConditional", Try},
                {"bottomup", Try},
                //TODO rstoll TINS-161 inference OOP
//                {"classDefinition", Try},
                //TODO rstoll TINS-162 definition phase - scopes
//                {"constant", Try},
//                {"constantDefinitionList", Try},
                //TODO rstoll TINS-161 inference OOP
//                {"constructDefinition", Try},
                {"exitNamespace", Try},
                {"exitScope", Try},
                //TODO rstoll TINS-162 definition phase - scopes
//                {"foreachLoop", Try},
                //TODO rstoll TINS-161 inference OOP
//                {"interfaceDefinition", Try},
//                {"methodFunctionCall", Try},
                {"namespaceDefinition", Try},
                //TODO rstoll TINS-162 definition phase - scopes
//                {"parameterDeclaration", Try},
//                {"parameterDeclarationList", Try},
//                {"primitiveTypesWithoutResource",Try},
//                {"returnBreakContinue", Try},
                {"topdown", Try},
                //TODO rstoll TINS-162 definition phase - scopes
//                {"useDeclaration", Try},
//                {"useDefinitionList", Try},
        });
    }
}

