/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class TestScopeFactory from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.common.IScope;
import ch.tsphp.tinsphp.common.scopes.IConditionalScope;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;

public class TestConditionalScopeFactory extends TestNamespaceScopeFactory
{

    public TestConditionalScopeFactory(IScopeHelper theScopeHelper, IInferenceErrorReporter theInferenceErrorReporter) {
        super(theScopeHelper, theInferenceErrorReporter);
    }

    @Override
    public IConditionalScope createConditionalScope(IScope currentScope) {
        IConditionalScope scope = super.createConditionalScope(currentScope);
        scopes.add(scope);
        return scope;
    }
}
