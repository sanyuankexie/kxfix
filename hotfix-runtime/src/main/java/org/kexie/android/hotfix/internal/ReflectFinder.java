package org.kexie.android.hotfix.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import androidx.annotation.Keep;

/**
 * @author Luke
 */
@Keep
final class ReflectFinder {

    private ReflectFinder() {
        throw new AssertionError();
    }

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

    static Field findField(
            Class type,
            String name
    ) throws NoSuchFieldException {
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