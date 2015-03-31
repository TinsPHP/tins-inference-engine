/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.tinsphp.common.IInferenceEngine;
import ch.tsphp.tinsphp.common.inference.IInferenceEngineInitialiser;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.issues.IIssueLogger;
import ch.tsphp.tinsphp.common.issues.IssueReporterHelper;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPDefinitionWalker;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPInferenceWalker;
import ch.tsphp.tinsphp.inference_engine.antlrmod.ErrorReportingTinsPHPReferenceWalker;
import ch.tsphp.tinsphp.inference_engine.config.HardCodedInferenceEngineInitialiser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.TreeNodeStream;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumSet;

public class InferenceEngine implements IInferenceEngine, IIssueLogger
{
    protected IInferenceEngineInitialiser inferenceEngineInitialiser;

    private final ITSPHPAstAdaptor astAdaptor;
    private final IInferenceIssueReporter inferenceIssueReporter;
    private final Collection<IIssueLogger> issueLoggers = new ArrayDeque<>();

    private EnumSet<EIssueSeverity> foundIssues = EnumSet.noneOf(EIssueSeverity.class);

    public InferenceEngine(ITSPHPAstAdaptor theAstAdaptor) {
        astAdaptor = theAstAdaptor;
        inferenceEngineInitialiser = new HardCodedInferenceEngineInitialiser();
        inferenceIssueReporter = inferenceEngineInitialiser.getInferenceErrorReporter();
    }

    @Override
    public void enrichWithDefinitions(ITSPHPAst ast, TreeNodeStream treeNodeStream) {
        treeNodeStream.reset();
        ErrorReportingTinsPHPDefinitionWalker definitionWalker = new ErrorReportingTinsPHPDefinitionWalker(
                treeNodeStream, inferenceEngineInitialiser.getDefinitionPhaseController());

        for (IIssueLogger logger : issueLoggers) {
            definitionWalker.registerIssueLogger(logger);
        }
        definitionWalker.registerIssueLogger(this);
        definitionWalker.downup(ast);
    }

    @Override
    public void enrichWithReferences(ITSPHPAst ast, TreeNodeStream treeNodeStream) {
        treeNodeStream.reset();
        ErrorReportingTinsPHPReferenceWalker referenceWalker = new ErrorReportingTinsPHPReferenceWalker(
                treeNodeStream, inferenceEngineInitialiser.getReferencePhaseController(), astAdaptor);

        for (IIssueLogger logger : issueLoggers) {
            referenceWalker.registerIssueLogger(logger);
        }
        referenceWalker.registerIssueLogger(this);
        try {
            referenceWalker.compilationUnit();
        } catch (RecognitionException ex) {
            // should never happen, ErrorReportingTSPHPReferenceWalker should catch it already.
            // but just in case and to be complete
            log(new TSPHPException(ex), EIssueSeverity.FatalError);
            for (IIssueLogger logger : issueLoggers) {
                logger.log(new TSPHPException(ex), EIssueSeverity.FatalError);
            }
        }
    }

    @Override
    public void enrichtWithTypes(ITSPHPAst ast, TreeNodeStream treeNodeStream) {
        treeNodeStream.reset();
        ErrorReportingTinsPHPInferenceWalker referenceWalker = new ErrorReportingTinsPHPInferenceWalker(
                treeNodeStream,
                inferenceEngineInitialiser.getInferencePhaseController(),
                inferenceEngineInitialiser.getDefinitionPhaseController().getGlobalDefaultNamespace());

        for (IIssueLogger logger : issueLoggers) {
            referenceWalker.registerIssueLogger(logger);
        }
        referenceWalker.registerIssueLogger(this);
        try {
            referenceWalker.compilationUnit();
        } catch (RecognitionException ex) {
            // should never happen, ErrorReportingTSPHPReferenceWalker should catch it already.
            // but just in case and to be complete
            log(new TSPHPException(ex), EIssueSeverity.FatalError);
            for (IIssueLogger logger : issueLoggers) {
                logger.log(new TSPHPException(ex), EIssueSeverity.FatalError);
            }
        }
    }

    @Override
    public void registerIssueLogger(IIssueLogger issueReporter) {
        issueLoggers.add(issueReporter);
        inferenceIssueReporter.registerIssueLogger(issueReporter);
    }

    @Override
    public boolean hasFound(EnumSet<EIssueSeverity> severities) {
        return IssueReporterHelper.hasFound(foundIssues, severities)
                || inferenceIssueReporter.hasFound(severities);
    }

    @Override
    public void reset() {
        foundIssues = EnumSet.noneOf(EIssueSeverity.class);
        inferenceEngineInitialiser.reset();
    }

    @Override
    public void log(TSPHPException ex, EIssueSeverity severity) {
        foundIssues.add(severity);
    }
}
