/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.IConstraint;
import ch.tsphp.common.symbols.ITypeSymbol;

import java.util.List;
import java.util.Map;

public class IntersectionConstraint implements IConstraint
{
    private List<RefConstraint> variables;

    private List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> overloads;

    public IntersectionConstraint(List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> theOverloads) {
        overloads = theOverloads;
    }

    public List<Map.Entry<List<RefTypeConstraint>, ITypeSymbol>> getOverloads() {
        return overloads;
    }
}
