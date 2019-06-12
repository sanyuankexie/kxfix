package org.kexie.android.hotfix.internal;

public class CodeScopeTest extends CodeScope {

    java.lang.Class[] onLoadClasses()throws java.lang.Throwable {
        return new Class[]{(java.lang.Class.forName("org.kexie.android.hotfix.sample.Overload-MainActivity", false, this.getClassLoader()))};
    }
}
