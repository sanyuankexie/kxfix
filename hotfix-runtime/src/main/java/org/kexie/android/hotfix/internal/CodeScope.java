package org.kexie.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.CodeSignature;

import java.util.HashMap;

import androidx.annotation.Keep;

@Keep
abstract class CodeScope {

    private final HashMap<Class, HotCode> includes = new HashMap<>();

    private CodeScopeManager context;

    abstract Class[] onLoadClasses() throws Throwable;

    void loadClasses(CodeScopeManager context) throws Throwable {
        this.context = context;
        for (Class clazz : onLoadClasses()) {
            includes.put(clazz, HotCode.create(context, clazz));
        }
    }

    final ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    final Object dispatchInvoke(ProceedingJoinPoint joinPoint)
            throws Throwable {
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        Class type = codeSignature.getDeclaringType();
        HotCode hotCode = includes.get(type);
        if (hotCode != null) {
            String name = codeSignature.getName();
            Class[] pramsTypes = codeSignature.getParameterTypes();
            int id = hotCode.hasMethod(name, pramsTypes);
            if (id != HotCode.ID_NOT_FOUND) {
                Object o = joinPoint.getTarget();
                HotCodeExecutor executor = hotCode.lockExecutor(o);
                if (executor != null) {
                    return executor.receiveInvokeById(id, o, joinPoint.getArgs());
                }
            }
        }
        if (context.isThatScope(this)) {
            return joinPoint.proceed();
        } else {
            return context.invoke(codeSignature.getDeclaringType(),
                    codeSignature.getName(),
                    codeSignature.getParameterTypes(),
                    joinPoint.getTarget(),
                    joinPoint.getArgs());
        }
    }

    final Object dispatchInvoke(Class type,
                                String name,
                                Class[] pramsTypes,
                                Object o,
                                Object[] prams)
            throws Throwable {
        HotCode hotCode = includes.get(type);
        if (hotCode != null) {
            int id = hotCode.hasMethod(name, pramsTypes);
            if (id != HotCode.ID_NOT_FOUND) {
                HotCodeExecutor executor = hotCode.lockExecutor(o);
                if (executor != null) {
                    return executor.receiveInvokeById(id, o, prams);
                }
            }
        }
        if (context.isThatScope(this)) {
            return context.getBaseContext().invoke(type, name, pramsTypes, o, prams);
        } else {
            return context.invoke(type, name, pramsTypes, o, prams);
        }
    }

    final Object dispatchAccess(Class type,
                                String name,
                                Object o)
            throws Throwable {
        HotCode hotCode = includes.get(type);
        if (hotCode != null) {
            int id = hotCode.hasField(name);
            if (id != HotCode.ID_NOT_FOUND) {
                HotCodeExecutor executor = hotCode.lockExecutor(o);
                if (executor != null) {
                    return executor.receiveAccessById(id, o);
                }
            }
        }
        if (!context.isThatScope(this)) {
            return context.access(type, name, o);
        }
        throw new NoSuchFieldException();
    }

    final void dispatchModify(Class type,
                              String name,
                              Object o,
                              Object newValue)
            throws Throwable {
        HotCode hotCode = includes.get(type);
        if (hotCode != null) {
            int id = hotCode.hasField(name);
            if (id != HotCode.ID_NOT_FOUND) {
                HotCodeExecutor executor = hotCode.lockExecutor(o);
                if (executor != null) {
                    executor.receiveModifyById(id, o, newValue);
                    return;
                }
            }
        }
        if (!context.isThatScope(this)) {
            context.modify(type, name, o, newValue);
            return;
        }
        throw new NoSuchFieldException();
    }
}