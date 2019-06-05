package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ExecutableLoader {

    void apply(String dexPath, String cacheDir) throws Throwable;

    boolean isExecuteThat(Executable executable);

    ExecutableLoader INSTANCE = HotfixExecutionEngine.INSTANCE;
}