/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ReferenceScopeTestStruct from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.reference;

import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestStruct;

import java.util.List;

public class TypeScopeTestStruct extends ScopeTestStruct
{

    public String typeText;
    public String typeScope;

    public TypeScopeTestStruct(String theAstText, String theSymbolScope, List<Integer> theAstAccessOrder,
            String theTypeText, String theTypeScope) {
        super(theAstText, theSymbolScope, theAstAccessOrder);
        typeText = theTypeText;
        typeScope = theTypeScope;

    }
}
