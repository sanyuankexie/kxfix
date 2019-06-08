package org.kexie.android.hotfix.plugins.workflow;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import javassist.ClassPool;

public class ContextWith<T> extends Context {
    private final ContextImpl context;
    private final T data;

    ContextWith(ContextImpl context, T data) {
        this.context = context;
        this.data = data;
    }

    public T getData() {
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
    void setTaskName(String name) {
        context.setTaskName(name);
    }

    @Override
    public String getTaskName() {
        return context.getTaskName();
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
