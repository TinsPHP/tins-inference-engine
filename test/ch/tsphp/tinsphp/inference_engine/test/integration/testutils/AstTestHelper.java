/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AstTestHelper from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.TSPHPAst;
import org.antlr.runtime.CommonToken;

public class AstTestHelper
{

    private AstTestHelper() {
    }

    public static ITSPHPAst getAstWithTokenText(String text, IScope scope) {
        ITSPHPAst ast = getAstWithTokenText(text);
        ast.setScope(scope);
        return ast;
    }

    public static ITSPHPAst getAstWithTokenText(String text) {
        return new TSPHPAst(new CommonToken(0, text));
    }
}
