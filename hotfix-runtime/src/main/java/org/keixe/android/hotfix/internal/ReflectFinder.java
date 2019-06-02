package org.keixe.android.hotfix.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import androidx.annotation.Keep;

/**
 * @author Luke
 */
@Keep
final class ReflectFinder {

    static final ExecutionEngine JVM = ReflectExecutionEngine.INSTANCE;

    private ReflectFinder() {
        throw new AssertionError();
    }

    static Constructor<?> constructorBy(
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

    static Field fieldBy(
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

    static Method methodBy(
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