package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;

import org.apache.http.util.TextUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.expr.Cast;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;


/**
 * 生成补丁
 */
final class BuildTask extends Work<List<CtClass>, List<CtClass>> {

    @Override
    ContextWith<List<CtClass>>
    doWork(ContextWith<List<CtClass>> context) throws TransformException {
        try {
            return context.with(new BuildContext(context).build(context.getData()));
        } catch (NotFoundException | CannotCompileException e) {
            throw new TransformException(e);
        }
    }

    private final static class BuildContext extends ContextWrapper {

        //void modifyWithId(int id, Object o, Object newValue)throws Throwable {throw new NoSuchFieldException(); }
        private static final String EMPTY_MODIFY
                = "void modifyWithId(int id, java.lang.Object o, java.lang.Object newValue)" +
                "throws java.lang.Throwable " +
                "{throw new java.lang.NoSuchFieldException(); }";
        //Object accessWithId(int id, Object o)throws Throwable {throw new NoSuchFieldException(); }
        private static final String EMPTY_ACCESS
                = "java.lang.Object accessWithId(int id, java.lang.Object o)" +
                "throws java.lang.Throwable " +
                "{throw new java.lang.NoSuchFieldException(); }";
        //Object invokeWithId(int id, Object o, Object[] args)throws Throwable { throw new NoSuchMethodException(); }
        private static final String EMPTY_INVOKE
                = "java.lang.Object invokeWithId(int id, java.lang.Object o, java.lang.Object[] args)" +
                "throws java.lang.Throwable " +
                "{ throw new NoSuchMethodException(); }";
        private static final String SUPER_CLASS_NAME
                = "org.kexie.android.hotfix.internal.OverloadObject";

        private final Map<CtClass, CtClass> primitiveMapping;

        BuildContext(Context context) throws NotFoundException {
            super(context);
            primitiveMapping = getPrimitiveMapping(getClasses());
        }

        List<CtClass> build(List<CtClass> classes)
                throws NotFoundException, CannotCompileException {
            Map<CtMember, Integer> hashIds = new HashMap<>();
            List<CtClass> result = new LinkedList<>();
            for (CtClass clazz : classes) {
                hashIds.clear();
                CtClass clone = cloneClass(clazz);
                fixClass(hashIds, clone, clazz);
                buildEntry(hashIds, clone);
                result.add(clone);
            }
            return result;
        }

        private CtClass cloneClass(CtClass source)
                throws NotFoundException, CannotCompileException {
            String packageName = source.getPackageName();
            String cloneName
                    = packageName
                    + (TextUtils.isEmpty(packageName) ? "" : ".")
                    +"Overload-"
                    + source.getSimpleName();
            CtClass clone = getClasses().makeClass(cloneName);
            clone.getClassFile().setMajorVersion(ClassFile.JAVA_7);
            clone.defrost();
            clone.setSuperclass(source.getSuperclass());
            for (CtField field : source.getDeclaredFields()) {
                if (field.hasAnnotation(Annotations.OVERLOAD_ANNOTATION)) {
                    clone.addField(new CtField(field, clone));
                }
            }
            ClassMap classMap = new ClassMap();
            classMap.put(cloneName, source.getName());
            classMap.fix(source);
            for (CtMethod method : source.getDeclaredMethods()) {
                if (method.hasAnnotation(Annotations.OVERLOAD_ANNOTATION)) {
                    clone.addMethod(new CtMethod(method, clone, classMap));
                }
            }
            clone.setModifiers(AccessFlag.clear(clone.getModifiers(), AccessFlag.ABSTRACT));
            return clone;
        }

        private void fixClass(Map<CtMember, Integer> hashIds, CtClass clone, CtClass source)
                throws NotFoundException, CannotCompileException {
            CtClass fieldInfoClass = getClasses().get(Annotations.FIELD_INFO_ANNOTATION);
            CtClass methodInfoClass = getClasses().get(Annotations.METHOD_INFO_ANNOTATION);
            ConstPool constPool = clone.getClassFile().getConstPool();
            for (CtField field : clone.getDeclaredFields()) {
                getLogger().quiet("fixed field " + field.getName());
                fixFieldAnnotation(hashIds, constPool, fieldInfoClass, field);
            }
            for (CtMethod method : clone.getDeclaredMethods()) {
                getLogger().quiet("fixed method " + method.getName());
                fixMethodAnnotation(hashIds, constPool, methodInfoClass, method);
                fixMethod(method, source);
            }
            clone.setSuperclass(getClasses().get(SUPER_CLASS_NAME));
        }

