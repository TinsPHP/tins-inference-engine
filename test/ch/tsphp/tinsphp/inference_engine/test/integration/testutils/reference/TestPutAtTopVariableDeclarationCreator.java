/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.IDefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.resolver.PutAtTopVariableDeclarationCreator;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;

public class TestPutAtTopVariableDeclarationCreator extends PutAtTopVariableDeclarationCreator
{

    public TestPutAtTopVariableDeclarationCreator(
            IAstModificationHelper theAstModificationHelper, IDefinitionPhaseController theDefinitionPhaseController) {
        super(theAstModificationHelper, theDefinitionPhaseController);
    }

    @Override
    protected void insertVariableDeclarationList(ITSPHPAst block, ITSPHPAst variableDeclarationList) {

    }
}
