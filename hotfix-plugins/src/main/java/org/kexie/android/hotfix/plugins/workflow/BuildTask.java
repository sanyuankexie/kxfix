package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;


/**
 * 生成Executable的class
 */
final class BuildTask extends Work<List<CtClass>, CtClass> {

    @Override
    ContextWith<CtClass>
    doWork(ContextWith<List<CtClass>> context) throws TransformException, IOException {
        try {
            List<CtBehavior> methods = context.getData().stream()
                    .flatMap((Function<CtClass, Stream<CtBehavior>>)
                            ctClass -> Arrays.stream(ctClass.getDeclaredBehaviors()))
                    .filter(method -> method.hasAnnotation(Annotations.PATCHED_ANNOTATION))
                    .collect(Collectors.toCollection(LinkedList::new));
            List<CtField> fields = context.getData().stream()
                    .flatMap((Function<CtClass, Stream<CtField>>)
                            ctClass -> Arrays.stream(ctClass.getDeclaredFields()))
                    .filter(field -> field.hasAnnotation(Annotations.PATCHED_ANNOTATION))
                    .collect(Collectors.toCollection(LinkedList::new));
            Builder builder = new Builder(context);
            return context.with(builder.skeleton(methods, fields)
                    .inline(context.getData())
                    .build());
        } catch (NotFoundException | CannotCompileException e) {
            throw new TransformException(e);
        }
    }

    private final static class Builder extends ContextWrapper {
        private static final String CONSTRUCTOR_SOURCE
                = "public ExecutableImpl" +
                "(org.kexie.android.hotfix.internal.DynamicExecutionEngine executionEngine)" +
                "{super(executionEngine);}";
        private static final String PATCH_SUPER_CLASS_NAME
                = "org.kexie.android.hotfix.internal.Executable";
        private static final String PATCH_CLASS_NAME
                = "org.kexie.android.hotfix.internal.ExecutableImpl";

        private final Map<CtBehavior, Integer> hashIds = new HashMap<>();
        private final Map<CtClass, CtClass> primitiveMapping;
        private CtClass clazz;

        Builder(Context context)
                throws NotFoundException {
            super(context);
            primitiveMapping = getMapping(context.getClasses());
            clazz = getClasses().makeClass(PATCH_CLASS_NAME);
            clazz.defrost();
        }

        /**
         * 构造骨架类
         */
        Builder skeleton(List<CtBehavior> methods,
                         List<CtField> fields)
                throws NotFoundException,
                CannotCompileException {
            clazz.setSuperclass(getClasses().get(PATCH_SUPER_CLASS_NAME));
            clazz.addConstructor(getConstructor());
            clazz.addMethod(getMethodImpl1(methods));
            clazz.addMethod(getMethodImpl2(fields));
            return this;
        }

        /**
         * 内联受调用方法
         */
        Builder inline(List<CtClass> classes) throws IOException, CannotCompileException {
            for (CtClass clazz : classes) {
                InputStream inputStream = toInputStream(clazz);

            }
            return this;
        }

        CtClass build() {
            clazz.freeze();
            return clazz;
        }

        private CtConstructor getConstructor()
                throws CannotCompileException {
            return CtNewConstructor.make(CONSTRUCTOR_SOURCE, clazz);
        }

        private CtMethod getMethodImpl1(List<CtBehavior> methods)
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
            for (CtBehavior method : methods) {
                int id = hash(method);
                methodsBuilder.append("case ")
                        .append(id)
                        .append(":{");
                CtClass[] pramTypes = method.getParameterTypes();
                for (int i = 0; i < pramTypes.length; ++i) {
                    CtClass pramType = pramTypes[i];
                    methodsBuilder.append(pramType.getName())
                            .append(" $")
                            .append(i);
                    CtClass box;
                    if ((box = primitiveMapping.get(pramType)) != null) {
                        methodsBuilder.append("=((")
                                .append(box.getName())
                                .append(")prams[")
                                .append(i)
                                .append("]).")
                                .append(pramType.getName())
                                .append("Value();");
                    } else if (Modifier.isPackage(pramType.getModifiers())) {
                        methodsBuilder.append("=pram[")
                                .append(i)
                                .append("];");
                    } else {
                        methodsBuilder
                                .append("=(")
                                .append(pramType.getName())
                                .append(")prams[")
                                .append(i)
                                .append("];");
                    }
                }
                methodsBuilder.append("return null;}");
            }
            methodsBuilder.append("default:{throw new java.lang.NoSuchMethodException();}}}");
            String source = methodsBuilder.toString();
            getLogger().quiet(source);
            return CtNewMethod.make(source, clazz);
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
            for (Map.Entry<CtBehavior, Integer> entry : hashIds.entrySet()) {
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
            String source = methodsBuilder.toString();
            getLogger().quiet(source);
            return CtNewMethod.make(source, clazz);
        }

        /**
         * 开地址法确保散列始终不会碰撞
         * {@link Integer#MIN_VALUE}是无效值
         */
        private int hash(CtBehavior method) {
            int id = method.getLongName().hashCode();
            while (true) {
                if (!hashIds.containsValue(id)) {
                    hashIds.put(method, id);
                    return id;
                }
                id = id == Integer.MAX_VALUE ? Integer.MIN_VALUE + 1 : id + 1;
            }
        }

        private static InputStream toInputStream(CtClass ctClass)
                throws IOException, CannotCompileException {
            byte[] bytes = ctClass.toBytecode();
            return new ByteArrayInputStream(bytes);
        }

        private CtClass toCtClass(byte[] bytes) throws IOException {
            InputStream inputStream = new ByteArrayInputStream(bytes);
            return getClasses().makeClass(inputStream);
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