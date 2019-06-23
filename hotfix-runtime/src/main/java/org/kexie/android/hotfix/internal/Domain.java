package org.kexie.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.CodeSignature;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import androidx.annotation.Keep;

@Keep
abstract class Domain {

    Domain() {
    }

    private final HashMap<Class, RedirectTable> includes = new HashMap<>();

    private DomainManageEngine context;

    abstract Class[] loadEntries() throws Throwable;

    void loadClasses(DomainManageEngine context) throws Throwable {
        this.context = context;
        ClassLoader classLoader = getClassLoader();
        for (Class clazz : loadEntries()) {
            Package pack = clazz.getPackage();
            Class hotClass = classLoader.loadClass(
                    (pack == null ? "" : pack.getName() + ".")
                            + "Overload$" + clazz.getSimpleName());
            includes.put(clazz, new RedirectTable(hotClass));
        }
    }

    final ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    final Object dispatchInvoke(ProceedingJoinPoint joinPoint)
            throws Throwable {
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        Class type = codeSignature.getDeclaringType();
        RedirectTable hotCode = includes.get(type);
        if (hotCode != null) {
            String name = codeSignature.getName();
            Class[] pramsTypes = codeSignature.getParameterTypes();
            Method method = ReflectFinder.findMethodNoThrow(
                    hotCode.getType(),
                    name,
                    pramsTypes
            );
            if (method != null) {
                Object o = joinPoint.getTarget();
                RedirectTarget executor = hotCode.getTarget(o);
                if (executor != null) {
                    return method.invoke(executor, joinPoint.getArgs());
                }
            }
        }
        if (context.isThat(this)) {
            return joinPoint.proceed();
        } else {
            return context.invoke(
                    false,
                    codeSignature.getDeclaringType(),
                    codeSignature.getName(),
                    codeSignature.getParameterTypes(),
                    joinPoint.getTarget(),
                    joinPoint.getArgs()
            );
        }
    }

    final Object dispatchInvoke(
            boolean nonVirtual,
            Class type,
            String name,
            Class[] pramsTypes,
            Object o,
            Object[] prams)
            throws Throwable {
        RedirectTable hotCode = includes.get(!nonVirtual ? type : type.getSuperclass());
        if (hotCode != null) {
            Method method = ReflectFinder.findMethodNoThrow(
                    hotCode.getType(),
                    name,
                    pramsTypes
            );
            if (method != null) {
                RedirectTarget executor = hotCode.getTarget(o);
                if (executor != null) {
                    return method.invoke(executor, prams);
                }
            }
        }
        if (context.isThat(this)) {
            return context.getLowLevel()
                    .invoke(nonVirtual,
                            type,
                            name,
                            pramsTypes,
                            o,
                            prams);
        } else {
            return context.invoke(
                    nonVirtual,
                    type,
                    name,
                    pramsTypes,
                    o,
                    prams);
        }
    }

    final Object dispatchAccess(
            Class type,
            String name,
            Object o)
            throws Throwable {
        RedirectTable hotCode = includes.get(type);
        if (hotCode != null) {
            Field field = ReflectFinder.findFieldNoThrow(
                    hotCode.getType(),
                    name
            );
            if (field != null) {
                RedirectTarget executor = hotCode.getTarget(o);
                if (executor != null) {
                    return field.get(executor);
                }
            }
        }
        if (!context.isThat(this)) {
            return context.access(type, name, o);
        } else {
            return context.getLowLevel().access(type, name, o);
        }
    }

    final void dispatchModify(
            Class type,
            String name,
            Object o,
            Object newValue)
            throws Throwable {
        RedirectTable hotCode = includes.get(type);
        if (hotCode != null) {
            Field field = ReflectFinder.findFieldNoThrow(
                    hotCode.getType(),
                    name
            );
            if (field != null) {
                RedirectTarget executor = hotCode.getTarget(o);
                if (executor != null) {
                    field.set(o, newValue);
                    return;
                }
            }
        }
        if (!context.isThat(this)) {
            context.modify(type, name, o, newValue);
        } else {
            context.getLowLevel().modify(type, name, o, newValue);
        }
    }
}