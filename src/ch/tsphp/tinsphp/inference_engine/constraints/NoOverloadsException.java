/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;

import java.util.Deque;

public class NoOverloadsException extends RuntimeException
{
    private Deque<WorklistDto> workDeque;
    private IMethodSymbol methodSymbol;
    private String dependency;

    public NoOverloadsException(
            Deque<WorklistDto> theWorkDeque, IMethodSymbol theMethodSymbol, String theDependency) {
        workDeque = theWorkDeque;
        methodSymbol = theMethodSymbol;
        dependency = theDependency;
    }

    public Deque<WorklistDto> getWorklist() {
        return workDeque;
    }

    public IMethodSymbol getMethodSymbol() {
        return methodSymbol;
    }

    public String getDependency() {
        return dependency;
    }
}
