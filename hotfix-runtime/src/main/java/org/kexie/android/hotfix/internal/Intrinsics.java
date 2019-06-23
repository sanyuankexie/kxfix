package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

@Keep
@SuppressWarnings("WeakerAccess")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class Intrinsics {

    private Intrinsics() {
        throw new AssertionError();
    }

    public static Object checkArgument(RedirectTarget $this, Object arg) {
        return $this == arg ? $this.target : arg;
    }

    public static Class typeOf(String name)
            throws Throwable {
        return HotfixEngine.INSTANCE.typeOf(name);
    }

    public static Object invoke(boolean nonVirtual,
                                Class type,
                                String name,
                                Class[] pramsTypes,
                                Object target,
                                Object[] prams)
            throws Throwable {
        return HotfixEngine.INSTANCE.invoke(nonVirtual, type, name, pramsTypes, target, prams);
    }

    public static Object access(Class type,
                                String name,
                                Object target)
            throws Throwable {
        return HotfixEngine.INSTANCE.access(type, name, target);
    }

    public static void modify(Class type,
                              String name,
                              Object target,
                              Object newValue)
            throws Throwable {
        HotfixEngine.INSTANCE.modify(type, name, target, newValue);
    }

    public static Object newInstance(
            Class<?> type,
            Class[] pramTypes,
            Object[] prams)
            throws Throwable {
        return HotfixEngine.INSTANCE.newInstance(type, pramTypes, prams);
    }
}
