/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.IConstraint;
import ch.tsphp.common.IScope;

public class RefConstraint implements IConstraint
{
    private ScopeVariableDto variable;

    public RefConstraint(IScope theRefScope, String theReferenceName) {
        variable = new ScopeVariableDto(theRefScope, theReferenceName);
    }

    public ScopeVariableDto getScopeVariableDto() {
        return variable;
    }
}
