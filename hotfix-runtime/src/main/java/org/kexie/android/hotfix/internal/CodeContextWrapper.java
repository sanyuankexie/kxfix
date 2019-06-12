package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;

@Keep
class CodeContextWrapper extends CodeContext {

    private CodeContext baseContext;

    CodeContextWrapper(CodeContext baseContext) {
        this.baseContext = baseContext;
    }

    void setBaseContext(CodeContext baseContext) {
        this.baseContext = baseContext;
    }

    CodeContext getBaseContext() {
        return baseContext;
    }

    @Override
    public Class typeOf(String name) throws Throwable {
        return baseContext.typeOf(name);
    }

    @Override
    public Object invoke(Class type,
                         String name,
                         Class[] pramsTypes,
                         Object target,
                         Object[] prams) throws
            Throwable {
        return baseContext.invoke(type, name, pramsTypes, target, prams);
    }

    @Override
    public Object access(Class type,
                         String name,
                         Object target)
            throws Throwable {
        return baseContext.access(type, name, target);
    }

    @Override
    public void modify(Class type,
                       String name,
                       Object target,
                       Object newValue)
            throws Throwable {
        baseContext.modify(type, name, target, newValue);
    }

    @Override
    public Object InvokeNonVirtual(Class type,
                                   String name,
                                   Class[] pramTypes,
                                   Object target,
                                   Object[] prams)
            throws Throwable {
        return baseContext.InvokeNonVirtual(type, name, pramTypes, target, prams);
    }

    @Override
    public Object newInstance(Class<?> type,
                              Class[] pramTypes,
                              Object[] prams)
            throws Throwable {
        return baseContext.newInstance(type, pramTypes, prams);
    }
}
