package org.kexie.android.hotfix.internal;

import java.lang.reflect.Constructor;

import androidx.annotation.Keep;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;

@Keep
final class DomainClassLoader extends DexClassLoader {

    private static final String DOMAIN_TYPE_NAME
            = "org.kexie.android.hotfix.internal.Overload$Domain";

    private final Domain domain;

    /**
     * 保证能够正确加载启动类型
     */
    DomainClassLoader(String dexPath, String cacheDir, ClassLoader parent) throws Exception {
        super(dexPath, cacheDir, null, parent);
        Class<?> clazz = loadClass(DOMAIN_TYPE_NAME);
        Constructor<?> constructor = clazz.getConstructor();
        constructor.setAccessible(true);
        domain = (Domain) constructor.newInstance();
    }

    Domain getDomain() {
        return domain;
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
        ClassLoader parent = getParent();
        if (parent != null) {
            if (parent instanceof BaseDexClassLoader) {
                result = ((BaseDexClassLoader) parent)
                        .findLibrary(name);
            } else {
                try {
                    result = (String) Intrinsics.invoke(
                            false,
                            parent.getClass(),
                            "findLibrary",
                            null,
                            parent,
                            null
                    );
                } catch (Throwable ignored) {

                }
            }
        }
        return result;
    }
}
