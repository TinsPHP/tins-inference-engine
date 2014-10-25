/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class IDefinitionPhaseController from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.scopes.IConditionalScope;
import ch.tsphp.tinsphp.inference_engine.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.inference_engine.scopes.INamespaceScope;
import ch.tsphp.tinsphp.inference_engine.symbols.IMethodSymbol;

/**
 * Represents the interface between the TSPHPDefinitionWalker (ANTLR generated) and the logic.
 */
public interface IDefinitionPhaseController
{

    ILowerCaseStringMap<IGlobalNamespaceScope> getGlobalNamespaceScopes();

    IGlobalNamespaceScope getGlobalDefaultNamespace();

    INamespaceScope defineNamespace(String name);

    void defineUse(INamespaceScope currentScope, ITSPHPAst type, ITSPHPAst alias);

    //TODO rstoll TINS-161 inference OOP
//    @SuppressWarnings("checkstyle:parameternumber")
//    IClassTypeSymbol defineClass(IScope currentScope, ITSPHPAst modifier, ITSPHPAst identifier,
//            ITSPHPAst extendsIds, ITSPHPAst implementsIds);

    IConditionalScope defineConditionalScope(IScope currentScope);

    void defineConstant(IScope currentScope, ITSPHPAst modifier, ITSPHPAst type, ITSPHPAst identifier);

    //TODO rstoll TINS-161 inference OOP
//    @SuppressWarnings("checkstyle:parameternumber")
//    IMethodSymbol defineConstruct(IScope currentScope, ITSPHPAst methodModifier, ITSPHPAst returnTypeModifier,
//            ITSPHPAst returnType, ITSPHPAst identifier);
//
//    @SuppressWarnings("checkstyle:parameternumber")
//    IInterfaceTypeSymbol defineInterface(IScope currentScope, ITSPHPAst modifier, ITSPHPAst identifier,
//            ITSPHPAst extendsIds);
//
    @SuppressWarnings("checkstyle:parameternumber")
    IMethodSymbol defineMethod(IScope currentScope, ITSPHPAst methodModifier,
            ITSPHPAst returnTypeModifier, ITSPHPAst returnType, ITSPHPAst identifier);

    void defineVariable(IScope currentScope, ITSPHPAst modifier, ITSPHPAst type, ITSPHPAst variableId);
}
