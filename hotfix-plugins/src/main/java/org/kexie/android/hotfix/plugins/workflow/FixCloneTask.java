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
import javassist.expr.Cast;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

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
                fixClass(context, hashIds, source, clone);
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
                          CtClass source,
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
            fixMethod(context, source, clone, method);
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
            String checked = checkCast(javaLangObject, parameterTypes[0], "($2[0])");
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

    private void fixMethod(Context context,
                           CtClass source,
                           CtClass clone,
                           CtMethod cloneMethod)
            throws CannotCompileException {
        boolean isInStatic;
        int currentModifiers = cloneMethod.getModifiers();
        if (Modifier.isStatic(currentModifiers)) {
            isInStatic = true;
            currentModifiers = Modifier.clear(currentModifiers, Modifier.STATIC);
            cloneMethod.setModifiers(currentModifiers);
        } else {
            isInStatic = false;
        }
        cloneMethod.instrument(new ExprEditor() {

            @Override
            public void edit(Cast c) throws CannotCompileException {
                super.edit(c);
            }

            @Override
            public void edit(NewExpr e) throws CannotCompileException {
                try {
                    CtConstructor constructor = e.getConstructor();
                    CtClass declaringClass = constructor.getDeclaringClass();
                    boolean isAccessible = clone.getPackageName()
                            .equals(declaringClass.getPackageName());
                    boolean isOverload = declaringClass
                            .hasAnnotation(Annotations.OVERLOAD_ANNOTATION);
                    boolean isEnclose = declaringClass.getEnclosingBehavior() != null
                            || declaringClass.getDeclaringClass() != null;
                    boolean isDirect;
                    if (isAccessible) {
                        isDirect = true;
                    }else {
                        if (isOverload) {
                            isDirect = true;
                        } else {
                            
                        }
                    }

                    if (!isPrimitiveOrPrimitiveArray(declaringClass)) {
                        StringBuilder builder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                        builder.append("this.newInstance(this.typeOf(\"")
                                .append(declaringClass.getName())
                                .append("\",");

                        String source = builder.toString();
                        e.replace(source);
                    }
                } catch (NotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    CtMethod method = m.getMethod();
                    StringBuilder builder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                    CtClass declaringClass = method.getDeclaringClass();
                    boolean isAccessible = isAccessible(clone, method);
                    boolean isOverload = method.hasAnnotation(Annotations.OVERLOAD_ANNOTATION);
                    boolean isSource = declaringClass.equals(source);
                    boolean isSuper = m.isSuper() && method.getDeclaringClass()
                            .equals(source.getSuperclass());
                    boolean isDirect;
                    if (isOverload) {
                        if (isSource) {
                            isDirect = !isSuper;
                        } else {
                            isDirect = false;
                        }
                    } else {
                        if (isAccessible) {
                            isDirect = !isSuper;
                        } else {
                            isDirect = false;
                        }
                    }
                    if (isDirect) {
                        String pramThis;
                        if (Modifier.isStatic(method.getModifiers())) {
                            if (isOverload) {
                                pramThis = clone.getName();
                            } else {
                                pramThis = declaringClass.getName();
                            }
                        } else {
                            //$0 is a ref
                            if (isInStatic) {
                                pramThis = "$0";
                            } else {
                                pramThis = checkTarget(declaringClass, "$0");
                            }
                        }
                        if (!CtClass.voidType.equals(method.getReturnType())) {
                            builder.append("$_=");
                        }
                        builder.append(pramThis)
                                .append(".")
                                .append(method.getName())
                                .append("(");
                        //no box
                        CtClass[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length > 0) {
                            String checked;
                            if (isInStatic) {
                                checked = "$1";
                            } else {
                                if (parameterTypes[0].isPrimitive()) {
                                    checked = "$1";
                                } else {
                                    checked = checkTarget(parameterTypes[0], "$1");
                                }
                            }
                            builder.append(checked);
                            for (int i = 1; i < parameterTypes.length; ++i) {
                                int index = i + 1;
                                if (isInStatic) {
                                    checked = "$" + index;
                                } else {
                                    if (parameterTypes[i].isPrimitive()) {
                                        checked = "$" + index;
                                    } else {
                                        checked = checkTarget(parameterTypes[i], "$" + index);
                                    }
                                }
                                builder.append(",")
                                        .append(checked);
                            }
                        }
                        builder.append(");");
                    } else {
                        String pramThis;
                        if (Modifier.isStatic(method.getModifiers())) {
                            pramThis = "null";
                        } else {
                            //$0 is no ref
                            if (isInStatic) {
                                pramThis = "$0";
                            } else {
                                pramThis = "(($0==this)?(this.getTarget()):($0))";
                            }
                        }
                        CtClass returnType = method.getReturnType();
                        CtClass[] parameterTypes = method.getParameterTypes();
                        StringBuilder invokeBuilder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                        StringBuilder pramsBuilder;
                        StringBuilder pramTypesBuilder;
                        if (parameterTypes.length > 0) {
                            pramsBuilder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                            pramTypesBuilder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                            pramsBuilder.append("new java.lang.Object[]{");
                            pramTypesBuilder.append("new java.lang.Class[]{");
                            String checked;
                            if (isInStatic) {
                                checked = "$1";
                            } else {
                                if (parameterTypes[0].isPrimitive()) {
                                    checked = "(($w)$1)";
                                } else {
                                    checked = "(($1==this)?(this.getTarget()):($1))";
                                }
                            }
                            pramsBuilder.append(checked);

                            if (isPrimitiveOrPrimitiveArray(parameterTypes[0])) {
                                pramTypesBuilder.append(parameterTypes[0].getName())
                                        .append(".class");
                            } else {
                                pramTypesBuilder.append("this.typeOf(\"")
                                        .append(parameterTypes[0].getName())
                                        .append("\")");
                            }

                            for (int i = 1; i < parameterTypes.length; ++i) {
                                pramTypesBuilder.append(",");
                                if (isPrimitiveOrPrimitiveArray(parameterTypes[i])) {
                                    pramTypesBuilder.append(parameterTypes[i].getName())
                                            .append(".class");
                                } else {
                                    pramTypesBuilder.append("this.typeOf(\"")
                                            .append(parameterTypes[i].getName())
                                            .append("\")");
                                }
                                ////////////////////////////////////////
                                int index = i + 1;
                                if (isInStatic) {
                                    checked = "$" + index;
                                } else {
                                    if (parameterTypes[i].isPrimitive()) {
                                        checked = "(($w)$" + index + ")";
                                    } else {
                                        checked = "(($" + index + "==this)?(this.getTarget())"
                                                + ":($" + index + "))";
                                    }
                                }
                                pramsBuilder.append(",")
                                        .append(checked);
                            }
                            pramsBuilder.append("}");
                            pramTypesBuilder.append("}");
                        } else {
                            pramsBuilder = pramTypesBuilder = new StringBuilder("null");
                        }
                        if (!CtClass.voidType.equals(returnType)) {
                            builder.append("$_=");
                        }
                        invokeBuilder.append("this.invoke(")
                                .append(isSuper)
                                .append(",this.typeOf(\"")
                                .append(declaringClass.getName())
                                .append("\"),\"")
                                .append(method.getName())
                                .append("\",")
                                .append(pramTypesBuilder)
                                .append(",")
                                .append(pramThis)
                                .append(",")
                                .append(pramsBuilder)
                                .append(")");
                        if (!CtClass.voidType.equals(returnType)) {
                            String invokeChecked = checkCast(javaLangObject,
                                    method.getReturnType(),
                                    invokeBuilder.toString());
                            builder.append(invokeChecked);
                        } else {
                            builder.append(invokeBuilder);
                        }
                        builder.append(";");
                    }
                    String source = builder.toString();
                    context.getLogger().quiet(source);
                    m.replace(source);
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
                    StringBuilder builder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                    CtField field = f.getField();
                    CtClass declaringClass = field.getDeclaringClass();
                    boolean isAccessible = isAccessible(clone, field);
                    boolean isOverload = field.hasAnnotation(Annotations.OVERLOAD_ANNOTATION);
                    boolean isSource = declaringClass.equals(source);
                    boolean isDirect;
                    //1 被重载的
                    //1.1 来自原始类，已经复制到补丁类上，可直接调用
                    //1.2 否则来自其他补丁类，不可直接调用
                    //2 未被重载的，来自基准包
                    //2.1 补丁类可以直接访问的
                    //2.2 否则需要反射
                    if (isOverload) {
                        isDirect = isSource;
                    } else {
                        isDirect = isAccessible;
                    }
                    if (f.isReader()) {
                        builder.append("$_=");
                    }
                    if (isDirect) {
                        String pramThis;
                        if (f.isStatic()) {
                            if (isOverload) {
                                pramThis = clone.getName();
                            } else {
                                pramThis = declaringClass.getName();
                            }
                        } else {
                            //$0 is no a ref
                            if (isInStatic) {
                                pramThis = "$0";
                            } else {
                                pramThis = checkTarget(declaringClass, "$0");
                            }
                        }
                        builder.append(pramThis)
                                .append(".")
                                .append(field.getName());
                        if (f.isWriter()) {
                            String pramNewValue;
                            if (isInStatic || field.getType().isPrimitive()) {
                                pramNewValue = "$1";
                            } else {
                                pramNewValue = checkTarget(field.getType(), "$1");
                            }
                            builder.append("=")
                                    .append(pramNewValue);
                        }
                        builder.append(";");
                    } else {
                        String pramThis;
                        if (f.isStatic()) {
                            pramThis = "null";
                        } else {
                            if (isInStatic) {
                                pramThis = "$0";
                            } else {
                                pramThis = "(($0==this)?(this.getTarget()):($0))";
                            }
                        }
                        if (f.isReader()) {
                            builder.append("this.access(");
                            if (isPrimitiveOrPrimitiveArray(field.getType())) {
                                builder.append(field.getType().getName())
                                        .append(".class");
                            }
                            else {
                                builder.append("this.typeOf(\"")
                                        .append(declaringClass.getName())
                                        .append("\")");
                            }
                            builder.append(",\"")
                                    .append(field.getName())
                                    .append("\",")
                                    .append(pramThis)
                                    .append(")");
                        } else {
                            String pramNewValue;
                            if (f.isStatic()) {
                                pramNewValue = "$1";
                            } else {
                                if (isInStatic) {
                                    pramNewValue = "$1";
                                } else {
                                    if (field.getType().isPrimitive()) {
                                        pramNewValue = "(($w)$1)";
                                    } else {
                                        pramNewValue = "(($1==this)?(this.getTarget()):($1))";
                                    }
                                }
                            }
                            builder.append("this.modify(");
                            if (isPrimitiveOrPrimitiveArray(field.getType())) {
                                builder.append(field.getType().getName())
                                        .append(".class");
                            }
                            else {
                                builder.append("this.typeOf(\"")
                                        .append(declaringClass.getName())
                                        .append("\")");
                            }
                            builder.append(",\"")
                                    .append(field.getName())
                                    .append("\",")
                                    .append(pramThis)
                                    .append(",")
                                    .append(pramNewValue)
                                    .append(")");
                        }
                        builder.append(";");
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

    /**
     * 使用typeOf时一定要先用此函数检查是否为基本类型或者其数组
     */
    private static boolean isPrimitiveOrPrimitiveArray(CtClass clazz) throws NotFoundException {
        if (clazz.isPrimitive()) {
            return true;
        }
        while (clazz.isArray()) {
            CtClass next = clazz.getComponentType();
            if (next.isPrimitive()) {
                return true;
            } else {
                clazz = next;
            }
        }
        return false;
    }

    private String checkTarget(CtClass type, String name) {
        return "((" + type.getName() + ")((" + name + "==this)?(this.getTarget())" +
                ":((java.lang.Object)" + name + ")))";
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