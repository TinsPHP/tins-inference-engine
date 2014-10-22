/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ADefinitionSymbolTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestHelper;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.List;
import java.util.Map;

@Ignore
public abstract class ADefinitionSymbolTest extends ADefinitionTest
{

    protected String expectedResult;

    public ADefinitionSymbolTest(String testString, String theExpectedResult) {
        super(testString);
        expectedResult = theExpectedResult;
    }

    @Override
    protected void verifyDefinitions() {
        super.verifyDefinitions();
        Assert.assertEquals(testString + " failed.", expectedResult, getSymbolsAsString());
    }

    public String getSymbolsAsString() {
        List<Map.Entry<ISymbol, ITSPHPAst>> symbols = definitionPhaseController.getSymbols();
        StringBuilder stringBuilder = new StringBuilder();
        boolean isFirstSymbol = true;
        for (Map.Entry<ISymbol, ITSPHPAst> entry : symbols) {
            if (!isFirstSymbol) {
                stringBuilder.append(" ");
            }
            isFirstSymbol = false;
            stringBuilder.append(getTypesAsString(entry.getValue()))
                    .append(ScopeTestHelper.getEnclosingScopeNames(entry.getKey().getDefinitionScope()))
                    .append(entry.getKey().toString());
        }
        return stringBuilder.toString();
    }

    protected String getTypesAsString(ITSPHPAst types) {
        String typesAsString;

        if (types == null) {
            typesAsString = "";
        } else if (types.getChildCount() == 0) {
            typesAsString = getSingleTypeAsString(types);
        } else {
            typesAsString = getMultipleTypesAsString(types);
        }

        return typesAsString;
    }

    protected String getSingleTypeAsString(ITSPHPAst type) {
        return ScopeTestHelper.getEnclosingScopeNames(type.getScope()) + type.getText() + " ";
    }

    protected String getMultipleTypesAsString(ITSPHPAst types) {

        StringBuilder stringBuilder = new StringBuilder();
        int length = types.getChildCount();
        for (int i = 0; i < length; ++i) {
            stringBuilder.append(getSingleTypeAsString(types.getChild(i)));
        }
        return stringBuilder.toString();
    }
}
