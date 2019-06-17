package org.kexie.android.hotfix.internal;

import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class OverloadObjectTable {
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final WeakHashMap<Object, OverloadObject> overloadObjects = new WeakHashMap<>();
    private final Class overloadType;

    OverloadObjectTable(Class overloadType) {
        this.overloadType = overloadType;
    }

    Class getOverloadType() {
        return overloadType;
    }

    private OverloadObject newOverloadObject(Object o) {
        try {
            OverloadObject overloadObject = (OverloadObject) overloadType.newInstance();
            overloadObject.target = o;
            return overloadObject;
        } catch (IllegalAccessException
                | InstantiationException e) {
            throw new AssertionError(e);
        }
    }

    OverloadObject lockOverloadObject(Object o) {
        readWriteLock.readLock().lock();
        OverloadObject overloadObject = overloadObjects.get(o);
        readWriteLock.readLock().unlock();
        if (overloadObject == null) {
            readWriteLock.writeLock().lock();
            overloadObject = overloadObjects.get(o);
            if (overloadObject == null) {
                overloadObject = newOverloadObject(o);
                overloadObjects.put(o, overloadObject);
            }
            readWriteLock.writeLock().unlock();
        }
        return overloadObject;
    }
}
