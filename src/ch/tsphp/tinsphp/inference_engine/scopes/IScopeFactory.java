/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class IScopeFactory from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.scopes;

public interface IScopeFactory
{

    IGlobalNamespaceScope createGlobalNamespaceScope(String name);

    INamespaceScope createNamespaceScope(String name, IGlobalNamespaceScope currentScope);

    //TODO rstoll TINS-162 definition phase - scopes
    //IConditionalScope createConditionalScope(IScope currentScope);
}
