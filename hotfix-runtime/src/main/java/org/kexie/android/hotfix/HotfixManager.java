package org.kexie.android.hotfix;

import android.app.Application;
import android.content.Context;

import androidx.annotation.Keep;

import org.kexie.android.hotfix.internal.PatchLoader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Keep
public final class HotfixManager {

    private static HotfixManager instance;

    private final Application context;

    private final ExecutorService singleTask;

    private HotfixManager(Context context) {
        this.context = (Application) context.getApplicationContext();
        singleTask = Executors.newSingleThreadExecutor();
    }

    public void load(final String path) {
        singleTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String cacheDir = context
                            .getDir("hotfix", Context.MODE_PRIVATE)
                            .getAbsolutePath();
                    PatchLoader.INSTANCE.load(cacheDir, path);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static HotfixManager getInstance(Context context) {
        if (instance == null) {
            synchronized (HotfixManager.class) {
                if (instance == null) {
                    instance = new HotfixManager(context);
                }
            }
        }
        return instance;
    }
}
