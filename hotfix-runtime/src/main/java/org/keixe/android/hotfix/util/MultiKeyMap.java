package org.keixe.android.hotfix.util;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MultiKeyMap<K,V> implements Map<K[],V> {

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

    static final class EntryValue<K, V> implements Entry<K[], V> {

        final K[] mArray;

        V mValue;

        EntryValue(K[] array, V value) {
            this.mArray = array;
            this.mValue = value;
        }

        @Override
        public K[] getKey() {
            return mArray;
        }

        @Override
        public V getValue() {
            return mValue;
        }

        @Override
        public V setValue(V value) {
            return mValue = value;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof MultiKeyMap.EntryValue) {
                return Arrays.deepEquals(mArray, ((EntryValue) o).mArray)
                        && Objects.equals(mValue, ((EntryValue) o).mValue);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(mArray) ^ Objects.hashCode(mValue);
        }
    }

    final class KeySetWrapper extends AbstractSet<K[]> {
        public final int size() {
            return mMap.size();
        }

        public final void clear() {
            mMap.clear();
        }

        public final Iterator<K[]> iterator() {
            return new KeyIteratorWrapper<>(mMap.keySet().iterator());
        }

        public final boolean contains(Object o) {
            return containsKey(o);
        }

        public final boolean remove(Object key) {
            return mMap.keySet().remove(MultiKeyMap.<K>keyOf(key));
        }
    }

    final class EntrySetWrapper extends AbstractSet<Entry<K[], V>> {
        public final int size() {
            return mMap.size();
        }

        public final void clear() {
            mMap.clear();
        }

        public final Iterator<Map.Entry<K[], V>> iterator() {
            return new EntryIteratorWrapper<>(mMap.entrySet().iterator());
        }

        public final boolean contains(Object o) {
            return mMap.entrySet().contains(keyOf(o));
        }

        public final boolean remove(Object o) {
            return mMap.entrySet().remove(keyOf(o));
        }
    }

    final class ValuesWrapper extends AbstractCollection<V> {

        public final int size() {
            return mMap.size();
        }

        public final void clear() {
            mMap.clear();
        }

        public final Iterator<V> iterator() {
            return new ValueIteratorWrapper<>(mMap.values().iterator());
        }

        public final boolean contains(Object o) {
            return containsValue(o);
        }
    }

    static final class KeyIteratorWrapper<K> implements Iterator<K[]> {

        final Iterator<DeepEqualsKeys<K>> mInner;

        KeyIteratorWrapper(Iterator<DeepEqualsKeys<K>> inner) {
            mInner = inner;
        }

        @Override
        public boolean hasNext() {
            return mInner.hasNext();
        }

        @Override
        public K[] next() {
            return mInner.next().mArray;
        }
    }

    static final class EntryIteratorWrapper<K, V> implements Iterator<Entry<K[], V>> {
        private final Iterator<Entry<DeepEqualsKeys<K>, EntryValue<K, V>>> mInner;

        public final boolean hasNext() {
            return mInner.hasNext();
        }

        EntryIteratorWrapper(Iterator<Entry<DeepEqualsKeys<K>, EntryValue<K, V>>> inner) {
            this.mInner = inner;
        }

        @Override
        public Entry<K[], V> next() {
            return mInner.next().getValue();
        }
    }

    static final class ValueIteratorWrapper<K, V> implements Iterator<V> {

        final Iterator<EntryValue<K, V>> mInner;

        ValueIteratorWrapper(Iterator<EntryValue<K, V>> mInner) {
            this.mInner = mInner;
        }


        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public V next() {
            return mInner.next().getValue();
        }
    }

    private HashMap<DeepEqualsKeys<K>, EntryValue<K, V>> mMap = new HashMap<>();

    private KeySetWrapper mKeySet;

    private ValuesWrapper mValues;

    private EntrySetWrapper mEntrySet;

    @Override
    public V get(Object key) {
        EntryValue<K, V> entry;
        return ((entry = mMap.get(MultiKeyMap.<K>keyOf(key))) == null) ? null : entry.getValue();
    }

    @SuppressWarnings("unchecked")
    private static <K, V> EntryValue<K, V> valueOf(Object key, Object value) {
        if (key != null && !(key instanceof Object[])) {
            throw new IllegalArgumentException();
        }
        return value == null ? null : new EntryValue<>((K[]) key, (V) value);
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

    @Override
    public V put(K[] key, V value) {
        EntryValue<K, V> entry;
        return ((entry = mMap.put(MultiKeyMap.<K>keyOf(key), MultiKeyMap.<K, V>valueOf(key, value))) == null)
                ? null : entry.getValue();
    }

    @Nullable
    @Override
    public V remove(@Nullable Object key) {
        EntryValue<K, V> entry;
        return ((entry = mMap.remove(MultiKeyMap.<K>keyOf(key))) == null) ? null : entry.getValue();
    }

    @Override
    public void putAll(@NonNull Map<? extends K[], ? extends V> m) {
        for (Entry<? extends K[], ? extends V> entry : m.entrySet()) {
            mMap.put(MultiKeyMap.<K>keyOf(entry.getKey()),
                    MultiKeyMap.<K, V>valueOf(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public int size() {
        return mMap.size();
    }

    @Override
    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return mMap.containsKey(MultiKeyMap.<K>keyOf(key));
    }

    @Override
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

    @Override
    public Set<K[]> keySet() {
        if (mKeySet == null) {
            mKeySet = new KeySetWrapper();
        }
        return mKeySet;
    }

    @Override
    public Collection<V> values() {
        if (mValues == null) {
            mValues = new ValuesWrapper();
        }
        return mValues;
    }

    @Override
    public void clear() {
        mMap.clear();
    }

    @Override
    public Set<Entry<K[], V>> entrySet() {
        if (mEntrySet == null) {
            mEntrySet = new EntrySetWrapper();
        }
        return mEntrySet;
    }
}
