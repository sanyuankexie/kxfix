package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.InitializerSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class Patch {


    protected final void addHotfixTarget(Method method, int id) {
        mFixedExecutableIds.put(method, id);
    }


    /**
     * 被修复的方法集合
     * 包括{@link Method}和{@link java.lang.reflect.Constructor}
     * 每个被修复的方法对应一个补丁内的一个id
     * 这个集合对id不是一个满射
     * 新增的方法不在此集合中保存
     */
    private final Map<Object, Integer> mFixedExecutableIds = new HashMap<>();

    /**
     * 传入切点并检查补丁是否存在
     * 若存在则修复该函数
     * 否则走正常逻辑
     */
    final Object apply(ProceedingJoinPoint joinPoint, Object target, Object[] prams) throws Throwable {
        Signature signature = joinPoint.getSignature();
        Object executable = null;
        if (signature instanceof MethodSignature) {
            executable = ((MethodSignature) signature).getMethod();
        } else if (signature instanceof ConstructorSignature) {
            executable = ((ConstructorSignature) signature).getConstructor();
        } else if (signature instanceof InitializerSignature) {
            executable = ((InitializerSignature) signature).getInitializer();
        }
        Integer methodId = executable != null ? mFixedExecutableIds.get(executable) : null;
        return methodId == null ? joinPoint.proceed() : apply(methodId, target, prams);
    }

    /**
     * 所有的方法都放置在此方法的实现内
     * 并使用id索引,内部使用switch走不同方法
     * 字段访问和方法调用使用{@link Intrinsics}所定义的指令执行
     */
    protected abstract Object apply(int methodId, Object target, Object[] prams) throws Throwable;
}
