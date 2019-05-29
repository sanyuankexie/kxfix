package org.keixe.android.hotfix.internal;

import android.app.Application;
import android.content.Context;

import java.util.concurrent.atomic.AtomicBoolean;

final class PatchManager {
    private static final AtomicBoolean sInit = new AtomicBoolean(false);
    private static PatchManager sInstance;

    static void init(Context context) {
        if (sInit.compareAndSet(false, true)) {
            Application application = (Application) context.getApplicationContext();
            sInstance = new PatchManager(application);
        }
    }

    public static PatchManager getsInstance() {
        return sInstance;
    }

    private PatchManager(Application application) {
        this.mApplication = application;
    }

    private final Application mApplication;
    private Patch mPatch;



}
