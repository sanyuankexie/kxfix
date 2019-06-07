package org.kexie.android.hotfix.plugins.workflow;

import java.util.List;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

public class ScanResult {
    private final List<CtClass> classes;
    private final List<CtField> fields;
    private final List<CtMethod> methods;

    ScanResult(List<CtClass> classes,
               List<CtField> fields,
               List<CtMethod> methods) {
        this.classes = classes;
        this.fields = fields;
        this.methods = methods;
    }

    public List<CtClass> getClasses() {
        return classes;
    }

    public List<CtField> getFields() {
        return fields;
    }

    public List<CtMethod> getMethods() {
        return methods;
    }
}
