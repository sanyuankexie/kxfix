package org.kexie.android.hotfix.internal;

import android.content.Context;

import org.aspectj.lang.ProceedingJoinPoint;

import androidx.annotation.Keep;

/**
 * 动态化
 */
@Keep
final class HotfixEngine
        extends DomainManageEngine
        implements Hooker,
        PatchLoader {

    static final HotfixEngine INSTANCE = new HotfixEngine();

    private HotfixEngine() {
        super(new ReflectEngine());
    }

    @Override
    public final void load(Context context, String path) throws Throwable {
        String cacheDir = context.getApplicationContext()
                .getDir("hotfix", Context.MODE_PRIVATE)
                .getAbsolutePath();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        classLoader = new DomainClassLoader(path, cacheDir, classLoader);
        Domain domain = ((DomainClassLoader) classLoader).getDomain();
        domain.loadClasses(this);
        apply(domain);
    }

    @Override
    public final Object hook(ProceedingJoinPoint joinPoint) throws Throwable {
        Domain domain = this.domain;
        return domain != null
                ? domain.dispatchInvoke(joinPoint)
                : joinPoint.proceed();
    }

    @Override
    final Class typeOf(String name) throws Throwable {
        Domain domain = this.domain;
        if (domain != null) {
            return Class.forName(name, false, domain.getClassLoader());
        }
        return super.typeOf(name);
    }

    @Override
    final Object invoke(
            boolean nonVirtual,
            Class type,
            String name,
            Class[] pramsTypes,
            Object target,
            Object[] prams)
            throws Throwable {
        Domain domain = this.domain;
        return domain == null ? super.invoke(nonVirtual, type, name, pramsTypes, target, prams)
                : domain.dispatchInvoke(nonVirtual, type, name, pramsTypes, target, prams);
    }

    @Override
    final Object access(
            Class type,
            String name,
            Object target)
            throws Throwable {
        Domain domain = this.domain;
        return domain == null
                ? super.access(type, name, target)
                : domain.dispatchAccess(type, name, target);
    }

    @Override
    final void modify(
            Class type,
            String name,
            Object target,
            Object newValue)
            throws Throwable {
        Domain domain = this.domain;
        if (domain == null) {
            super.modify(type, name, target, newValue);
        } else {
            domain.dispatchModify(type, name, target, newValue);
        }
    }
}