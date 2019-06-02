package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;

import androidx.annotation.Keep;

@Keep
interface Hooker {

    Object hook(ProceedingJoinPoint joinPoint) throws Throwable;

    Hooker INSTANCE = HotfixExecutionEngine.INSTANCE;
}
