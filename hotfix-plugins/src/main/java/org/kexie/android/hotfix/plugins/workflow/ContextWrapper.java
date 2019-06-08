package org.kexie.android.hotfix.plugins.workflow;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.util.Queue;

import javassist.ClassPool;

public class ContextWrapper extends Context {

    private final Context context;

    ContextWrapper(Context context) {
        this.context = context;
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
