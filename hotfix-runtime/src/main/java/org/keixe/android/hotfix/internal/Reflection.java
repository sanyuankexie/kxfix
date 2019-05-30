package org.keixe.android.hotfix.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Reflection implements Intrinsics {

    static {
        System.loadLibrary("intrinsics");
    }

    static final Reflection JVM = new Reflection();

    @Override
    public final Object newInstance(Class<?> type,
                                    Class[] pramTypes,
                                    Object[] prams) throws Throwable {

        return constructorBy(type, pramTypes).newInstance(prams);
    }

    @Override
    public void modify(Class type,
                             String name,
                             Object target,
                             Object newValue) throws Throwable {
        fieldBy(type, name).set(target, newValue);
    }

    public Object access(Class type,
                               String name,
                               Object target
    ) throws Throwable {
        return fieldBy(type, name).get(target);
    }

    @Override
    public Object invoke(Class type,
                               String name,
                               Class[] pramTypes,
                               Object target,
                               Object[] prams) throws Throwable {
        Method method = methodBy(type, name, pramTypes);
        return method.invoke(target, prams);
    }

    @Override
    public final Object InvokeNonVirtual(
            Class type,
            String name,
            Class[] pramTypes,
            Object target,
            Object[] prams) throws Throwable {
        Method method = methodBy(type, name, pramTypes);
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

    private static Constructor<?> constructorBy(
            Class<?> type,
            Class[] pramTypes
    ) throws Throwable {
        Constructor<?> constructor;
        try {
            constructor = type.getConstructor(pramTypes);
        } catch (NoSuchMethodException e) {
            constructor = type.getDeclaredConstructor(pramTypes);
            constructor.setAccessible(true);
        }
        return constructor;
    }

    private static Field fieldBy(
            Class type,
            String name
    ) throws Throwable {
        Field field = null;
        while (type != null) {
            try {
                field = type.getDeclaredField(name);
                field.setAccessible(true);
                break;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException();
        }
        return field;
    }

    private static Method methodBy(
            Class<?> type,
            String name,
            Class[] pramTypes
    ) throws Throwable {
        Method method = null;
        while (type != null) {
            try {
                method = type.getDeclaredMethod(name, pramTypes);
                method.setAccessible(true);
                break;
            } catch (NoSuchMethodException e) {
                type = type.getSuperclass();
            }
        }
        if (method == null) {
            throw new NoSuchMethodException();
        }
        return method;
    }
}
