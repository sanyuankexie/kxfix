package org.kexie.android.hotfix.internal;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ExecutableLoader {

    void load(Context context, String dexPath) throws Throwable;

    boolean isExecuteThat(Executable executable);

    ExecutableLoader INSTANCE = HotfixExecutionEngine.INSTANCE;
}
