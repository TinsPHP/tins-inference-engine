/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IBinding;

import java.util.Deque;

public class WorklistDto
{
    public Deque<WorklistDto> workDeque;
    public int pointer;
    public IBinding binding;

    public WorklistDto(Deque<WorklistDto> theWorkDeque, int thePointer, IBinding theBinding) {
        workDeque = theWorkDeque;
        pointer = thePointer;
        binding = theBinding;
    }
}
