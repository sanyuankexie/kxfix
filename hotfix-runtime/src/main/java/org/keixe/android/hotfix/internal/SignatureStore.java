package org.keixe.android.hotfix.internal;


import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author Luke
 * @see android.util.LruCache
 */
@Keep
final class SignatureStore {

    private SignatureStore() {
        throw new AssertionError();
    }

    private static final class Keys implements Cloneable {

        private Object[] mArray = new Object[3];

        Class getType() {
            return (Class) mArray[0];
        }

        String getName() {
            return (String) mArray[1];
        }

        Class[] getPramTypes() {
            return (Class[]) mArray[2];
        }

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

    private static final class KeysCache extends ThreadLocal<Keys> {

        private static final KeysCache sThreadLocal = new KeysCache();

        private static Keys of(Class type, String name, Class[] pramTypes) {
            Keys keys = of(type, name);
            keys.mArray[2] = pramTypes;
            return keys;
        }

        private static Keys of(Class type, String name) {
            Keys keys = sThreadLocal.get();
            keys.mArray[0] = type;
            keys.mArray[1] = name;
            return keys;
        }

        @SuppressWarnings("ConstantConditions")
        @NonNull
        @Override
        public Keys get() {
            return super.get();
        }

        @Override
        protected Keys initialValue() {
            return new Keys();
        }
    }

    private static final class LruCache {

        private static final int MAX_SIZE = 128 * 1024;

        private final ReentrantReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

        private final LinkedHashMap<Keys, String> mTable
                = new LinkedHashMap<>(0, 0.75f, true);

        private static int mCurrentSize;

        String getOrCreate(Keys keys) {
            String mapValue;
            mReadWriteLock.readLock().lock();
            mapValue = mTable.get(keys);
            mReadWriteLock.readLock().unlock();
            if (mapValue == null) {
                boolean needTrimToSize = false;
                mReadWriteLock.writeLock().lock();
                mapValue = mTable.get(keys);
                if (mapValue == null) {
                    String typeName = keys.getType().getName();
                    String name = keys.getName();
                    Class[] pramsTypes = keys.getPramTypes();
                    mapValue = pramsTypes != null
                            ? methodCreate(typeName, name, pramsTypes)
                            : fieldCreate(typeName, name);
                    mTable.put(keys, mapValue);
                    needTrimToSize = true;
                }
                mReadWriteLock.writeLock().unlock();
                if (needTrimToSize) {
                    trimToSize();
                }
            }
            return mapValue;
        }

        private void trimToSize() {
            while (true) {
                Keys key;
                String value;
                mReadWriteLock.writeLock().lock();
                if (mCurrentSize <= MAX_SIZE) {
                    break;
                }
                // BEGIN LAYOUTLIB CHANGE
                // getOrCreate the last item in the linked list.
                // This is not efficient, the goal here is to minimize the changes
                // compared to the platform version.
                Map.Entry<Keys, String> toEvict = null;
                for (Map.Entry<Keys, String> entry : mTable.entrySet()) {
                    toEvict = entry;
                }
                // END LAYOUTLIB CHANGE
                if (toEvict == null) {
                    break;
                }
                key = toEvict.getKey();
                value = toEvict.getValue();
                mTable.remove(key);
                mCurrentSize -= sizeOf(value);
                mReadWriteLock.writeLock().unlock();
            }
        }

        private static int sizeOf(String value) {
            return Character.SIZE / Byte.SIZE * value.length();
        }

    }

    private static final LruCache sCache = new LruCache();

    static String methodGet(
            Class type,
            String name,
            Class[] pramsTypes) {
        Keys keys = KeysCache.of(type, name, pramsTypes);
        String result = sCache.getOrCreate(keys);
        keys.recycle();
        return result;
    }

    static String methodGet(
            String typeName,
            String name,
            String[] pramsTypeNames) {
        return methodCreate(typeName, name, pramsTypeNames);
    }

    static String fieldGet(
            Class type,
            String name) {
        Keys keys = KeysCache.of(type, name);
        String result = sCache.getOrCreate(keys);
        keys.recycle();
        return result;
    }

    static String fieldGet(
            String typeName,
            String name) {
        return fieldCreate(typeName, name);
    }

    private static String fieldCreate(
            String typeName,
            String name) {
        return typeName + '@' + name;
    }

    private static String methodCreate(
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
}