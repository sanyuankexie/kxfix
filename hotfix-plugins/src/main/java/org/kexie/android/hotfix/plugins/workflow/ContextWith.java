package org.kexie.android.hotfix.plugins.workflow;

/**
 * 携带数据的{@link Context}
 */
final class ContextWith<T> extends ContextWrapper {
    private final T data;

    ContextWith(Context context, T data) {
        super(context);
        this.data = data;
    }

    T getData() {
        return data;
    }
}
