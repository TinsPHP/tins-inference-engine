/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.error;

import ch.tsphp.common.IErrorReporter;
import ch.tsphp.common.exceptions.DefinitionException;
import ch.tsphp.common.symbols.ISymbol;

public interface IInferenceErrorReporter extends IErrorReporter
{
    DefinitionException alreadyDefined(ISymbol existingSymbol, ISymbol newSymbol);
}
