package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;

/**
 * 执行引擎
 * 定义操作原语
 */
@Keep
interface ExecutionEngine {

    Class typeOf(String name)
            throws Throwable;

    Object invoke(Class type,
                  String name,
                  Class[] pramsTypes,
                  Object target,
                  Object[] prams)
            throws Throwable;

    Object access(Class type,
                  String name,
                  Object target)
            throws Throwable;

    void modify(Class type,
                String name,
                Object target,
                Object newValue)
            throws Throwable;

    Object InvokeNonVirtual(
            Class type,
            String name,
            Class[] pramTypes,
            Object target,
            Object[] prams
    ) throws Throwable;

    Object newInstance(Class<?> type,
                       Class[] pramTypes,
                       Object[] prams)
            throws Throwable;

}