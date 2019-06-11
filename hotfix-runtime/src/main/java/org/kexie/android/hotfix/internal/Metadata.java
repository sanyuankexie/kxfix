package org.kexie.android.hotfix.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.annotation.Keep;

@Keep
final class Metadata {

    static final int ID_NOT_FOUND = Integer.MIN_VALUE;

    private static final class MethodId {
        final int id;
        final Class[] pramTypes;

        MethodId(int id, Class[] pramTypes) {
            this.pramTypes = pramTypes;
            this.id = id;
        }
    }

    private final Object sharedObject;
    private final Map<String, Id> fieldIds;
    private final Map<String, List<MethodId>> methodId;

    private Metadata(Object sharedObject,
                     Map<String, Id> fieldIds,
                     Map<String, List<MethodId>> methodId) {
        this.sharedObject = sharedObject;
        this.fieldIds = fieldIds;
        this.methodId = methodId;
    }

    private static OverloadObject newInstance(Class<?> clazz) {
        try {
            return (OverloadObject) clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new AssertionError(e);
        }
    }

    static Metadata loadByType(Class clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Object shared = fields.length == 0 ? newInstance(clazz) : clazz;
        Map<String, Id> fieldId = new HashMap<>();
        Map<String, List<MethodId>> methodId = new HashMap<>();
        for (Field field : fields) {
            Id id = field.getAnnotation(Id.class);
            if (id != null) {
                fieldId.put(field.getName(), id);
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            Id id = method.getAnnotation(Id.class);
            if (id != null) {
                Class[] pramTypes = method.getParameterTypes();
                if (!Modifier.isStatic(method.getModifiers()) && pramTypes.length > 1) {
                    Class[] classes = new Class[pramTypes.length];
                    System.arraycopy(pramTypes, 1, classes,
                            0, pramTypes.length - 1);
                    pramTypes = classes;
                }
                String name = method.getName();
                List<MethodId> methods = methodId.get(name);
                if (methods == null) {
                    methods = new LinkedList<>();
                    methodId.put(name, methods);
                }
                methods.add(new MethodId(id.value(), pramTypes));
            }
        }
        return new Metadata(shared, fieldId, methodId);
    }

    int hasMethod(String name, Class[] pramTypes) {
        List<MethodId> methods = methodId.get(name);
        if (methods != null) {
            for (MethodId method : methods) {
                if (Arrays.equals(method.pramTypes, pramTypes)) {
                    return method.id;
                }
            }
        }
        return ID_NOT_FOUND;
    }

    int hasField(String name) {
        Id id = fieldIds.get(name);
        return id != null ? id.value() : ID_NOT_FOUND;
    }

    OverloadObject obtainObject() {
        return sharedObject instanceof OverloadObject
                ? (OverloadObject) sharedObject
                : newInstance((Class) sharedObject);
    }
}
