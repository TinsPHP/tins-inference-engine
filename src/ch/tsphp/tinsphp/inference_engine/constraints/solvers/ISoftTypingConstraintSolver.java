/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.tinsphp.common.inference.constraints.IConstraintCollection;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

/**
 * Responsible to solve constraints of a method which cannot be solved regularly and requires a fall back to soft
 * typing.
 */
public interface ISoftTypingConstraintSolver
{
    void solveConstraints(IConstraintCollection constraintCollection, WorkItemDto workItemDto);
}
