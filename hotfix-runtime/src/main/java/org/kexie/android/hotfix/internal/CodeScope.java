package org.kexie.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.CodeSignature;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import androidx.annotation.Keep;

@Keep
abstract class CodeScope {

    /**
     * 读取的概率远大于写的概率,并且两者的粒度都非常小
     * 所以使用读写锁来做并发优化
     */
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * 被修复的字段随{@link CodeScope}生命周期存在
     */
    private final WeakHashMap<Object, OverloadObject> overloadObjects = new WeakHashMap<>();

    private final HashMap<Class, Metadata> includes = new HashMap<>();

    private HotfixEngine context;

    abstract Class[] includeTypes()throws Throwable;

    private OverloadObject getOverloadObject(Class clazz, Object object) {
        if (object == null) {
            object = clazz;
        }
        //read
        readWriteLock.readLock().lock();
        OverloadObject overloadObject = overloadObjects.get(object);
        readWriteLock.readLock().unlock();
        //write
        if (overloadObject == null) {
            overloadObject = loadOverloadObject(clazz, object);
        }
        return overloadObject;
    }

    private OverloadObject loadOverloadObject(Class clazz, Object object) {
        readWriteLock.writeLock().lock();
        OverloadObject overloadObject = overloadObjects.get(object);
        if (overloadObject == null) {
            Metadata metadata = includes.get(clazz);
            if (metadata != null) {
                overloadObject = metadata.obtainObject();
                overloadObject.setBaseContext(context);
                overloadObjects.put(object, overloadObject);
            } else {
                throw new AssertionError();
            }
        }
        readWriteLock.writeLock().unlock();
        return overloadObject;
    }

    private Object dispatchInvokeById(
            int id,
            Class type,
            Object target,
            Object[] prams) {
        return getOverloadObject(type, target)
                .receiveInvokeById(id, target, prams);
    }

    private Object dispatchAccessById(
            int id,
            Class type,
            Object target) {
        return getOverloadObject(type, target)
                .receiveAccessById(id, target);
    }

    private void dispatchModifyById(
            int id,
            Class type,
            Object target,
            Object newValue) {
        getOverloadObject(type, target)
                .receiveModifyById(id, target, newValue);
    }

    private int hasMethod(
            Class type,
            String name,
            Class[] pramsTypes) {
        Metadata metadata = includes.get(type);
        if (metadata == null) {
            return Metadata.ID_NOT_FOUND;
        }
        return metadata.hasMethod(name, pramsTypes);
    }

    void init(HotfixEngine context) throws Throwable {
        this.context = context;
        for (Class clazz : includeTypes()) {
            includes.put(clazz, Metadata.loadByType(clazz));
        }
    }

    private int hasField(Class type, String name) {
        Metadata metadata = includes.get(type);
        if (metadata == null) {
            return Metadata.ID_NOT_FOUND;
        }
        return metadata.hasField(name);
    }

    final ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    final Object dispatchInvoke(ProceedingJoinPoint joinPoint)
            throws Throwable {
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        Class type = codeSignature.getDeclaringType();
        String name = codeSignature.getName();
        Class[] pramsTypes = codeSignature.getParameterTypes();
        int id = hasMethod(type, name, pramsTypes);
        if (id != Metadata.ID_NOT_FOUND) {
            return dispatchInvokeById(id, type,
                    joinPoint.getTarget(), joinPoint.getArgs());
        } else {
            return joinPoint.proceed();
        }
    }

    final Object dispatchInvoke(Class type,
                                String name,
                                Class[] pramsTypes,
                                Object target,
                                Object[] prams)
            throws Throwable {
        int id = hasMethod(type, name, pramsTypes);
        if (id != Metadata.ID_NOT_FOUND) {
            return dispatchInvokeById(id, type,
                    target, prams);
        } else {
            if (context.isThat(this)) {
                return context.getBaseContext()
                        .invoke(type, name, pramsTypes, target, prams);
            } else {
                return context.invoke(type, name, pramsTypes, target, prams);
            }
        }
    }

    final Object dispatchAccess(Class type,
                                String name,
                                Object o)
            throws Throwable {
        int id = hasField(type, name);
        if (id != Metadata.ID_NOT_FOUND) {
            return dispatchAccessById(id, type, o);
        } else {
            if (context.isThat(this)) {
                throw new NoSuchFieldException();
            } else {
                return context.access(type, name, o);
            }
        }
    }

    final void dispatchModify(Class type,
                              String name,
                              Object o,
                              Object newValue)
            throws Throwable {
        int id = hasField(type, name);
        if (id != Metadata.ID_NOT_FOUND) {
            dispatchModifyById(id, type, o, newValue);
        } else {
            if (context.isThat(this)) {
                throw new NoSuchFieldException();
            } else {
                context.modify(type, name, o, newValue);
            }
        }
    }
}