/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;

import java.util.Map;

public class AggregateBindingDto
{
    public IConstraint constraint;
    public IFunctionType overload;
    public IOverloadBindings bindings;
    public WorklistDto worklistDto;
    public int iterateCount = 0;
    public Map<String, ITypeVariableReference> mapping;
    public IVariable bindingVariable;
    public String overloadVariableId;
    public boolean needToReIterate;
    public int implicitConversionCounter;
    public boolean hasNarrowedArguments;

    public AggregateBindingDto(
            IConstraint theConstraint,
            IFunctionType theOverload,
            IOverloadBindings theBindings,
            WorklistDto theWorkListDto) {
        constraint = theConstraint;
        overload = theOverload;
        bindings = theBindings;
        worklistDto = theWorkListDto;
    }
}