package org.keixe.android.hotfix.internal;

import android.util.ArrayMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.annotation.Keep;

@Keep
final class Metadata {

    Metadata() {
    }

    private static final List<MemberInfo> IS_FIELD_MARK = Collections.emptyList();

    private final ArrayMap<Class, Map<String, List<MemberInfo>>> mData = new ArrayMap<>();

    final void addMethod(String typeName,
                         String name,
                         String[] pramTypeNames) {
        try {
            Class type = Class.forName(typeName);
            Class[] pramTypes = toClassArray(pramTypeNames);
            Map<String, List<MemberInfo>> typeData = mData.get(type);
            if (typeData == null) {
                typeData = new ArrayMap<>();
                mData.put(type, typeData);
            }
            List<MemberInfo> methods = typeData.get(name);
            if (methods == null) {
                methods = new LinkedList<>();
                typeData.put(name, methods);
            }
            methods.add(new MemberInfo(pramTypes,
                    cachedSignature(typeName, name, pramTypeNames)));
        } catch (Exception ignored) {

        }
    }

    final void addField(String typeName, String name) {
        try {
            Class type = Class.forName(typeName);
            Map<String, List<MemberInfo>> typeData = mData.get(type);
            if (typeData == null) {
                typeData = new ArrayMap<>();
                mData.put(type, typeData);
            }
            List<MemberInfo> fields = typeData.get(name);
            if (fields == null) {
                typeData.put(name, Collections.<MemberInfo>emptyList());
            }
        } catch (Exception ignored) {

        }
    }

    final String hasMethod(
            Class type,
            String name,
            Class[] pramTypes) {
        Map<String, List<MemberInfo>> typeData = mData.get(type);
        if (typeData == null) {
            return null;
        }
        List<MemberInfo> methods = typeData.get(name);
        if (methods != null && !IS_FIELD_MARK.equals(methods)) {
            for (MemberInfo method : methods) {
                if (Arrays.equals(method.getParameterTypes(), pramTypes)) {
                    return method.getSignature();
                }
            }
        }
        return null;
    }

    final boolean hasField(Class type, String name) {
        Map<String, List<MemberInfo>> typeData = mData.get(type);
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


    private static String cachedSignature(
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
