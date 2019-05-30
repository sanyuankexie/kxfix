package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.FieldSignature;
import org.aspectj.lang.reflect.InitializerSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.HashSet;

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


    //------------------------热补丁的元数据-------------------------------

    private final FixedObjectManager mFixedObjectManager;

    /**
     * 需要补丁为其开辟额外空间的类型
     */
    private final HashSet<Class> mRequestCacheTypes = new HashSet<>();

    /**
     * 被修复的可执行代码段和字段的集合
     * 包括
     * {@link java.lang.reflect.Method}对应方法
     * {@link java.lang.reflect.Constructor}对应实例构造器
     * {@link java.lang.reflect.Field}对应字段
     * {@link Class}对应静态初始化块
     * 每个被修复的方法和字段对应一个补丁内的一个id
     * 这个集合对id不是一个满射
     * 新增的方法和字段不在此集合中保存
     */
    private final HashMap<AnnotatedElement, Integer> mFixedMemberIds = new HashMap<>();

    protected Patch(FixedObjectManager fixedObjectManager) {
        this.mFixedObjectManager = fixedObjectManager;
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
        return mFixedObjectManager.getObjectFieldCache(target).onGet(fieldId);
    }

    @SuppressWarnings("WeakerAccess")
    protected void setWithId(int fieldId, Object target, Object newValue) {
        mFixedObjectManager.getObjectFieldCache(target).onSet(fieldId, newValue);
    }

    //------------------------------包级函数------------------------------------

    Integer mappingToId(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        AnnotatedElement marker = null;
        if (signature instanceof ConstructorSignature) {
            marker = ((ConstructorSignature) signature).getConstructor();
        } else if (signature instanceof MethodSignature) {
            marker = ((MethodSignature) signature).getMethod();
        } else if (signature instanceof FieldSignature) {
            marker = ((FieldSignature) signature).getField();
        } else if (signature instanceof InitializerSignature) {
            marker = signature.getDeclaringType();
        }
        return marker == null ? null : mFixedMemberIds.get(marker);
    }

    boolean isRequestCacheType(Class type) {
        return mRequestCacheTypes.contains(type);
    }

}