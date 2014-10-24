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
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.inference_engine.scopes.INamespaceScope;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeFactory;

import java.util.ArrayList;
import java.util.List;

public class TestNamespaceScopeFactory extends ScopeFactory
{

    public List<IScope> scopes = new ArrayList<>();

    public TestNamespaceScopeFactory(IScopeHelper theScopeHelper, IInferenceErrorReporter theInferenceErrorReporter) {
        super(theScopeHelper, theInferenceErrorReporter);
    }

    @Override
    public INamespaceScope createNamespaceScope(String name, IGlobalNamespaceScope currentScope) {
        INamespaceScope scope = super.createNamespaceScope(name, currentScope);
        scopes.add(scope);
        return scope;

    }
}
