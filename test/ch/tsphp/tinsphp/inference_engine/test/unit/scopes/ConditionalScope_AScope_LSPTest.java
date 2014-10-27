/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ConditionalScope_AScope_LSPTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.scopes;

import ch.tsphp.common.IScope;
import ch.tsphp.tinsphp.inference_engine.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.scopes.AScope;
import ch.tsphp.tinsphp.inference_engine.scopes.ConditionalScope;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeHelper;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ConditionalScope_AScope_LSPTest extends AScopeTest
{

    @Override
    protected AScope createScope(IScopeHelper scopeHelper, String name, IScope enclosingScope) {
        return new ConditionalScope(scopeHelper, enclosingScope, mock(IInferenceErrorReporter.class));
    }

    @Override
    @Test
    public void getScopeName_Standard_ReturnsNamePassedInConstructor() {
        // different behaviour - returns always "cScope"
        // yet, does not really violate the Liskov Substitution Principle since it returns the name
        // it's just not possible to set the name for a ConditionalScope

        String name = "doesn't matter";

        AScope scope = createScope(mock(IScope.class));
        String result = scope.getScopeName();

        assertThat(result, is("cScope"));
    }
}
