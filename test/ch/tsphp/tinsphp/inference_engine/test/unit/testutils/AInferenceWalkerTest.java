/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AReferenceWalkerTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.testutils;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.inference.IInferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference.TestTinsPHPInferenceWalker;
import org.antlr.runtime.tree.TreeNodeStream;
import org.junit.Ignore;

import static org.mockito.Mockito.mock;

@Ignore
public abstract class AInferenceWalkerTest extends AWalkerTest
{
    protected TreeNodeStream treeNodeStream;
    protected IInferencePhaseController inferencePhaseController;

    protected TestTinsPHPInferenceWalker createWalker(ITSPHPAst ast) {
        treeNodeStream = createTreeNodeStream(ast);
        inferencePhaseController = mock(IInferencePhaseController.class);
        return new TestTinsPHPInferenceWalker(treeNodeStream, inferencePhaseController);
    }
}
