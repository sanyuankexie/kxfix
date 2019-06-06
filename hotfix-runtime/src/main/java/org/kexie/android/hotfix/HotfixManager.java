package org.kexie.android.hotfix;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import org.kexie.android.hotfix.internal.ExecutableLoader;

import androidx.annotation.Keep;
import androidx.annotation.MainThread;

@Keep
public final class HotfixManager {

    private final Context mContext;
    private final HandlerThread mWorkThread;
    private final Handler mHandler;

    private static final String PATCH_CLASS_NAME = "org.kexie.android.hotfix.internal.Executable$";

    public HotfixManager(Context context) {
        this.mContext = context;
        mWorkThread = new HandlerThread("Patch Loader Thread");
        mWorkThread.start();
        mHandler = new Handler(mWorkThread.getLooper());
    }

    @MainThread
    public void apply(final Patch patch) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    ExecutableLoader.INSTANCE.load(mContext, patch.getDexPath());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
