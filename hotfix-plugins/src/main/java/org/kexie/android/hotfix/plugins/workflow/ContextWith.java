package org.kexie.android.hotfix.plugins.workflow;

public class ContextWith<T> {
    private final Context context;
    private final T input;

    ContextWith(Context context, T input) {
        this.context = context;
        this.input = input;
    }

    public Context getContext() {
        return context;
    }

    public T getInput() {
        return input;
    }
}
