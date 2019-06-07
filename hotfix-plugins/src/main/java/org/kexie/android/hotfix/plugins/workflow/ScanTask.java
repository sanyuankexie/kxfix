package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;
import com.android.utils.Pair;

import java.util.LinkedList;
import java.util.List;

import javassist.CtClass;


public class ScanTask implements Workflow<List<CtClass>, Pair<List<CtClass>,List<CtClass>>> {

    private static final String HOTFIX_ANNOTATION = "org.kexie.android.hotfix.Hotfix";
    private static final String PATCHED_ANNOTATION = "org.kexie.android.hotfix.Patched";

    @Override
    public ContextWith<Pair<List<CtClass>, List<CtClass>>>
    apply(ContextWith<List<CtClass>> input) throws Exception {
        List<CtClass> added = new LinkedList<>();
        List<CtClass> fixed = new LinkedList<>();
        for (CtClass clazz : input.getInput()) {
            boolean patched = clazz.hasAnnotation(PATCHED_ANNOTATION);
            boolean hotfix = clazz.hasAnnotation(HOTFIX_ANNOTATION);
            if (patched && hotfix) {
                throw new TransformException("注解 " + HOTFIX_ANNOTATION
                        + " 和注解 " + PATCHED_ANNOTATION
                        + " 不能同时在class上出现");
            }
            if (patched) {
                input.getContext().getLogger()
                        .quiet("added class " + clazz.getName());
                added.add(clazz);
                continue;
            }
            if (hotfix) {
                input.getContext().getLogger()
                        .quiet("fixed class " + clazz.getName());
                fixed.add(clazz);
            }
        }
        return input.getContext().with(Pair.of(added, fixed));
    }
}