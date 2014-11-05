/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class DefinitionErrorDto from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.error;

/**
 * Represents the meta-data of a definition error.
 * <p/>
 * One example of a definition error is double variable declarations.
 */
public class DefinitionErrorDto extends ReferenceErrorDto
{

    public String identifierNewDefinition;
    public int lineNewDefinition;
    public int positionNewDefinition;

    public DefinitionErrorDto(
            String theExistingIdentifier, int theLineExistingDefinition, int thePositionExistingDefinition,
            String theNewIdentifier, int theLineNewDefinition, int thePositionNewDefinition) {
        super(theNewIdentifier, theLineExistingDefinition, thePositionExistingDefinition);
        identifier = theExistingIdentifier;
        line = theLineExistingDefinition;
        position = thePositionExistingDefinition;
        identifierNewDefinition = theNewIdentifier;
        lineNewDefinition = theLineNewDefinition;
        positionNewDefinition = thePositionNewDefinition;
    }

    @Override
    public String toString() {
        return identifier + " " + line + "|" + position + " "
                + lineNewDefinition + "|" + positionNewDefinition;
    }
}
