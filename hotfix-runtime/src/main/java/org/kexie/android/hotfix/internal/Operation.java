package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

/**
 * 定义操作原语
 */
@Keep
@SuppressWarnings("WeakerAccess")
@RestrictTo(RestrictTo.Scope.LIBRARY)
abstract class Operation {

    public abstract Class typeOf(String name)
            throws Throwable;

    public abstract Object invoke(Class type,
                                  String name,
                                  Class[] pramsTypes,
                                  Object target,
                                  Object[] prams)
            throws Throwable;

    public abstract Object access(Class type,
                                  String name,
                                  Object target)
            throws Throwable;

    public abstract void modify(Class type,
                                String name,
                                Object target,
                                Object newValue)
            throws Throwable;

    public abstract Object InvokeNonVirtual(
            Class type,
            String name,
            Class[] pramTypes,
            Object target,
            Object[] prams
    ) throws Throwable;

    public abstract Object newInstance(
            Class<?> type,
            Class[] pramTypes,
            Object[] prams)
            throws Throwable;

}