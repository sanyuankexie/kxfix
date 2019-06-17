package org.kexie.android.hotfix.plugins.workflow;

final class TypeNames {

    static final String CODE_SCOPE_CLASS_NAME
            = "org.kexie.android.hotfix.internal.Overload$CodeScope";
    static final String CODE_SCOPE_SUPER_CLASS_NAME
                    = "org.kexie.android.hotfix.internal.CodeScope";

    static final String OBJECT_SUPER_CLASS_NAME
            = "org.kexie.android.hotfix.internal.OverloadObject";

    static final String UTIL_CLASS_NAME
            = "org.kexie.android.hotfix.internal.Intrinsics";

    static final String HOTFIX_ANNOTATION = "org.kexie.android.hotfix.Hotfix";
    static final String OVERLOAD_ANNOTATION = "org.kexie.android.hotfix.Overload";

    private TypeNames() {
        throw new AssertionError();
    }
}
