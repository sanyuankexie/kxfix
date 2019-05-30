package org.keixe.android.hotfix.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import androidx.annotation.RestrictTo;

@Aspect
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class HotfixDispatcher {

    private static final String TAG = "HotfixDispatcher";

    /**
     * 核心AspectJ表达式
     * <code>
     * execution(* (@org.keixe.android.hotfix.Hotfix *).*(..))
     * ||execution((@org.keixe.android.hotfix.Hotfix *).new(..))
     * ||staticinitialization((@org.keixe.android.hotfix.Hotfix *))
     * </code>
     * 匹配使用了{@link org.keixe.android.hotfix.Hotfix}注解的类型
     * 不能使用within拦截所有调用,这样插入的代码太多了
     * 所以只拦截方法调用,构造函数,静态初始化块这三个部分
     * 因为新增的方法只能以原本已经存在的方法作为入口点
     * 所以可以直接记录id即可
     */
    @Around("execution(* (@org.keixe.android.hotfix.Hotfix *).*(..))" +
            "||execution((@org.keixe.android.hotfix.Hotfix *).new(..))" +
            "||staticinitialization((@org.keixe.android.hotfix.Hotfix *))")
    public final Object dispatchInvoke(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
