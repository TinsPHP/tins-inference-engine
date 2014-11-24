/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ScopeFactory from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.scopes;

import ch.tsphp.common.IScope;
import ch.tsphp.tinsphp.common.scopes.IConditionalScope;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeFactory;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;

public class ScopeFactory implements IScopeFactory
{
    private final IScopeHelper scopeHelper;

    public ScopeFactory(IScopeHelper theScopeHelper) {
        scopeHelper = theScopeHelper;
    }

    @Override
    public IGlobalNamespaceScope createGlobalNamespaceScope(String name) {
        return new GlobalNamespaceScope(scopeHelper, name);
    }

    @Override
    public INamespaceScope createNamespaceScope(String name, IGlobalNamespaceScope currentScope) {
        return new NamespaceScope(name, currentScope);
    }

    @Override
    public IConditionalScope createConditionalScope(IScope currentScope) {
        return new ConditionalScope(scopeHelper, currentScope);
    }
}
