package org.kexie.android.hotfix.internal;

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

    static final int METHOD_NOT_FOUND = Integer.MIN_VALUE;

    private static final List<MemberInfo> IS_FIELD_MARK = Collections.emptyList();

    private final ArrayMap<Class, Map<String, List<MemberInfo>>> mData = new ArrayMap<>();

    final void addMethod(int id,
                         String typeName,
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
            methods.add(new MemberInfo(id, pramTypes));
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
                typeData.put(name, IS_FIELD_MARK);
            }
        } catch (Exception ignored) {

        }
    }

    final int hasMethod(
            Class type,
            String name,
            Class[] pramTypes) {
        Map<String, List<MemberInfo>> typeData = mData.get(type);
        if (typeData == null) {
            return METHOD_NOT_FOUND;
        }
        List<MemberInfo> methods = typeData.get(name);
        if (methods != null && !IS_FIELD_MARK.equals(methods)) {
            for (MemberInfo method : methods) {
                if (Arrays.equals(method.mParameterTypes, pramTypes)) {
                    return method.mId;
                }
            }
        }
        return METHOD_NOT_FOUND;
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

    static final class MemberInfo {
        final Class[] mParameterTypes;
        final int mId;
        MemberInfo(int id, Class[] parameterTypes) {
            this.mId = id;
            this.mParameterTypes = parameterTypes;
        }
    }
}