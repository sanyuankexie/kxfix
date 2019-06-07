package org.kexie.android.hotfix.plugins.workflow;

import com.android.utils.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.exceptions.Exceptions;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;

public class BuildTask implements Workflow<Pair<List<CtField>,List<CtMethod>>, CtClass> {

    private static final String PATCH_SUPER_CLASS_NAME = "org.kexie.android.hotfix.internal.Executable";
    private static final String PATCH_CLASS_NAME_SUFFIX = "Impl";

    @Override
    public ContextWith<CtClass>
    apply(ContextWith<Pair<List<CtField>, List<CtMethod>>> contextWith) {
        List<CtField> fields = contextWith.getInput().getFirst();
        List<CtMethod> methods = contextWith.getInput().getSecond();
        try {
            CtClass patch = contextWith
                    .getContext()
                    .getClasses()
                    .makeClass(
                            PATCH_SUPER_CLASS_NAME + PATCH_CLASS_NAME_SUFFIX
                    );
            patch.defrost();
            CtClass superClass = contextWith
                    .getContext()
                    .getClasses()
                    .get(PATCH_SUPER_CLASS_NAME);
            patch.setSuperclass(superClass);
            Map<CtMethod, Integer> hashIds = new HashMap<>();
            String source = buildInvokeDynamic(contextWith.getContext()
                    .getClasses(), hashIds, methods);
            patch.addMethod(CtNewMethod.make(source, patch));
            source = buildOnLoad(hashIds, fields);
            patch.addMethod(CtNewMethod.make(source, patch));
            source = buildConstructor();
            patch.addConstructor(CtNewConstructor.make(source, patch));
            patch.freeze();
            return contextWith.getContext().with(patch);
        } catch (NotFoundException | CannotCompileException e) {
            throw Exceptions.propagate(e);
        }
    }

    private static String buildConstructor() {
        return "public ExecutableImpl" +
                "(org.kexie.android.hotfix.internal.DynamicExecutionEngine executionEngine)" +
                "{super(executionEngine);}";
    }

    private static String buildInvokeDynamic(
            ClassPool classPool,
            Map<CtMethod, Integer> hashIds,
            List<CtMethod> methods) throws NotFoundException {
        Map<CtClass, CtClass> bootTypes = new HashMap<>();
        bootTypes.put(CtClass.booleanType, classPool.get(Boolean.class.getName()));
        bootTypes.put(CtClass.charType, classPool.get(Character.class.getName()));
        bootTypes.put(CtClass.doubleType, classPool.get(Double.class.getName()));
        bootTypes.put(CtClass.floatType, classPool.get(Float.class.getName()));
        bootTypes.put(CtClass.intType, classPool.get(Integer.class.getName()));
        bootTypes.put(CtClass.shortType, classPool.get(Short.class.getName()));
        bootTypes.put(CtClass.longType, classPool.get(Long.class.getName()));
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
            int id = hashMethodId(hashIds, method);
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
                if ((box = bootTypes.get(pType)) != null) {
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
            methodsBuilder.append("return null;}");
        }
        methodsBuilder.append("default:{throw new java.lang.NoSuchMethodException();}}}");
        return methodsBuilder.toString();
    }


    private static String buildOnLoad(
            Map<CtMethod, Integer> hashIds,
            List<CtField> fields) throws NotFoundException {


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
        System.out.println(methodsBuilder);
        return methodsBuilder.toString();
    }

    /**
     * 开地址法确保散列始终不会碰撞
     * {@link Integer#MIN_VALUE}是无效值
     */
    private static int hashMethodId(
            Map<CtMethod, Integer> hashIds,
            CtMethod method) {
        int id = method.getLongName().hashCode();
        while (true) {
            if (!hashIds.containsValue(id)) {
                hashIds.put(method, id);
                return id;
            }
            id = id == Integer.MAX_VALUE ? Integer.MIN_VALUE + 1 : id + 1;
        }
    }
}
