package org.kexie.android.hotfix.plugins.workflow.beans;

import java.util.List;

import javassist.CtClass;

public final class AnnotateOutput {
    public final List<CtClass> added;
    public final List<CtClass> fixed;

    public AnnotateOutput(
            List<CtClass> added,
            List<CtClass> fixed
    ) {
        this.added = added;
        this.fixed = fixed;
    }
}
