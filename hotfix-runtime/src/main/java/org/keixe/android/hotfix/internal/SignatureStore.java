package org.keixe.android.hotfix.internal;


import android.util.LruCache;

import java.util.Arrays;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

@Keep
final class SignatureStore {

    private SignatureStore() {
        throw new AssertionError();
    }

    private static final class Keys {

        private final Object[] mArray;

        static Keys of(Object... keys) {
            return new Keys(keys);
        }

        private Keys(Object[] keys) {
            this.mArray = keys;
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(mArray);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Keys) {
                return Arrays.deepEquals(this.mArray, ((Keys) obj).mArray);
            }
            return false;
        }
    }

    private static final LruCache<Keys, String> sCache
            = new LruCache<Keys, String>(1024) {
        @Override
        protected int sizeOf(Keys key, String value) {
            return Character.SIZE / Byte.SIZE * value.length();
        }
    };

    static String methodGet(
            Class type,
            String name,
            Class[] pramsTypes) {
        String result;
        Keys keys = Keys.of(type, name, pramsTypes);
        result = sCache.get(keys);
        if (result == null) {
            synchronized (sCache) {
                result = sCache.get(keys);
                if (result == null) {
                    result = methodGet(type.getName(), name, pramsTypes);
                    sCache.put(keys, result);
                }
            }
        }
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
        String result;
        Keys keys = Keys.of(type, name);
        result = sCache.get(keys);
        if (result == null) {
            synchronized (sCache) {
                result = sCache.get(keys);
                if (result == null) {
                    result = fieldGet(type.getName(), name);
                    sCache.put(keys, result);
                }
            }
        }
        return result;
    }

    static String fieldGet(
            String typeName,
            String name) {
        return typeName + '@' + name;
    }
}