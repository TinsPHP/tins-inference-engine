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

import ch.tsphp.common.IErrorLogger;
import ch.tsphp.common.IParser;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.tinsphp.common.inference.error.IInferenceErrorReporter;
import ch.tsphp.tinsphp.inference_engine.error.InferenceErrorReporter;
import ch.tsphp.tinsphp.parser.ParserFacade;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.List;

@Ignore
public abstract class ATest implements IErrorLogger
{

    protected List<Exception> exceptions = new ArrayList<>();
    protected IParser parser;
    protected IInferenceErrorReporter inferenceErrorReporter;

    public ATest() {
        parser = createParser();
        registerParserErrorLogger();

        inferenceErrorReporter = createInferenceErrorReporter();
        inferenceErrorReporter.registerErrorLogger(this);
    }

    public void log(TSPHPException exception) {
        exceptions.add(exception);
    }

    protected IParser createParser() {
        return new ParserFacade();
    }

    protected void registerParserErrorLogger() {
        parser.registerErrorLogger(new WriteExceptionToConsole());
    }

    protected IInferenceErrorReporter createInferenceErrorReporter() {
        return new InferenceErrorReporter();
    }
}
