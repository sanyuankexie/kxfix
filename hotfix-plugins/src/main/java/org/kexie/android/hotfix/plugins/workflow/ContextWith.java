package org.kexie.android.hotfix.plugins.workflow;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.util.Queue;

import javassist.ClassPool;


/**
 * 携带数据的{@link Context}
 */
final class ContextWith<T> extends Context {
    private final Context context;
    private final T data;

    ContextWith(Context context, T data) {
        this.context = context;
        this.data = data;
    }

    T getData() {
        return data;
    }

    @Override
    Project getProject() {
        return context.getProject();
    }

    @Override
    ClassPool getClasses() {
        return context.getClasses();
    }

    @Override
    Logger getLogger() {
        return context.getLogger();
    }

    @Override
    void setProgress(float value) {
        context.setProgress(value);
    }

    @Override
    void pushNewTask(Class<? extends Work> name) {
        context.pushNewTask(name);
    }

    @Override
    public Queue<String> getTaskQueue() {
        return context.getTaskQueue();
    }

    @Override
    public float getProgress() {
        return context.getProgress();
    }

    @Override
    public <X> ContextWith<X> with(X data) {
        return context.with(data);
    }
}
