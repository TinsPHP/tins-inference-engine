/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class TestDefinitionPhaseController from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.inference_engine.DefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.IDefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.scopes.INamespaceScope;
import ch.tsphp.tinsphp.inference_engine.scopes.IScopeFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDefinitionPhaseController extends DefinitionPhaseController implements IDefinitionPhaseController,
        ICreateSymbolListener
{

    private List<Map.Entry<ISymbol, ITSPHPAst>> symbols = new ArrayList<>();
    private ISymbol newlyCreatedSymbol;

    public TestDefinitionPhaseController(TestSymbolFactory aSymbolFactory, IScopeFactory aScopeFactory) {
        super(aSymbolFactory, aScopeFactory);
        aSymbolFactory.registerListener(this);
    }

    public List<Map.Entry<ISymbol, ITSPHPAst>> getSymbols() {
        return symbols;
    }

    @Override
    public void defineUse(INamespaceScope currentScope, ITSPHPAst type, ITSPHPAst alias) {
        super.defineUse(currentScope, type, alias);
        symbols.add(new HashMap.SimpleEntry<>(alias.getSymbol(), type));
    }

    //TODO rstoll TINS-161 inference OOP
//    @Override
//    @SuppressWarnings("checkstyle:parameternumber")
//    public IInterfaceTypeSymbol defineInterface(IScope currentScope, ITSPHPAst modifier, ITSPHPAst identifier,
//            ITSPHPAst extendsIds) {
//        IInterfaceTypeSymbol symbol = super.defineInterface(currentScope, modifier, identifier, extendsIds);
//        ITSPHPAst identifiers = null;
//        if (extendsIds.getChildCount() > 0) {
//            identifiers = new TSPHPAst();
//            appendChildrenFromTo(extendsIds, identifiers);
//        }
//        symbols.add(new HashMap.SimpleEntry<>(newlyCreatedSymbol, identifiers));
//        return symbol;
//    }

//    @Override
//    @SuppressWarnings("checkstyle:parameternumber")
//    public IClassTypeSymbol defineClass(IScope currentScope, ITSPHPAst modifier, ITSPHPAst identifier,
//            ITSPHPAst extendsIds, ITSPHPAst implementsIds) {
//        IClassTypeSymbol scope = super.defineClass(currentScope, modifier, identifier, extendsIds, implementsIds);
//
//        ITSPHPAst identifiers = null;
//        if (extendsIds.getChildCount() > 0 || implementsIds.getChildCount() > 0) {
//            identifiers = new TSPHPAst();
//            appendChildrenFromTo(extendsIds, identifiers);
//            appendChildrenFromTo(implementsIds, identifiers);
//        }
//        symbols.add(new HashMap.SimpleEntry<>(newlyCreatedSymbol, identifiers));
//
//        return scope;
//    }

//    @Override
//    @SuppressWarnings("checkstyle:parameternumber")
//    public IMethodSymbol defineMethod(IScope currentScope, ITSPHPAst methodModifier,
//            ITSPHPAst returnTypeModifier, ITSPHPAst returnType, ITSPHPAst identifier) {
//        IMethodSymbol scope = super.defineMethod(currentScope, methodModifier, returnTypeModifier, returnType,
//                identifier);
//        symbols.add(new HashMap.SimpleEntry<>(newlyCreatedSymbol, returnType));
//        return scope;
//    }

    @Override
    public void defineVariable(IScope currentScope, ITSPHPAst modifier, ITSPHPAst type, ITSPHPAst variableId) {
        super.defineVariable(currentScope, modifier, type, variableId);
        symbols.add(new HashMap.SimpleEntry<>(newlyCreatedSymbol, type));
    }

    @Override
    public void setNewlyCreatedSymbol(ISymbol symbol) {
        newlyCreatedSymbol = symbol;
    }

//    private void appendChildrenFromTo(ITSPHPAst source, ITSPHPAst target) {
//        int length = source.getChildCount();
//        for (int i = 0; i < length; ++i) {
//            target.addChild(AstHelperRegistry.get().copyAst(source.getChild(i)));
//        }
//    }
}
