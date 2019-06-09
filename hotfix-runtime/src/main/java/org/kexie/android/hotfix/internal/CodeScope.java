package org.kexie.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.CodeSignature;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import androidx.annotation.Keep;

@Keep
final class CodeScope {

    /**
     * 读取的概率远大于写的概率,并且两者的粒度都非常小
     * 所以使用读写锁来做并发优化
     */
    private final ReentrantReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

    /**
     * 被修复的字段随{@link CodeScope}生命周期存在
     */
    private final WeakHashMap<Object, OverloadObject> mOverloadObjects = new WeakHashMap<>();

    private final HashMap<Class, Metadata> mMetadata = new HashMap<>();

    private final DynamicOperation mDynamicOperation;

    private final ClassLoader mClassLoader;

    private OverloadObject getOverloadObject(Class clazz, Object object) {
        if (object == null) {
            object = clazz;
        }
        //read
        mReadWriteLock.readLock().lock();
        OverloadObject overloadObject = mOverloadObjects.get(object);
        if (overloadObject == null) {
            Metadata metadata = mMetadata.get(clazz);
            if (Metadata.EMPTY.equals(metadata)) {
                mReadWriteLock.readLock().unlock();
                return null;
            }
            if (metadata != null) {
                overloadObject = (OverloadObject) metadata.getObject();
            }
        }
        mReadWriteLock.readLock().unlock();
        //write
        if (overloadObject == null) {
            overloadObject = loadOverloadObject(clazz, object);
        }
        return overloadObject;
    }

    private OverloadObject loadOverloadObject(Class clazz, Object object) {
        mReadWriteLock.writeLock().lock();
        OverloadObject overloadObject = mOverloadObjects.get(object);
        if (overloadObject == null) {
            //检查元数据是否已经加载
            Metadata metadata = mMetadata.get(clazz);
            if (Metadata.EMPTY.equals(metadata)) {
                mReadWriteLock.writeLock().unlock();
                return null;
            }
            if (metadata == null) {
                Package pack = clazz.getPackage();
                String overloadClassName
                        = (pack == null ? "" : pack.getName())
                        + (pack == null ? "" : ".")
                        + clazz.getSimpleName()
                        + "$$Overload";
                try {
                    Class overloadClass = mClassLoader.loadClass(overloadClassName);
                    metadata = new OverloadMetadata(overloadClass);
                    mMetadata.put(clazz, metadata);
                    overloadObject = (OverloadObject) metadata.getObject();
                    mOverloadObjects.put(object, overloadObject);
                } catch (ClassNotFoundException ignored) {
                    mMetadata.put(clazz, Metadata.EMPTY);
                    overloadObject = null;
                }
            }
        }
        mReadWriteLock.writeLock().unlock();
        return overloadObject;
    }

    private Object invokeOverloadMethod(
            int id,
            Class type,
            Object target,
            Object[] prams) {
        OverloadObject overloadObject = getOverloadObject(type, target);
        if (overloadObject != null) {
            return overloadObject.invokeWithId(id, target, prams);
        } else {
            throw new AssertionError();
        }
    }

    private Object accessOverloadField(
            int id,
            Class type,
            Object target) {
        OverloadObject overloadObject = getOverloadObject(type, target);
        if (overloadObject != null) {
            return overloadObject.accessWithId(id, target);
        } else {
            throw new AssertionError();
        }
    }

    private void modifyOverloadField(
            int id,
            Class type,
            Object target,
            Object newValue) {
        OverloadObject overloadObject = getOverloadObject(type, target);
        if (overloadObject != null) {
            overloadObject.modifyWithId(id, target, newValue);
        } else {
            throw new AssertionError();
        }
    }

    private int hasMethod(
            Class type,
            String name,
            Class[] pramsTypes) {
        mReadWriteLock.readLock().lock();
        Metadata metadata = mMetadata.get(type);
        int id = Metadata.EMPTY.equals(metadata) || metadata == null
                ? Metadata.ID_NOT_FOUND
                : metadata.hasMethod(name, pramsTypes);
        mReadWriteLock.readLock().unlock();
        return id;
    }

    private int hasField(Class type, String name) {
        mReadWriteLock.readLock().lock();
        Metadata metadata = mMetadata.get(type);
        int id = Metadata.EMPTY.equals(metadata) || metadata == null
                ? Metadata.ID_NOT_FOUND
                : metadata.hasField(name);
        mReadWriteLock.readLock().unlock();
        return id;
    }

    CodeScope(DynamicOperation executionEngine,
              ClassLoader classLoader) {
        mDynamicOperation = executionEngine;
        mClassLoader = classLoader;
    }

    final ClassLoader getClassLoader() {
        return mClassLoader;
    }

    final Object receiveInvoke(ProceedingJoinPoint joinPoint)
            throws Throwable {
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        Class type = codeSignature.getDeclaringType();
        String name = codeSignature.getName();
        Class[] pramsTypes = codeSignature.getParameterTypes();
        int id = hasMethod(type, name, pramsTypes);
        if (id != Metadata.ID_NOT_FOUND) {
            return invokeOverloadMethod(id, type,
                    joinPoint.getTarget(), joinPoint.getArgs());
        } else {
            return joinPoint.proceed();
        }
    }

    final Object receiveInvoke(Class type,
                               String name,
                               Class[] pramsTypes,
                               Object target,
                               Object[] prams)
            throws Throwable {
        int id = hasMethod(type, name, pramsTypes);
        if (id != Metadata.ID_NOT_FOUND) {
            return invokeOverloadMethod(id, type,
                    target, prams);
        } else {
            if (mDynamicOperation.isThat(this)) {
                return mDynamicOperation.getInner()
                        .invoke(type, name, pramsTypes, target, prams);
            } else {
                return mDynamicOperation.invoke(type, name, pramsTypes, target, prams);
            }
        }
    }

    final Object receiveAccess(Class type,
                               String name,
                               Object o)
            throws Throwable {
        int id = hasField(type, name);
        if (id != Metadata.ID_NOT_FOUND) {
            return accessOverloadField(id, type, o);
        } else {
            if (mDynamicOperation.isThat(this)) {
                throw new NoSuchFieldException();
            } else {
                return mDynamicOperation.access(type, name, o);
            }
        }
    }

    final void receiveModify(Class type,
                             String name,
                             Object o,
                             Object newValue)
            throws Throwable {
        int id = hasField(type, name);
        if (id != Metadata.ID_NOT_FOUND) {
            modifyOverloadField(id, type, o, newValue);
        } else {
            if (mDynamicOperation.isThat(this)) {
                throw new NoSuchFieldException();
            } else {
                mDynamicOperation.modify(type, name, o, newValue);
            }
        }
    }
}
