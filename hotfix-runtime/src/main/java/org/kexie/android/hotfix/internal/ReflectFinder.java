package org.kexie.android.hotfix.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import androidx.annotation.Keep;

/**
 * @author Luke
 * 反射的工具类,主要用于查找方法和字段
 */
@Keep
final class ReflectFinder {

    private ReflectFinder() {
        throw new AssertionError();
    }

    /**
     * 只应该查找公开的构造函数
     * 或者是那个类声明的构造函数
     */
    static Constructor<?> findConstructor(
            Class<?> type,
            Class[] pramTypes
    ) throws NoSuchMethodException {
        Constructor<?> constructor;
        try {
            constructor = type.getConstructor(pramTypes);
        } catch (NoSuchMethodException e) {
            constructor = type.getDeclaredConstructor(pramTypes);
            constructor.setAccessible(true);
        }
        return constructor;
    }


    static Field findFieldNoThrow(
            Class type,
            String name
    ) {
        try {
            return findField(type, name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * 一直向下查找,找到的最近的那个字段
     * 就是字节码里需要但是只能反射的字段
     */
    static Field findField(
            Class type,
            String name
    ) throws NoSuchFieldException {
        Field field = null;
        while (type != null && !Objects.equals(type.getClassLoader(),
                Object.class.getClassLoader())) {
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


    static Method findMethodNoThrow(
            Class<?> type,
            String name,
            Class[] pramTypes
    ) {
        try {
            return findMethod(type, name, pramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 一直向下查找,找到的最近的那个方法
     * 就是字节码里需要但是只能反射的方法
     */
    static Method findMethod(
            Class<?> type,
            String name,
            Class[] pramTypes
    ) throws NoSuchMethodException {
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