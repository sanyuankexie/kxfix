package org.kexie.android.hotfix.plugins.workflow;

import com.android.utils.Pair;

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
final class CloneHotfixClassTask extends Work<List<CtClass>,List<Pair<CtClass,CtClass>>> {

    @Override
    ContextWith<List<Pair<CtClass, CtClass>>> doWork(ContextWith<List<CtClass>> context) {
        List<Pair<CtClass, CtClass>> result = new LinkedList<>();
        try {
            for (CtClass source : context.getData()) {
                String packageName = source.getPackageName();
                String cloneName = packageName + (isEmptyText(packageName) ? "" : ".")
                        + "Overload$" + source.getSimpleName();
                CtClass clone = context.getClasses().makeClass(cloneName);
                clone.getClassFile().setMajorVersion(ClassFile.JAVA_7);
                clone.defrost();
                clone.setSuperclass(source.getSuperclass());
                for (CtField field : source.getDeclaredFields()) {
                    if (field.hasAnnotation(RefNames.OVERLOAD_ANNOTATION)) {
                        clone.addField(new CtField(field, clone));
                    }
                }
                ClassMap classMap = new ClassMap();
                classMap.put(clone, source);
                classMap.fix(source);
                for (CtMethod method : source.getDeclaredMethods()) {
                    if (method.hasAnnotation(RefNames.OVERLOAD_ANNOTATION)) {
                        clone.addMethod(new CtMethod(method, clone, classMap));
                    }
                }
                for (CtConstructor constructor : source.getDeclaredConstructors()) {
                    if (constructor.hasAnnotation(RefNames.OVERLOAD_ANNOTATION)) {
                        clone.addMethod(constructor.toMethod("$init$", clone));
                    }
                }
                int mod = clone.getModifiers();
                mod = AccessFlag.clear(mod, AccessFlag.ABSTRACT);
                mod = AccessFlag.setPublic(mod);
                clone.setModifiers(mod);
                result.add(Pair.of(source, clone));
            }
            return context.with(result);
        } catch (NotFoundException | CannotCompileException e) {
            throw Exceptions.propagate(e);
        }
    }

    private static boolean isEmptyText(String s) {
        if (s == null) {
            return true;
        } else {
            return s.length() == 0;
        }
    }
}