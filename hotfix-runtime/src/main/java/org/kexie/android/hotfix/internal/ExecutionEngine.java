package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;

/**
 * 定义操作原语
 * 代码执行的上下文
 */
@Keep
abstract class ExecutionEngine {

    abstract Class typeOf(String name)
            throws Throwable;

    abstract Object invoke(boolean nonVirtual,
                           Class type,
                           String name,
                           Class[] pramsTypes,
                           Object target,
                           Object[] prams)
            throws Throwable;

    abstract Object access(Class type,
                           String name,
                           Object target)
            throws Throwable;

    abstract void modify(Class type,
                         String name,
                         Object target,
                         Object newValue)
            throws Throwable;

    abstract Object newInstance(
            Class<?> type,
            Class[] pramTypes,
            Object[] prams)
            throws Throwable;

}