package org.kexie.android.hotfix;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;


import org.kexie.android.hotfix.internal.PatchLoader;

import androidx.annotation.Keep;
import androidx.annotation.MainThread;

@Keep
public final class HotfixManager {

    private final Context context;
    private final HandlerThread workThread;
    private final Handler handler;

    public HotfixManager(Context context) {
        this.context = context;
        workThread = new HandlerThread("Patch Loader Thread");
        workThread.start();
        handler = new Handler(workThread.getLooper());
    }

    @MainThread
    public void apply(final Patch patch) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    PatchLoader.INSTANCE.load(context, patch.getDexPath());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
