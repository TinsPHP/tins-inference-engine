/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.utils;

import ch.tsphp.common.ITSPHPAst;

/**
 * Provides methods which shall simplify the modification of an AST.
 */
public interface IAstModificationHelper
{
    ITSPHPAst createNullLiteral();

    ITSPHPAst createReturnStatement(ITSPHPAst expression);

    ITSPHPAst getVariableDeclaration(String variableId);

    void insertChildAt(ITSPHPAst parent, ITSPHPAst child, int index);
}
