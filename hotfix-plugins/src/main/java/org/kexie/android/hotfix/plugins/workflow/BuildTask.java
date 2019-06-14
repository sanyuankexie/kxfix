package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;

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
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;


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
                = "void receiveModifyById(int id,java.lang.Object newValue)" +
                "throws java.lang.Throwable " +
                "{throw new java.lang.NoSuchFieldException();}";
        private static final String EMPTY_ACCESS
                = "java.lang.Object receiveAccessById(int id)" +
                "throws java.lang.Throwable " +
                "{throw new java.lang.NoSuchFieldException();}";
        private static final String EMPTY_INVOKE
                = "java.lang.Object receiveInvokeById(int id,java.lang.Object[] args)" +
                "throws java.lang.Throwable " +
                "{ throw new NoSuchMethodException();}";
        private static final String OBJECT_SUPER_CLASS_NAME
                = "org.kexie.android.hotfix.internal.HotCodeExecutor";

        private static final int BASE_STRING_BUILDER_SIZE = 64;

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
                CtClass clone = cloneSourceClass(clazz);
                fixCloneClass(hashIds, clone);
                buildEntryPoint(hashIds, clone);
                result.add(clone);
            }
            CtClass clazz = buildCodeScope(result);
            result.add(clazz);
            return result;
        }

        private static boolean isEmptyText(String s) {
            if (s == null) {
                return true;
            } else {
                return s.length() == 0;
            }
        }

        private CtClass cloneSourceClass(CtClass source)
                throws NotFoundException, CannotCompileException {
            String packageName = source.getPackageName();
            String cloneName
                    = packageName
                    + (isEmptyText(packageName) ? "" : ".")
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
            classMap.put(clone, source);
            classMap.fix(source);
            for (CtMethod method : source.getDeclaredMethods()) {
                if (method.hasAnnotation(Annotations.OVERLOAD_ANNOTATION)) {
                    CtMethod added = new CtMethod(method, clone, classMap);
                    int mod = Modifier.clear(added.getModifiers(), Modifier.STATIC);
                    added.setModifiers(Modifier.setPrivate(mod));
                    clone.addMethod(added);
                }
            }
            for (CtConstructor constructor : source.getDeclaredConstructors()) {
                if (constructor.hasAnnotation(Annotations.OVERLOAD_ANNOTATION)) {
                    CtMethod method = constructor.toMethod("$init$", clone);
                    clone.addMethod(method);
                }
            }
            clone.setModifiers(AccessFlag.clear(clone.getModifiers(), AccessFlag.ABSTRACT));
            return clone;
        }

        private void fixCloneClass(Map<CtMember, Integer> hashIds,
                                   CtClass clone)
                throws NotFoundException, CannotCompileException {
            clone.setSuperclass(getClasses().get(OBJECT_SUPER_CLASS_NAME));
            CtClass annotationType = getClasses().get(Annotations.ID_ANNOTATION);
            ConstPool constPool = clone.getClassFile().getConstPool();
            for (CtField field : clone.getDeclaredFields()) {
                fixAnnotation(hashIds, annotationType, constPool, field);
            }
            for (CtMethod method : clone.getDeclaredMethods()) {
                fixAnnotation(hashIds, annotationType, constPool, method);
                fixMethod(method);
            }
        }

        private static AnnotationsAttribute
        getAnnotations(CtMember member, String name) {
            ConstPool constPool = member.getDeclaringClass()
                    .getClassFile()
                    .getConstPool();
            AnnotationsAttribute attribute = null;
            if (member instanceof CtField) {
                attribute = (AnnotationsAttribute) ((CtField) member)
                        .getFieldInfo()
                        .getAttribute(name);
            } else if (member instanceof CtMethod) {
                attribute = (AnnotationsAttribute) ((CtMethod) member)
                        .getMethodInfo()
                        .getAttribute(name);
            } else if (member instanceof CtConstructor) {
                attribute = (AnnotationsAttribute) ((CtConstructor) member)
                        .getMethodInfo()
                        .getAttribute(name);
            }
            if (attribute == null && name.equals(AnnotationsAttribute.visibleTag)) {
                attribute = new AnnotationsAttribute(constPool,
                        AnnotationsAttribute.visibleTag);
            }
            return attribute;
        }

        private static void
        setAnnotations(CtMember member, AnnotationsAttribute attribute) {
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

        private void fixAnnotation(Map<CtMember, Integer> hashIds,
                                   CtClass annotationType,
                                   ConstPool constPool,
                                   CtMember member)
                throws NotFoundException {
            AnnotationsAttribute attribute = getAnnotations(member,
                    AnnotationsAttribute.visibleTag);

            Annotation annotation = new Annotation(constPool, annotationType);
            IntegerMemberValue value = new IntegerMemberValue(constPool);
            int id = hash(hashIds, member);
            value.setValue(id);
            annotation.addMemberValue("value", value);
            attribute.addAnnotation(annotation);

            setAnnotations(member, attribute);

            attribute = getAnnotations(member, AnnotationsAttribute.invisibleTag);

            if (attribute != null) {
                attribute.removeAnnotation(Annotations.OVERLOAD_ANNOTATION);
                setAnnotations(member, attribute);
            }
        }

        private void fixMethod(CtMethod fix)
                throws CannotCompileException {
            fix.instrument(new ExprConverter(fix.getDeclaringClass()));
        }

        private final class ExprConverter extends ExprEditor {
            private final CtClass clone;

            ExprConverter(CtClass clone) {
                this.clone = clone;
            }

            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    CtClass objectClass = getClasses().get("java.lang.Object");
                    CtMethod method = m.getMethod();
                    int modifiers = method.getModifiers();
                    CtClass declaringClass = method.getDeclaringClass();
                    boolean needReflect;
                    if (!method.hasAnnotation(Annotations.OVERLOAD_ANNOTATION)) {
                        String packageName = declaringClass.getPackageName();
                        if (packageName.equals(clone.getPackageName())
                                || !Modifier.isPrivate(modifiers)) {
                            if (Modifier.isStatic(modifiers)) {
                                return;
                            } else {
                                needReflect = m.isSuper();
                            }
                        } else {
                            needReflect = true;
                        }
                    } else {
                        needReflect = false;
                    }
                    if (needReflect) {
                        //reflect invoke method
                        StringBuilder builder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                        CtClass returnType = method.getReturnType();
                        CtClass[] parameterTypes = method.getParameterTypes();
                        StringBuilder typesBuilder;
                        StringBuilder pramsBuilder;
                        if (parameterTypes.length > 0) {
                            typesBuilder = new StringBuilder("" +
                                    "new java.lang.Class[]{typeOf(\""
                                    + parameterTypes[0].getName()
                                    + "\")");
                            pramsBuilder = new StringBuilder("new java.lang.Object[]{")
                                    .append(buildCast(parameterTypes[0], objectClass, "$1"));
                            for (int i = 1; i < parameterTypes.length; ++i) {
                                typesBuilder.append(",typeOf(\"")
                                        .append(parameterTypes[i].getName())
                                        .append("\")");
                                pramsBuilder.append(",")
                                        .append(buildCast(parameterTypes[i], objectClass,
                                                "$" + (i + 1)));
                            }
                            typesBuilder.append("}");
                            pramsBuilder.append("}");
                        } else {
                            typesBuilder = pramsBuilder = new StringBuilder("null");
                        }

                        if (!CtClass.voidType.equals(returnType)) {
                            builder.append("java.lang.Object result=");
                        }
                        builder.append("this.invoke(")
                                .append(m.isSuper())
                                .append(',')
                                .append("typeOf(\"")
                                .append(declaringClass.getName())
                                .append("\"),\"")
                                .append(method.getName())
                                .append("\",")
                                .append(typesBuilder)
                                .append(",$0,")
                                .append(pramsBuilder)
                                .append(");");
                        if (!CtClass.voidType.equals(returnType)) {
                            builder.append("$_=")
                                    .append(buildCast(getClasses().get("java.lang.Object"),
                                            returnType, "result"))
                                    .append(";");
                        }
                        String source = builder.toString();
                        getLogger().quiet(source);
                        m.replace(source);
                    } else {
                        //direct invoke method
                        StringBuilder builder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                        CtClass returnType = method.getReturnType();
                        if (!CtClass.voidType.equals(returnType)) {
                            builder.append("$_=");
                        }
                        if (!Modifier.isStatic(modifiers)) {
                            builder.append(buildCast(objectClass, declaringClass,
                                    "(($0==this)?(this.target):($0))"))
                                    .append(".");
                        }
                        builder.append(method.getName())
                                .append("(");
                        CtClass[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length > 0) {
                            builder.append("$1");
                            for (int i = 1; i < parameterTypes.length; ++i) {
                                builder.append(",$")
                                        .append(i + 1);
                            }
                        }
                        builder.append(");");
                        String source = builder.toString();
                        getLogger().quiet(source);
                        m.replace(source);
                    }
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void edit(ConstructorCall c) throws CannotCompileException {
                c.replace(";");
            }

            @Override
            public void edit(FieldAccess f) throws CannotCompileException {
                try {
                    CtClass objectClass = getClasses().get("java.lang.Object");
                    CtField field = f.getField();
                    int modifiers = field.getModifiers();
                    CtClass declaringClass = field.getDeclaringClass();
                    boolean needReflect;
                    if (!field.hasAnnotation(Annotations.OVERLOAD_ANNOTATION)) {
                        String packageName = declaringClass.getPackageName();
                        if (packageName.equals(clone.getPackageName())
                                || !Modifier.isPrivate(modifiers)) {
                            if (Modifier.isStatic(modifiers)) {
                                return;
                            } else {
                                needReflect = false;
                            }
                        } else {
                            needReflect = true;
                        }
                    } else {
                        needReflect = false;
                    }
                    StringBuilder builder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                    if (needReflect) {
                        if (f.isReader()) {
                            builder.append(buildCast(objectClass, field.getType(),
                                    "(this.access(this.typeOf(\"" +
                                            field.getDeclaringClass().getName() +
                                            "\",\"" + field.getName() + "\",$0))"))
                                    .append(";");
                        } else {
                            builder.append("this.modify(this.typeOf(\"")
                                    .append(field.getDeclaringClass().getName())
                                    .append("\",\"")
                                    .append(field.getName())
                                    .append(",\",$0,")
                                    .append(buildCast(field.getType(), objectClass, "$1"))
                                    .append(");");
                        }
                    } else {
                        if (f.isReader()) {
                            builder.append("$_=")
                                    .append(buildCast(objectClass, field.getType(),
                                            "(($0==this)?(this.target):($0))"))
                                    .append(".")
                                    .append(field.getName())
                                    .append(";");
                        } else {
                            
                        }
                    }
                    String source = builder.toString();
                    getLogger().quiet(source);
                    f.replace(source);
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void buildEntryPoint(Map<CtMember, Integer> hashIds,
                                     CtClass clone)
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
                        .append(")$2).")
                        .append(field.getType().getName())
                        .append("Value();");
            } else {
                builder.append("(")
                        .append(field.getType().getName())
                        .append(")$2;");
            }
            return builder.append("return;}")
                    .toString();
        }

        private String buildMethodInvoke(CtMethod member, int id)
                throws NotFoundException {
            StringBuilder builder = new StringBuilder("if(")
                    .append(id)
                    .append("==$1){");
            CtClass resultType = member.getReturnType();
            if (!CtClass.voidType.equals(resultType)) {
                builder.append(resultType.getName())
                        .append(" result=");
            }
            builder.append(member.getName())
                    .append('(');
            CtClass objectType = getClasses().get("java.lang.Object");
            CtClass[] parameterTypes = member.getParameterTypes();
            if (parameterTypes.length > 0) {
                builder.append(buildCast(objectType, parameterTypes[0], "$2[0]"));
                for (int i = 1; i < parameterTypes.length; ++i) {
                    builder.append(",").append(buildCast(objectType,
                            parameterTypes[i],
                            "$2[" + i + ']'));
                }
            }
            builder.append(");");
            if (!CtClass.voidType.equals(resultType)) {
                builder.append("return ")
                        .append(buildCast(resultType, objectType, "result"))
                        .append(";");
            }
            else {
                builder.append("return null;");
            }
            builder.append('}');
            String source = builder.toString();
            getLogger().quiet(source);
            return source;
        }

        private String buildCast(CtClass form, CtClass to, String name) {
            if (form.isPrimitive()) {
                return "(" + primitiveMapping.get(form).getName() + ".valueOf(" + name + "))";
            } else {
                CtClass box = primitiveMapping.get(to);
                if (box == null) {
                    return "(" +
                            (!form.equals(to) ? ("(" + to.getName() + ")") : "")
                            + name +
                            ")";
                } else {
                    return "(((" + box.getName() + ")" + name + ")." + to.getName() + "Value())";
                }
            }
        }

        private CtClass buildCodeScope(List<CtClass> classes)
                throws NotFoundException, CannotCompileException {
            CtClass clazz = getClasses().makeClass(CODE_SCOPE_CLASS_NAME);
            clazz.defrost();
            clazz.setSuperclass(getClasses().get(CODE_SCOPE_SUPER_CLASS_NAME));
            StringBuilder builder = new StringBuilder("java.lang.Class[] " +
                    "onLoadClasses(org.kexie.android.hotfix.internal.CodeContext context)" +
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
}