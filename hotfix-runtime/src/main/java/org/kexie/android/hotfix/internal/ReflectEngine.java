package org.kexie.android.hotfix.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import androidx.annotation.Keep;

/**
 * JVM反射执行引擎
 * 由于Java API的限制,所以部分使用JNI实现
 * 但是完全没有平台依赖,只使用C++11,是可移植并对Rom无要求的
 */
@Keep
final class ReflectEngine extends ExecutionEngine {

    ReflectEngine() {
    }

    static {
        System.loadLibrary("jni-reflect");
    }

    /**
     * 使用默认{@link ClassLoader}加载类
     */
    @Override
    public Class typeOf(String name) throws Throwable {
        return Class.forName(name);
    }

    @Override
    public final Object newInstance(Class<?> type,
                                    Class[] pramTypes,
                                    Object[] prams)
            throws Throwable {
        return ReflectFinder.findConstructor(type, pramTypes)
                .newInstance(prams);
    }

    @Override
    public void modify(Class type,
                       String name,
                       Object target,
                       Object newValue)
            throws Throwable {
        ReflectFinder.findField(type, name)
                .set(target, newValue);
    }

    public Object access(Class type,
                         String name,
                         Object target
    ) throws Throwable {
        return ReflectFinder.findField(type, name)
                .get(target);
    }

    /**
     * 实现invoke执行原言
     * 分为两种调用模式:
     * 1.一种是使用虚函数,与Java默认行为保持一致
     * 2.一种是不使用虚函数,根据类型调用类型上声明的方法,主要是为invoke-super指令准备
     *
     * @param nonVirtual 是否为非虚调用
     */
    @Override
    public Object invoke(boolean nonVirtual,
                         Class type,
                         String name,
                         Class[] pramTypes,
                         Object target,
                         Object[] prams) throws Throwable {
        Method method = ReflectFinder.findMethod(type, name, pramTypes);
        int modifiers = method.getModifiers();
        if (nonVirtual) {
            //非虚方法调用时,应该满足以下条件
            if (!Modifier.isFinal(modifiers) && !Modifier.isStatic(modifiers)
                    && !Modifier.isAbstract(modifiers)
                    && !Modifier.isPrivate(modifiers)) {
                Class returnType = method.getReturnType();
                return invokeNonVirtual(type, method, pramTypes, returnType, target, prams);
            } else {
                //否则就是补丁生成插件有bug
                throw new AssertionError("Patch Plugin has bugs!!!");
            }
        } else {
            return method.invoke(target, prams);
        }
    }

    /**
     * 跳转到JNI
     * 使用JNIEnv->CallNonvirtual[TYPE]Method实现
     * 主要是为了实现invoke-super指令
     * 允许抛出异常,在native捕获之后抛出到java层
     */
    private static native Object
    invokeNonVirtual(Class type,
                     Method method,
                     Class[] pramTypes,
                     Class returnType,
                     Object object,
                     Object[] prams
    ) throws Throwable;

}
