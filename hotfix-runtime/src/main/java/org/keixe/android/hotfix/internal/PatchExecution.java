package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.CodeSignature;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 补丁的执行器
 */
final class PatchExecution extends Reflection {

    private static final AtomicReferenceFieldUpdater<PatchExecution, Patch> sPatchUpdater
            = AtomicReferenceFieldUpdater
            .newUpdater(PatchExecution.class, Patch.class, "mPatch");

    private volatile Patch mPatch;

    final Object apply(ProceedingJoinPoint joinPoint) throws Throwable {
        Patch patch = mPatch;
        if (patch != null && patch.isJoinPointEntry(joinPoint)) {
            CodeSignature signature = (CodeSignature) joinPoint.getSignature();
            return patch.receiveInvoke(
                    signature.getDeclaringType(),
                    signature.getName(),
                    signature.getParameterTypes(),
                    joinPoint.getTarget(),
                    joinPoint.getArgs()
            );
        }
        return joinPoint.proceed();
    }

    @Override
    public final Object invoke(
            Class type,
            String name,
            Class[] pramsTypes,
            Object target,
            Object[] prams)
            throws Throwable {
        Patch patch = mPatch;
        if (patch == null) {
            return super.invoke(type, name, pramsTypes, target, prams);
        } else {
            return patch.receiveInvoke(type, name, pramsTypes, target, prams);
        }
    }

    @Override
    public final Object access(
            Class type,
            String name,
            Object target)
            throws Throwable {
        Patch patch = mPatch;
        if (patch == null) {
            return super.access(type, name, target);
        } else {
            return patch.receiveAccess(type, name, target);
        }
    }

    @Override
    public final void modify(
            Class type,
            String name,
            Object target,
            Object newValue)
            throws Throwable {
        Patch patch = mPatch;
        if (patch == null) {
            super.modify(type, name, target, newValue);
        } else {
            patch.receiveModify(type, name, target, newValue);
        }
    }
}