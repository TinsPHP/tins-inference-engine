/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.exceptions.TypeCheckerException;

import java.util.List;

/**
 * Represents an exception which occurs when a call is made and the actual parameters match to multiple signatures.
 */
public class AmbiguousCallException extends TypeCheckerException
{

    private final List<OverloadDto> ambiguousOverloads;

    public AmbiguousCallException(List<OverloadDto> theAmbiguousMethodDtos) {
        ambiguousOverloads = theAmbiguousMethodDtos;
    }

    public List<OverloadDto> getAmbiguousOverloads() {
        return ambiguousOverloads;
    }
}
