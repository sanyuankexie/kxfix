package org.kexie.android.hotfix.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import androidx.annotation.Keep;

/***
 * JVM反射执行引擎
 */
@Keep
@SuppressWarnings("WeakerAccess")
class ReflectExecutionEngine implements ExecutionEngine {

    protected ReflectExecutionEngine() { }

    static final ReflectExecutionEngine JVM = new ReflectExecutionEngine();

    static {
        System.loadLibrary("reflection");
    }

    @Override
    public final Object newInstance(Class<?> type,
                                    Class[] pramTypes,
                                    Object[] prams)
            throws Throwable {
        return ReflectFinder.constructorBy(type, pramTypes).newInstance(prams);
    }

    @Override
    public void modify(Class type,
                       String name,
                       Object target,
                       Object newValue)
            throws Throwable {
        ReflectFinder.fieldBy(type, name).set(target, newValue);
    }

    public Object access(Class type,
                         String name,
                         Object target
    ) throws Throwable {
        return ReflectFinder.fieldBy(type, name).get(target);
    }

    @Override
    public Object invoke(Class type,
                         String name,
                         Class[] pramTypes,
                         Object target,
                         Object[] prams) throws Throwable {
        Method method = ReflectFinder.methodBy(type, name, pramTypes);
        return method.invoke(target, prams);
    }

    @Override
    public final Object InvokeNonVirtual(
            Class type,
            String name,
            Class[] pramTypes,
            Object target,
            Object[] prams) throws Throwable {
        Method method = ReflectFinder.methodBy(type, name, pramTypes);
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException();
        }
        return invokeNonVirtual(type, method, target, prams);
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
