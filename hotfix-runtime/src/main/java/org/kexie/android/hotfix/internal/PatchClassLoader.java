package org.kexie.android.hotfix.internal;

import dalvik.system.DexClassLoader;

/**
 * 如果是补丁类就自己加载,否则交给系统类加载器加载
 */
final class PatchClassLoader extends DexClassLoader {
    PatchClassLoader(String dexPath, String cacheDir) {
        super(dexPath, cacheDir, null, ClassLoader.getSystemClassLoader());
    }
}
