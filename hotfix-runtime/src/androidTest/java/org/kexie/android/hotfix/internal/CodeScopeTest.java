package org.kexie.android.hotfix.internal;

public class CodeScopeTest extends CodeScope {
    @Override
    Class[] loadEntryClasses(ExecutionEngine context) {
        return new Class[0];
    }
}
