package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;

import java.util.LinkedList;
import java.util.List;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

public class ScanTask implements Workflow<List<CtClass>,ScanResult> {

    private static final String HOTFIX_ANNOTATION = "org.kexie.android.hotfix.Hotfix";
    private static final String PATCHED_ANNOTATION = "org.kexie.android.hotfix.Patched";

    @Override
    public ContextWith<ScanResult>
    apply(ContextWith<List<CtClass>> input) throws Exception {
        List<CtClass> outClasses = new LinkedList<>();
        List<CtField> outFields = new LinkedList<>();
        List<CtMethod> outMethods = new LinkedList<>();
        for (CtClass ctClass : input.getInput()) {
            boolean patched = ctClass.hasAnnotation(HOTFIX_ANNOTATION);
            boolean hotfix = ctClass.hasAnnotation(PATCHED_ANNOTATION);
            if (patched && hotfix) {
                throw new TransformException("注解 " + HOTFIX_ANNOTATION
                        + " 和注解 " + PATCHED_ANNOTATION
                        + " 不能同时在class上出现");
            }
            if (patched) {
                outClasses.add(ctClass);
                continue;
            }
            if (hotfix) {
                for (CtField field : ctClass.getDeclaredFields()) {
                    if (field.hasAnnotation(PATCHED_ANNOTATION)) {
                        outFields.add(field);
                    }
                }
                for (CtMethod method : ctClass.getDeclaredMethods()) {
                    if (method.hasAnnotation(PATCHED_ANNOTATION)) {
                        outMethods.add(method);
                    }
                }
            }
        }
        return input.getContext().with(new ScanResult(outClasses,
                outFields, outMethods));
    }
}