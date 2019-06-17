package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class RedirectTarget {
    public RedirectTarget() {
    }

    Object target;
}
