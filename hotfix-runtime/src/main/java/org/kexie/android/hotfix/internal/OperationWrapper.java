package org.kexie.android.hotfix.internal;

class OperationWrapper extends Operation {

    private final Operation inner;

    OperationWrapper(Operation inner) {
        this.inner = inner;
    }

    Operation getInner() {
        return inner;
    }

    @Override
    public Class typeOf(String name) throws Throwable {
        return inner.typeOf(name);
    }

    @Override
    public Object invoke(Class type, String name, Class[] pramsTypes, Object target, Object[] prams) throws Throwable {
        return inner.invoke(type, name, pramsTypes, target, prams);
    }

    @Override
    public Object access(Class type, String name, Object target) throws Throwable {
        return inner.access(type, name, target);
    }

    @Override
    public void modify(Class type, String name, Object target, Object newValue) throws Throwable {
        inner.modify(type, name, target, newValue);
    }

    @Override
    public Object InvokeNonVirtual(Class type, String name, Class[] pramTypes, Object target, Object[] prams) throws Throwable {
        return inner.InvokeNonVirtual(type, name, pramTypes, target, prams);
    }

    @Override
    public Object newInstance(Class<?> type, Class[] pramTypes, Object[] prams) throws Throwable {
        return inner.newInstance(type, pramTypes, prams);
    }
}
