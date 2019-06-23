package org.kexie.gradle.hotfix.workflow.beans;

import java.util.List;

import javassist.CtClass;

public final class AddedOutput {
    public final List<CtClass> inner;
    public final List<CtClass> topper;

    public AddedOutput(List<CtClass> inner,
                       List<CtClass> topper) {
        this.inner = inner;
        this.topper = topper;
    }
}
