/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

/**
 * Responsible to solve constraints which point to a dependency which was previously unsolved.
 */
public interface IDependencyConstraintSolver
{

    void setDependencies(
            IConstraintSolver constraintSolver,
            IConstraintSolverHelper constraintSolverHelper,
            ISoftTypingConstraintSolver softTypingConstraintSolver);

    void solveDependency(Pair<WorkItemDto, Integer> pair);

}

