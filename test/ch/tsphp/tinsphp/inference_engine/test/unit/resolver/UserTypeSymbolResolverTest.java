/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.resolver;

import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.symbols.resolver.ISymbolResolverController;
import ch.tsphp.tinsphp.common.symbols.resolver.ITypeSymbolResolver;
import ch.tsphp.tinsphp.inference_engine.resolver.UserTypeSymbolResolver;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class UserTypeSymbolResolverTest
{

    @Test
    public void resolveTypeFor_NotSupportedAstType_DelegatesToNextInChain() {
        ITypeSymbolResolver nextTypeSymbolResolver = mock(ITypeSymbolResolver.class);
        ITSPHPAst ast = createAst(TokenTypes.VariableId);

        ITypeSymbolResolver typeSymbolResolver = createTypeSymbolResolver();
        typeSymbolResolver.setNextInChain(nextTypeSymbolResolver);
        typeSymbolResolver.resolveTypeFor(ast);

        verify(nextTypeSymbolResolver).resolveTypeFor(ast);
    }

    @Test
    public void resolveTypeFor_NotSupportedAstTypeAndLastInChain_ReturnsNull() {
        ITSPHPAst ast = createAst(TokenTypes.VariableId);

        ITypeSymbolResolver typeSymbolResolver = createTypeSymbolResolver();
        ITypeSymbol result = typeSymbolResolver.resolveTypeFor(ast);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void resolveTypeFor_TypeName_DelegatesToSymbolResolver() {
        ITSPHPAst ast = createAst(TokenTypes.TYPE_NAME);
        ISymbolResolverController symbolResolverController = mock(ISymbolResolverController.class);
        ITypeSymbol typeSymbol = mock(ITypeSymbol.class);
        when(symbolResolverController.resolveClassLikeIdentifier(ast)).thenReturn(typeSymbol);

        ITypeSymbolResolver typeSymbolResolver = createTypeSymbolResolver(symbolResolverController);
        ITypeSymbol result = typeSymbolResolver.resolveTypeFor(ast);

        verify(symbolResolverController).resolveClassLikeIdentifier(ast);
        assertThat(result, is(typeSymbol));
    }

    @Test
    public void resolveTypeFor_NonExistingTypeName_DelegatesToNextInChain() {
        ITypeSymbolResolver nextTypeSymbolResolver = mock(ITypeSymbolResolver.class);
        ITSPHPAst ast = createAst(TokenTypes.TYPE_NAME);

        ITypeSymbolResolver typeSymbolResolver = createTypeSymbolResolver();
        typeSymbolResolver.setNextInChain(nextTypeSymbolResolver);
        typeSymbolResolver.resolveTypeFor(ast);

        verify(nextTypeSymbolResolver).resolveTypeFor(ast);
    }

    @Test
    public void resolveTypeFor_NonExistingTypeNameAstTypeAndLastInChain_ReturnsNull() {
        ITSPHPAst ast = createAst(TokenTypes.TYPE_NAME);

        ITypeSymbolResolver typeSymbolResolver = createTypeSymbolResolver();
        ITypeSymbol result = typeSymbolResolver.resolveTypeFor(ast);

        assertThat(result, is(nullValue()));
    }

    private ITypeSymbolResolver createTypeSymbolResolver() {
        return createTypeSymbolResolver(mock(ISymbolResolverController.class));
    }

    protected ITypeSymbolResolver createTypeSymbolResolver(ISymbolResolverController theSymbolResolverController) {
        return new UserTypeSymbolResolver(theSymbolResolverController);
    }

    private ITSPHPAst createAst(int tokenType) {
        ITSPHPAst ast = mock(ITSPHPAst.class);
        when(ast.getType()).thenReturn(tokenType);
        return ast;
    }

}
