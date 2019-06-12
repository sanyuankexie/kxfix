package org.kexie.android.hotfix.internal;

import androidx.annotation.RestrictTo;

@SuppressWarnings("WeakerAccess")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class HotCodeExecutor extends CodeContextWrapper {

    public HotCodeExecutor() {
        super(null);
    }

    abstract void receiveModifyById(int id, Object o, Object newValue);

    abstract Object receiveAccessById(int id, Object o);

    abstract Object receiveInvokeById(int id, Object o, Object[] args);

}
