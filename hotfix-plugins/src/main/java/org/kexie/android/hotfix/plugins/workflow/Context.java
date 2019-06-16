package org.kexie.android.hotfix.plugins.workflow;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import javassist.ClassPool;

/**
 * 任务运行的上下文环境
 * 装饰者模式
 */
public abstract class Context {

    abstract Project getProject();

    abstract ClassPool getClasses();

    abstract Logger getLogger();

    public abstract <T> ContextWith<T> with(T data);

    public static Context make(Project project) {
        return new ContextImpl(project);
    }
}
