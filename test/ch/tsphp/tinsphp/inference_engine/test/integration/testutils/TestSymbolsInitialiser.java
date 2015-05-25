/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;


import ch.tsphp.tinsphp.common.scopes.IScopeFactory;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.symbols.config.HardCodedSymbolsInitialiser;

public class TestSymbolsInitialiser extends HardCodedSymbolsInitialiser
{
    private ISymbolFactory symbolFactory;
    private IScopeFactory scopeFactory;

    public TestSymbolsInitialiser() {
        symbolFactory = new TestSymbolFactory(getScopeHelper(), getModifierHelper(), getTypeHelper());
        scopeFactory = new TestNamespaceScopeFactory(getScopeHelper());
    }

    @Override
    public ISymbolFactory getSymbolFactory() {
        return symbolFactory;
    }

    @Override
    public IScopeFactory getScopeFactory() {
        return scopeFactory;
    }
}
