package org.kexie.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.kexie.android.hotfix.Patch;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import androidx.annotation.Keep;

/**
 * 热更新执行引擎
 */
@Keep
final class HotfixExecutionEngine
        extends ReflectExecutionEngine
        implements DynamicExecutionEngine, Hooker {

    static final HotfixExecutionEngine INSTANCE = new HotfixExecutionEngine();

    private static final AtomicReferenceFieldUpdater<HotfixExecutionEngine, Executable>
            sExecutableUpdater = AtomicReferenceFieldUpdater
            .newUpdater(HotfixExecutionEngine.class, Executable.class, "mExecutable");

    private volatile Executable mExecutable;

    @Override
    public final boolean isExecuteThat(Executable executable) {
        return executable == mExecutable;
    }

    @Override
    public final void apply(Patch patch,String cacheDir) throws Throwable {
//        Executable executable = (Executable) ReflectFinder
//                .constructorBy(executableType, new Class[]{DynamicExecutionEngine.class})
//                .newInstance(this);
//        sExecutableUpdater.set(this, executable);
    }

    @Override
    public final Object hook(ProceedingJoinPoint joinPoint) throws Throwable {
        Executable executable = mExecutable;
        return executable != null
                ? executable.receiveInvoke(joinPoint)
                : joinPoint.proceed();
    }

    @Override
    public Class typeOf(String name) throws Throwable {
        Executable executable = mExecutable;
        if (executable != null) {
            try {
                return Class.forName(name, true, executable.getClass().getClassLoader());
            } catch (ClassNotFoundException ignored) {
            }
        }
        return super.typeOf(name);
    }

    @Override
    public final Object invoke(
            Class type,
            String name,
            Class[] pramsTypes,
            Object target,
            Object[] prams)
            throws Throwable {
        Executable executable = mExecutable;
        return executable == null
                ? super.invoke(type, name, pramsTypes, target, prams)
                : executable.receiveInvoke(type, name, pramsTypes, target, prams);
    }

    @Override
    public final Object access(
            Class type,
            String name,
            Object target)
            throws Throwable {
        Executable executable = mExecutable;
        return executable == null
                ? super.access(type, name, target)
                : executable.receiveAccess(type, name, target);
    }

    @Override
    public final void modify(
            Class type,
            String name,
            Object target,
            Object newValue)
            throws Throwable {
        Executable executable = mExecutable;
        if (executable == null) {
            super.modify(type, name, target, newValue);
        } else {
            executable.receiveModify(type, name, target, newValue);
        }
    }
}