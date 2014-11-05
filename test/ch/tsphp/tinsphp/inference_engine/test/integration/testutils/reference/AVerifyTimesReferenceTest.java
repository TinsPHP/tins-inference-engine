/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AVerifyTimesReferenceTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference;

import ch.tsphp.tinsphp.common.ICore;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IModifierHelper;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.inference_engine.IReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.utils.IAstModificationHelper;
import org.junit.Ignore;
import org.mockito.exceptions.base.MockitoAssertionError;

import static org.mockito.Mockito.spy;

@Ignore
public abstract class AVerifyTimesReferenceTest extends AReferenceTest
{
    protected int howManyTimes;

    public AVerifyTimesReferenceTest(String testString, int times) {
        super(testString);
        howManyTimes = times;
    }

    protected abstract void verifyTimes();

    @Override
    protected void verifyReferences() {
        try {
            verifyTimes();
        } catch (MockitoAssertionError e) {
            System.err.println(testString + " failed.");
            throw e;
        }
    }

    @Override
    protected IReferencePhaseController createReferencePhaseController(
            ISymbolFactory theSymbolFactory,
            IInferenceErrorReporter theInferenceErrorReporter,
            IAstModificationHelper theAstModificationHelper,
            ICore theCore,
            IModifierHelper theModifierHelper,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {
        return spy(super.createReferencePhaseController(
                theSymbolFactory,
                theInferenceErrorReporter,
                theAstModificationHelper,
                theCore,
                theModifierHelper,
                theGlobalDefaultNamespace
        ));
    }

}
