package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;

interface EntryPointHooker {

    Object hook(ProceedingJoinPoint joinPoint) throws Throwable;

    EntryPointHooker INSTANCE = HotfixExecutionEngine.INSTANCE;
}
