package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;

import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

final class FixedObjectCache {

    @NonNull
    private final Patch mAttachPatch;
    private final ConcurrentHashMap<Integer, Object> mFieldCache;

    FixedObjectCache(@NonNull Patch patch) {
        mAttachPatch = patch;
        mFieldCache = new ConcurrentHashMap<>();
    }

    //-------------------------进入可执行体的外部接口---------------------------

    @AnyThread
    Object receiveGet(ProceedingJoinPoint joinPoint) throws Throwable {
        Integer id = mAttachPatch.mappingToId(joinPoint);
        if (id != null) {
            return onGet(id);
        }
        return joinPoint.proceed();
    }

    @AnyThread
    void receiveSet(ProceedingJoinPoint joinPoint) throws Throwable {
        Integer id = mAttachPatch.mappingToId(joinPoint);
        if (id != null) {
            onSet(id, joinPoint.getArgs()[0]);
            return;
        }
        joinPoint.proceed();
    }

    @AnyThread
    Object receiveInvoke(ProceedingJoinPoint joinPoint) throws Throwable {
        Integer id = mAttachPatch.mappingToId(joinPoint);
        if (id != null) {
            return mAttachPatch.invokeWithId(id, joinPoint.getTarget(), joinPoint.getArgs());
        }
        return joinPoint.proceed();
    }

    Object onGet(int id) {
        return mFieldCache.get(id);
    }

    void onSet(int id, Object newValue) {
        mFieldCache.put(id, newValue);
    }
}
