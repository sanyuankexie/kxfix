package org.kexie.android.hotfix.plugins.tasks;

import com.android.build.api.transform.TransformException;

import org.kexie.android.hotfix.Hotfix;
import org.kexie.android.hotfix.Patched;

import java.util.LinkedList;
import java.util.List;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

public class ScanningTask implements Task<List<CtClass>, ScanningTask.Result> {

    @Override
    public Result apply(Context context, List<CtClass> inputs) throws TransformException {
        List<CtClass> outClasses = new LinkedList<>();
        List<CtField> outFields = new LinkedList<>();
        List<CtMethod> outMethods = new LinkedList<>();
        for (CtClass ctClass : inputs) {
            //ClassPool使用了默认的类加载器
            boolean patched = ctClass.hasAnnotation(Patched.class);
            boolean hotfix = ctClass.hasAnnotation(Hotfix.class);
            if (patched && hotfix) {
                throw new TransformException("注解 " + Patched.class.getName()
                        + " 和注解 " + Hotfix.class.getName()
                        + " 不能同时在class上出现");
            }
            if (patched) {
                outClasses.add(ctClass);
                continue;
            }
            if (hotfix) {
                for (CtField field : ctClass.getDeclaredFields()) {
                    if (field.hasAnnotation(Patched.class)) {
                        outFields.add(field);
                    }
                }
                for (CtMethod method : ctClass.getDeclaredMethods()) {
                    if (method.hasAnnotation(Patched.class)) {
                        outMethods.add(method);
                    }
                }
            }
        }
        return new Result(outClasses, outFields, outMethods);
    }

    public static final class Result {
        public final List<CtClass> mClasses;
        public final List<CtField> mFields;
        public final List<CtMethod> mMethods;

        Result(List<CtClass> classes,
               List<CtField> fields,
               List<CtMethod> methods) {
            mClasses = classes;
            mFields = fields;
            mMethods = methods;
        }
    }
}
