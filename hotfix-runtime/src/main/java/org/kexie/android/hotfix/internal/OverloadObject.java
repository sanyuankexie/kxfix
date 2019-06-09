package org.kexie.android.hotfix.internal;

import androidx.annotation.RestrictTo;

@SuppressWarnings("WeakerAccess")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class OverloadObject extends CodeContextWrapper {

    public OverloadObject() { }

    abstract void modifyWithId(int id, Object o, Object newValue);

    abstract Object accessWithId(int id, Object o);

    abstract Object invokeWithId(int id, Object o, Object[] args);

}
