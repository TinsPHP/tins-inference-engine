/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.inference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestStruct;

import java.util.List;

public class SignatureTestStruct extends ScopeTestStruct
{
    public final List<String> signatures;

    public SignatureTestStruct(
            String theAstText, String theAstScope, List<Integer> theAstAccessOrder, List<String> theSignatures) {
        super(theAstText, theAstScope, theAstAccessOrder);
        signatures = theSignatures;
    }
}
