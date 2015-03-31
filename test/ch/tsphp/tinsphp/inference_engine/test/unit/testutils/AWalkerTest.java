/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class AWalkerTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.testutils;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.TSPHPAst;
import ch.tsphp.common.TSPHPAstAdaptor;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.TreeNodeStream;
import org.junit.Ignore;

import static org.mockito.Mockito.mock;

@Ignore
public abstract class AWalkerTest
{

    protected TreeNodeStream createTreeNodeStream(ITSPHPAst ast) {
        return new CommonTreeNodeStream(new TSPHPAstAdaptor(), ast);
    }

    protected ITSPHPAst createAst(int tokenType) {
        TSPHPAst ast = new TSPHPAst(new CommonToken(tokenType));
        ast.setParent(new TSPHPAst(new CommonToken(TokenTypes.NAMESPACE_BODY)));
        return ast;
    }

    protected ITSPHPAst createVariable() {
        ITSPHPAst variable = createAst(TokenTypes.VariableId);
        variable.setSymbol(mock(IVariableSymbol.class));
        return variable;
    }
}
