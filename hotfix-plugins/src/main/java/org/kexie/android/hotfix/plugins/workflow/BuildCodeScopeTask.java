package org.kexie.android.hotfix.plugins.workflow;


import java.util.List;

import io.reactivex.exceptions.Exceptions;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;

final class BuildCodeScopeTask extends Work<List<CtClass>,CtClass> {

    private static final String CODE_SCOPE_CLASS_NAME
            = "org.kexie.android.hotfix.internal.Overload$CodeScope";

    private static final String CODE_SCOPE_SUPER_CLASS_NAME
            = "org.kexie.android.hotfix.internal.CodeScope";

    private static final String EMPTY_INIT = "public Overload$CodeScope(){super();}";

    @Override
    ContextWith<CtClass> doWork(ContextWith<List<CtClass>> context) {
        try {
            List<CtClass> classes = context.getData();
            CtClass clazz = context.getClasses().makeClass(CODE_SCOPE_CLASS_NAME);
            clazz.defrost();
            clazz.setSuperclass(context.getClasses().get(CODE_SCOPE_SUPER_CLASS_NAME));
            StringBuilder builder = new StringBuilder("java.lang.Class[] " +
                    "loadEntryClasses(org.kexie.android.hotfix.internal.CodeContext context)" +
                    "throws java.lang.Throwable {return ");
            if (classes.size() > 0) {
                builder.append("new Class[]{(context.typeOf(\"")
                        .append(classes.get(0).getName())
                        .append("\"))");
                for (int i = 1; i < classes.size(); ++i) {
                    builder.append(",(context.typeOf(\"")
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
