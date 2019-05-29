package org.keixe.android.hotfix.internal;

import android.util.Log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import androidx.annotation.RestrictTo;

@Aspect
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class HotfixManager {

    private static final String TAG = "HotfixManager";

    @Around("execution(@org.keixe.android.hotfix.Hotfix * *(..))")
    public Object doHotfix(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Log.d(TAG, "dispatch: " + methodSignature.getMethod() +
                " " + System.identityHashCode(joinPoint) +
                " " + System.identityHashCode(this));
        return joinPoint.proceed();
    }
}
