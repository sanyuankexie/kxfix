package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface DynamicExecutor {

    void apply(Class executableType) throws Throwable;

    boolean isExecuteThat(Executable executable);

    DynamicExecutor INSTANCE = HotfixExecutionEngine.INSTANCE;
}
