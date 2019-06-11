package org.kexie.android.hotfix.plugins.workflow;

final class Annotations {
    private Annotations() {
        throw new AssertionError();
    }

    static final String HOTFIX_ANNOTATION = "org.kexie.android.hotfix.Hotfix";
    static final String OVERLOAD_ANNOTATION = "org.kexie.android.hotfix.Overload";
    static final String ID_ANNOTATION = "org.kexie.android.hotfix.internal.Id";
}
