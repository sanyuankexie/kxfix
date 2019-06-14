package org.kexie.android.hotfix.internal;

public class CodeScopeTest extends CodeScope {
    @Override
    Class[] onLoadClasses(CodeContext context) {
        return new Class[0];
    }
}
