package org.kexie.android.hotfix.internal;

import androidx.annotation.RestrictTo;

@SuppressWarnings("WeakerAccess")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class HotCodeExecutor extends CodeContextWrapper {

    public HotCodeExecutor() {
        super(null);
    }

    //protected Target target;

    abstract void receiveModifyById(int id, Object newValue);

    abstract Object receiveAccessById(int id);

    abstract Object receiveInvokeById(int id, Object[] args);

}
