package org.kexie.android.hotfix.internal;

import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class RedirectTable {
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final WeakHashMap<Object, RedirectTarget> overloadObjects = new WeakHashMap<>();
    private final Class overloadType;

    RedirectTable(Class type) {
        this.overloadType = type;
    }

    Class getType() {
        return overloadType;
    }

    private RedirectTarget newInstance(Object o) {
        try {
            RedirectTarget redirectTarget = (RedirectTarget) overloadType.newInstance();
            redirectTarget.target = o;
            return redirectTarget;
        } catch (IllegalAccessException
                | InstantiationException e) {
            throw new AssertionError(e);
        }
    }

    RedirectTarget getTarget(Object o) {
        readWriteLock.readLock().lock();
        RedirectTarget redirectTarget = overloadObjects.get(o);
        readWriteLock.readLock().unlock();
        if (redirectTarget == null) {
            readWriteLock.writeLock().lock();
            redirectTarget = overloadObjects.get(o);
            if (redirectTarget == null) {
                redirectTarget = newInstance(o);
                overloadObjects.put(o, redirectTarget);
            }
            readWriteLock.writeLock().unlock();
        }
        return redirectTarget;
    }
}
