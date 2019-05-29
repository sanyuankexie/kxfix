package org.keixe.android.hotfix.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 指令内部代码类
 */
final class Intrinsics {

    static {
        System.loadLibrary("intrinsics");
    }

    private Intrinsics() {
        throw new AssertionError();
    }

    static Object newInstance(Class<?> type,
                              Class[] pramTypes,
                              Object[] prams) throws Throwable {

        return constructorBy(type, pramTypes).newInstance(prams);
    }

    static void modify(Class type,
                       String name,
                       Object target,
                       Object newValue) throws Throwable {
        fieldBy(type, name).set(target, newValue);
    }

    static Object access(Class type,
                         Object target,
                         String name) throws Throwable {
        return fieldBy(type, name).get(target);
    }

    static Object invoke(Class type,
                         String name,
                         Class[] pramTypes,
                         boolean nonVirtual,
                         Object target,
                         Object[] prams) throws Throwable {
        Method method = methodBy(type, name, pramTypes);
        return !nonVirtual && !Modifier.isStatic(method.getModifiers())
                ? method.invoke(target, prams)
                : invokeNonVirtual(type, method, target, prams);
    }

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
        Field field;
        try {
            field = type.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            field = type.getField(name);
            field.setAccessible(true);
        }
        return field;
    }

    private static Method methodBy(
            Class<?> type,
            String name,
            Class[] pramTypes
    ) throws Throwable {
        Method method;
        try {
            method = type.getMethod(name, pramTypes);
        } catch (NoSuchMethodException e) {
            method = type.getDeclaredMethod(name, pramTypes);
            method.setAccessible(true);
        }
        return method;
    }

    //JNI->CallNonvirtualVoidMethod
    //主要是为了实现invoke-super指令
    //会抛出异常,在native捕获之后抛出到java层
    private static native Object
    invokeNonVirtual(
            Class type,
            Method target,
            Object object,
            Object[] prams
    ) throws Throwable;
}
