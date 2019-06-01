package org.keixe.android.hotfix.internal;


import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

@Keep
final class SignatureStore {

    private SignatureStore() {
        throw new AssertionError();
    }

    private static final int MAX_SIZE = 128 * 1024;

    private static int sCurrentSize;

    private static final class Keys implements Cloneable {

        private Object[] mArray = new Object[3];

        void recycle() {
            Arrays.fill(mArray, null);
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

        @Override
        public Keys clone() {
            try {
                Keys keys = (Keys) super.clone();
                keys.mArray = mArray.clone();
                return keys;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    private static final ThreadLocal<Keys> sKeysThreadCache
            = new ThreadLocal<Keys>() {
        @Override
        protected Keys initialValue() {
            return new Keys();
        }
    };

    private static final ReentrantReadWriteLock sReadWriteLock = new ReentrantReadWriteLock();

    private static final LinkedHashMap<Keys, String> sLruCache
            = new LinkedHashMap<>(0, 0.75f, true);

    static String methodGet(
            Class type,
            String name,
            Class[] pramsTypes) {
        String result;
        Keys keys = of(type, name, pramsTypes);
        result = get(keys);
        if (result == null) {
            sReadWriteLock.writeLock().lock();
            result = get(keys);
            if (result == null) {
                result = methodGet(type.getName(), name, pramsTypes);
                put(keys, result);
            }
            sReadWriteLock.writeLock().unlock();
        }
        keys.recycle();
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
        Keys keys = of(type, name);
        result = get(keys);
        if (result == null) {
            sReadWriteLock.writeLock().lock();
            result = get(keys);
            if (result == null) {
                result = fieldGet(type.getName(), name);
                put(keys, result);
            }
            sReadWriteLock.writeLock().unlock();
        }
        keys.recycle();
        return result;
    }

    static String fieldGet(
            String typeName,
            String name) {
        return typeName + '@' + name;
    }

    private static Keys of(Class type, String name, Class[] pramTypes) {
        Keys keys = of(type, name);
        keys.mArray[2] = pramTypes;
        return keys;
    }

    private static Keys of(Class type, String name) {
        Keys keys = sKeysThreadCache.get();
        assert keys != null;
        keys.mArray[0] = type;
        keys.mArray[1] = name;
        return keys;
    }

    private static void put(Keys key, String value) {
        key = key.clone();
        sReadWriteLock.writeLock().lock();
        sCurrentSize += sizeOf(value);
        sLruCache.put(key, value);
        sReadWriteLock.writeLock().unlock();
        trimToSize();
    }

    private static String get(Keys key) {
        String mapValue;
        sReadWriteLock.readLock().lock();
        mapValue = sLruCache.get(key);
        sReadWriteLock.readLock().unlock();
        return mapValue;
    }

    private static void trimToSize() {
        while (true) {
            Keys key;
            String value;
            sReadWriteLock.writeLock().lock();
            if (sCurrentSize <= MAX_SIZE) {
                break;
            }
            // BEGIN LAYOUTLIB CHANGE
            // get the last item in the linked list.
            // This is not efficient, the goal here is to minimize the changes
            // compared to the platform version.
            Map.Entry<Keys, String> toEvict = null;
            for (Map.Entry<Keys, String> entry : sLruCache.entrySet()) {
                toEvict = entry;
            }
            // END LAYOUTLIB CHANGE
            if (toEvict == null) {
                break;
            }
            key = toEvict.getKey();
            value = toEvict.getValue();
            sLruCache.remove(key);
            sCurrentSize -= sizeOf(value);
            sReadWriteLock.writeLock().unlock();
        }
    }

    private static int sizeOf(String value) {
        return Character.SIZE / Byte.SIZE * value.length();
    }
}