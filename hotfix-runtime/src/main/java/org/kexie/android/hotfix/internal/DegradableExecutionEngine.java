package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;

@Keep
class DegradableExecutionEngine extends ExecutionEngine {

    private final ExecutionEngine lowLevel;

    DegradableExecutionEngine(ExecutionEngine lowLevel) {
        this.lowLevel = lowLevel;
    }

    ExecutionEngine getLowLevel() {
        return lowLevel;
    }

    @Override
    public Class typeOf(String name) throws Throwable {
        return lowLevel.typeOf(name);
    }

    @Override
    public Object invoke(boolean nonVirtual,
                         Class type,
                         String name,
                         Class[] pramsTypes,
                         Object target,
                         Object[] prams) throws
            Throwable {
        return lowLevel.invoke(nonVirtual, type, name, pramsTypes, target, prams);
    }

    @Override
    public Object access(Class type,
                         String name,
                         Object target)
            throws Throwable {
        return lowLevel.access(type, name, target);
    }

    @Override
    public void modify(Class type,
                       String name,
                       Object target,
                       Object newValue)
            throws Throwable {
        lowLevel.modify(type, name, target, newValue);
    }

    @Override
    public Object newInstance(Class<?> type,
                              Class[] pramTypes,
                              Object[] prams)
            throws Throwable {
        return lowLevel.newInstance(type, pramTypes, prams);
    }
}
