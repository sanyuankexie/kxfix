package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;

import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.AnyThread;

final class FixedObjectCache {

    private final Patch mAttachPatch;
    private final ConcurrentHashMap<Integer, Object> mFieldCache;

    FixedObjectCache(Patch patch) {
        mAttachPatch = patch;
        mFieldCache = new ConcurrentHashMap<>();
    }

    //-------------------------进入可执行体的外部接口---------------------------

    /**
     * 传入切点并检查补丁是否存在
     * 若存在则修复该函数
     * 否则走正常逻辑
     */
    @AnyThread
    Object receiveGet(ProceedingJoinPoint joinPoint) throws Throwable {
        if (mAttachPatch != null) {
            Integer id = mAttachPatch.mappingToId(joinPoint);
            if (id != null) {
                return mAttachPatch.getWithId(id, joinPoint.getTarget());
            }
        }
        return joinPoint.proceed();
    }

    Object onGet(int id) {
        return mFieldCache.get(id);
    }

    void onSet(int id, Object newValue) {
        mFieldCache.put(id, newValue);
    }

    @AnyThread
    void receiveSet(ProceedingJoinPoint joinPoint) throws Throwable {
        if (mAttachPatch != null) {
            Integer id = mAttachPatch.mappingToId(joinPoint);
            if (id != null) {
                mAttachPatch.setWithId(id, joinPoint.getTarget(), joinPoint.getArgs()[0]);
                return;
            }
        }
        joinPoint.proceed();
    }

    @AnyThread
    Object receiveInvoke(ProceedingJoinPoint joinPoint) throws Throwable {
        if (mAttachPatch != null) {
            Integer id = mAttachPatch.mappingToId(joinPoint);
            if (id != null) {
                return mAttachPatch.invokeWithId(id, joinPoint.getTarget(), joinPoint.getArgs());
            }
        }
        return joinPoint.proceed();
    }
}
