package org.kexie.android.hotfix.plugins.workflow;

import com.google.common.util.concurrent.AtomicDouble;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javassist.ClassPool;

/**
 * 具体实现类
 */
final class ContextImpl extends Context {
    private final ClassPool classPool = new ClassPool();
    private final Project project;
    private final AtomicDouble progress = new AtomicDouble();
    private final ConcurrentLinkedQueue<String> taskNames = new ConcurrentLinkedQueue<>();

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
    void pushNewTask(Class<? extends Work> name) {
        taskNames.add(name.getName());
        progress.set(0);
    }

    @Override
    void setProgress(float value) {
        progress.set(value);
    }

    @Override
    public Queue<String> getTaskQueue() {
        return taskNames;
    }

    @Override
    public float getProgress() {
        return progress.floatValue();
    }

    public <T> ContextWith<T> with(T input) {
        return new ContextWith<>(this, input);
    }
}
