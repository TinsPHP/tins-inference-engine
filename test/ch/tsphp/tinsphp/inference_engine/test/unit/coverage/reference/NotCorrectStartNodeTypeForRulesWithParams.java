/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class NotCorrectStartNodeTypeForRulesWithParams from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.coverage.reference;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.TSPHPAst;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference.TestTinsPHPReferenceWalker;
import ch.tsphp.tinsphp.inference_engine.test.unit.testutils.AReferenceWalkerTest;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Try;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class NotCorrectStartNodeTypeForRulesWithParams extends AReferenceWalkerTest
{
    @Test
    public void allTypesOrUnknown_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.allTypesOrUnknown(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void arrayType_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.arrayType(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void block_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.block(false);

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void classInterfaceType_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.classInterfaceType(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

    //TODO TINS-210 - reference phase - class definitions
//    @Test
//    public void classExtendsDeclaration_WrongStartNode_reportNoViableAltException() throws RecognitionException {
//        ITSPHPAst ast = createAst(Try);
//
//        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
//        walker.classExtendsDeclaration(new TSPHPAst());
//
//        verify(walker).reportError(any(NoViableAltException.class));
//    }

//    @Test
//    public void implementsDeclaration_WrongStartNode_reportNoViableAltException() throws RecognitionException {
//        ITSPHPAst ast = createAst(Try);
//
//        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
//        walker.implementsDeclaration(new TSPHPAst());
//
//        verify(walker).reportError(any(NoViableAltException.class));
//    }

    @Test
    public void constDeclaration_WrongStartNode_reportNoViableAltException() throws RecognitionException {

        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.constDeclaration(mock(ITypeSymbol.class));

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void instructions_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.instructions(false);

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void instruction_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.instruction(false);

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void ifCondition_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.ifCondition(false);

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void blockConditional_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.blockConditional(false);

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void switchCondition_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.switchCondition(false);

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void switchContents_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.switchContents(false);

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void doWhileLoop_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.doWhileLoop(false);

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void tryCatch_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.tryCatch(false);

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void catchBlocks_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.catchBlocks(false);

        verify(walker).reportError(any(NoViableAltException.class));
    }

    //TODO rstoll TINS-211 reference phase - interface definitions
//    @Test
//    public void interfaceExtendsDeclaration_WrongStartNode_reportNoViableAltException() throws RecognitionException {
//        ITSPHPAst ast = createAst(Try);
//
//        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
//        walker.interfaceExtendsDeclaration(new TSPHPAst());
//
//        verify(walker).reportError(any(NoViableAltException.class));
//    }

    @Test
    public void parameterNormalOrOptional_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.parameterNormalOrOptional(mock(ITypeSymbol.class));

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void scalarTypesOrUnknown_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.scalarTypesOrUnknown(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void scalarTypes_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.scalarTypes(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

//    @Test
//    public void variableDeclaration_WrongStartNode_reportNoViableAltException() throws RecognitionException {
//        ITSPHPAst ast = createAst(Try);
//
//        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
//        walker.variableDeclaration(mock(ITypeSymbol.class), false);
//
//        verify(walker).reportError(any(NoViableAltException.class));
//    }

    @Test
    public void returnTypesOrUnknown_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Try);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.returnTypesOrUnknown(mock(ITSPHPAst.class));

        verify(walker).reportError(any(NoViableAltException.class));
    }
}
