package org.kexie.gradle.hotfix.workflow;

import org.kexie.gradle.hotfix.workflow.beans.AnnotatedOutput;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javassist.CtClass;


/**
 * 扫描输入中的所有符合条件的类
 */
final class FilterAnnotatedTask
        extends Work<List<CtClass>, AnnotatedOutput> {

    @Override
    ContextWith<AnnotatedOutput>
    doWork(ContextWith<List<CtClass>> context) {
        List<CtClass> added = new LinkedList<>();
        List<CtClass> fixed = new LinkedList<>();
        for (CtClass clazz : context.getData()) {
            boolean patched = clazz.hasAnnotation(Context.OVERLOAD_ANNOTATION);
            boolean hotfix = clazz.hasAnnotation(Context.HOTFIX_ANNOTATION);
            if (patched && hotfix) {
                throw new RuntimeException("注解 " + Context.HOTFIX_ANNOTATION
                        + " 和注解 " + Context.OVERLOAD_ANNOTATION
                        + " 不能同时在class上出现");
            }
            if (patched) {
                context.getLogger().quiet("added class " + clazz.getName());
                added.add(clazz);
                continue;
            }
            if (hotfix && (Arrays.stream(clazz.getDeclaredFields())
                    .anyMatch(ctField -> ctField.hasAnnotation(Context.OVERLOAD_ANNOTATION))
                    || Arrays.stream(clazz.getDeclaredBehaviors())
                    .anyMatch(ctBehavior -> ctBehavior.hasAnnotation(Context.OVERLOAD_ANNOTATION)))) {
                context.getLogger()
                        .quiet("fixed class " + clazz.getName());
                fixed.add(clazz);
            }
        }
        return context.with(new AnnotatedOutput(added, fixed));
    }
}