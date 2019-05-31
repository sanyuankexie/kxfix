package org.keixe.android.hotfix.util;

import java.util.Arrays;
import java.util.HashMap;

import androidx.annotation.Nullable;

public final class MultiKeyMap<K,V> {

    static final class DeepEqualsKeys<T> {
        final T[] mArray;

        DeepEqualsKeys(T[] array) {
            this.mArray = array;
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(mArray);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof MultiKeyMap.DeepEqualsKeys && Arrays.deepEquals(mArray, ((DeepEqualsKeys) obj).mArray);
        }
    }

    private HashMap<DeepEqualsKeys<K>, V> mMap = new HashMap<>();

    public V get(Object key) {
        return mMap.get(MultiKeyMap.<K>keyOf(key));
    }

    @SuppressWarnings("unchecked")
    private static <K> DeepEqualsKeys<K> keyOf(Object key) {
        if (key == null) {
            return null;
        } else if (key instanceof Object[]) {
            return new DeepEqualsKeys<>((K[]) key);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public V put(K[] key, V value) {
        return mMap.put(MultiKeyMap.<K>keyOf(key), value);
    }

    @Nullable
    public V remove(@Nullable Object key) {
        return mMap.remove(MultiKeyMap.<K>keyOf(key));
    }

    public int size() {
        return mMap.size();
    }

    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return mMap.containsKey(MultiKeyMap.<K>keyOf(key));
    }

    public boolean containsValue(Object value) {
        return mMap.containsValue(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof MultiKeyMap) {
            return this.mMap.equals(((MultiKeyMap) o).mMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mMap.hashCode();
    }

}
