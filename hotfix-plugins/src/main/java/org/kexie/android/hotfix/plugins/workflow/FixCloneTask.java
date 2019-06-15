package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;
import com.android.utils.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

final class FixCloneTask extends Work<List<Pair<CtClass,CtClass>>,List<CtClass>> {

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
    private static final int BASE_STRING_BUILDER_SIZE = 64;
    private static final String OBJECT_SUPER_CLASS_NAME
            = "org.kexie.android.hotfix.internal.HotCodeExecutor";

    private CtClass javaLangObject;
    private Map<CtClass, CtClass> boxMapping;

    @Override
    ContextWith<List<CtClass>> doWork(ContextWith<List<Pair<CtClass, CtClass>>> context)
            throws TransformException {
        try {
            javaLangObject = context.getClasses().get("java.lang.Object");
            boxMapping = loadBoxMapping(context);
            List<CtClass> classes = new LinkedList<>();
            Map<CtMember, Integer> hashIds = new HashMap<>();
            for (Pair<CtClass, CtClass> entry : context.getData()) {
                hashIds.clear();
                CtClass source = entry.getFirst();
                CtClass clone = entry.getSecond();
                fixClass(context, hashIds, clone);
                makeEntries(context, hashIds, clone);
                classes.add(clone);
            }
            return context.with(classes);
        } catch (NotFoundException | CannotCompileException e) {
            throw new TransformException(e);
        }
    }

    private void fixClass(Context context,
                          Map<CtMember, Integer> hashIds,
                          CtClass clone)
            throws NotFoundException, CannotCompileException {
        clone.setSuperclass(context.getClasses().get(OBJECT_SUPER_CLASS_NAME));
        CtClass annotationType = context.getClasses().get(Annotations.ID_ANNOTATION);
        ConstPool constPool = clone.getClassFile().getConstPool();
        for (CtField field : clone.getDeclaredFields()) {
            fixAnnotation(hashIds, annotationType, constPool, field);
        }
        for (CtMethod method : clone.getDeclaredMethods()) {
            fixAnnotation(hashIds, annotationType, constPool, method);
            fixMethod(context, method);
        }
        clone.addConstructor(CtNewConstructor.make("public " +
                clone.getSimpleName() + "(){super();}", clone));
    }


    private void makeEntries(Context context,
                             Map<CtMember, Integer> hashIds,
                             CtClass clone)
            throws CannotCompileException, NotFoundException {
        CtMethod invoke = CtNewMethod.make(EMPTY_INVOKE, clone);
        CtMethod modify = CtNewMethod.make(EMPTY_MODIFY, clone);
        CtMethod access = CtNewMethod.make(EMPTY_ACCESS, clone);
        for (Map.Entry<CtMember, Integer> entry : hashIds.entrySet()) {
            CtMember member = entry.getKey();
            if (member instanceof CtMethod) {
                String source = makeInvokeEntry(context, (CtMethod) entry.getKey(), entry.getValue());
                invoke.insertBefore(source);
            } else if (member instanceof CtField) {
                CtField field = (CtField) member;
                if (!Modifier.isFinal(field.getModifiers())) {
                    modify.insertBefore(makeModifyEntry(field, entry.getValue()));
                }
                access.insertBefore(makeAccessEntry(field, entry.getValue()));
            }
        }
        clone.addMethod(invoke);
        clone.addMethod(modify);
        clone.addMethod(access);
    }

    private String makeAccessEntry(CtField field, int id) {
        return "if(" + id + "==" + "$1" + "){return " + field.getName() + ";}";
    }

    private String makeModifyEntry(CtField field, int id) {
        return "if(" + id + "==" + "$1" + "){" + "this." + field.getName() + "=" + "return;}";
    }

