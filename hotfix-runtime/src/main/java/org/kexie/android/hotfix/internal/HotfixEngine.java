package org.kexie.android.hotfix.internal;

import android.content.Context;

import org.aspectj.lang.ProceedingJoinPoint;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import androidx.annotation.Keep;

/**
 * 动态化
 */
@Keep
final class HotfixEngine
        extends CodeContextWrapper
        implements Hooker,
        CodeScopeManager {

    static final HotfixEngine INSTANCE = new HotfixEngine();

    private static final String CODE_SCOPE_TYPE_NAME = "org.kexie.android.hotfix.internal.Overload-CodeScope";

    private static final AtomicReferenceFieldUpdater<HotfixEngine, CodeScope>
            sCodeScopeUpdater = AtomicReferenceFieldUpdater
            .newUpdater(HotfixEngine.class, CodeScope.class, "codeScope");

    private volatile CodeScope codeScope;

    private HotfixEngine() {
        super(new ReflectEngine());
    }

    @Override
    public final boolean isThat(CodeScope codeScope) {
        return codeScope == this.codeScope;
    }

    @Override
    public final void apply(Context context, String path) throws Throwable {
        String cacheDir = context.getApplicationContext()
                .getDir("patched", Context.MODE_PRIVATE)
                .getAbsolutePath();
        CodeScopeClassLoader classLoader = new CodeScopeClassLoader(path, cacheDir);
        CodeScope codeScope = (CodeScope) classLoader
                .loadClass(CODE_SCOPE_TYPE_NAME)
                .newInstance();
        codeScope.init(this);
        sCodeScopeUpdater.set(this, codeScope);
    }

    @Override
    public final Object hook(ProceedingJoinPoint joinPoint) throws Throwable {
        CodeScope codeScope = this.codeScope;
        return codeScope != null
                ? codeScope.dispatchInvoke(joinPoint)
                : joinPoint.proceed();
    }

    @Override
    public final Class typeOf(String name) throws Throwable {
        CodeScope executable = codeScope;
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
        CodeScope executable = codeScope;
        return executable == null
                ? super.invoke(type, name, pramsTypes, target, prams)
                : executable.dispatchInvoke(type, name, pramsTypes, target, prams);
    }

    @Override
    public final Object access(
            Class type,
            String name,
            Object target)
            throws Throwable {
        CodeScope executable = codeScope;
        return executable == null
                ? super.access(type, name, target)
                : executable.dispatchAccess(type, name, target);
    }

    @Override
    public final void modify(
            Class type,
            String name,
            Object target,
            Object newValue)
            throws Throwable {
        CodeScope executable = codeScope;
        if (executable == null) {
            super.modify(type, name, target, newValue);
        } else {
            executable.dispatchModify(type, name, target, newValue);
        }
    }
}