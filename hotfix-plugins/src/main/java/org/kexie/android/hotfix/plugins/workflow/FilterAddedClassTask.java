package org.kexie.android.hotfix.plugins.workflow;

import com.android.utils.Pair;

import java.util.LinkedList;
import java.util.List;

import io.reactivex.exceptions.Exceptions;
import javassist.CtClass;
import javassist.NotFoundException;

final class FilterAddedClassTask
        extends Work<List<CtClass>,Pair<List<CtClass>,List<CtClass>>> {
    @Override
    ContextWith<Pair<List<CtClass>, List<CtClass>>>
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
        return context.with(Pair.of(added, inner));
    }
}
