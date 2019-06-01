package org.keixe.android.hotfix;

import org.junit.Test;
import org.keixe.android.hotfix.util.MultiKeyHashMap;
import org.keixe.android.hotfix.util.MultiKeyMap;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

public class MultiKeyMapUnitTest {
    private Object[] keys = {"adas", "adsas", 1, new Object[]{1, 1.1, System.in, System.out}};
    private Object value = new Object();

    @Test
    public void putAndRemove() {
        MultiKeyMap<Object, Object> multiKeyMap = new MultiKeyHashMap<>();
        multiKeyMap.put(keys, value);
        assertEquals(multiKeyMap.remove(keys), value);
        assertFalse(multiKeyMap.containsValue(keys));
    }

    @Test
    public void putAndGet() {
        MultiKeyMap<Object, Object> multiKeyMap = new MultiKeyHashMap<>();
        multiKeyMap.put(keys, value);
        assertEquals(multiKeyMap.get(keys), value);
    }
}