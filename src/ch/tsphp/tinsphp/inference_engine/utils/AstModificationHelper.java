/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.utils;

import ch.tsphp.common.IAstHelper;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;

public class AstModificationHelper implements IAstModificationHelper
{
    private IAstHelper astHelper;

    public AstModificationHelper(IAstHelper theAstHelper) {
        astHelper = theAstHelper;
    }

    @Override
    public ITSPHPAst getNullReturnStatement() {
        ITSPHPAst returnAst = astHelper.createAst(TokenTypes.Return, "return");
        returnAst.addChild(astHelper.createAst(TokenTypes.Null, "null"));
        return returnAst;
    }

    @Override
    public ITSPHPAst getVariableDeclaration(String variableId) {
        ITSPHPAst variableDeclarationList = astHelper.createAst(TokenTypes.VARIABLE_DECLARATION_LIST, "vars");
        ITSPHPAst type = astHelper.createAst(TokenTypes.TYPE, "type");
        type.addChild(astHelper.createAst(TokenTypes.TYPE_MODIFIER, "tMod"));
        type.addChild(astHelper.createAst(TokenTypes.QuestionMark, "?"));
        variableDeclarationList.addChild(type);
        variableDeclarationList.addChild(astHelper.createAst(TokenTypes.VariableId, variableId));
        return variableDeclarationList;
    }

    @Override
    public void insertChildAt(ITSPHPAst parent, ITSPHPAst child, int index) {
        int count = parent.getChildCount();
        if (count <= index) {
            throw new IndexOutOfBoundsException("parent " + parent.getText() + " has only " + count + " children "
                    + "and thus cannot insert the given child " + child.getText() + " at index " + index);
        } else if (index < 0) {
            throw new IndexOutOfBoundsException("index needs to be bigger than 0");
        }

        ITSPHPAst next = null;
        ITSPHPAst tmp = child;
        for (int i = index; i < count; ++i) {
            next = parent.getChild(i);
            parent.setChild(i, tmp);
            tmp = next;
        }
        parent.addChild(next);

    }
}
