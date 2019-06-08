package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;


/**
 * 生成Executable的class
 */
final class BuildTask extends Work<List<CtClass>, CtClass> {

    private static final String PATCHED_ANNOTATION = "org.kexie.android.hotfix.Patched";

    @Override
    ContextWith<CtClass>
    doWork(ContextWith<List<CtClass>> context) throws TransformException {
        try {
            List<CtMethod> methods = context.getData().stream()
                    .flatMap((Function<CtClass, Stream<CtMethod>>)
                            ctClass -> Arrays.stream(ctClass.getDeclaredMethods()))
                    .filter(method -> method.hasAnnotation(PATCHED_ANNOTATION))
                    .collect(Collectors.toCollection(LinkedList::new));
            Builder builder = new Builder(context, methods,
                    Collections.emptyList());
            return context.with(builder.get());
        } catch (NotFoundException | CannotCompileException e) {
            throw new TransformException(e);
        }
    }

    private final static class Builder extends ContextWrapper {
        private static final String CONSTRUCTOR_SOURCE = "public ExecutableImpl" +
                "(org.kexie.android.hotfix.internal.DynamicExecutionEngine executionEngine)" +
                "{super(executionEngine);}";
        private static final String PATCH_SUPER_CLASS_NAME
                = "org.kexie.android.hotfix.internal.Executable";
        private static final String PATCH_CLASS_NAME
                = "org.kexie.android.hotfix.internal.ExecutableImpl";

        private final Map<CtMethod, Integer> hashIds = new HashMap<>();
        private final Map<CtClass, CtClass> primitiveMapping;
        private final CtClass clazz;

        Builder(
                Context context,
                List<CtMethod> methods,
                List<CtField> fields)
                throws NotFoundException, CannotCompileException {
            super(context);
            primitiveMapping = getMapping(context.getClasses());
            clazz = context.getClasses().makeClass(PATCH_CLASS_NAME);
            clazz.setSuperclass(context.getClasses().get(PATCH_SUPER_CLASS_NAME));
            clazz.addConstructor(getConstructor());
            clazz.addMethod(getMethodImpl1(methods));
            clazz.addMethod(getMethodImpl2(fields));
        }

        private CtConstructor getConstructor() throws CannotCompileException {
            return CtNewConstructor.make(CONSTRUCTOR_SOURCE, clazz);
        }

        private CtMethod getMethodImpl1(List<CtMethod> methods)
                throws NotFoundException, CannotCompileException {
            StringBuilder methodsBuilder = new StringBuilder(
                    "protected java.lang.Object " +
                            "invokeDynamicMethod(" +
                            "int id," +
                            "java.lang.Object target," +
                            "java.lang.Object[] prams)" +
                            "throws java.lang.Throwable{" +
                            "org.kexie.android.hotfix.internal.ExecutionEngine " +
                            "executionEngine=this.getExecutionEngine();" +
                            "switch(id){"
            );
            for (CtMethod method : methods) {
                int id = hash(method);
                methodsBuilder.append("case ")
                        .append(id)
                        .append(":{");
                CtClass[] pramTypes = method.getParameterTypes();
                for (int i = 0; i < pramTypes.length; ++i) {
                    CtClass pType = pramTypes[i];
                    methodsBuilder.append(pType.getName())
                            .append(" $")
                            .append(i);
                    CtClass box;
                    if ((box = primitiveMapping.get(pType)) != null) {
                        methodsBuilder.append("=((")
                                .append(box.getName())
                                .append(")prams[")
                                .append(i)
                                .append("]).")
                                .append(pType.getName())
                                .append("Value();");
                    } else {
                        methodsBuilder
                                .append("=(")
                                .append(pType.getName())
                                .append(")prams[")
                                .append(i)
                                .append("];");
                    }
                }
                methodsBuilder.append("return ")
                        .append(method.getName())
                        .append("(");
                if (pramTypes.length > 1) {
                    methodsBuilder.append("$0");
                    for (int i = 1; i < pramTypes.length; ++i) {
                        methodsBuilder.append(",$")
                                .append(i);
                    }
                }
                methodsBuilder.append(");}");

            }
            methodsBuilder.append("default:{throw new java.lang.NoSuchMethodException();}}}");
            return CtNewMethod.make(methodsBuilder.toString(), clazz);
        }

        private CtMethod getMethodImpl2(List<CtField> fields)
                throws CannotCompileException, NotFoundException {
            StringBuilder methodsBuilder = new StringBuilder("protected void " +
                    "onLoad(org.kexie.android.hotfix.internal.Metadata metadata){");
            for (CtField field : fields) {
                methodsBuilder.append("metadata.addFiled(\"")
                        .append(field.getDeclaringClass().getName())
                        .append("\",\"")
                        .append(field.getName())
                        .append("\");");
            }
            for (Map.Entry<CtMethod, Integer> entry : hashIds.entrySet()) {
                methodsBuilder.append("metadata.addMethod(")
                        .append(entry.getValue())
                        .append(",\"")
                        .append(entry.getKey().getDeclaringClass().getName())
                        .append("\",\"")
                        .append(entry.getKey().getName())
                        .append("\",");
                CtClass[] pramTypes = entry.getKey().getParameterTypes();
                if (pramTypes.length < 1) {
                    methodsBuilder.append("null");
                } else {
                    methodsBuilder.append("new java.lang.String[]{\"")
                            .append(pramTypes[0].getName())
                            .append('\"');
                    for (int i = 1; i < pramTypes.length; ++i) {
                        methodsBuilder.append(",\"")
                                .append(pramTypes[i].getName())
                                .append("\"");
                    }
                    methodsBuilder.append("}");
                }
                methodsBuilder.append(");");
            }
            methodsBuilder.append("}");
            return CtNewMethod.make(methodsBuilder.toString(), clazz);
        }


        CtClass get() {
            return clazz;
        }

        /**
         * 开地址法确保散列始终不会碰撞
         * {@link Integer#MIN_VALUE}是无效值
         */
        private int hash(CtMethod method) {
            int id = method.getLongName().hashCode();
            while (true) {
                if (!hashIds.containsValue(id)) {
                    hashIds.put(method, id);
                    return id;
                }
                id = id == Integer.MAX_VALUE ? Integer.MIN_VALUE + 1 : id + 1;
            }
        }

        private static Map<CtClass, CtClass> getMapping(ClassPool classPool)
                throws NotFoundException {
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