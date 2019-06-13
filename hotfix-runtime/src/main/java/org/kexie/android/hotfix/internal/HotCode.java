package org.kexie.android.hotfix.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import androidx.annotation.Keep;

@Keep
final class HotCode {

    static final int ID_NOT_FOUND = Integer.MIN_VALUE;

    private static final class MethodId {
        final int id;
        final Class[] pramTypes;

        MethodId(int id, Class[] pramTypes) {
            this.pramTypes = pramTypes;
            this.id = id;
        }
    }

    /**
     * 读取的概率远大于写的概率,并且两者的粒度都非常小
     * 所以使用读写锁来做并发优化
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final WeakHashMap<Object, HotCodeExecutor> executors = new WeakHashMap<>();
    private final CodeContext context;
    private final Class type;
    private final Map<String, Id> fieldIds;
    private final Map<String, List<MethodId>> methodId;

    private HotCode(CodeContext context,
                    Class type,
                    Map<String, Id> fieldIds,
                    Map<String, List<MethodId>> methodId) {
        this.context = context;
        this.type = type;
        this.fieldIds = fieldIds;
        this.methodId = methodId;
    }

    private static HotCodeExecutor newExecutor(
            CodeContext context,
            Object o,
            Class<?> clazz) {
        try {
            HotCodeExecutor executor = (HotCodeExecutor) clazz.newInstance();
            executor.setBaseContext(context);
            executor.setTarget(o);
            return executor;
        } catch (IllegalAccessException
                | InstantiationException e) {
            throw new AssertionError(e);
        }
    }

    static HotCode loadHotCode(CodeContext context, Class clazz) {
        Map<String, Id> fieldId = new HashMap<>();
        Map<String, List<MethodId>> methodId = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
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
        return new HotCode(context, clazz, fieldId, methodId);
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

    HotCodeExecutor lockExecutor(Object o) {
        readWriteLock.readLock().lock();
        HotCodeExecutor executor = executors.get(o);
        readWriteLock.readLock().unlock();
        if (executor == null) {
            readWriteLock.writeLock().lock();
            executor = executors.get(o);
            if (executor == null) {
                executor = newExecutor(context, o, type);
                executors.put(o, executor);
            }
            readWriteLock.writeLock().unlock();
        }
        return executor;
    }
}