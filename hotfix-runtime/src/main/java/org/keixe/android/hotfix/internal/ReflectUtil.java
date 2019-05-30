package org.keixe.android.hotfix.internal;

import android.util.LruCache;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author Luke
 */
final class ReflectUtil {

    private ReflectUtil() {
        throw new AssertionError();
    }

    private static final LruCache<List<Object>, String> sCache = new LruCache<>(16);

    static String makeMethodSignature(
            Class type,
            String name,
            Class[] pramsTypes) {
        Object[] arr = new Object[pramsTypes.length + 2];
        System.arraycopy(pramsTypes, 0, arr, 2, pramsTypes.length);
        arr[0] = type;
        arr[1] = name;
        List<Object> list = Arrays.asList(arr);
        String signature = sCache.get(list);
        if (signature != null) {
            return signature;
        }
        String[] strings = new String[pramsTypes.length];
        for (int i = 0; i < pramsTypes.length; i++) {
            strings[i] = pramsTypes[i].getName();
        }
        signature = makeMethodSignature(type.getName(), name, strings);
        sCache.put(list, signature);
        return signature;
    }


    static String makeMethodSignature(
            String typeName,
            String name,
            String[] pramsTypeNames) {
        StringBuilder builder = new StringBuilder(64)
                .append('M')
                .append(' ')
                .append(typeName)
                .append('-')
                .append('>')
                .append(name)
                .append('(');
        if (pramsTypeNames.length > 1) {
            builder.append(pramsTypeNames[0]);
            for (int i = 1; i < pramsTypeNames.length; ++i) {
                builder.append(',')
                        .append(pramsTypeNames[i]);
            }
        }
        builder.append(')');
        return builder.toString();
    }


    static String makeFieldSignature(
            Class type,
            String name) {
        List<Object> list = Arrays.<Object>asList(type, name);
        String signature = sCache.get(list);
        if (signature != null) {
            return signature;
        }
        signature = makeFieldSignature(type.getName(), name);
        sCache.put(list, signature);
        return signature;
    }

    static String makeFieldSignature(String typeName, String name) {
        return "F " + typeName + "->" + name;
    }

    static Class[] toClassArray(String[] pramTypeNames) throws ClassNotFoundException {
        Class[] pramTypes = new Class[pramTypeNames.length];
        for (int i = 0; i < pramTypeNames.length; ++i) {
            pramTypes[i] = Class.forName(pramTypeNames[i]);
        }
        return pramTypes;
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
