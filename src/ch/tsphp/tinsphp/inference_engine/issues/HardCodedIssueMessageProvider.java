/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.issues;

import ch.tsphp.tinsphp.common.issues.AIssueMessageProvider;
import ch.tsphp.tinsphp.common.issues.DefinitionIssueDto;
import ch.tsphp.tinsphp.common.issues.IIssueMessageProvider;
import ch.tsphp.tinsphp.common.issues.ReferenceIssueDto;
import ch.tsphp.tinsphp.common.issues.WrongArgumentTypeIssueDto;

import java.util.HashMap;
import java.util.Map;

public class HardCodedIssueMessageProvider extends AIssueMessageProvider implements IIssueMessageProvider
{

    @Override
    protected Map<String, String> loadDefinitionIssueMessages() {
        Map<String, String> map = new HashMap<>();
        map.put("alreadyDefined", "Line %lineN%|%posN% - %idN% was already defined on line %line%|%pos% %id%");
        map.put("aliasForwardReference", "Line %lineN%|%posN% - alias %idN% is used before its use declaration. "
                        + "Corresponding use declaration is on line %line%|%pos%"
        );
        map.put("forwardReference", "Line %lineN%|%posN% - %idN% is used before its declaration. "
                        + "Corresponding declaration is on line %line%|%pos%"
        );
        map.put("variablePartiallyInitialised", "Line %lineN%|%posN% - variable %idN% was not initialised in all"
                + " branches up to this point and thus cannot be used.\n"
                + "Local variables have to be initialised before their first usage.");
        map.put("variableNotInitialised", "Line %lineN%|%posN% - variable %idN% was never initialised "
                + "and thus cannot be used.\n"
                + "Local variables have to be initialised before their first usage.");
        return map;
    }

    @Override
    protected Map<String, String> loadReferenceIssueMessages() {
        Map<String, String> map = new HashMap<>();
        map.put("notDefined", "Line %line%|%pos% - %id% was never defined.");
        map.put("unknownType", "Line %line%|%pos% - The type \"%id%\" could not be resolved.");
        map.put("partialReturnFromFunction",
                "Line %line%|%pos% - function %id% does not return/throw in all branches.");
        map.put("noReturnFromFunction", "Line %line%|%pos% - function %id% does not contain "
                + "one single return/throw statement.");
        return map;
    }

    @Override
    protected Map<String, String> loadWrongArgumentTypeIssueMessages() {
        Map<String, String> map = new HashMap<>();
        map.put("wrongFunctionCall", "Line %line%|%pos% - no applicable overload found for the "
                + "function %id%.\n"
                + "Given argument types: %args%\n"
                + "existing overloads: %overloads%");
        map.put("wrongOperatorUsage", "Line %line%|%pos% - usage of operator %id% is wrong.\n"
                + "It cannot be applied to the given arguments: %args%\n"
                + "existing overloads: %overloads%");

        return map;
    }

    @Override
    protected String getStandardDefinitionIssueMessage(String identifier, DefinitionIssueDto dto) {
        return "A definition issue occurred, corresponding issue message for \"" + identifier + "\" not defined. "
                + "Please report bug to http://tsphp.ch/tins/jira\n"
                + "However, the following information was gathered.\n"
                + "Line " + dto.line + "|" + dto.position + " - " + dto.identifier + " was already defined on line "
                + dto.lineNewDefinition + "|" + dto.positionNewDefinition + ".";
    }

    @Override
    protected String getStandardReferenceIssueMessage(String identifier, ReferenceIssueDto dto) {
        return "A reference issue occurred, corresponding error message for \"" + identifier + "\" is not defined. "
                + "Please report bug to http://tsphp.ch/tins/jira\n"
                + "However, the following information was gathered.\n"
                + "Line " + dto.line + "|" + dto.position + " - " + dto.identifier + " could not been resolved to its "
                + "corresponding reference.";
    }

    @Override
    protected String getStandardWrongArgumentTypeIssueMessage(String identifier, WrongArgumentTypeIssueDto dto) {
        return "A wrong argument type issue occurred, corresponding error message for \"" + identifier + "\" "
                + "is not defined. Please report bug to http://tsphp.ch/tins/jira\n"
                + "However, the following information was gathered.\n"
                + "Line " + dto.line + "|" + dto.position + " - usage of " + dto.identifier + " was wrong.\n"
                + "types actual parameters: " + getArguments(dto.actualParameterTypes) + "\n"
                + "existing overloads: " + getOverloadSignatures(dto.possibleOverloads);
    }
}
