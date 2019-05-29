package org.keixe.android.hotfix.internal;

import android.content.Context;

import org.aspectj.lang.ProceedingJoinPoint;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

final class PatchManager {
    private static final AtomicBoolean sInit = new AtomicBoolean(false);
    private static PatchManager sInstance;

    static void init(Context context) {
        if (sInit.compareAndSet(false, true)) {

        }
    }

    public static PatchManager getsInstance() {
        return sInstance;
    }

    PatchManager() {
    }


    /**
     * 保证原子性
     */
    private static final AtomicReferenceFieldUpdater<PatchManager, Patch> sPatchUpdater =
            AtomicReferenceFieldUpdater.newUpdater(PatchManager.class, Patch.class, "mPatch");
    private volatile Patch mPatch;

    Object receiveGet(ProceedingJoinPoint joinPoint) throws Throwable {
        Patch patch = mPatch;
        return patch == null ? joinPoint.proceed() : patch.applyGet(joinPoint);
    }

    void receiveSet(ProceedingJoinPoint joinPoint) throws Throwable {
        Patch patch = mPatch;
        if (patch == null) {
            joinPoint.proceed();
        } else {
            patch.applySet(joinPoint);
        }
    }

    Object receiveInvoke(ProceedingJoinPoint joinPoint) throws Throwable {
        Patch patch = mPatch;
        return patch == null ? joinPoint.proceed() : patch.applyInvoke(joinPoint);
    }
}
