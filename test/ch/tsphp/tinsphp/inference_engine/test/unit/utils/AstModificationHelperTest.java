/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.utils;

import ch.tsphp.common.IAstHelper;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.gen.TokenTypes;
import ch.tsphp.tinsphp.inference_engine.utils.AstModificationHelper;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AstModificationHelperTest
{

    @Test
    public void createNullLiteral_Standard_DelegatesToAstHelper() {
        IAstHelper astHelper = mock(IAstHelper.class);
        ITSPHPAst nullLiteral = mock(ITSPHPAst.class);
        when(astHelper.createAst(TokenTypes.Null, "null")).thenReturn(nullLiteral);

        IAstModificationHelper astModificationHelper = createAstModificationHelper(astHelper);

        ITSPHPAst result = astModificationHelper.createNullLiteral();

        assertThat(result, is(nullLiteral));
    }

    @Test
    public void createReturnStatement_Standard_DelegatesToAstHelperAndFirstChildIsPassedException() {
        IAstHelper astHelper = mock(IAstHelper.class);
        ITSPHPAst returnAst = mock(ITSPHPAst.class);
        when(astHelper.createAst(TokenTypes.Return, "return")).thenReturn(returnAst);
        ITSPHPAst expr = mock(ITSPHPAst.class);

        IAstModificationHelper astModificationHelper = createAstModificationHelper(astHelper);

        ITSPHPAst result = astModificationHelper.createReturnStatement(expr);

        verify(returnAst).addChild(expr);
        assertThat(result, is(returnAst));
    }

    @Test
    public void getVariableDeclaration_Standard_DelegatesToAstHelper() {
        IAstHelper astHelper = mock(IAstHelper.class);
        String variableId = "$a";
        ITSPHPAst variableDeclarationList = mock(ITSPHPAst.class);
        when(astHelper.createAst(TokenTypes.VARIABLE_DECLARATION_LIST, "vars")).thenReturn(variableDeclarationList);
        ITSPHPAst type = mock(ITSPHPAst.class);
        when(astHelper.createAst(TokenTypes.TYPE, "type")).thenReturn(type);

        IAstModificationHelper astModificationHelper = createAstModificationHelper(astHelper);
        ITSPHPAst result = astModificationHelper.getVariableDeclaration(variableId);

        verify(astHelper).createAst(TokenTypes.VariableId, variableId);
        verify(variableDeclarationList).addChild(type);
        assertThat(result, is(variableDeclarationList));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void insertChildAt_IndexGreaterThanParent_ThrowsIndexOutOfBoundsException() {
        ITSPHPAst parent = mock(ITSPHPAst.class);
        when(parent.getChildCount()).thenReturn(2);

        IAstModificationHelper astModificationHelper = createAstModificationHelper();
        astModificationHelper.insertChildAt(parent, mock(ITSPHPAst.class), 3);

        //assert in annotation
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void insertChildAt_IndexLessThan0_ThrowsIndexOutOfBoundsException() {
        //no arrange necessary

        IAstModificationHelper astModificationHelper = createAstModificationHelper();
        astModificationHelper.insertChildAt(mock(ITSPHPAst.class), mock(ITSPHPAst.class), -1);

        //assert in annotation
    }

    @Test
    public void insertChildAt_Standard_MovesChildrenToBeAbleToInsertTheNewOne() {
        ITSPHPAst parent = mock(ITSPHPAst.class);
        when(parent.getChildCount()).thenReturn(1);
        ITSPHPAst child = mock(ITSPHPAst.class);
        when(parent.getChild(0)).thenReturn(child);
        ITSPHPAst newChild = mock(ITSPHPAst.class);

        IAstModificationHelper astModificationHelper = createAstModificationHelper();
        astModificationHelper.insertChildAt(parent, newChild, 0);

        verify(parent).getChild(0);
        verify(parent).setChild(0, newChild);
        verify(parent).addChild(child);
    }

    private IAstModificationHelper createAstModificationHelper() {
        return createAstModificationHelper(mock(IAstHelper.class));
    }

    protected IAstModificationHelper createAstModificationHelper(IAstHelper astHelper) {
        return new AstModificationHelper(astHelper);
    }
}
