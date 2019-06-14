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
    final void setBaseContext(CodeContext baseContext) {
        super.setBaseContext(baseContext);
    }

    @Override
    public Object invoke(boolean nonVirtual,
                         Class type,
                         String name,
                         Class[] pramsTypes,
                         Object target,
                         Object[] prams) throws Throwable {
        return super.invoke(nonVirtual, type, name, pramsTypes, target, prams);
    }

    @Override
    final CodeContext getBaseContext() {
        return super.getBaseContext();
    }

    @Override
    public final Object newInstance(Class<?> type,
                                    Class[] pramTypes,
                                    Object[] prams) throws Throwable {
        return super.newInstance(type, pramTypes, prams);
    }

    @Override
    public final Class typeOf(String name) throws Throwable {
        return super.typeOf(name);
    }

    @Override
    public final void modify(Class type,
                             String name,
                             Object target,
                             Object newValue) throws Throwable {
        super.modify(type, name, target, newValue);
    }

    @Override
    public final Object access(Class type,
                               String name,
                               Object target) throws Throwable {
        return super.access(type, name, target);
    }

    final void setTarget(Object target) {
        this.target = target;
    }
}
