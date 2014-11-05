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
}
