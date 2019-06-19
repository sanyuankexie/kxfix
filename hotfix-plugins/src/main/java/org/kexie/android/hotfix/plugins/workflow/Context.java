package org.kexie.android.hotfix.plugins.workflow;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import javassist.ClassPool;

/**
 * 任务运行的上下文环境
 * 装饰者模式
 */
public abstract class Context {

    static final String CODE_SCOPE_CLASS_NAME
            = "org.kexie.android.hotfix.internal.Overload$CodeScope";
    static final String CODE_SCOPE_SUPER_CLASS_NAME
            = "org.kexie.android.hotfix.internal.CodeScope";
    static final String OBJECT_SUPER_CLASS_NAME
            = "org.kexie.android.hotfix.internal.RedirectTarget";
    /**
     * 内部指令所使用的工具类名字
     */
    static final String UTIL_CLASS_NAME
            = "org.kexie.android.hotfix.internal.Intrinsics";
    static final String HOTFIX_ANNOTATION = "org.kexie.android.hotfix.Hotfix";
    static final String OVERLOAD_ANNOTATION = "org.kexie.android.hotfix.Overload";
    /**
     * @see <a href="https://developer.android.google.cn/studio/write/java8-support?hl=zh_cn"/>
     * desugar 处理过的lambda表达式的前缀
     */
    static final String LAMBDA_CLASS_NAME_PREFIX = "-$$Lambda$";
    static final String LAMBDA_METHOD_NAME_PREFIX = "lambda$";

    abstract Project getProject();

    abstract ClassPool getClasses();

    abstract Logger getLogger();

    public abstract <T> ContextWith<T> with(T data);

    public static Context make(Project project) {
        return new ContextImpl(project);
    }
}
