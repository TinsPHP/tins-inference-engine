/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class IReferencePhaseController from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;

/**
 * Represents the interface between the TSPHPReferenceWalker (ANTLR generated) and the logic.
 */
public interface IReferencePhaseController
{

    IVariableSymbol resolveConstant(ITSPHPAst ast);

    //TODO rstoll TINS-223 reference phase - resolve this and self
//    IVariableSymbol resolveThisSelf(ITSPHPAst $this);

    //TODO rstoll TINS-225 reference phase - resolve parent
//    IVariableSymbol resolveParent(ITSPHPAst $this);

    IVariableSymbol resolveVariable(ITSPHPAst variableId);

    //TODO rstoll TINS-224 reference phase - resolve types
//    IScalarTypeSymbol resolveScalarType(ITSPHPAst typeAst, ITSPHPAst typeModifierAst);
//
//    ITypeSymbol resolvePrimitiveType(ITSPHPAst typeASt, ITSPHPAst typeModifierAst);
//
//    /**
//     * Try to resolve the type for the given typeAst and returns an
//     * {@link IErroneousTypeSymbol} if the type could not be found.
//     *
//     * @param typeAst The AST node which contains the type name. For instance, int, MyClass, \Exception etc.
//     * @return The corresponding type or a {@link IErroneousTypeSymbol} if could not be found.
//     */
//    ITypeSymbol resolveType(ITSPHPAst typeAst, ITSPHPAst typeModifierAst);
//
//    IErroneousTypeSymbol createErroneousTypeSymbol(ITSPHPErrorAst typeAst);
//
//    IErroneousTypeSymbol createErroneousTypeSymbol(ITSPHPAst typeAst, RecognitionException ex);

    ITypeSymbol resolveUseType(ITSPHPAst typeAst, ITSPHPAst alias);

    boolean checkUseDefinition(ITSPHPAst alias);

    boolean checkIsNotForwardReference(ITSPHPAst identifier);

    boolean checkIsNotDoubleDefinition(ITSPHPAst identifier);

    boolean checkIsNotDoubleDefinitionCaseInsensitive(ITSPHPAst identifier);


    //TODO rstoll TINS-219 reference phase - check are variables initialised
//    boolean checkVariableIsInitialised(ITSPHPAst variableId);
//
//    void sendUpInitialisedSymbols(ITSPHPAst blockConditional);
//
//    void sendUpInitialisedSymbolsAfterIf(ITSPHPAst ifBlock, ITSPHPAst elseBlock);
//
//    void sendUpInitialisedSymbolsAfterSwitch(List<ITSPHPAst> conditionalBlocks, boolean hasDefaultLabel);
//
//    void sendUpInitialisedSymbolsAfterTryCatch(List<ITSPHPAst> conditionalBlocks);

    void addImplicitReturnStatementIfRequired(
            boolean isReturning, boolean hasAtLeastOneReturnOrThrow, ITSPHPAst identifier, ITSPHPAst block);

//    void checkReturnsFromMethod(boolean isReturning, boolean hasAtLeastOneReturnOrThrow, ITSPHPAst identifier);


}
