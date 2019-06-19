package org.kexie.android.hotfix.plugins.workflow;

import org.apache.http.util.TextUtils;
import org.kexie.android.hotfix.plugins.workflow.beans.CloneMapping;

import java.util.LinkedList;
import java.util.List;

import io.reactivex.exceptions.Exceptions;
import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;

/**
 * 克隆原始类
 * 但只保留被修复的方法和字段
 * 类的法访问权限被修改为public并且移除abstract
 */
final class CloneHotfixClassTask extends Work<List<CtClass>,List<CloneMapping>> {

    @Override
    ContextWith<List<CloneMapping>> doWork(ContextWith<List<CtClass>> context) {
        List<CloneMapping> result = new LinkedList<>();
        try {
            for (CtClass source : context.getData()) {
                String packageName = source.getPackageName();
                String cloneName = packageName + (TextUtils.isEmpty(packageName) ? "" : ".")
                        + "Overload$" + source.getSimpleName();
                CtClass clone = context.getClasses().makeClass(cloneName);
                clone.getClassFile().setMajorVersion(ClassFile.JAVA_7);
                clone.defrost();
                clone.setSuperclass(source.getSuperclass());
                for (CtField field : source.getDeclaredFields()) {
                    if (field.hasAnnotation(Context.OVERLOAD_ANNOTATION)) {
                        clone.addField(new CtField(field, clone));
                    }
                }
                ClassMap classMap = new ClassMap();
                classMap.put(clone, source);
                classMap.fix(source);
                for (CtMethod method : source.getDeclaredMethods()) {
                    if (method.hasAnnotation(Context.OVERLOAD_ANNOTATION)) {
                        clone.addMethod(new CtMethod(method, clone, classMap));
                    }
                }
                for (CtConstructor constructor : source.getDeclaredConstructors()) {
                    if (constructor.hasAnnotation(Context.OVERLOAD_ANNOTATION)) {
                        clone.addMethod(constructor.toMethod("$init$", clone));
                    }
                }
                int mod = clone.getModifiers();
                mod = AccessFlag.clear(mod, AccessFlag.ABSTRACT);
                mod = AccessFlag.setPublic(mod);
                clone.setModifiers(mod);
                result.add(new CloneMapping(source, clone));
            }
            return context.with(result);
        } catch (NotFoundException | CannotCompileException e) {
            throw Exceptions.propagate(e);
        }
    }
}