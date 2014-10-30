/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ScopeFactoryTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.scopes;

import ch.tsphp.common.IScope;
import ch.tsphp.tinsphp.common.scopes.IConditionalScope;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.scopes.IScopeFactory;
import ch.tsphp.tinsphp.common.scopes.IScopeHelper;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.scopes.ScopeFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ScopeFactoryTest
{
    public static final String SCOPE_NAME = "scopeName";
    private IScopeHelper scopeHelper;

    @Before
    public void setUp() {
        scopeHelper = mock(IScopeHelper.class);
    }

    @Test
    public void createGlobalNamespaceScope_GetScopeNameAfterwards_ReturnName() {
        //no arrange needed

        IScopeFactory scopeFactory = createScopeFactory();
        IGlobalNamespaceScope result = scopeFactory.createGlobalNamespaceScope(SCOPE_NAME);

        assertThat(result.getScopeName(), is(SCOPE_NAME));
    }

    @Test
    public void createConditionalScope_GetEnclosingScopeAfterwards_ReturnPassedScope() {
        INamespaceScope namespaceScope = mock(INamespaceScope.class);

        IScopeFactory scopeFactory = createScopeFactory();
        IConditionalScope result = scopeFactory.createConditionalScope(namespaceScope);

        assertThat(result.getEnclosingScope(), is((IScope) namespaceScope));
    }

    @Test
    public void createNamespaceScope_GetEnclosingScopeAfterwards_ReturnPassedScope() {
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);

        IScopeFactory scopeFactory = createScopeFactory();
        INamespaceScope result = scopeFactory.createNamespaceScope(SCOPE_NAME, globalNamespaceScope);

        assertThat(result.getEnclosingScope(), is((IScope) globalNamespaceScope));
    }

    @Test
    public void createNamespaceScope_GetScopeNameAfterwards_ReturnName() {
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);

        IScopeFactory scopeFactory = createScopeFactory();
        INamespaceScope result = scopeFactory.createNamespaceScope(SCOPE_NAME, globalNamespaceScope);

        assertThat(result.getScopeName(), is(SCOPE_NAME));
    }

    @Test
    public void createGlobalNamespaceScope_Twice_ReturnToDifferentObjects() {
        //no arrange needed

        IScopeFactory scopeFactory = createScopeFactory();
        IGlobalNamespaceScope result = scopeFactory.createGlobalNamespaceScope(SCOPE_NAME);
        IGlobalNamespaceScope result2 = scopeFactory.createGlobalNamespaceScope(SCOPE_NAME);

        assertThat(result, not(result2));
    }

    @Test
    public void createConditionalScope_Twice_ReturnToDifferentObjects() {
        INamespaceScope namespaceScope = mock(INamespaceScope.class);

        IScopeFactory scopeFactory = createScopeFactory();
        IConditionalScope result = scopeFactory.createConditionalScope(namespaceScope);
        IConditionalScope result2 = scopeFactory.createConditionalScope(namespaceScope);

        assertThat(result, not(result2));
    }

    @Test
    public void createNamespaceScope_Twice_ReturnToDifferentObjects() {
        IGlobalNamespaceScope globalNamespaceScope = mock(IGlobalNamespaceScope.class);

        IScopeFactory scopeFactory = createScopeFactory();
        INamespaceScope result = scopeFactory.createNamespaceScope(SCOPE_NAME, globalNamespaceScope);
        INamespaceScope result2 = scopeFactory.createNamespaceScope(SCOPE_NAME, globalNamespaceScope);

        assertThat(result, not(result2));
    }

    private IScopeFactory createScopeFactory() {
        return new ScopeFactory(scopeHelper, mock(IInferenceErrorReporter.class));
    }

}
