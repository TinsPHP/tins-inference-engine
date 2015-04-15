/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;

import java.util.Deque;

public class WorklistDto
{
    public Deque<WorklistDto> workDeque;
    public int pointer;
    public IOverloadBindings overloadBindings;

    public WorklistDto(Deque<WorklistDto> theWorkDeque, int thePointer, IOverloadBindings theOverloadBindings) {
        workDeque = theWorkDeque;
        pointer = thePointer;
        overloadBindings = theOverloadBindings;
    }
}
