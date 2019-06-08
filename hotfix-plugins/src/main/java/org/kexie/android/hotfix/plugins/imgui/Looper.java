package org.kexie.android.hotfix.plugins.imgui;

import org.kexie.android.hotfix.plugins.workflow.Context;

public class Looper {
    private final Context context;

    private Looper(Context context) {
        this.context = context;

    }

    public static Looper make(Context context) {
        return new Looper(context);
    }

    public void loop() {

    }
}
