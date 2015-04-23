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

import static ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker.Else;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class NotCorrectStartNodeTypeForRulesWithParams extends AReferenceWalkerTest
{
    @Test
    public void allTypesOrUnknown_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.allTypesOrUnknown(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void arrayType_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.arrayType(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void classInterfaceType_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.classInterfaceType(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

    //TODO TINS-210 - reference phase - class definitions
//    @Test
//    public void classExtendsDeclaration_WrongStartNode_reportNoViableAltException() throws RecognitionException {
//        ITSPHPAst ast = createAst(Else);
//
//        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
//        walker.classExtendsDeclaration(new TSPHPAst());
//
//        verify(walker).reportError(any(NoViableAltException.class));
//    }

//    @Test
//    public void implementsDeclaration_WrongStartNode_reportNoViableAltException() throws RecognitionException {
//        ITSPHPAst ast = createAst(Else);
//
//        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
//        walker.implementsDeclaration(new TSPHPAst());
//
//        verify(walker).reportError(any(NoViableAltException.class));
//    }

    @Test
    public void constDeclaration_WrongStartNode_reportNoViableAltException() throws RecognitionException {

        ITSPHPAst ast = createAst(Else);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.constantDefinition(mock(ITypeSymbol.class));

        verify(walker).reportError(any(NoViableAltException.class));
    }

    //TODO rstoll TINS-211 reference phase - interface definitions
//    @Test
//    public void interfaceExtendsDeclaration_WrongStartNode_reportNoViableAltException() throws RecognitionException {
//        ITSPHPAst ast = createAst(Else);
//
//        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
//        walker.interfaceExtendsDeclaration(new TSPHPAst());
//
//        verify(walker).reportError(any(NoViableAltException.class));
//    }


    @Test
    public void scalarTypesOrUnknown_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.scalarTypesOrUnknown(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void scalarTypesOrArrayType_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.scalarTypesOrArrayType(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void scalarTypes_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.scalarTypes(new TSPHPAst());

        verify(walker).reportError(any(NoViableAltException.class));
    }

    @Test
    public void returnTypesOrUnknown_WrongStartNode_reportNoViableAltException() throws RecognitionException {
        ITSPHPAst ast = createAst(Else);

        TestTinsPHPReferenceWalker walker = spy(createWalker(ast));
        walker.returnTypesOrUnknown(mock(ITSPHPAst.class));

        verify(walker).reportError(any(NoViableAltException.class));
    }
}
