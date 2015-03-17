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

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference;

import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPReferenceWalker;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;

public class TestTinsPHPReferenceWalker extends ErrorReportingTinsPHPReferenceWalker
{

    public TestTinsPHPReferenceWalker(
            TreeNodeStream input, IReferencePhaseController controller, ITSPHPAstAdaptor astAdaptor) {
        super(input, controller, astAdaptor);
    }

    public RecognizerSharedState getState() {
        return state;
    }
}
