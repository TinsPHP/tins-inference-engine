/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine;

import ch.tsphp.common.IErrorLogger;
import ch.tsphp.common.IErrorReporter;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.tinsphp.common.IInferenceEngine;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;
import ch.tsphp.tinsphp.common.inference.IInferenceEngineInitialiser;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPReferenceWalker;
import ch.tsphp.tinsphp.inference_engine.config.HardCodedInferenceEngineInitialiser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.TreeNodeStream;

import java.util.ArrayDeque;
import java.util.Collection;

public class InferenceEngine implements IInferenceEngine, IErrorLogger
{
    protected IInferenceEngineInitialiser inferenceEngineInitialiser;

    private final IDefinitionPhaseController definitionPhaseController;
    private final IReferencePhaseController referencePhaseController;
    private final IErrorReporter inferenceErrorReporter;

    private final Collection<IErrorLogger> errorLoggers = new ArrayDeque<>();
    private boolean hasFoundError = false;

    public InferenceEngine() {
        inferenceEngineInitialiser = new HardCodedInferenceEngineInitialiser();
        definitionPhaseController = inferenceEngineInitialiser.getDefinitionPhaseController();
        referencePhaseController = inferenceEngineInitialiser.getReferencePhaseController();
        inferenceErrorReporter = inferenceEngineInitialiser.getInferenceErrorReporter();
    }

    @Override
    public void enrichWithDefinitions(ITSPHPAst ast, TreeNodeStream treeNodeStream) {
        treeNodeStream.reset();
        ErrorReportingTinsPHPDefinitionWalker definitionWalker =
                new ErrorReportingTinsPHPDefinitionWalker(treeNodeStream, definitionPhaseController);
        definitionWalker.registerErrorLogger(this);
        definitionWalker.downup(ast);
    }

    @Override
    public void enrichWithReferences(ITSPHPAst ast, TreeNodeStream treeNodeStream) {
        treeNodeStream.reset();
        ErrorReportingTinsPHPReferenceWalker referenceWalker =
                new ErrorReportingTinsPHPReferenceWalker(treeNodeStream, referencePhaseController);
        referenceWalker.registerErrorLogger(this);
        try {
            referenceWalker.compilationUnit();
        } catch (RecognitionException ex) {
            // should never happen, ErrorReportingTSPHPReferenceWalker should catch it already.
            // but just in case and to be complete
            hasFoundError = true;
            for (IErrorLogger logger : errorLoggers) {
                logger.log(new TSPHPException(ex));
            }
        }
    }

    @Override
    public void enrichtWithTypes(ITSPHPAst ast, TreeNodeStream treeNodeStream) {
    }

    @Override
    public boolean hasFoundError() {
        return hasFoundError || inferenceErrorReporter.hasFoundError();
    }

    @Override
    public void registerErrorLogger(IErrorLogger errorLogger) {
        errorLoggers.add(errorLogger);
        inferenceErrorReporter.registerErrorLogger(errorLogger);
    }

    @Override
    public void reset() {
        hasFoundError = false;
        inferenceEngineInitialiser.reset();
    }

    @Override
    public void log(TSPHPException exception) {
        hasFoundError = true;
    }
}
