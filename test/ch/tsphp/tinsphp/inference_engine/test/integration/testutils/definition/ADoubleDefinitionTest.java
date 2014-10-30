/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class ADoubleDefinitionTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils.definition;

import ch.tsphp.common.ILowerCaseStringMap;
import ch.tsphp.common.IScope;
import ch.tsphp.common.symbols.ISymbol;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.inference_engine.test.integration.testutils.ScopeTestStruct;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Ignore
public abstract class ADoubleDefinitionTest extends ADefinitionTest
{

    protected ScopeTestStruct[] testStructs;
    protected String namespace;
    protected String identifier;
    protected int occurrence;

    public ADoubleDefinitionTest(String testString, String theNamespace, String theIdentifier, int howMany) {
        super(testString);
        namespace = theNamespace;
        identifier = theIdentifier;
        occurrence = howMany;
    }

    @Override
    protected void verifyDefinitions() {
        ILowerCaseStringMap<IGlobalNamespaceScope> globalNamespaces
                = definitionPhaseController.getGlobalNamespaceScopes();

        IScope globalNamespace = globalNamespaces.get(namespace);
        Assert.assertNotNull(errorMessagePrefix + " failed, global namespace " + namespace + " could not be found.",
                globalNamespace);

        Map<String, List<ISymbol>> symbols = globalNamespace.getSymbols();
        Assert.assertNotNull(errorMessagePrefix + " failed, symbols was null.", symbols);
        Assert.assertTrue(errorMessagePrefix + " failed. " + identifier + " not found.",
                symbols.containsKey(identifier));
        Assert.assertEquals(errorMessagePrefix + " failed. size was wrong", this.occurrence,
                symbols.get(identifier).size());
    }

    protected static Collection<Object[]> getDifferentNamespaces(String statements, String identifiers,
            int occurrence) {
        return Arrays.asList(new Object[][]{
                {statements, "\\", identifiers, occurrence},
                {"namespace b;" + statements, "\\b\\", identifiers, occurrence},
                {"namespace b\\c;" + statements, "\\b\\c\\", identifiers, occurrence},
                {"namespace{" + statements + "}", "\\", identifiers, occurrence},
                {"namespace b{" + statements + "}", "\\b\\", identifiers, occurrence},
                {"namespace b\\c\\e\\R{" + statements + "}", "\\b\\c\\e\\R\\", identifiers, occurrence},
                {"namespace{" + statements + "} namespace{" + statements + "}", "\\", identifiers, occurrence * 2},
                {
                        "namespace b{" + statements + "} namespace b{" + statements + "}",
                        "\\b\\", identifiers, occurrence * 2
                },
                {
                        "namespace c{" + statements + "} namespace a{" + statements + "} "
                                + "namespace b{" + statements + "}",
                        "\\c\\", identifiers, occurrence
                },
                {
                        "namespace c{" + statements + "} namespace a{" + statements + "} "
                                + "namespace c{" + statements + "}",
                        "\\c\\", identifiers, 2 * occurrence
                },
                {
                        "namespace{" + statements + "} namespace {" + statements + "} namespace c{" + statements + "}",
                        "\\", identifiers, 2 * occurrence
                }
        });
    }
}
