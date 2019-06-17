package org.kexie.android.hotfix.plugins.workflow;

import com.android.utils.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.reactivex.exceptions.Exceptions;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.expr.Cast;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

final class FixCloneClassTask extends Work<List<Pair<CtClass,CtClass>>,List<CtClass>> {

    private static final int BASE_STRING_BUILDER_SIZE = 64;

    private CtClass javaLangObject;
    private Map<CtClass, CtClass> boxMapping;

    @Override
    ContextWith<List<CtClass>>
    doWork(ContextWith<List<Pair<CtClass, CtClass>>> context) {
        try {
            javaLangObject = context.getClasses().get("java.lang.Object");
            boxMapping = loadBoxMapping(context);
            List<CtClass> classes = new LinkedList<>();
            for (Pair<CtClass, CtClass> entry : context.getData()) {
                CtClass source = entry.getFirst();
                CtClass clone = entry.getSecond();
                fixClass(context, source, clone);
                classes.add(clone);
            }
            return context.with(classes);
        } catch (NotFoundException | CannotCompileException e) {
            throw Exceptions.propagate(e);
        }
    }

    private void fixClass(Context context,
                          CtClass source,
                          CtClass clone)
            throws NotFoundException, CannotCompileException {
        clone.setSuperclass(context.getClasses().get(TypeNames.OBJECT_SUPER_CLASS_NAME));
        for (CtField field : clone.getDeclaredFields()) {
            fixAnnotation(field);
        }
        for (CtMethod method : clone.getDeclaredMethods()) {
            fixAnnotation(method);
            fixMethod(context, source, clone, method);
        }
        clone.addConstructor(CtNewConstructor.make("public " +
                clone.getSimpleName() + "(){super();}", clone));
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
//                super.edit(c);
            }

