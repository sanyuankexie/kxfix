package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;

import java.util.LinkedList;
import java.util.List;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;

public class ScanTask implements Workflow<List<CtClass>, ScanTask.Output> {

    private static final String HOTFIX_ANNOTATION = "org.kexie.android.hotfix.Hotfix";
    private static final String PATCHED_ANNOTATION = "org.kexie.android.hotfix.Patched";

    @Override
    public ContextWith<Output>
    apply(ContextWith<List<CtClass>> input) throws Exception {
        List<CtClass> outClasses = new LinkedList<>();
        List<CtField> outFields = new LinkedList<>();
        List<CtMethod> outMethods = new LinkedList<>();
        List<CtConstructor> outConstructors = new LinkedList<>();
        for (CtClass ctClass : input.getInput()) {
            boolean patched = ctClass.hasAnnotation(PATCHED_ANNOTATION);
            boolean hotfix = ctClass.hasAnnotation(HOTFIX_ANNOTATION);
            if (patched && hotfix) {
                throw new TransformException("注解 " + HOTFIX_ANNOTATION
                        + " 和注解 " + PATCHED_ANNOTATION
                        + " 不能同时在class上出现");
            }
            if (patched) {
                input.getContext().getLogger()
                        .quiet("patch class " + ctClass.getName());
                outClasses.add(ctClass);
                continue;
            }
            if (hotfix) {
                for (CtField field : ctClass.getDeclaredFields()) {
                    if (field.hasAnnotation(PATCHED_ANNOTATION)) {
                        input.getContext().getLogger()
                                .quiet("patch field " + field.getName());
                        outFields.add(field);
                    }
                }
                for (CtMethod method : ctClass.getDeclaredMethods()) {
                    if (method.hasAnnotation(PATCHED_ANNOTATION)) {
                        input.getContext().getLogger()
                                .quiet("patch method " + method.getName());
                        outMethods.add(method);
                    }
                }
                for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
                    if (constructor.hasAnnotation(PATCHED_ANNOTATION)) {
                        input.getContext().getLogger()
                                .quiet("patch constructor " + constructor.getDeclaringClass().getName());
                        outConstructors.add(constructor);
                    }
                }
            }
        }
        return input.getContext().with(new Output(outClasses,
                outFields, outMethods, outConstructors));
    }

    public static class Output {
        private final List<CtClass> classes;
        private final List<CtField> fields;
        private final List<CtMethod> methods;
        private final List<CtConstructor> constructors;

        Output(List<CtClass> classes,
               List<CtField> fields,
               List<CtMethod> methods,
               List<CtConstructor> constructors) {
            this.classes = classes;
            this.fields = fields;
            this.methods = methods;
            this.constructors = constructors;
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

        public List<CtConstructor> getConstructors() {
            return constructors;
        }
    }
}