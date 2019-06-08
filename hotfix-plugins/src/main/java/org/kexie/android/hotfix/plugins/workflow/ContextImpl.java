package org.kexie.android.hotfix.plugins.workflow;

import com.google.common.util.concurrent.AtomicDouble;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.util.concurrent.atomic.AtomicReference;

import javassist.ClassPool;

class ContextImpl extends Context {
    private final ClassPool classPool = new ClassPool();
    private final Project project;
    private final AtomicDouble progress = new AtomicDouble();
    private final AtomicReference<String> taskName = new AtomicReference<>();

    ContextImpl(Project project) {
        this.project = project;
    }

    Project getProject() {
        return project;
    }

    ClassPool getClasses() {
        return classPool;
    }

    Logger getLogger() {
        return project.getLogger();
    }

    @Override
    void setTaskName(String name) {
        taskName.set(name == null ? "" : name);
    }

    @Override
    void setProgress(float value) {
        progress.set(value);
    }

    @Override
    public String getTaskName() {
        return taskName.get();
    }

    @Override
    public float getProgress() {
        return progress.floatValue();
    }

    public <T> ContextWith<T> with(T input) {
        return new ContextWith<>(this, input);
    }
}
