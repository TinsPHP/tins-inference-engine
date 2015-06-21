/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;

import java.util.Deque;
import java.util.List;
import java.util.Map;

public class WorklistDto
{

    public Deque<WorklistDto> workDeque;
    public IConstraintCollection constraintCollection;
    public int pointer;
    public boolean isSolvingMethod;
    public IOverloadBindings overloadBindings;
    public ConvertibleAnalysisDto convertibleAnalysisDto;

    public List<Integer> unsolvedConstraints;
    public boolean isInIterativeMode;
    public boolean isSolvingDependency;
    public boolean isInSoftTypingMode;
    public Map<String, List<String>> param2LowerParams;

    public WorklistDto(
            Deque<WorklistDto> theWorkDeque,
            IConstraintCollection theConstraintCollection,
            int thePointer,
            boolean isItSolvingMethod,
            IOverloadBindings theOverloadBindings) {
        workDeque = theWorkDeque;
        constraintCollection = theConstraintCollection;
        pointer = thePointer;
        isSolvingMethod = isItSolvingMethod;
        overloadBindings = theOverloadBindings;
        convertibleAnalysisDto = new ConvertibleAnalysisDto();
    }

    public WorklistDto(WorklistDto dto, int newPointer, IOverloadBindings theOverloadBindings) {
        workDeque = dto.workDeque;
        constraintCollection = dto.constraintCollection;
        isSolvingMethod = dto.isSolvingMethod;
        pointer = newPointer;
        overloadBindings = theOverloadBindings;

        unsolvedConstraints = dto.unsolvedConstraints;
        isInIterativeMode = dto.isInIterativeMode;
        isSolvingDependency = dto.isSolvingDependency;
        isInSoftTypingMode = dto.isInSoftTypingMode;
        convertibleAnalysisDto = dto.convertibleAnalysisDto;
    }
}