    private String makeInvokeEntry(Context context, CtMethod member, int id)
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
        CtClass[] parameterTypes = member.getParameterTypes();
        if (parameterTypes.length > 0) {
            String checked = checkCast(javaLangObject, parameterTypes[0], "($2[0]))");
            builder.append(checked);
            for (int i = 1; i < parameterTypes.length; ++i) {
                checked = checkCast(javaLangObject, parameterTypes[0], "($2[" + i + "])");
                builder.append(",")
                        .append(checked);
            }
        }
        builder.append(");");
        if (!CtClass.voidType.equals(resultType)) {
            builder.append("return result;");
        } else {
            builder.append("return null;");
        }
        builder.append('}');
        String source = builder.toString();
        context.getLogger().quiet(source);
        return source;
    }

    private void fixMethod(Context context, CtMethod current)
            throws CannotCompileException, NotFoundException {
        CtClass currentClass = current.getDeclaringClass();
        boolean isInStatic;
        int currentModifiers = current.getModifiers();
        if (Modifier.isStatic(currentModifiers)) {
            isInStatic = true;
            currentModifiers = Modifier.clear(currentModifiers, Modifier.STATIC);
            current.setModifiers(currentModifiers);
        } else {
            isInStatic = false;
        }
        current.instrument(new ExprEditor() {

            @Override
            public void edit(MethodCall m) throws CannotCompileException {

            }

            @Override
            public void edit(ConstructorCall c) throws CannotCompileException {
                c.replace(";");
            }

            @Override
            public void edit(FieldAccess f) throws CannotCompileException {
                try {
                    StringBuilder builder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                    CtField field = f.getField();
                    boolean accessible = isAccessible(currentClass, field);
                    if (f.isReader()) {
                        builder.append("$_=");
                    }
                    if (accessible) {
                        CtClass declaringClass = field.getDeclaringClass();
                        if (f.isStatic()) {
                            builder.append(declaringClass.getName());
                        } else {
                            if (isInStatic) {
                                builder.append("$0");
                            } else {
                                builder.append(checkThis(declaringClass, "$0"));
                            }
                        }
                        builder.append(".")
                                .append(field.getName());
                        if (f.isWriter()) {
                            builder.append("=$1");
                        }
                        builder.append(";");
                    } else {
                        if (f.isReader()) {
                            builder.append("this.access(this.typeOf\"")
                                    .append(field.getName())
                                    .append("\",\"")
                                    .append(field.getName())
                                    .append("\",");

                        } else {
                            
                        }
                    }
                    String source = builder.toString();
                    context.getLogger().quiet(source);
                    f.replace(source);
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    private void
    fixAnnotation(Map<CtMember, Integer> hashIds,
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
        attribute.removeAnnotation(Annotations.OVERLOAD_ANNOTATION);
        setAnnotations(member, attribute);
    }


    private static AnnotationsAttribute
    getAnnotations(CtMember member,
                   String name) {
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
        if (attribute == null) {
            attribute = new AnnotationsAttribute(constPool, name);
        }
        return attribute;
    }

    private static void
    setAnnotations(CtMember member,
                   AnnotationsAttribute attribute) {
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

    private static boolean isAccessible(CtClass clone, CtMember member) {
        CtClass declaringClass = member.getDeclaringClass();
        if (Modifier.isPublic(declaringClass.getModifiers())) {
            if (Modifier.isPublic(member.getModifiers())) {
                return true;
            }
        }
        if (declaringClass.getPackageName().equals(clone.getPackageName())) {
            return !Modifier.isPrivate(member.getModifiers());
        }
        return false;
    }

    private String checkThis(CtClass type, String name) {
        if (type.isPrimitive()) {
            return name;
        } else {
            return "((" + type.getName() + ")((this==" + name + ")?(this.getTarget())" +
                    ":((java.lang.Object)this)))";
        }
    }

    private String checkCast(CtClass from, CtClass to, String name) {
        if (from.equals(to)) {
            return name;
        }
        if (!from.isPrimitive() && to.isPrimitive()) {
            CtClass box = boxMapping.get(to);
            if (!box.equals(from)) {
                return "((" + box.getName() + ")" + name + ")." + to.getName() + "Value()";
            } else {
                return name + "." + to.getName() + "Value()";
            }
        }
        if (from.isPrimitive() && !to.isPrimitive()) {
            return boxMapping.get(from).getName() + ".valueOf(" + name + ")";
        }
        return "((" + to.getName() + ")" + name + ")";
    }

    private static Map<CtClass, CtClass> loadBoxMapping(Context context) {
        try {
            ClassPool classPool = context.getClasses();
            Map<CtClass, CtClass> result = new HashMap<>();
            for (Class clazz : new Class[]{
                    Byte.class,
                    Short.class,
                    Integer.class,
                    Long.class,
                    Float.class,
                    Double.class,
                    Character.class,
                    Boolean.class
            }) {
                Class c = (Class) clazz.getDeclaredField("TYPE").get(null);
                result.put(classPool.get(c.getName()), classPool.get(clazz.getName()));
            }
            return result;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
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
}