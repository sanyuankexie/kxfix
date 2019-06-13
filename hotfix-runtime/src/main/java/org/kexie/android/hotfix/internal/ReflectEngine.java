package org.kexie.android.hotfix.internal;

import java.lang.reflect.Method;

import androidx.annotation.Keep;

/***
 * JVM反射执行引擎
 */
@Keep
final class ReflectEngine extends CodeContext {

    ReflectEngine() {
    }

    static {
        System.loadLibrary("jni-reflect");
    }

    @Override
    public Class typeOf(String name) throws Throwable {
        return Class.forName(name);
    }

    @Override
    public final Object newInstance(Class<?> type,
                                    Class[] pramTypes,
                                    Object[] prams)
            throws Throwable {
        return ReflectFinder.findConstructor(type, pramTypes)
                .newInstance(prams);
    }

    @Override
    public void modify(Class type,
                       String name,
                       Object target,
                       Object newValue)
            throws Throwable {
        ReflectFinder.findField(type, name)
                .set(target, newValue);
    }

    public Object access(Class type,
                         String name,
                         Object target
    ) throws Throwable {
        return ReflectFinder.findField(type, name)
                .get(target);
    }

    @Override
    public Object invoke(boolean nonVirtual,
                         Class type,
                         String name,
                         Class[] pramTypes,
                         Object target,
                         Object[] prams) throws Throwable {
        Method method = ReflectFinder.findMethod(type, name, pramTypes);
        return !nonVirtual ? method.invoke(target, prams)
                : invokeNonVirtual(type, method, target, prams);
    }

    /**
     * JNI->CallNonvirtualVoidMethod
     * 主要是为了实现invoke-super指令
     * 会抛出异常,在native捕获之后抛出到java层
     */
    private static native Object
    invokeNonVirtual(
            Class type,
            Method target,
            Object object,
            Object[] prams
    ) throws Throwable;
}
