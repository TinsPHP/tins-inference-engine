/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ATest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.tinsphp.common.IParser;
import ch.tsphp.tinsphp.common.config.IParserInitialiser;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.issues.IIssueLogger;
import ch.tsphp.tinsphp.common.issues.IIssueMessageProvider;
import ch.tsphp.tinsphp.inference_engine.issues.HardCodedIssueMessageProvider;
import ch.tsphp.tinsphp.inference_engine.issues.InferenceIssueReporter;
import ch.tsphp.tinsphp.parser.config.HardCodedParserInitialiser;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.List;

@Ignore
public abstract class ATest implements IIssueLogger
{

    protected List<Exception> exceptions = new ArrayList<>();
    protected IParser parser;
    protected IInferenceIssueReporter inferenceErrorReporter;
    protected IIssueMessageProvider issueMessageProvider;
    protected ITSPHPAstAdaptor astAdaptor;


    public ATest() {
        astAdaptor = createAstAdaptor();

        IParserInitialiser parserInitialiser = createParserInitialiser(astAdaptor);
        parser = parserInitialiser.getParser();
        registerParserErrorLogger();

        issueMessageProvider = createIssueMessageProvider();

        inferenceErrorReporter = createInferenceErrorReporter(issueMessageProvider);
        inferenceErrorReporter.registerIssueLogger(this);
    }


    @Override
    public void log(TSPHPException exception, EIssueSeverity severity) {
        exceptions.add(exception);
    }

    protected ITSPHPAstAdaptor createAstAdaptor() {
        return new TSPHPAstAdaptor();
    }

    protected IParserInitialiser createParserInitialiser(ITSPHPAstAdaptor anAstAdaptor) {
        return new HardCodedParserInitialiser(anAstAdaptor);
    }

    protected void registerParserErrorLogger() {
        parser.registerIssueLogger(new WriteExceptionToConsole());
    }

    protected IIssueMessageProvider createIssueMessageProvider() {
        return new HardCodedIssueMessageProvider();
    }

    protected IInferenceIssueReporter createInferenceErrorReporter(IIssueMessageProvider theIssueMessageProvider) {
        return new InferenceIssueReporter(theIssueMessageProvider);
    }
}
