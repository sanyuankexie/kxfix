package org.kexie.gradle.hotfix.workflow;

import java.util.Collection;

import io.reactivex.functions.Function;

/**
 * 添加新的工作流程请继承此类
 * 重写{@link Work#doWork(ContextWith)}
 * 并在{@link Workflow#doWorks(Context, Collection)}中
 * 添加代码
 */
abstract class Work<I,O> implements Function<ContextWith<I>,ContextWith<O>> {

    @Override
    public final ContextWith<O> apply(ContextWith<I> in) throws Exception {
        return doWork(in);
    }

    /**
     * 重写该方法添加工作流程
     */
    abstract ContextWith<O> doWork(ContextWith<I> context) throws Exception;

}
