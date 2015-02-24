/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ArrayKeyValueBranchesTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.coverage.reference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.TreeAdaptor;
import org.antlr.runtime.tree.TreeNodeStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.exceptions.base.MockitoAssertionError;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collection;

import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.ACTUAL_PARAMETERS;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.ARRAY_ACCESS;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Assign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.At;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.BitwiseAnd;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.BitwiseAndAssign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.BitwiseNot;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.BitwiseOr;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.BitwiseOrAssign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.BitwiseXor;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.BitwiseXorAssign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.CAST;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.CONSTANT;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Clone;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Divide;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.DivideAssign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.EOF;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Equal;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Exit;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.FUNCTION_CALL;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.False;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Float;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.GreaterEqualThan;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.GreaterThan;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Identical;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Instanceof;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Int;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.LessEqualThan;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.LessThan;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.LogicAnd;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.LogicAndWeak;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.LogicNot;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.LogicOr;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.LogicOrWeak;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.LogicXorWeak;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Minus;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.MinusAssign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Modulo;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.ModuloAssign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Multiply;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.MultiplyAssign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.NotEqual;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.NotIdentical;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Null;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.POST_DECREMENT;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.POST_INCREMENT;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.PRE_DECREMENT;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.PRE_INCREMENT;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Plus;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.PlusAssign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.QuestionMark;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.ShiftLeft;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.ShiftLeftAssign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.ShiftRight;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.ShiftRightAssign;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.String;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.TYPE;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.TYPE_MODIFIER;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.TYPE_NAME;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.True;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.TypeInt;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.UNARY_MINUS;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.UNARY_PLUS;
import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.VariableId;
import static org.antlr.runtime.tree.TreeParser.DOWN;
import static org.antlr.runtime.tree.TreeParser.UP;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ArrayKeyValueBranchesTest
{
    private int counter = 0;
    private String operator;
    private Integer[] tokens;

    public ArrayKeyValueBranchesTest(String theOperator, Integer[] theTokens) {
        operator = theOperator;
        tokens = theTokens;
    }

    @Test
    public void arrayKeyValue_WithoutKey_CallsExpressionThrice() throws RecognitionException {
        withoutKeySuccess(operator, tokens);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testStrings() {
        return Arrays.asList(
                entry("ARRAY_ACCESS", ARRAY_ACCESS, DOWN, VariableId, True, UP),
                entry("Assign", Assign, DOWN, VariableId, Int, UP),
                entry("At", At, DOWN, FUNCTION_CALL, DOWN, TYPE_NAME, ACTUAL_PARAMETERS, UP, UP),
                entry("BitwiseAnd", BitwiseAnd, DOWN, Int, Int, UP),
                entry("BitwiseAndAssign", BitwiseAndAssign, DOWN, Int, Int, UP),
                entry("BitwiseNot", BitwiseNot, DOWN, Int, UP),
                entry("BitwiseOr", BitwiseOr, DOWN, Int, Int, UP),
                entry("BitwiseOrAssign", BitwiseOrAssign, DOWN, Int, Int, UP),
                entry("BitwiseXor", BitwiseXor, DOWN, Int, Int, UP),
                entry("BitwiseXorAssign", BitwiseXorAssign, DOWN, Int, Int, UP),
                entry("CAST", CAST, DOWN, TYPE, DOWN, TYPE_MODIFIER, TypeInt, UP, String, UP),
                //TODO TINS-161 inference OOP
//                entry("FIELD_ACCESS", FIELD_ACCESS, TreeParser.DOWN, VariableId, Identifier, TreeParser.UP),
                entry("CONSTANT", CONSTANT),
                entry("Clone", Clone, DOWN, VariableId, UP),
                entry("Divide", Divide, DOWN, Int, Int, UP),
                entry("DivideAssign", DivideAssign, DOWN, VariableId, Int, UP),
                entry("Equal", Equal, DOWN, VariableId, Int, UP),
                entry("Exit", Exit, DOWN, Int, UP),
                entry("False", False),
                entry("FUNCTION_CALL", FUNCTION_CALL, DOWN, TYPE_NAME, ACTUAL_PARAMETERS, UP),
                entry("Float", Float),
                entry("GreaterEqualThan", GreaterEqualThan, DOWN, VariableId, Int, UP),
                entry("GreaterThan", GreaterThan, DOWN, VariableId, Int, UP),
                entry("Identical", Identical, DOWN, VariableId, Int, UP),
                entry("Instanceof", Instanceof, DOWN, VariableId, TYPE_NAME, UP),
                entry("Int", Int),
                entry("LessEqualThan", LessEqualThan, DOWN, VariableId, Int, UP),
                entry("LessThan", LessThan, DOWN, VariableId, Int, UP),
                entry("LogicAnd", LogicAnd, DOWN, VariableId, Int, UP),
                entry("LogicAndWeak", LogicAndWeak, DOWN, VariableId, Int, UP),
                entry("LogicNot", LogicNot, DOWN, VariableId, UP),
                entry("LogicOr", LogicOr, DOWN, VariableId, Int, UP),
                entry("LogicOrWeak", LogicOrWeak, DOWN, VariableId, Int, UP),
                entry("LogicXorWeak", LogicXorWeak, DOWN, VariableId, Int, UP),
                //TODO TINS-161 inference OOP
//                entry("METHOD_CALL", METHOD_CALL, DOWN, VariableId, Identifier, ACTUAL_PARAMETERS, UP),
//                entry("METHOD_CALL_POSTFIX", METHOD_CALL_POSTFIX, DOWN, VariableId, Identifier, ACTUAL_PARAMETERS,
// UP),
//                entry("METHOD_CALL_STATIC", METHOD_CALL_STATIC, DOWN, TYPE_NAME, Identifier, ACTUAL_PARAMETERS, UP),
                entry("Minus", Minus, DOWN, VariableId, Int, UP),
                entry("MinusAssign", MinusAssign, DOWN, VariableId, Int, UP),
                entry("Modulo", Modulo, DOWN, VariableId, Int, UP),
                entry("ModuloAssign", ModuloAssign, DOWN, VariableId, Int, UP),
                entry("Multiply", Multiply, DOWN, VariableId, Int, UP),
                entry("MultiplyAssign", MultiplyAssign, DOWN, VariableId, Int, UP),
                //TODO TINS-161 inference OOP
//                entry("New", New, DOWN, TYPE_NAME, ACTUAL_PARAMETERS, UP),
                entry("NotEqual", NotEqual, DOWN, VariableId, VariableId, UP),
                entry("NotIdentical", NotIdentical, DOWN, VariableId, VariableId, UP),
                entry("Null", Null),
                entry("POST_INCREMENT", POST_INCREMENT, DOWN, VariableId, UP),
                entry("POST_DECREMENT", POST_DECREMENT, DOWN, VariableId, UP),
                entry("PRE_INCREMENT", PRE_INCREMENT, DOWN, VariableId, UP),
                entry("PRE_DECREMENT", PRE_DECREMENT, DOWN, VariableId, UP),
                entry("Plus", Plus, DOWN, VariableId, Int, UP),
                entry("PlusAssign", PlusAssign, DOWN, VariableId, Int, UP),
                entry("QuestionMark", QuestionMark, DOWN, True, VariableId, Int, UP),
                entry("ShiftLeft", ShiftLeft, DOWN, VariableId, Int, UP),
                entry("ShiftLeftAssign", ShiftLeftAssign, DOWN, VariableId, Int, UP),
                entry("ShiftRight", ShiftRight, DOWN, VariableId, Int, UP),
                entry("ShiftRightAssign", ShiftRightAssign, DOWN, VariableId, Int, UP),
                entry("String", String),
                entry("True", True),
                //TODO TINS-161 inference OOP
//                entry("This", This),
                entry("UNARY_MINUS", UNARY_MINUS, DOWN, VariableId, UP),
                entry("UNARY_PLUS", UNARY_PLUS, DOWN, VariableId, UP)
        );
    }

    private static Object[] entry(final String operator, final Integer... tokens) {
        return new Object[]{operator, tokens};
    }

    private void withoutKeySuccess(final String operator, final Integer... tokens) throws RecognitionException {
        counter = 0;

        TreeNodeStream input = mock(TreeNodeStream.class);
        when(input.LA(1)).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return tokens[counter];
            }
        });
        when(input.LA(2)).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (counter + 1 < tokens.length) {
                    return tokens[counter + 1];
                } else {
                    return EOF;
                }
            }
        });

        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                ++counter;
                return true;
            }
        }).when(input).consume();

        TreeAdaptor treeAdaptor = mock(TreeAdaptor.class);
        when(input.getTreeAdaptor()).thenReturn(treeAdaptor);

        when(treeAdaptor.getType(anyObject())).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new MockitoAssertionError("operator: " + operator + " failed. current counter: " + counter);
            }
        });
        when(input.LT(1)).thenReturn(mock(ITSPHPAst.class));
        IReferencePhaseController controller = mock(IReferencePhaseController.class);
        ITSPHPAstAdaptor astAdaptor = mock(ITSPHPAstAdaptor.class);

        TinsPHPReferenceWalker walker = spy(new TinsPHPReferenceWalker(input, controller, astAdaptor));
        walker.arrayKeyValue();

        verify(input, times(tokens.length)).consume();
    }
}
