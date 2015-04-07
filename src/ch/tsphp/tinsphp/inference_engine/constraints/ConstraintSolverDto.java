/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IBinding;

import java.util.Deque;

public class ConstraintSolverDto
{
    public int pointer;
    public IBinding binding;
    public Deque<IBinding> workList;
    public Deque<IBinding> solvedBindings;

    public ConstraintSolverDto(int thePointer, IBinding theBinding) {
        pointer = thePointer;
        binding = theBinding;
    }
}
