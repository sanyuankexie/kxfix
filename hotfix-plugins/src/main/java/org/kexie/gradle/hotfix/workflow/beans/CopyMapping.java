package org.kexie.gradle.hotfix.workflow.beans;

import java.io.File;

import javassist.CtClass;

public final class CopyMapping {
    public final CtClass clazz;
    public final File file;

    public CopyMapping(
            CtClass clazz,
            File file) {
        this.clazz = clazz;
        this.file = file;
    }
}
