package org.keixe.android.hotfix.internal;

import org.keixe.android.hotfix.util.MultiKeyHashMap;
import org.keixe.android.hotfix.util.MultiKeyMap;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import androidx.annotation.Keep;

@Keep
final class SignatureStore {

    private SignatureStore() {
        throw new AssertionError();
    }

    private static final ReentrantReadWriteLock sReadWriteLock = new ReentrantReadWriteLock();
    private static final MultiKeyMap<Object, WeakReference<String>> sCache = new MultiKeyHashMap<>();

    static String methodGet(
            Class type,
            String name,
            Class[] pramsTypes) {
        Object[] keys = new Object[pramsTypes.length + 2];
        keys[0] = type;
        keys[1] = name;
        System.arraycopy(pramsTypes, 0, keys, 2, pramsTypes.length);
        String result = null;
        sReadWriteLock.readLock().lock();
        WeakReference<String> reference = sCache.get(keys);
        if (reference != null) {
            result = reference.get();
        }
        if (result == null) {
            sReadWriteLock.writeLock().lock();
            reference = sCache.get(keys);
            if (reference != null) {
                result = reference.get();
            }
            if (result == null) {
                result = methodGet(type.getName(), name, pramsTypes);
                reference = new WeakReference<>(result);
                sCache.put(keys, reference);
            }
            sReadWriteLock.writeLock().unlock();
        }
        sReadWriteLock.readLock().unlock();
        return result;
    }

    static String methodGet(
            String typeName,
            String name,
            String[] pramsTypeNames) {
        return methodGet(typeName, name, (Object[]) pramsTypeNames);
    }

    private static String methodGet(
            String typeName,
            String name,
            Object[] pramsTypeNames) {
        StringBuilder builder = new StringBuilder(
                typeName.length()
                        + name.length()
                        + 16 * pramsTypeNames.length)
                .append(typeName)
                .append("#")
                .append(name)
                .append('(');
        if (pramsTypeNames instanceof String[]) {
            if (pramsTypeNames.length >= 1) {
                builder.append(pramsTypeNames[0]);
                for (int i = 1; i < pramsTypeNames.length; i++) {
                    builder.append(',')
                            .append(pramsTypeNames[i]);
                }
            }
        } else if (pramsTypeNames instanceof Class[]) {
            if (pramsTypeNames.length >= 1) {
                builder.append(((Class) pramsTypeNames[0]).getName());
                for (int i = 1; i < pramsTypeNames.length; i++) {
                    builder.append(',')
                            .append(((Class) pramsTypeNames[i]).getName());
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
        builder.append(')');
        return builder.toString();
    }

    static String fieldGet(
            Class type,
            String name) {
        return fieldGet(type.getName(), name);
    }

    static String fieldGet(
            String typeName,
            String name) {
        return typeName + '@' + name;
    }
}
