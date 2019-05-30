package org.keixe.android.hotfix.internal;

import android.util.LruCache;

import java.util.Arrays;
import java.util.List;

/**
 * @author Luke
 */
final class SignatureMaker {

    private static final LruCache<List<Object>, String> sCache = new LruCache<>(16);

    static String makeMethodSignature(Class type,
                                      String name,
                                      Class[] pramsTypes) {
        Object[] arr = new Object[pramsTypes.length];
        System.arraycopy(pramsTypes, 0, arr, 2, pramsTypes.length);
        arr[0] = type;
        arr[1] = name;
        String signature = sCache.get(Arrays.asList(arr));
        if (signature != null) {
            return signature;
        }
        return null;
    }

    static String makeFieldSignature(Class type,
                                     String name) {
        String signature = sCache.get(Arrays.<Object>asList(type, name));
        if (signature != null) {
            return signature;
        }
        return null;
    }
}
