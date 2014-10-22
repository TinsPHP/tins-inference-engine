/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/*
 * This class is based on the class MapHelperTest from the TSPHP project.
 * TSPHP is also published under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.utils;

import ch.tsphp.tinsphp.inference_engine.utils.MapHelper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MapHelperTest
{

    @Test
    public void test() {
        Map<String, List<String>> map = new HashMap<>();
        assertThat(map.size(), is(0));

        List<String> list = new ArrayList<>();
        list.add("v");
        MapHelper.addToListMap(map, "k", "v");
        assertThat(map.size(), is(1));
        assertThat(map.get("k"), is(list));

        list.add("v2");
        MapHelper.addToListMap(map, "k", "v2");
        assertThat(map.size(), is(1));
        assertThat(map.get("k"), is(list));

        List<String> list2 = new ArrayList<>();
        list2.add("a");
        MapHelper.addToListMap(map, "x", "a");
        assertThat(map.size(), is(2));
        assertThat(map.get("k"), is(list));
        assertThat(map.get("x"), is(list2));
    }
}
