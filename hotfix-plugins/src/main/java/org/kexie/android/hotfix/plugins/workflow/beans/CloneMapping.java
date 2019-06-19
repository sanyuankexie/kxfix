package org.kexie.android.hotfix.plugins.workflow.beans;

import javassist.CtClass;

public final class CloneMapping {
    public final CtClass source;
    public final CtClass clone;

    public CloneMapping(
            CtClass source,
            CtClass clone
    ) {
        this.source = source;
        this.clone = clone;
    }
}
