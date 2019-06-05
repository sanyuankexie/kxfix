package org.kexie.android.hotfix.internal;

import dalvik.system.DexClassLoader;

/**
 * 破坏双亲委托模型
 * 优先从自己加载
 * 自己加载失败再尝试从系统类加载器加载
 * {@link ClassLoader#getSystemClassLoader()}
 */
final class HotfixClassLoader extends DexClassLoader {

    /**
     * parent 设置成null
     * 也就是BootClassLoader
     * 保证能够正确加载启动类型
     */
    HotfixClassLoader(String dexPath, String cacheDir) {
        super(dexPath, cacheDir, null, null);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                c = super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                c = ClassLoader.getSystemClassLoader().loadClass(name);
            }
            if (c == null) {
                c = findClass(name);
            }
        }
        return c;
    }
}
