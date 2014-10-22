/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class TypeSystem from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine;

import ch.tsphp.common.IAstHelper;
import ch.tsphp.tinsphp.inference_engine.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.inference_engine.symbols.ISymbolFactory;

public class TypeSystem implements ITypeSystem
{
    private final ISymbolFactory symbolFactory;
    private final IAstHelper astHelper;
    private final IGlobalNamespaceScope globalDefaultNamespace;

    public TypeSystem(ISymbolFactory theSymbolFactory, IAstHelper theAstHelper,
            IGlobalNamespaceScope theGlobalDefaultNamespace) {

        symbolFactory = theSymbolFactory;
        astHelper = theAstHelper;
        globalDefaultNamespace = theGlobalDefaultNamespace;

    }
}