        private void fixFieldAnnotation(Map<CtMember, Integer> hashIds,
                                        ConstPool constPool,
                                        CtClass annotationClass,
                                        CtField field)
                throws NotFoundException {
            AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute)
                    field.getFieldInfo().getAttribute(AnnotationsAttribute.visibleTag);
            if (annotationsAttribute == null) {
                annotationsAttribute = new AnnotationsAttribute(constPool,
                        AnnotationsAttribute.visibleTag);
            }
            Annotation annotation = new Annotation(constPool, annotationClass);
            int id = hash(hashIds, field);
            IntegerMemberValue integerMemberValue = new IntegerMemberValue(constPool);
            integerMemberValue.setValue(id);
            annotation.addMemberValue("id", integerMemberValue);
            annotationsAttribute.addAnnotation(annotation);
        }

        private void fixMethodAnnotation(Map<CtMember, Integer> hashIds,
                                         ConstPool constPool,
                                         CtClass annotationClass,
                                         CtMethod method)
                throws NotFoundException {
            AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute)
                    method.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);
            if (annotationsAttribute == null) {
                annotationsAttribute = new AnnotationsAttribute(constPool,
                        AnnotationsAttribute.visibleTag);
            }
            Annotation annotation = new Annotation(constPool, annotationClass);
            int id = hash(hashIds, method);
            IntegerMemberValue integerMemberValue = new IntegerMemberValue(constPool);
            integerMemberValue.setValue(id);
            annotation.addMemberValue("id", integerMemberValue);
            ArrayMemberValue arrayMemberValue = new ArrayMemberValue(constPool);
            CtClass[] parameterTypes = method.getParameterTypes();
            ClassMemberValue[] classMemberValues
                    = new ClassMemberValue[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                CtClass parameterType = parameterTypes[i];
                ClassMemberValue classMemberValue = new ClassMemberValue(constPool);
                classMemberValue.setValue(parameterType.getName());
                classMemberValues[i] = classMemberValue;
            }
            arrayMemberValue.setValue(classMemberValues);
            annotation.addMemberValue("parameterTypes", arrayMemberValue);
            annotationsAttribute.addAnnotation(annotation);
        }

        private void fixMethod(CtMethod method, CtClass source)
                throws CannotCompileException {
            if (!Modifier.isStatic(method.getModifiers())) {
                method.insertParameter(source);
            }
            method.instrument(new MethodTransform(source));
        }

        private void buildEntry(Map<CtMember, Integer> hashIds, CtClass clone)
                throws CannotCompileException, NotFoundException {
            CtMethod invoke = CtNewMethod.make(EMPTY_INVOKE, clone);
            CtMethod modify = CtNewMethod.make(EMPTY_MODIFY, clone);
            CtMethod access = CtNewMethod.make(EMPTY_ACCESS, clone);
            for (Map.Entry<CtMember, Integer> entry : hashIds.entrySet()) {
                CtMember member = entry.getKey();
                if (member instanceof CtMethod) {
                    invoke.insertBefore(buildMethodInvoke(
                            (CtMethod) entry.getKey(),
                            entry.getValue())
                    );
                } else if (member instanceof CtField) {
                    CtField field = (CtField) member;
                    if (Modifier.isFinal(field.getModifiers())) {
                        continue;
                    }
                    modify.insertBefore(buildFieldModify(field, entry.getValue()));
                    access.insertBefore(buildFieldAccess(field, entry.getValue()));
                }
            }
            clone.addMethod(invoke);
            clone.addMethod(modify);
            clone.addMethod(access);
        }

        private String buildFieldAccess(CtField field, int id) throws NotFoundException {
            StringBuilder builder = new StringBuilder("if(")
                    .append(id)
                    .append("==")
                    .append("$1")
                    .append("){return ");
            CtClass box;
            if ((box = primitiveMapping.get(field.getType())) != null) {
                builder.append(box.getName())
                        .append(".valueOf(this.")
                        .append(field.getName())
                        .append(");}");
            } else {
                builder.append("this.")
                        .append(field.getName())
                        .append(";}");
            }
            return builder.toString();
        }

        private String buildFieldModify(CtField field, int id) throws NotFoundException {
            StringBuilder builder = new StringBuilder("if(")
                    .append(id)
                    .append("==")
                    .append("$1")
                    .append("){")
                    .append("this.")
                    .append(field.getName())
                    .append("=");
            CtClass box;
            if ((box = primitiveMapping.get(field.getType())) != null) {
                builder.append("((")
                        .append(box.getName())
                        .append(")$3).")
                        .append(field.getType().getName())
                        .append("Value();");
            } else {
                builder.append("(")
                        .append(field.getType().getName())
                        .append(")$3;");
            }
            return builder.append("return;}")
                    .toString();
        }

        private String buildMethodInvoke(CtMethod member, int id)
                throws NotFoundException {
            StringBuilder builder = new StringBuilder("if(")
                    .append(id)
                    .append("==")
                    .append("$1")
                    .append("){");
            CtClass[] parameterTypes = member.getParameterTypes();
            int offset = Modifier.isStatic(member.getModifiers()) ? 0 : 1;
            for (int i = offset; i < parameterTypes.length; ++i) {
                CtClass pramType = parameterTypes[i];
                //real index
                int index = i - offset;
                builder.append(pramType.getName())
                        .append(" p")
                        .append(index);
                CtClass box;
                if ((box = primitiveMapping.get(pramType)) != null) {
                    builder.append("=((")
                            .append(box.getName())
                            .append(")$3[")
                            .append(index)
                            .append("]).")
                            .append(pramType.getName())
                            .append("Value();");
                } else {
                    builder.append("=(")
                            .append(pramType.getName())
                            .append(")$3[")
                            .append(index)
                            .append("];");
                }
            }
            builder.append("return ")
                    .append(member.getName())
                    .append("(");
            //a no static method
            if (offset != 0) {
                builder.append("(")
                        .append(parameterTypes[0].getName())
                        .append(")$2,");
            }
            if (parameterTypes.length - offset > 0) {
                builder.append("p0");
                for (int i = 1; i < parameterTypes.length - offset; ++i) {
                    builder.append(",p")
                            .append(i);
                }
            }
            builder.append(");}");
            String source = builder.toString();
            getLogger().quiet(source);
            return source;
        }

        /**
         * 开地址法确保散列始终不会碰撞
         * {@link Integer#MIN_VALUE}是无效值
         */
        private static int hash(Map<CtMember, Integer> hashIds, CtMember member) {
            int id = System.identityHashCode(member);
            while (true) {
                if (!hashIds.containsValue(id)) {
                    hashIds.put(member, id);
                    return id;
                }
                id = id == Integer.MAX_VALUE ? Integer.MIN_VALUE + 1 : id + 1;
            }
        }

        private static Map<CtClass, CtClass>
        getPrimitiveMapping(ClassPool classPool) throws NotFoundException {
            Map<CtClass, CtClass> primitiveMapping = new HashMap<>();
            primitiveMapping.put(CtClass.booleanType, classPool.get(Boolean.class.getName()));
            primitiveMapping.put(CtClass.charType, classPool.get(Character.class.getName()));
            primitiveMapping.put(CtClass.doubleType, classPool.get(Double.class.getName()));
            primitiveMapping.put(CtClass.floatType, classPool.get(Float.class.getName()));
            primitiveMapping.put(CtClass.intType, classPool.get(Integer.class.getName()));
            primitiveMapping.put(CtClass.shortType, classPool.get(Short.class.getName()));
            primitiveMapping.put(CtClass.longType, classPool.get(Long.class.getName()));
            return primitiveMapping;
        }
    }

    private static final class MethodTransform extends ExprEditor {
        private final CtClass source;
        private MethodTransform(CtClass source) {
            this.source = source;
        }

        @Override
        public void edit(Cast c) throws CannotCompileException {
        }

        @Override
        public void edit(NewExpr e) throws CannotCompileException {
        }

        @Override
        public void edit(MethodCall m) throws CannotCompileException {
        }

        @Override
        public void edit(FieldAccess f) throws CannotCompileException {
        }
    }
}