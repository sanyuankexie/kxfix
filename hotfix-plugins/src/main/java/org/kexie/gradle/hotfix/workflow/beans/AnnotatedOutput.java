package org.kexie.gradle.hotfix.workflow.beans;

import java.util.List;

import javassist.CtClass;

public final class AnnotatedOutput {
    public final List<CtClass> added;
    public final List<CtClass> fixed;

    public AnnotatedOutput(
            List<CtClass> added,
            List<CtClass> fixed
    ) {
        this.added = added;
        this.fixed = fixed;
    }
}
