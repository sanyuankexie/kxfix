package org.kexie.android.hotfix.internal;

import androidx.annotation.RestrictTo;

@SuppressWarnings("WeakerAccess")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class OverloadObject extends OperationWrapper {

    public OverloadObject(Operation inner) {
        super(inner);
    }

    abstract void modifyWithId(int id, Object o, Object newValue);

    abstract Object accessWithId(int id, Object o);

    abstract Object invokeWithId(int id, Object o, Object[] args);

}
