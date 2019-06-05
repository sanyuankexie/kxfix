package org.kexie.android.hotfix.internal;

import org.kexie.android.hotfix.Patch;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ExecutableLoader {

    void apply(Patch patch, String cacheDir) throws Throwable;

    boolean isExecuteThat(Executable executable);

    ExecutableLoader INSTANCE = HotfixExecutionEngine.INSTANCE;
}
