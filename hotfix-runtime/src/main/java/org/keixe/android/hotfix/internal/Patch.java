package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.InitializerSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
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
 */
public abstract class Patch {

    //----------------------------------注册元数据-----------------------------------


    //------------------------热补丁的元数据---------------------------------

    private final ReentrantReadWriteLock mFieldCacheLock = new ReentrantReadWriteLock();

    /**
     * 被修复的字段随{@link Patch}生命周期存在
     * 使用{@link ConcurrentHashMap}保存字段映射
     * 防止在JDK<1.8时多线程的情况下{@link HashMap}出现死循环的问题
     * 但是这样也并没有保证可见性和原子性,符合Java的默认行为
     * 字段采用宽松类型约束,不保存类型信息,编译时做检查,运行时不做检查
     */
    private final WeakHashMap<Object, ConcurrentHashMap<Integer, Object>> mFieldCache = new WeakHashMap<>();

    /**
     * 被修复的可执行代码段的集合
     * 包括:
     * 1.{@link java.lang.reflect.Method}对应方法
     * 2.{@link java.lang.reflect.Constructor}对应实例构造器
     * 3.{@link java.lang.Class}对应静态初始化块
     * <p>
     * 每个被修复方法,新增的方法,新增的字段都对应一个补丁内的一个id
     *
     * @see Patch#mFixedEntryIds 这个集合对id不是一个满射
     * 新增的方法和字段不在此集合中保存,这个只包含了入口
     */
    private final HashMap<AnnotatedElement, Integer> mFixedEntryIds = new HashMap<>();

    //------------------------外部入口-----------------------------------------

    final Object apply(ProceedingJoinPoint joinPoint) throws Throwable {
        Integer id = mappingToId(joinPoint.getSignature());
        return id != null ? invokeWithId(id, joinPoint.getTarget(), joinPoint.getArgs()) : joinPoint.proceed();
    }

    //----------------------在可执行体内部可出现的函数----------------------------

    /**
     * 所有的方法都放置在此方法的实现内
     * 并使用id索引,内部使用switch走不同方法
     * 字段访问和方法调用使用{@link Intrinsics}所定义的指令执行
     * 新增的方法和字段可直接用id索引,不需要走{@link Intrinsics}
     */
    protected abstract Object invokeWithId(int methodId, Object target, Object[] prams) throws Throwable;

    protected final Object accessWithId(int fieldId, Object o) {
        return getFieldCacheBy(o).get(fieldId);
    }

    protected final void modifyWithId(int fieldId, Object o, Object newValue) {
        getFieldCacheBy(o).put(fieldId, newValue);
    }

    //------------------------------私有函数------------------------------------

    private ConcurrentHashMap<Integer, Object> getFieldCacheBy(Object o) {
        mFieldCacheLock.readLock().lock();
        ConcurrentHashMap<Integer, Object> cache = mFieldCache.get(o);
        if (cache == null) {
            mFieldCacheLock.writeLock().lock();
            cache = mFieldCache.get(o);
            if (cache == null) {
                cache = new ConcurrentHashMap<>();
                mFieldCache.put(o, cache);
            }
            mFieldCacheLock.writeLock().unlock();
        }
        mFieldCacheLock.readLock().unlock();
        return cache;
    }

    private Integer mappingToId(Signature signature) {
        AnnotatedElement marker = null;
        if (signature instanceof ConstructorSignature) {
            marker = ((ConstructorSignature) signature).getConstructor();
        } else if (signature instanceof MethodSignature) {
            marker = ((MethodSignature) signature).getMethod();
        } else if (signature instanceof InitializerSignature) {
            marker = signature.getDeclaringType();
        }
        return marker == null ? null : mFixedEntryIds.get(marker);
    }
}