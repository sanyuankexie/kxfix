package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.CodeSignature;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import androidx.annotation.Keep;

/**
 * @author Luke
 *
 * 热补丁基于下面几个事实:
 * 1.被修复的方法是可以被AspectJ拦截的
 * 2.新增的方法只会在被修复的方法中出现
 * 3.新增的字段只会在被修复或者新增的方法中出现
 * 4.新增字段若未初始化则默认为null
 *
 * 支持的功能:
 * 1.支持修复方法,基于事实1
 * 2.支持增加方法,基于事实2
 * 3.支持增加字段,基于事实3
 *
 * 不支持的功能:
 * 1.修改原始已经打包进去了的字段,考虑过实现,但问题过多弃用
 *
 * 特别警告:
 * 1.热补丁也是有极限的,它更多的是提供修复功能,不能用它完全替代发版
 * 2.因为事实4的存在,所以功能3是危险的,在使用时请确认你知道自己在干什么
 * 3.被修复或者新增的字段在运行时是无类型的
 * 4.由于初始化逻辑不一定得到执行,所以新字段可能会造成空指针异常
 * 5.添加的字段随补丁上线下线,若补丁下线,则字段会变成重新变成空
 *
 * @see DynamicExecutor
 * 可执行体
 */
@Keep
abstract class Executable {

    /**
     * 对于字段表来说,读取的概率远大于写的概率,并且两者的粒度都非常小
     * 所以使用读写锁来做并发优化
     */
    private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();

    /**
     * 被修复的字段随{@link Executable}生命周期存在
     * 使用{@link ConcurrentHashMap}保存字段映射
     * 防止在JDK<1.8时多线程的情况下{@link HashMap}出现死循环的问题
     * 但是这样也并没有保证可见性和原子性,符合Java的默认行为
     * 字段采用宽松类型约束,不保存类型信息,编译时做检查,运行时不做检查
     * 对于字段表来说,读取的概率远大于写的概率,并且两者的粒度都非常小
     * 所以使用读写锁来做并发优化
     */
    private WeakHashMap<Object, ConcurrentHashMap<String, Object>>
            mWeakRefFieldCache = new WeakHashMap<>();

    private final DynamicExecutionEngine mDynamicExecutionEngine;

    /**
     * 元数据
     */
    private final Metadata mMetadata;

    protected Executable(DynamicExecutionEngine dynamicExecutionEngine) {
        this.mDynamicExecutionEngine = dynamicExecutionEngine;
        mMetadata = onLoaded();
    }

    //----------------------暴露的接口----------------------------

    final Object receiveInvoke(ProceedingJoinPoint joinPoint)
            throws Throwable {
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        Class type = codeSignature.getDeclaringType();
        String name = codeSignature.getName();
        Class[] pramsTypes = codeSignature.getParameterTypes();
        String signature = mMetadata.hasMethod(type, name, pramsTypes);
        return signature != null ? invokeDynamicMethod(
                signature, joinPoint.getTarget(),
                joinPoint.getArgs())
                : joinPoint.proceed();
    }

    final Object receiveInvoke(Class type,
                               String name,
                               Class[] pramsTypes,
                               Object target,
                               Object[] prams)
            throws Throwable {
        String signature = mMetadata.hasMethod(type, name, pramsTypes);
        if (signature != null) {
            return invokeDynamicMethod(signature, target, prams);
        } else {
            if (mDynamicExecutionEngine.isExecuteThat(this)) {
                return ReflectExecutionEngine.JVM.invoke(type, name, pramsTypes, target, prams);
            } else {
                return mDynamicExecutionEngine.invoke(type, name, pramsTypes, target, prams);
            }
        }
    }

    final Object receiveAccess(Class type,
                               String name,
                               Object o)
            throws Throwable {
        if (mMetadata.hasField(type, name)) {
            return myTable(type, o).get(name);
        } else {
            if (mDynamicExecutionEngine.isExecuteThat(this)) {
                throw new NoSuchFieldException();
            } else {
                return mDynamicExecutionEngine.access(type, name, o);
            }
        }
    }

    final void receiveModify(Class type,
                             String name,
                             Object o,
                             Object newValue)
            throws Throwable {
        if (mMetadata.hasField(type, name)) {
            myTable(type, o).put(name, newValue);
        } else {
            if (mDynamicExecutionEngine.isExecuteThat(this)) {
                throw new NoSuchFieldException();
            } else {
                mDynamicExecutionEngine.modify(type, name, o, newValue);
            }
        }
    }

    //------------------------------生命周期----------------------------

    protected abstract Metadata onLoaded();

    //------------------------------内部使用函数----------------------------

    /**
     * 所有的方法都放置在此方法的实现内
     * 并使用id索引,内部使用switch走不同方法
     * 字段访问和方法调用使用{@link ExecutionEngine}所定义的指令执行
     *
     * @param signature 直接索引,不需要走{@link ExecutionEngine}
     */
    protected abstract Object invokeDynamicMethod(
            String signature,
            Object target,
            Object[] prams)
            throws Throwable;

    ExecutionEngine getExecutionEngine() {
        return mDynamicExecutionEngine;
    }

    private Map<String, Object> myTable(Class type, Object o) {
        if (o == null) {
            o = type;
        }
        mLock.readLock().lock();
        ConcurrentHashMap<String, Object> cache = mWeakRefFieldCache.get(o);
        mLock.readLock().unlock();
        if (cache == null) {
            mLock.writeLock().lock();
            cache = mWeakRefFieldCache.get(o);
            if (cache == null) {
                cache = new ConcurrentHashMap<>();
                mWeakRefFieldCache.put(o, cache);
            }
            mLock.writeLock().unlock();
        }
        return cache;
    }
}