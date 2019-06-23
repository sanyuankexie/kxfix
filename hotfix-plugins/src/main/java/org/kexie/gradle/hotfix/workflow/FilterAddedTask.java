package org.kexie.gradle.hotfix.workflow;

import org.kexie.gradle.hotfix.workflow.beans.AddedOutput;

import java.util.LinkedList;
import java.util.List;

import io.reactivex.exceptions.Exceptions;
import javassist.CtClass;
import javassist.NotFoundException;

final class FilterAddedTask
        extends Work<List<CtClass>, AddedOutput> {

    /**
     * 这里处理的是有名内部类
     * 无名内部类无法直接找到,所以在{@link AdjustCloneClassTask}处理
     */
    @Override
    ContextWith<AddedOutput>
    doWork(ContextWith<List<CtClass>> context) {
        List<CtClass> added = new LinkedList<>();
        List<CtClass> inner = new LinkedList<>();
        try {
            for (CtClass clazz : context.getData()) {
                if (clazz.getEnclosingBehavior() != null
                        || clazz.getDeclaringClass() != null) {
                    inner.add(clazz);
                } else {
                    added.add(clazz);
                }
            }
        } catch (NotFoundException e) {
            throw Exceptions.propagate(e);
        }
        return context.with(new AddedOutput(added, inner));
    }
}
