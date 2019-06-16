package org.kexie.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;
/**
 * @author Luke
 * 用于转发调用的切面
 */

@Keep
@Aspect
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class InvokeDispatcher {

    /**
     * 匹配使用了{@link org.kexie.android.hotfix.Hotfix}注解的类型
     * 不能使用within拦截所有调用,这样插入的代码太多了
     * 所以只拦截方法调用,构造函数,静态初始化块这三个部分
     * 因为新增的方法只能以原本已经存在的方法作为入口点
     * 所以可以直接记录id即可
     */
    @Around("execution(* (@org.kexie.android.hotfix.Hotfix *).*(..))" +
            "||execution((@org.kexie.android.hotfix.Hotfix *).new(..))")
    public final Object dispatchInvoke(ProceedingJoinPoint joinPoint) throws Throwable {
        return Hooker.INSTANCE.hook(joinPoint);
    }
}
