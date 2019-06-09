package org.kexie.android.hotfix.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.Keep;

@Keep
final class OverloadMetadata extends Metadata {
    private final boolean mIsMethodOnly;
    private final Class<?> mClass;
    private volatile OverloadObject mSharedOverloadObject;
    private final HashMap<String, FieldInfo> mFieldIds = new HashMap<>();
    private final HashMap<String, List<MethodInfo>> mMethodId = new HashMap<>();

    OverloadMetadata(Class clazz) {
        mClass = clazz;
        Field[] fields = clazz.getDeclaredFields();
        if (fields.length == 0) {
            mIsMethodOnly = true;
        } else {
            mIsMethodOnly = false;
            for (Field field : fields) {
                FieldInfo fieldInfo = field.getAnnotation(FieldInfo.class);
                if (fieldInfo != null) {
                    mFieldIds.put(field.getName(), fieldInfo);
                }
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            MethodInfo methodInfo = method.getAnnotation(MethodInfo.class);
            if (methodInfo != null) {
                String name = method.getName();
                List<MethodInfo> list = mMethodId.get(name);
                if (list == null) {
                    list = new LinkedList<>();
                    mMethodId.put(name, list);
                }
                list.add(methodInfo);
            }
        }
    }

    int hasField(String name) {
        FieldInfo info = mFieldIds.get(name);
        return info == null ? ID_NOT_FOUND : info.id();
    }

    int hasMethod(String name, Class[] parameterTypes) {
        List<MethodInfo> list = mMethodId.get(name);
        if (list != null) {
            for (MethodInfo methodInfo : list) {
                if (Arrays.equals(methodInfo.parameterTypes(), parameterTypes)) {
                    return methodInfo.id();
                }
            }
        }
        return ID_NOT_FOUND;
    }

    private OverloadObject newInstance(CodeContext codeContext) {
        try {
            Constructor<?> constructor = mClass.getConstructor(CodeContext.class);
            constructor.setAccessible(true);
            return (OverloadObject) constructor.newInstance(codeContext);
        } catch (IllegalAccessException
                | InstantiationException
                | NoSuchMethodException
                | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    OverloadObject getObject(CodeContext codeContext) {
        if (mIsMethodOnly) {
            if (mSharedOverloadObject == null) {
                synchronized (this) {
                    if (mSharedOverloadObject == null) {
                        mSharedOverloadObject = newInstance(codeContext);
                    }
                }
            }
            return mSharedOverloadObject;
        } else {
            return newInstance(codeContext);
        }
    }
}