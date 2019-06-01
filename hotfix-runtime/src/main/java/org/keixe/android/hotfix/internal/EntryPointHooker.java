package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;

import androidx.annotation.Keep;

@Keep
interface EntryPointHooker {

    Object hook(ProceedingJoinPoint joinPoint) throws Throwable;

    EntryPointHooker INSTANCE = HotfixExecutionEngine.INSTANCE;
}
