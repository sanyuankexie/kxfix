package org.kexie.android.hotfix.internal;

import androidx.annotation.RestrictTo;

@SuppressWarnings("WeakerAccess")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class HotCodeExecutor extends CodeContextWrapper {

    public HotCodeExecutor() {
        super(null);
    }

    protected Object target;

    abstract void receiveModifyById(int id, Object newValue) throws Throwable;

    abstract Object receiveAccessById(int id) throws Throwable;

    abstract Object receiveInvokeById(int id, Object[] args) throws Throwable;

    @Override
    public Object invoke(boolean nonVirtual,
                         Class type,
                         String name,
                         Class[] pramsTypes,
                         Object target,
                         Object[] prams) throws Throwable {
        return super.invoke(nonVirtual,
                type,
                name,
                pramsTypes,
                target == this ? this.target : target,
                prams);
    }

    void setTarget(Object target) {
        this.target = target;
    }
}
