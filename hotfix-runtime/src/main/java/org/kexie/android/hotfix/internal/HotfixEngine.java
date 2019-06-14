package org.kexie.android.hotfix.internal;

import android.content.Context;

import org.aspectj.lang.ProceedingJoinPoint;

import androidx.annotation.Keep;

/**
 * 动态化
 */
@Keep
final class HotfixEngine
        extends CodeScopeManager
        implements Hooker,
        PatchLoader {

    static final HotfixEngine INSTANCE = new HotfixEngine();

    private static final String CODE_SCOPE_TYPE_NAME
            = "org.kexie.android.hotfix.internal.Overload-CodeScope";


    private HotfixEngine() {
        super(new ReflectEngine());
    }

    @Override
    public final void load(Context context, String path) throws Throwable {
        String cacheDir = context.getApplicationContext()
                .getDir("hotfix", Context.MODE_PRIVATE)
                .getAbsolutePath();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        contextClassLoader = new CodeScopeClassLoader(path, cacheDir, contextClassLoader);
        CodeScope codeScope = (CodeScope) contextClassLoader
                .loadClass(CODE_SCOPE_TYPE_NAME)
                .newInstance();
        codeScope.loadClasses(this);
        apply(codeScope);
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
        CodeScope codeScope = this.codeScope;
        if (codeScope != null) {
            return Class.forName(name, false, codeScope.getClassLoader());
        }
        return super.typeOf(name);
    }

    @Override
    public final Object invoke(
            boolean nonVirtual,
            Class type,
            String name,
            Class[] pramsTypes,
            Object target,
            Object[] prams)
            throws Throwable {
        CodeScope codeScope = this.codeScope;
        return codeScope == null ? super.invoke(nonVirtual, type, name, pramsTypes, target, prams)
                : codeScope.dispatchInvoke(nonVirtual, type, name, pramsTypes, target, prams);
    }

    @Override
    public final Object access(
            Class type,
            String name,
            Object target)
            throws Throwable {
        CodeScope codeScope = this.codeScope;
        return codeScope == null
                ? super.access(type, name, target)
                : codeScope.dispatchAccess(type, name, target);
    }

    @Override
    public final void modify(
            Class type,
            String name,
            Object target,
            Object newValue)
            throws Throwable {
        CodeScope codeScope = this.codeScope;
        if (codeScope == null) {
            super.modify(type, name, target, newValue);
        } else {
            codeScope.dispatchModify(type, name, target, newValue);
        }
    }
}