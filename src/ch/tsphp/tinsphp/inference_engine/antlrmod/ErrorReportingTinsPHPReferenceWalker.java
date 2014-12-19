/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ErrorReportingTSPHPReferenceWalker from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.antlrmod;

import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IIssueLogger;
import ch.tsphp.tinsphp.common.issues.IIssueReporter;
import ch.tsphp.tinsphp.common.issues.IssueReporterHelper;
import ch.tsphp.tinsphp.inference_engine.antlr.TinsPHPReferenceWalker;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.TreeNodeStream;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumSet;

/**
 * Extends TinsPHPDefinitionWalker by IErrorReporter.
 */
public class ErrorReportingTinsPHPReferenceWalker extends TinsPHPReferenceWalker implements IIssueReporter
{

    private final Collection<IIssueLogger> issueLoggers = new ArrayDeque<>();
    private boolean hasFoundFatalError;

    public ErrorReportingTinsPHPReferenceWalker(TreeNodeStream input, IReferencePhaseController controller) {
        super(input, controller);
    }

    @Override
    public void reportError(RecognitionException exception) {
        hasFoundFatalError = true;
        IssueReporterHelper.reportIssue(issueLoggers, exception, "definition");
    }

    @Override
    public boolean hasFound(EnumSet<EIssueSeverity> severity) {
        return hasFoundFatalError && severity.contains(EIssueSeverity.FatalError);
    }

    @Override
    public void registerIssueLogger(IIssueLogger issueLogger) {
        issueLoggers.add(issueLogger);
    }
}
