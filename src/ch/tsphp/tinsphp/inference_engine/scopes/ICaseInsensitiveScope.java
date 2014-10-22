/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ICaseInsensitiveScope from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.scopes;

import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.ISymbol;

/**
 * Represents a scope which has case insensitive definition rules.
 * <p/>
 * That means, symbols defined in this scope are not case sensitive when it comes down to check whether a different
 * symbol with the same identifier already exists.
 * <p/>
 * As an example, foo, Foo, fOo, FOO are all the same identifier in such a scope.
 */
public interface ICaseInsensitiveScope extends IScope
{

    boolean doubleDefinitionCheckCaseInsensitive(ISymbol symbol);
}
