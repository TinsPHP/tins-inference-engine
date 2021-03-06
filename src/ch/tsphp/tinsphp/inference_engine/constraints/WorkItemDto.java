/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public class WorkItemDto
{

    public Deque<WorkItemDto> workDeque;
    public IConstraintCollection constraintCollection;
    public int pointer;
    public boolean isSolvingMethod;
    public IBindingCollection bindingCollection;
    public List<Integer> dependentConstraints;

    public Map<Integer, Map<String, ITypeVariableReference>> helperVariableMapping;
    public boolean isInIterativeMode;
    public boolean isSolvingDependency;
    public boolean isInSoftTypingMode;
    public ConvertibleAnalysisDto convertibleAnalysisDto;
    public Map<String, List<String>> param2LowerParams;
    public boolean hasChanged;

    public WorkItemDto(
            Deque<WorkItemDto> theWorkDeque,
            IConstraintCollection theConstraintCollection,
            int thePointer,
            boolean isItSolvingMethod,
            IBindingCollection theBindingCollection) {
        workDeque = theWorkDeque;
        constraintCollection = theConstraintCollection;
        pointer = thePointer;
        isSolvingMethod = isItSolvingMethod;
        bindingCollection = theBindingCollection;
        convertibleAnalysisDto = new ConvertibleAnalysisDto();
    }

    public WorkItemDto(
            WorkItemDto dto,
            int newPointer,
            IBindingCollection theBindingCollection,
            Map<Integer, Map<String, ITypeVariableReference>> theHelperVariableMapping,
            boolean hasItChanged) {
        workDeque = dto.workDeque;
        constraintCollection = dto.constraintCollection;
        isSolvingMethod = dto.isSolvingMethod;
        pointer = newPointer;
        bindingCollection = theBindingCollection;
        helperVariableMapping = theHelperVariableMapping;
        hasChanged = hasItChanged;
        if (dto.dependentConstraints != null) {
            dependentConstraints = new ArrayList<>(dto.dependentConstraints);
        }

        isInIterativeMode = dto.isInIterativeMode;
        isSolvingDependency = dto.isSolvingDependency;
        isInSoftTypingMode = dto.isInSoftTypingMode;
        convertibleAnalysisDto = dto.convertibleAnalysisDto;
    }
}
