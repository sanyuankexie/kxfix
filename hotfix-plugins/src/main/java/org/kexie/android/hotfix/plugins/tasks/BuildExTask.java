package org.kexie.android.hotfix.plugins.tasks;

import com.android.build.api.transform.TransformException;
import com.android.utils.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;

public class BuildExTask implements Task<Pair<List<CtField>,List<CtMethod>>, CtClass> {

    private static final String PATCH_SUPER_CLASS_NAME = "org.kexie.android.hotfix.internal.Executable";
    private static final String PATCH_CLASS_NAME_SUFFIX = "Impl";

    @Override
    public CtClass apply(Context context, Pair<List<CtField>, List<CtMethod>> input)
            throws TransformException {
        List<CtField> fields = input.getFirst();
        List<CtMethod> methods = input.getSecond();
        try {
            CtClass patch = context.mClassPool.makeClass(
                    PATCH_SUPER_CLASS_NAME + PATCH_CLASS_NAME_SUFFIX
            );
            patch.defrost();
            CtClass superClass = context.mClassPool.get(PATCH_SUPER_CLASS_NAME);
            patch.subclassOf(superClass);
            String source = buildConstructor();
            patch.addConstructor(CtNewConstructor.make(source, patch));
            Map<CtMethod, Integer> hashIds = new HashMap<>();
            source = buildInvokeDynamic(hashIds, methods);
            patch.addMethod(CtNewMethod.make(source, patch));
            source = buildOnLoad(hashIds, fields);
            patch.addMethod(CtNewMethod.make(source, patch));
            patch.freeze();
            return patch;
        } catch (NotFoundException | CannotCompileException e) {
            throw new TransformException(e);
        }
    }

    private static String buildConstructor() {
        return "public ExecutableImpl" +
                "(org.kexie.android.hotfix.internal.DynamicExecutionEngine executionEngine)" +
                "{super(executionEngine);}";
    }

    private static String buildInvokeDynamic(
            Map<CtMethod, Integer> hashIds,
            List<CtMethod> methods) throws NotFoundException, CannotCompileException {
        StringBuilder methodsBuilder = new StringBuilder(
                "protected java.lang.Object " +
                        "invokeDynamicMethod(" +
                        "int id, " +
                        "java.lang.Object target, " +
                        "java.lang.Object[] prams)" +
                        "throws java.lang.Throwable {" +
                        "org.kexie.android.hotfix.internal.ExecutionEngine " +
                        "executionEngine = this.getExecutionEngine();" +
                        "switch (id) {"
        );
        for (CtMethod method : methods) {
            int id = hashMethodId(hashIds, method);
            methodsBuilder.append("case ")
                    .append(id)
                    .append(": {");
            CtClass[] pramTypes = method.getParameterTypes();
            for (int i = 0; i < pramTypes.length; ++i) {
                methodsBuilder.append(pramTypes[i].getName())
                        .append(" $")
                        .append(i)
                        .append("=(")
                        .append(pramTypes[i].getName())
                        .append(")prams[")
                        .append(i)
                        .append("];");
            }

            methodsBuilder.append("return null;}");
        }
        methodsBuilder.append("default: {throw new java.lang.NoSuchMethodException();}}}");
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
                methodsBuilder.append("new java.lang.String[")
                        .append(pramTypes.length)
                        .append("]{\"")
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