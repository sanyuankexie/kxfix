package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;
import com.android.utils.Pair;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javassist.CtClass;


/**
 * 扫描输入中的所有符合条件的类
 */
final class ScanTask extends Work<List<CtClass>, Pair<List<CtClass>,List<CtClass>>> {


    @Override
    ContextWith<Pair<List<CtClass>, List<CtClass>>>
    doWork(ContextWith<List<CtClass>> context) throws TransformException {
        List<CtClass> added = new LinkedList<>();
        List<CtClass> fixed = new LinkedList<>();
        for (CtClass clazz : context.getData()) {
            boolean patched = clazz.hasAnnotation(Annotations.PATCHED_ANNOTATION);
            boolean hotfix = clazz.hasAnnotation(Annotations.HOTFIX_ANNOTATION);
            if (patched && hotfix) {
                throw new TransformException("注解 " + Annotations.HOTFIX_ANNOTATION
                        + " 和注解 " + Annotations.PATCHED_ANNOTATION
                        + " 不能同时在class上出现");
            }
            if (patched) {
                context.getLogger()
                        .quiet("added class " + clazz.getName());
                added.add(clazz);
                continue;
            }
            if (hotfix && (Arrays.stream(clazz.getDeclaredFields())
                    .anyMatch(ctField -> ctField.hasAnnotation(Annotations.PATCHED_ANNOTATION))
                    || Arrays.stream(clazz.getDeclaredBehaviors())
                    .anyMatch(ctBehavior -> ctBehavior.hasAnnotation(Annotations.PATCHED_ANNOTATION)))) {
                context.getLogger()
                        .quiet("fixed class " + clazz.getName());
                fixed.add(clazz);
            }
        }
        return context.with(Pair.of(added, fixed));
    }
}