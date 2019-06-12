package org.kexie.android.hotfix.internal;

import android.content.Context;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import androidx.annotation.Keep;

@Keep
class CodeScopeContext
        extends CodeContextWrapper
        implements CodeScopeManager {

    private static final AtomicReferenceFieldUpdater<CodeScopeContext, CodeScope>
            sCodeScopeUpdater = AtomicReferenceFieldUpdater
            .newUpdater(CodeScopeContext.class, CodeScope.class, "codeScope");

    private static final String CODE_SCOPE_TYPE_NAME
            = "org.kexie.android.hotfix.internal.Overload-CodeScope";

    volatile CodeScope codeScope;

    CodeScopeContext(CodeContext base) {
        super(base);
    }

    protected void load(Context context, String path) throws Throwable {
        String cacheDir = context.getApplicationContext()
                .getDir("hotfix", Context.MODE_PRIVATE)
                .getAbsolutePath();
        CodeScopeClassLoader classLoader = new CodeScopeClassLoader(path, cacheDir);
        CodeScope codeScope = (CodeScope) classLoader
                .loadClass(CODE_SCOPE_TYPE_NAME)
                .newInstance();
        codeScope.load(this);
        sCodeScopeUpdater.set(this, codeScope);
    }

    @Override
    public final boolean isThat(CodeScope codeScope) {
        return codeScope == this.codeScope;
    }
}
