package org.kexie.android.hotfix.internal;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface CodeScopeManager {
    boolean isThat(CodeScope codeScope);

    void apply(Context context, String path) throws Throwable;

    CodeScopeManager INSTANCE = DynamicExecutionEngine.INSTANCE;
}
