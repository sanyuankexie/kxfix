package org.kexie.android.hotfix.internal;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import androidx.annotation.Keep;

@Keep
class CodeContextScopedWrapper
        extends CodeContextWrapper
        implements CodeScopeManager {

    private static final AtomicReferenceFieldUpdater<CodeContextScopedWrapper, CodeScope>
            sCodeScopeUpdater = AtomicReferenceFieldUpdater
            .newUpdater(CodeContextScopedWrapper.class, CodeScope.class, "codeScope");

    private static final String CODE_SCOPE_TYPE_NAME
            = "org.kexie.android.hotfix.internal.Overload-CodeScope";

    volatile CodeScope codeScope;

    CodeContextScopedWrapper(CodeContext base) {
        super(base);
    }

    void loadCode(String cacheDir, String path) throws Throwable {
        CodeScopeClassLoader classLoader = new CodeScopeClassLoader(path, cacheDir);
        CodeScope codeScope = (CodeScope) classLoader
                .loadClass(CODE_SCOPE_TYPE_NAME)
                .newInstance();
        codeScope.loadClasses(this);
        sCodeScopeUpdater.set(this, codeScope);
    }

    @Override
    public final boolean isThatScope(CodeScope codeScope) {
        return codeScope == this.codeScope;
    }
}
