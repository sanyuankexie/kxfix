package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
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
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.expr.Cast;
import javassist.expr.ConstructorCall;
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

        private static final String CODE_SCOPE_CLASS_NAME
                = "org.kexie.android.hotfix.internal.Overload-CodeScope";

        private static final String CODE_SCOPE_SUPER_CLASS_NAME
                = "org.kexie.android.hotfix.internal.CodeScope";

        private static final String EMPTY_MODIFY
                = "void receiveModifyById(int id, java.lang.Object o, java.lang.Object newValue)" +
                "throws java.lang.Throwable " +
                "{throw new java.lang.NoSuchFieldException(); }";
        private static final String EMPTY_ACCESS
                = "java.lang.Object receiveAccessById(int id, java.lang.Object o)" +
                "throws java.lang.Throwable " +
                "{throw new java.lang.NoSuchFieldException(); }";
        private static final String EMPTY_INVOKE
                = "java.lang.Object receiveInvokeById(int id, java.lang.Object o, java.lang.Object[] args)" +
                "throws java.lang.Throwable " +
                "{ throw new NoSuchMethodException(); }";
        private static final String OBJECT_SUPER_CLASS_NAME
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
            CtClass clazz = buildCodeScope(result);
            result.add(clazz);
            return result;
        }

        private static boolean isEmpty(CharSequence s) {
            if (s == null) {
                return true;
            } else {
                return s.length() == 0;
            }
        }

        private CtClass cloneClass(CtClass source)
                throws NotFoundException, CannotCompileException {
            String packageName = source.getPackageName();
            String cloneName
                    = packageName
                    + (isEmpty(packageName) ? "" : ".")
                    + "Overload-"
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
            for (CtConstructor constructor : source.getDeclaredConstructors()) {
                if (constructor.hasAnnotation(Annotations.OVERLOAD_ANNOTATION)) {
                    clone.addMethod(constructor.toMethod(
                            "init$$" + Arrays.hashCode(
                                    constructor.getParameterTypes()
                            )
                            , clone));
                }
            }
            clone.setModifiers(AccessFlag.clear(clone.getModifiers(), AccessFlag.ABSTRACT));
            return clone;
        }

        private void fixClass(Map<CtMember, Integer> hashIds,
                              CtClass clone,
                              CtClass source)
                throws NotFoundException, CannotCompileException {
            CtClass annotation = getClasses().get(Annotations.ID_ANNOTATION);
            ConstPool constPool = clone.getClassFile().getConstPool();
            for (CtField field : clone.getDeclaredFields()) {
                fixAnnotation(hashIds, annotation, constPool, field);
                getLogger().quiet("fixed field " + field.getName());
            }
            for (CtMethod method : clone.getDeclaredMethods()) {
                fixAnnotation(hashIds, annotation, constPool, method);
                getLogger().quiet("fixed method " + method.getName());
                fixMethod(method, source);
            }
            clone.setSuperclass(getClasses().get(OBJECT_SUPER_CLASS_NAME));
        }

        private void fixAnnotation(Map<CtMember, Integer> hashIds,
                                   CtClass annotationType,
                                   ConstPool constPool,
                                   CtMember member)
                throws NotFoundException {
            AnnotationsAttribute attribute = null;
            if (member instanceof CtField) {
                attribute = (AnnotationsAttribute) ((CtField) member)
                        .getFieldInfo()
                        .getAttribute(AnnotationsAttribute.visibleTag);
            } else if (member instanceof CtMethod) {
                attribute = (AnnotationsAttribute) ((CtMethod) member)
                        .getMethodInfo()
                        .getAttribute(AnnotationsAttribute.visibleTag);
            } else if (member instanceof CtConstructor) {
                attribute = (AnnotationsAttribute) ((CtConstructor) member)
                        .getMethodInfo()
                        .getAttribute(AnnotationsAttribute.visibleTag);
            }
            if (attribute == null) {
                attribute = new AnnotationsAttribute(constPool,
                        AnnotationsAttribute.visibleTag);
            }

            Annotation annotation = new Annotation(constPool, annotationType);
            IntegerMemberValue value = new IntegerMemberValue(constPool);
            int id = hash(hashIds, member);
            value.setValue(id);
            annotation.addMemberValue("value", value);
            attribute.addAnnotation(annotation);

            if (member instanceof CtField) {
                ((CtField) member)
                        .getFieldInfo()
                        .addAttribute(attribute);
            } else if (member instanceof CtMethod) {
                ((CtMethod) member)
                        .getMethodInfo()
                        .addAttribute(attribute);
            } else if (member instanceof CtConstructor) {
                ((CtConstructor) member)
                        .getMethodInfo()
                        .addAttribute(attribute);
            }
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
                    if (!Modifier.isFinal(field.getModifiers())) {
                        modify.insertBefore(buildFieldModify(field, entry.getValue()));
                    }
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
            if (!member.getReturnType().equals(CtClass.voidType)) {
                builder.append("return ");
            }
            CtClass box;
            if ((box = primitiveMapping.get(member.getReturnType())) != null) {
                builder.append(box.getName())
                        .append(".valueOf(");
                //Integer.valueOf(xxx());
            }
            builder.append(member.getName())
                    .append("(");
            //a no static method
            if (offset != 0) {
                builder.append("(")
                        .append(parameterTypes[0].getName())
                        .append(")$2");
            }
            if (parameterTypes.length - offset > 0) {
                if (offset != 0) {
                    builder.append(',');
                }
                builder.append("p0");
                for (int i = 1; i < parameterTypes.length - offset; ++i) {
                    builder.append(",p")
                            .append(i);
                }
            }
            builder.append(')');
            if (box != null) {
                builder.append(')');
            }
            builder.append(';');
            if (member.getReturnType().equals(CtClass.voidType)) {
                builder.append("return null;");
            }
            builder.append("}");
            String source = builder.toString();
            getLogger().quiet(source);
            return source;
        }

        private CtClass buildCodeScope(List<CtClass> classes)
                throws NotFoundException, CannotCompileException {
            CtClass clazz = getClasses().makeClass(CODE_SCOPE_CLASS_NAME);
            clazz.defrost();
            clazz.setSuperclass(getClasses().get(CODE_SCOPE_SUPER_CLASS_NAME));
            StringBuilder builder = new StringBuilder("java.lang.Class[] onLoad()" +
                    "throws java.lang.Throwable {return ");
            if (classes.size() > 0) {
                builder.append("new Class[]{(java.lang.Class.forName(\"")
                        .append(classes.get(0).getName())
                        .append("\",false,this.getClassLoader()))");
                for (int i = 1; i < classes.size(); ++i) {
                    builder.append(",(java.lang.Class.forName(\"")
                            .append(classes.get(i).getName())
                            .append("\",false,this.getClassLoader()))");
                }
                builder.append("};");
            }
            else {
                builder.append("new Class[0];");
            }
            builder.append("}");
            getLogger().quiet(builder.toString());
            clazz.addMethod(CtNewMethod.make(builder.toString(), clazz));
            return clazz;
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
        public void edit(Cast c) {
        }

        @Override
        public void edit(NewExpr e) {
        }

        @Override
        public void edit(MethodCall m) {
        }

        @Override
        public void edit(FieldAccess f) {
        }

        @Override
        public void edit(ConstructorCall c) throws CannotCompileException {
            c.replace(";");
        }
    }
}