/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class MethodModifierHelper from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.integration.testutils;

public class MethodModifierHelper
{

    public static String[] getAbstractVariations() {
        return new String[]{
                "abstract",
                "abstract protected",
                "abstract public",
                "protected abstract",
                "public abstract"
        };
    }

    public static String[] getConstructDestructVariations() {
        return new String[]{
                "",
                "private",
                "private final",
                "protected",
                "protected final",
                "public",
                "public final",
                "final",
                "final private",
                "final protected",
                "final public",
        };
    }

    public static String[] getVariations() {
        return new String[]{
                "",
                //
                "private",
                "private static",
                "private final",
                "private static final",
                "private final static",
                //
                "protected",
                "protected static",
                "protected final",
                "protected static final",
                "protected final static",
                //
                "public",
                "public static",
                "public final",
                "public static final",
                "public final static",
                //
                "static",
                "static private",
                "static private final",
                "static protected",
                "static protected final",
                "static public",
                "static public final",
                "static final",
                "static final private",
                "static final protected",
                "static final public",
                //
                "final",
                "final static",
                "final private",
                "final private static",
                "final protected",
                "final protected static",
                "final public",
                "final public static",
                "final static private",
                "final static protected",
                "final static public"
        };
    }


}
