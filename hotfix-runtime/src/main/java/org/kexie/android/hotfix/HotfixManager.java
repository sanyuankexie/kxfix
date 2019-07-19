package org.kexie.android.hotfix;

import android.app.Application;
import android.content.Context;

import androidx.annotation.Keep;

import org.kexie.android.hotfix.internal.PatchLoader;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Keep
public final class HotfixManager {

    private static final String HOTFIX = "hotfix";

    private static HotfixManager instance;

    private final Application context;

    private final ExecutorService singleTask;

    private HotfixManager(Context context) {
        this.context = (Application) context.getApplicationContext();
        singleTask = Executors.newSingleThreadExecutor();
    }

    public void load(final String path) {
        singleTask.execute(new Runnable() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void run() {
                try {
                    File cacheDir = new File(context.getCacheDir(), HOTFIX);
                    cacheDir.mkdirs();
                    PatchLoader.INSTANCE.load(cacheDir.getAbsolutePath(), path);
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
