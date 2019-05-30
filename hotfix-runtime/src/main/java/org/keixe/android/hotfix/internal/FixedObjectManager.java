package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.ConstructorSignature;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import androidx.annotation.AnyThread;

final class FixedObjectManager {

    /**
     * 保证原子性
     */
    private static final AtomicReferenceFieldUpdater<FixedObjectManager, Patch>
            sPatchUpdater = AtomicReferenceFieldUpdater
            .newUpdater(FixedObjectManager.class, Patch.class, "mPatch");
    private volatile Patch mPatch;

    private final ReentrantReadWriteLock mFixedObjectCacheLock = new ReentrantReadWriteLock();

    /**
     * 被修复的字段随{@link Patch}生命周期存在
     * 使用{@link ConcurrentHashMap}保存字段映射
     * 防止在JDK<1.8时多线程的情况下{@link HashMap}出现死循环的问题
     * 但是这样也并没有保证可见性和原子性,符合Java的默认行为
     * 字段采用宽松类型约束,不保存类型信息,编译时做检查,运行时不做检查
     */
    private final WeakHashMap<Object, FixedObjectCache> mFixedObjectCache = new WeakHashMap<>();

    @AnyThread
    Object receiveGet(ProceedingJoinPoint joinPoint) throws Throwable {
        FixedObjectCache cache = getObjectFieldCache(joinPoint.getTarget());
        if (cache != null) {
            return cache.receiveGet(joinPoint);
        }
        return joinPoint.proceed();
    }

    @AnyThread
    void receiveSet(ProceedingJoinPoint joinPoint) throws Throwable {
        FixedObjectCache cache = getObjectFieldCache(joinPoint.getTarget());
        if (cache != null) {
            cache.receiveSet(joinPoint);
            return;
        }
        joinPoint.proceed();
    }

    @AnyThread
    Object receiveInvoke(ProceedingJoinPoint joinPoint) throws Throwable {
        Patch patch = mPatch;
        Signature signature = joinPoint.getSignature();
        FixedObjectCache cache = null;
        if (patch != null && signature instanceof ConstructorSignature
                && patch.isRequestCacheType(signature.getDeclaringType())) {
            cache = markRequestCacheObject(joinPoint.getTarget(), patch);
        }
        if (cache == null) {
            cache = getObjectFieldCache(joinPoint.getTarget());
        }
        if (cache != null) {
            return cache.receiveInvoke(joinPoint);
        }
        return joinPoint.proceed();
    }

    /**
     * 因为读是乐观的,总是读,但是不经常写,故使用读写锁提升效率
     * 这里读写分离
     */
    private FixedObjectCache markRequestCacheObject(Object object, Patch patch) {
        mFixedObjectCacheLock.writeLock().lock();
        FixedObjectCache cache = new FixedObjectCache(patch);
        mFixedObjectCache.put(object, cache);
        mFixedObjectCacheLock.writeLock().unlock();
        return cache;
    }

    FixedObjectCache getObjectFieldCache(Object target) {
        mFixedObjectCacheLock.readLock().lock();
        FixedObjectCache cache = mFixedObjectCache.get(target);
        mFixedObjectCacheLock.readLock().unlock();
        return cache;
    }
}
