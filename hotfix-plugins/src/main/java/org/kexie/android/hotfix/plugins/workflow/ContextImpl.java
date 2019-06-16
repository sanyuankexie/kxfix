package org.kexie.android.hotfix.plugins.workflow;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import javassist.ClassPool;

/**
 * 具体实现类
 */
final class ContextImpl extends Context {
    private final ClassPool classPool = new ClassPool();
    private final Project project;

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


    public <T> ContextWith<T> with(T input) {
        return new ContextWith<>(this, input);
    }
}
