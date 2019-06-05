package org.kexie.android.hotfix;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import org.kexie.android.hotfix.internal.DynamicExecutor;

import java.io.File;

import androidx.annotation.Keep;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;
import dalvik.system.DexClassLoader;

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
                    Class patchType = loadType(patch);
                    DynamicExecutor.INSTANCE.apply(patchType);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @WorkerThread
    private Class loadType(Patch patch) throws Throwable {
        File optimizedDirectory = mContext.getDir("patched", Context.MODE_PRIVATE);
        DexClassLoader classLoader = new DexClassLoader(
                patch.getDexPath().getAbsolutePath(),
                optimizedDirectory.getAbsolutePath(),
                null,
                null);
        return classLoader.loadClass(PATCH_CLASS_NAME + patch.getUUID());
    }
}
