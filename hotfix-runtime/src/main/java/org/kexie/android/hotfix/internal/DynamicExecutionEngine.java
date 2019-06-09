package org.kexie.android.hotfix.internal;

import android.content.Context;

import org.aspectj.lang.ProceedingJoinPoint;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import androidx.annotation.Keep;

/**
 * 动态化
 */
@Keep
final class DynamicExecutionEngine
        extends CodeContextWrapper
        implements Hooker,
        CodeScopeManager {

    static final DynamicExecutionEngine INSTANCE = new DynamicExecutionEngine();

    private static final AtomicReferenceFieldUpdater<DynamicExecutionEngine, CodeScope>
            sCodeScopeUpdater = AtomicReferenceFieldUpdater
            .newUpdater(DynamicExecutionEngine.class, CodeScope.class, "mCodeScope");

    private volatile CodeScope mCodeScope;

    private DynamicExecutionEngine() {
        super(new ReflectOperation());
    }

    @Override
    public final boolean isThat(CodeScope codeScope) {
        return codeScope == mCodeScope;
    }

    @Override
    public final void apply(Context context, String path) throws Throwable {
        String cacheDir = context.getApplicationContext()
                .getDir("patched", Context.MODE_PRIVATE)
                .getAbsolutePath();
        CodeScopeClassLoader classLoader = new CodeScopeClassLoader(path, cacheDir);
        CodeScope codeScope = new CodeScope(this, classLoader);
        sCodeScopeUpdater.set(this, codeScope);
    }

    @Override
    public final Object hook(ProceedingJoinPoint joinPoint) throws Throwable {
        CodeScope executable = mCodeScope;
        return executable != null
                ? executable.receiveInvoke(joinPoint)
                : joinPoint.proceed();
    }

    @Override
    public final Class typeOf(String name) throws Throwable {
        CodeScope executable = mCodeScope;
        if (executable != null) {
            return Class.forName(name, false, executable.getClassLoader());
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
        CodeScope executable = mCodeScope;
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
        CodeScope executable = mCodeScope;
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
        CodeScope executable = mCodeScope;
        if (executable == null) {
            super.modify(type, name, target, newValue);
        } else {
            executable.receiveModify(type, name, target, newValue);
        }
    }
}