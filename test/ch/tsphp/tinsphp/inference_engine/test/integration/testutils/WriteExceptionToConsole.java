/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class WriteExceptionToConsole from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;


import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.tinsphp.common.issues.EIssueSeverity;
import ch.tsphp.tinsphp.common.issues.IIssueLogger;

public class WriteExceptionToConsole implements IIssueLogger
{
    @Override
    public void log(TSPHPException exception, EIssueSeverity severity) {
        System.out.println(exception.getMessage());
    }
}