            @Override
            public void edit(NewExpr e) throws CannotCompileException {
//                try {
//                    CtConstructor constructor = e.getConstructor();
//                    CtClass declaringClass = constructor.getDeclaringClass();
//                    boolean isAccessible = Modifier.isPublic(declaringClass.getModifiers());
//                    boolean isAdded = declaringClass
//                            .hasAnnotation(TypeNames.OVERLOAD_ANNOTATION);
//                    boolean isEnclose = declaringClass.getEnclosingBehavior() != null
//                            || declaringClass.getDeclaringClass() != null;
//                    boolean isDirect;
//                    if (isAccessible) {
//                        isDirect = true;
//                    }else {
//                        //否则至少是跟它同在一个包下的类
//                        if (isAdded) {
//                            //新添加的类型无论如何都是可以被访问的
//                            //包括新添加的内部类
//                            isDirect = true;
//                        } else {
//                            //如果不是新添加的类
//                            //那么他们至少在一个包下
//                            //如果是内部类就要反射
//                            //如果是顶层类就可以直接访问
//                            isDirect = !isEnclose;
//                        }
//                    }
//                    //TODO
//                    StringBuilder builder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
//                    if (isDirect) {
//
//                    }else {
//                        if (!isPrimitiveOrPrimitiveArray(declaringClass)) {
//                            builder.append("this.newInstance(this.typeOf(\"")
//                                    .append(declaringClass.getName())
//                                    .append("\",");
//
//
//                        }
//                    }
//                    String source = builder.toString();
//                    e.replace(source);
//                } catch (NotFoundException ex) {
//                    throw new RuntimeException(ex);
//                }
            }

            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    CtMethod method = m.getMethod();
                    StringBuilder builder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                    CtClass declaringClass = method.getDeclaringClass();
                    boolean isAccessible = isAccessible(clone, method);
                    boolean isOverload = method.hasAnnotation(TypeNames.OVERLOAD_ANNOTATION);
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
                                pramThis = "(" + TypeNames.UTIL_CLASS_NAME + ".checkArgument(this,$0))";
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
                                    checked = "(" + TypeNames.UTIL_CLASS_NAME + ".checkArgument(this,$1))";
                                }
                            }
                            pramsBuilder.append(checked);

                            if (isPrimitiveOrPrimitiveArray(parameterTypes[0])) {
                                pramTypesBuilder.append(parameterTypes[0].getName())
                                        .append(".class");
                            } else {
                                pramTypesBuilder.append(TypeNames.UTIL_CLASS_NAME + ".typeOf(\"")
                                        .append(parameterTypes[0].getName())
                                        .append("\")");
                            }

                            for (int i = 1; i < parameterTypes.length; ++i) {
                                pramTypesBuilder.append(",");
                                if (isPrimitiveOrPrimitiveArray(parameterTypes[i])) {
                                    pramTypesBuilder.append(parameterTypes[i].getName())
                                            .append(".class");
                                } else {
                                    pramTypesBuilder.append(TypeNames.UTIL_CLASS_NAME + ".typeOf(\"")
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
                                        checked = "(" + TypeNames.UTIL_CLASS_NAME
                                                + ".checkArgument(this,$" + index + "))";
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
                        invokeBuilder.append(TypeNames.UTIL_CLASS_NAME + ".invoke(")
                                .append(isSuper)
                                .append("," + TypeNames.UTIL_CLASS_NAME + ".typeOf(\"")
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
                //超类调用处是无法直接修复的
                c.replace(";");
            }

            @Override
            public void edit(FieldAccess f) throws CannotCompileException {
                try {
                    StringBuilder builder = new StringBuilder(BASE_STRING_BUILDER_SIZE);
                    CtField field = f.getField();
                    CtClass declaringClass = field.getDeclaringClass();
                    boolean isAccessible = isAccessible(clone, field);
                    boolean isOverload = field.hasAnnotation(TypeNames.OVERLOAD_ANNOTATION);
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
                                pramThis = "(" + TypeNames.UTIL_CLASS_NAME + ".checkArgument(this,$0))";
                            }
                        }
                        if (f.isReader()) {
                            builder.append(TypeNames.UTIL_CLASS_NAME + ".access(");
                            if (isPrimitiveOrPrimitiveArray(field.getType())) {
                                builder.append(field.getType().getName())
                                        .append(".class");
                            } else {
                                builder.append(TypeNames.UTIL_CLASS_NAME + ".typeOf(\"")
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
                                        pramNewValue = "(" + TypeNames.UTIL_CLASS_NAME
                                                + ".checkArgument(this,$1))";
                                    }
                                }
                            }
                            builder.append(TypeNames.UTIL_CLASS_NAME + ".modify(");
                            if (isPrimitiveOrPrimitiveArray(field.getType())) {
                                builder.append(field.getType().getName())
                                        .append(".class");
                            } else {
                                builder.append(TypeNames.UTIL_CLASS_NAME + ".typeOf(\"")
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

    private void fixAnnotation(CtMember member) {
        AnnotationsAttribute attribute = getAnnotations(member);
        if (attribute != null) {
            attribute.removeAnnotation(TypeNames.OVERLOAD_ANNOTATION);
            setAnnotations(member, attribute);
        }
    }

    private static AnnotationsAttribute
    getAnnotations(CtMember member) {
        ConstPool constPool = member.getDeclaringClass()
                .getClassFile()
                .getConstPool();
        AnnotationsAttribute attribute = null;
        if (member instanceof CtField) {
            attribute = (AnnotationsAttribute) ((CtField) member)
                    .getFieldInfo()
                    .getAttribute(AnnotationsAttribute.invisibleTag);
        } else if (member instanceof CtMethod) {
            attribute = (AnnotationsAttribute) ((CtMethod) member)
                    .getMethodInfo()
                    .getAttribute(AnnotationsAttribute.invisibleTag);
        } else if (member instanceof CtConstructor) {
            attribute = (AnnotationsAttribute) ((CtConstructor) member)
                    .getMethodInfo()
                    .getAttribute(AnnotationsAttribute.invisibleTag);
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
        return "((" + type.getName() + ")" + TypeNames.UTIL_CLASS_NAME
                + ".checkArgument(this," + name + "))";
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

}