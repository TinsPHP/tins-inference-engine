/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class TestTinsPHPReferenceWalker from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

import ch.tsphp.tinsphp.common.inference.IInferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPInferenceWalker;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;

public class TestTinsPHPInferenceWalker extends ErrorReportingTinsPHPInferenceWalker
{

    public TestTinsPHPInferenceWalker(
            TreeNodeStream input, IInferencePhaseController controller) {
        super(input, controller);
    }

    public RecognizerSharedState getState() {
        return state;
    }
}
