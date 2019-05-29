package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 热补丁基于下面几个事实:
 * 1.被修复的方法是可以被AspectJ拦截的
 * 2.新增的方法只会在被修复的方法中出现
 * 3.被修复的字段是可以被AspectJ拦截的
 * 4.只有声明类型不同时字段才视为需要修复,此时上线补丁后原有的字段不再使用,新字段默认为null
 * 5.新增的字段只会在被修复或者新增的方法中出现
 */
public abstract class Patch {

    //----------------------------------注册元数据-----------------------------------

    /**
     * @param name 成员的完整路径名称
     */
    protected final void addHotfixTarget(String name, int id) {
        mFixedMemberIds.put(name, id);
    }

    //-------------------------------扩展字段的缓存----------------------------------

    private final ReentrantReadWriteLock mFixedFieldCacheLock = new ReentrantReadWriteLock();

    /**
     * 被修复的字段随{@link Patch}生命周期存在
     * 使用{@link ConcurrentHashMap}防止在多线程的情况下出问题
     */
    private final WeakHashMap<Object, ConcurrentHashMap<Integer, Object>> mFixedFieldCache = new WeakHashMap<>();

    //------------------------热补丁的元数据,只包含修改的部分-----------------

    /**
     * 被修复的可执行代码段和字段的集合
     * 包括{@link java.lang.reflect.Method},{@link java.lang.reflect.Constructor}和{@link java.lang.reflect.Field}
     * 每个被修复的方法和字段对应一个补丁内的一个id
     * 这个集合对id不是一个满射
     * 新增的方法和字段不在此集合中保存
     * 字段采用宽松类型约束,不保存类型信息,编译时做检查,运行时不做检查
     */
    private final HashMap<String, Integer> mFixedMemberIds = new HashMap<>();

    //-------------------------进入可执行体的外部接口---------------------------

    /**
     * 传入切点并检查补丁是否存在
     * 若存在则修复该函数
     * 否则走正常逻辑
     */
    final Object applyInvoke(ProceedingJoinPoint joinPoint) throws Throwable {
        Integer methodId = mappingToId(joinPoint);
        return methodId == null ? joinPoint.proceed()
                : invokeWithId(methodId, joinPoint.getTarget(), joinPoint.getArgs());
    }

    final Object applyGet(ProceedingJoinPoint joinPoint) throws Throwable {
        Integer id = mappingToId(joinPoint);
        return id == null ? joinPoint.proceed() : getWithId(id, joinPoint.getTarget());
    }

    final void applySet(ProceedingJoinPoint joinPoint) throws Throwable {
        Integer id = mappingToId(joinPoint);
        if (id == null) {
            joinPoint.proceed();
        } else {
            setWithId(id, joinPoint.getTarget(), joinPoint.getArgs()[0]);
        }
    }

    //----------------------在可执行体内部可出现的函数----------------------------

    /**
     * 所有的方法都放置在此方法的实现内
     * 并使用id索引,内部使用switch走不同方法
     * 字段访问和方法调用使用{@link Intrinsics}所定义的指令执行
     * 新增的方法和字段可直接用id索引,不需要走{@link Intrinsics}
     */
    protected abstract Object invokeWithId(int methodId, Object target, Object[] prams) throws Throwable;

    @SuppressWarnings("WeakerAccess")
    protected Object getWithId(int fieldId, Object target) {
        return getOrCreateFieldCache(target).get(fieldId);
    }

    @SuppressWarnings("WeakerAccess")
    protected void setWithId(int fieldId, Object target, Object newValue) {
        getOrCreateFieldCache(target).put(fieldId, newValue);
    }

    //------------------------------私有函数------------------------------------

    private Integer mappingToId(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        return mFixedMemberIds.get(signature.toLongString());
    }

    /**
     * 双重检查
     * 因为读是乐观的,总是读,但是不经常写,故使用读写锁提升效率
     */
    private ConcurrentHashMap<Integer, Object> getOrCreateFieldCache(Object target) {
        mFixedFieldCacheLock.readLock().lock();
        ConcurrentHashMap<Integer, Object> cache = mFixedFieldCache.get(target);
        if (cache == null) {
            mFixedFieldCacheLock.writeLock().lock();
            cache = mFixedFieldCache.get(target);
            if (cache == null) {
                cache = new ConcurrentHashMap<>();
                mFixedFieldCache.put(target, cache);
            }
            mFixedFieldCacheLock.writeLock().unlock();
        }
        mFixedFieldCacheLock.readLock().unlock();
        return cache;
    }
}
