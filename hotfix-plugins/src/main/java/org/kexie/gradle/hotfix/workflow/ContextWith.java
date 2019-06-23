package org.kexie.gradle.hotfix.workflow;

/**
 * 携带数据的{@link Context}
 */
final class ContextWith<D> extends ContextWrapper {
    private final D data;

    ContextWith(Context context, D data) {
        super(context);
        this.data = data;
    }

    D getData() {
        return data;
    }
}
