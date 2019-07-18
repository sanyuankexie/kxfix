package org.kexie.android.hotfix;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.Keep;
import androidx.annotation.MainThread;

import org.kexie.android.hotfix.internal.PatchLoader;

@Keep
public final class HotfixManager {

    private final HandlerThread workThread;
    private final Handler handler;

    public HotfixManager() {
        workThread = new HandlerThread("Patch Loader Thread");
        workThread.start();
        handler = new Handler(workThread.getLooper());
    }

    @MainThread
    public void load(final Context context, final String path) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    PatchLoader.INSTANCE.load(context, path);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
