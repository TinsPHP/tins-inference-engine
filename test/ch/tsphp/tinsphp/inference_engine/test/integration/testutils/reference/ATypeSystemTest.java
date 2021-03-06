/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference;

import ch.tsphp.tinsphp.common.ICore;
import org.junit.Ignore;

@Ignore
public abstract class ATypeSystemTest extends AReferenceTest
{
    protected ICore core;

    public ATypeSystemTest() {
        super("");
        core = coreInitialiser.getCore();
    }

    @Override
    public void runTest() {
        throw new UnsupportedOperationException("Extend AReferenceTest if you want to use this method.");
    }

    @Override
    protected void assertsInReferencePhase() {
        throw new UnsupportedOperationException("Extend AReferenceTest if you want to use this method.");
    }
}
