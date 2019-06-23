package org.kexie.gradle.hotfix.workflow;


import java.util.List;

import io.reactivex.exceptions.Exceptions;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;

final class BuildDomainTask extends Work<List<CtClass>,CtClass> {

    private static final String EMPTY_INIT = "public Overload$Domain(){super();}";

    @Override
    ContextWith<CtClass> doWork(ContextWith<List<CtClass>> context) {
        try {
            List<CtClass> classes = context.getData();
            CtClass clazz = context.getClasses().makeClass(Context.DOMAIN_CLASS_NAME);
            clazz.defrost();
            clazz.setSuperclass(context.getClasses().get(Context.DOMAIN_SUPER_CLASS_NAME));
            StringBuilder builder = new StringBuilder("java.lang.Class[] " +
                    "loadEntries()" +
                    "throws java.lang.Throwable {return ");
            if (classes.size() > 0) {
                builder.append("new Class[]{(" + Context.UTIL_CLASS_NAME + ".typeOf(\"")
                        .append(classes.get(0).getName())
                        .append("\"))");
                for (int i = 1; i < classes.size(); ++i) {
                    builder.append(",(" + Context.UTIL_CLASS_NAME + ".typeOf(\"")
                            .append(classes.get(i).getName())
                            .append("\"))");
                }
                builder.append("};");
            } else {
                builder.append("new Class[0];");
            }
            builder.append("}");
            context.getLogger().quiet(builder.toString());
            clazz.addConstructor(CtNewConstructor.make(EMPTY_INIT, clazz));
            clazz.addMethod(CtNewMethod.make(builder.toString(), clazz));
            return context.with(clazz);
        } catch (CannotCompileException | NotFoundException e) {
            throw Exceptions.propagate(e);
        }
    }
}
