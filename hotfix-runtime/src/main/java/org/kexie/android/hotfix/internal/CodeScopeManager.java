package org.kexie.android.hotfix.internal;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import androidx.annotation.Keep;

@Keep
class CodeScopeManager
        extends CodeContextWrapper {

    private static final AtomicReferenceFieldUpdater<CodeScopeManager, CodeScope>
            sCodeScopeUpdater = AtomicReferenceFieldUpdater
            .newUpdater(CodeScopeManager.class, CodeScope.class, "codeScope");

    volatile CodeScope codeScope;

    CodeScopeManager(CodeContext base) {
        super(base);
    }

    void apply(CodeScope codeScope) {
        sCodeScopeUpdater.set(this, codeScope);
    }

    final boolean isThatScope(CodeScope codeScope) {
        return codeScope == this.codeScope;
    }
}
