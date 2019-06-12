package org.kexie.android.hotfix.internal;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import androidx.annotation.Keep;

@Keep
class CodeScopeManager
        extends CodeContextWrapper {

    private static final AtomicReferenceFieldUpdater<CodeScopeManager, CodeScope>
            sCodeScopeUpdater = AtomicReferenceFieldUpdater
            .newUpdater(CodeScopeManager.class, CodeScope.class, "codeScope");

    private static final String CODE_SCOPE_TYPE_NAME
            = "org.kexie.android.hotfix.internal.Overload-CodeScope";

    volatile CodeScope codeScope;

    CodeScopeManager(CodeContext base) {
        super(base);
    }

    void loadCodeScope(String cacheDir, String path) throws Throwable {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        contextClassLoader = new CodeScopeClassLoader(path, cacheDir, contextClassLoader);
        CodeScope codeScope = (CodeScope) contextClassLoader
                .loadClass(CODE_SCOPE_TYPE_NAME)
                .newInstance();
        codeScope.loadClasses(this);
        sCodeScopeUpdater.set(this, codeScope);
    }

    final boolean isThatScope(CodeScope codeScope) {
        return codeScope == this.codeScope;
    }
}
