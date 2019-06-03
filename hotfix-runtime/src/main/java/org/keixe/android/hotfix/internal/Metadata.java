package org.keixe.android.hotfix.internal;

import android.util.ArrayMap;
import android.util.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.annotation.Keep;

@Keep
final class Metadata {

    Metadata() { }

    private static final List<Pair<Class[], String>> IS_FIELD_MARK = Collections.emptyList();

    private final ArrayMap<Class, Map<String, List<Pair<Class[], String>>>> mData = new ArrayMap<>();

    final void addMethod(String typeName, String name, String[] pramTypeNames) {
        try {
            Class type = Class.forName(typeName);
            Class[] pramTypes = toClassArray(pramTypeNames);
            Map<String, List<Pair<Class[], String>>> typeData = mData.get(type);
            if (typeData == null) {
                typeData = new ArrayMap<>();
                mData.put(type, typeData);
            }
            List<Pair<Class[], String>> methods = typeData.get(name);
            if (methods == null) {
                methods = new LinkedList<>();
                typeData.put(name, methods);
            }
            methods.add(Pair.create(pramTypes,
                    getMethodCachedSignature(type.getName(), name, pramTypeNames)));
        } catch (Exception ignored) {

        }
    }

    final void addField(String typeName, String name) {
        try {
            Class type = Class.forName(typeName);
            Map<String, List<Pair<Class[], String>>> typeData = mData.get(type);
            if (typeData == null) {
                typeData = new ArrayMap<>();
                mData.put(type, typeData);
            }
            List<Pair<Class[], String>> fields = typeData.get(name);
            if (fields == null) {
                typeData.put(name, Collections.<Pair<Class[], String>>emptyList());
            }
        } catch (Exception ignored) {

        }
    }

    final String hasMethod(Class type, String name, Class[] pramTypes) {
        Pair<Class[], String> pair;
        return (pair = hasMethod(mData.get(type), name, pramTypes)) == null
                ? null : pair.second;
    }

    final boolean hasField(Class type, String name) {
        Map<String, List<Pair<Class[], String>>> typeData = mData.get(type);
        return typeData != null && IS_FIELD_MARK.equals(typeData.get(name));
    }

    private static Class[] toClassArray(String[] pramTypeNames)
            throws ClassNotFoundException {
        Class[] pramTypes = new Class[pramTypeNames.length];
        for (int i = 0; i < pramTypeNames.length; ++i) {
            pramTypes[i] = Class.forName(pramTypeNames[i]);
        }
        return pramTypes;
    }

    private static Pair<Class[], String> hasMethod(
            Map<String, List<Pair<Class[], String>>> typeData,
            String name,
            Class[] pramTypes) {
        if (typeData == null) {
            return null;
        }
        List<Pair<Class[], String>> methods = typeData.get(name);
        if (methods != null && !IS_FIELD_MARK.equals(methods)) {
            for (Pair<Class[], String> method : methods) {
                if (Arrays.deepEquals(method.first, pramTypes)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static String getMethodCachedSignature(
            String typeName,
            String name,
            Object[] pramsTypeNames) {
        StringBuilder builder = new StringBuilder(
                typeName.length()
                        + name.length()
                        + 16 * pramsTypeNames.length)
                .append(typeName)
                .append("#")
                .append(name)
                .append('(');
        if (pramsTypeNames instanceof String[]) {
            if (pramsTypeNames.length >= 1) {
                builder.append(pramsTypeNames[0]);
                for (int i = 1; i < pramsTypeNames.length; i++) {
                    builder.append(',')
                            .append(pramsTypeNames[i]);
                }
            }
        } else if (pramsTypeNames instanceof Class[]) {
            if (pramsTypeNames.length >= 1) {
                builder.append(((Class) pramsTypeNames[0]).getName());
                for (int i = 1; i < pramsTypeNames.length; i++) {
                    builder.append(',')
                            .append(((Class) pramsTypeNames[i]).getName());
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
        builder.append(')');
        return builder.toString();
    }
}
