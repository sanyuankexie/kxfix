package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;

@Keep
final class CodeScopeClassLoader extends DexClassLoader {
    /**
     * parent设置成BootClassLoader
     * 保证能够正确加载启动类型
     */
    CodeScopeClassLoader(String dexPath, String cacheDir) {
        super(dexPath, cacheDir, null,
                Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                //先尝试从自己进行加载
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                //失败了转换为默认类加载器
                c = getParent().loadClass(name);
            }
        }
        return c;
    }

    @Override
    public String findLibrary(String name) {
        String result = super.findLibrary(name);
        ClassLoader parent;
        if (result == null && (parent = getParent()) instanceof BaseDexClassLoader) {
            result = ((BaseDexClassLoader) parent).findLibrary(name);
        }
        return result;
    }
}
