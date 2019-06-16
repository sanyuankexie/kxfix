package org.kexie.android.hotfix.plugins.workflow;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

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
    public <X> ContextWith<X> with(X data) {
        return context.with(data);
    }
}
