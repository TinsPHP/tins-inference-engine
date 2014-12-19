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

import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.tinsphp.common.IParser;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.issues.IIssueLogger;
import ch.tsphp.tinsphp.inference_engine.error.InferenceIssueReporter;
import ch.tsphp.tinsphp.parser.ParserFacade;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.List;

@Ignore
public abstract class ATest implements IIssueLogger
{

    protected List<Exception> exceptions = new ArrayList<>();
    protected IParser parser;
    protected IInferenceIssueReporter inferenceErrorReporter;

    public ATest() {
        parser = createParser();
        registerParserErrorLogger();

        inferenceErrorReporter = createInferenceErrorReporter();
        inferenceErrorReporter.registerIssueLogger(this);
    }

    @Override
    public void log(TSPHPException exception, EIssueSeverity severity) {
        exceptions.add(exception);
    }

    protected IParser createParser() {
        return new ParserFacade();
    }

    protected void registerParserErrorLogger() {
        parser.registerIssueLogger(new WriteExceptionToConsole());
    }

    protected IInferenceIssueReporter createInferenceErrorReporter() {
        return new InferenceIssueReporter();
    }
}
