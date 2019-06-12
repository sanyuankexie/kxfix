package org.kexie.android.hotfix.internal;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface PatchLoader {

    void load(Context context, String path) throws Throwable;

    PatchLoader INSTANCE = HotfixEngine.INSTANCE;
}
