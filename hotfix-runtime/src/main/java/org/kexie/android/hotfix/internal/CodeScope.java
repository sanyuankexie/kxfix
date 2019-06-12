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
    private final WeakHashMap<Object, ExtensionExecutor> extensionExecutors = new WeakHashMap<>();

    private final HashMap<Class, ExtensionMetadata> includes = new HashMap<>();

    private CodeContextScopedWrapper context;

    abstract Class[] onLoadClasses() throws Throwable;

    private ExtensionExecutor getExtensionExecutor(Class clazz, Object object) {
        if (object == null) {
            object = clazz;
        }
        //read
        readWriteLock.readLock().lock();
        ExtensionExecutor extensionExecutor = extensionExecutors.get(object);
        readWriteLock.readLock().unlock();
        //write
        if (extensionExecutor == null) {
            extensionExecutor = loadExtensionExecutor(clazz, object);
        }
        return extensionExecutor;
    }

    private ExtensionExecutor loadExtensionExecutor(Class clazz, Object object) {
        readWriteLock.writeLock().lock();
        ExtensionExecutor extensionExecutor = extensionExecutors.get(object);
        if (extensionExecutor == null) {
            ExtensionMetadata extensionMetadata = includes.get(clazz);
            if (extensionMetadata != null) {
                extensionExecutor = extensionMetadata.obtainExtension();
                extensionExecutor.setBaseContext(context);
                extensionExecutors.put(object, extensionExecutor);
            } else {
                throw new AssertionError();
            }
        }
        readWriteLock.writeLock().unlock();
        return extensionExecutor;
    }

    private Object dispatchInvokeById(
            int id,
            Class type,
            Object target,
            Object[] prams) {
        return getExtensionExecutor(type, target)
                .receiveInvokeById(id, target, prams);
    }

    private Object dispatchAccessById(
            int id,
            Class type,
            Object target) {
        return getExtensionExecutor(type, target)
                .receiveAccessById(id, target);
    }

    private void dispatchModifyById(
            int id,
            Class type,
            Object target,
            Object newValue) {
        getExtensionExecutor(type, target)
                .receiveModifyById(id, target, newValue);
    }

    private int hasMethod(
            Class type,
            String name,
            Class[] pramsTypes) {
        ExtensionMetadata extensionMetadata = includes.get(type);
        if (extensionMetadata == null) {
            return ExtensionMetadata.ID_NOT_FOUND;
        }
        return extensionMetadata.hasMethod(name, pramsTypes);
    }

    void loadClasses(CodeContextScopedWrapper context) throws Throwable {
        this.context = context;
        for (Class clazz : onLoadClasses()) {
            includes.put(clazz, ExtensionMetadata.loadByType(clazz));
        }
    }

    private int hasField(Class type, String name) {
        ExtensionMetadata extensionMetadata = includes.get(type);
        if (extensionMetadata == null) {
            return ExtensionMetadata.ID_NOT_FOUND;
        }
        return extensionMetadata.hasField(name);
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
        if (id != ExtensionMetadata.ID_NOT_FOUND) {
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
        if (id != ExtensionMetadata.ID_NOT_FOUND) {
            return dispatchInvokeById(id, type,
                    target, prams);
        } else {
            if (context.isThatScope(this)) {
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
        if (id != ExtensionMetadata.ID_NOT_FOUND) {
            return dispatchAccessById(id, type, o);
        } else {
            if (context.isThatScope(this)) {
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
        if (id != ExtensionMetadata.ID_NOT_FOUND) {
            dispatchModifyById(id, type, o, newValue);
        } else {
            if (context.isThatScope(this)) {
                throw new NoSuchFieldException();
            } else {
                context.modify(type, name, o, newValue);
            }
        }
    }
}