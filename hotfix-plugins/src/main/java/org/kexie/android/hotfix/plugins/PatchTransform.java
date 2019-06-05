package org.kexie.android.hotfix.plugins;

import com.android.SdkConstants;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.kexie.android.hotfix.Hotfix;
import org.kexie.android.hotfix.Patched;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Android Studio Plugin 可以完全使用Java来进行开发
 * 而Java是强类型非动态的
 * 特别适合用来构建复杂臃肿但又可靠的的系统
 * 所以这里没有使用groovy
 */
public class PatchTransform extends Transform {

    private static final String PATCH_SUPER_CLASS_NAME = "org.kexie.android.hotfix.internal.Executable";
    private static final String PATCH_CLASS_NAME_SUFFIX = "Impl";

    private final Logger mLogger;
    private final ClassPool mClassPool = new ClassPool();
    private final Project mProject;

    PatchTransform(Project project) {
        mProject = project;
        mLogger = project.getLogger();
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void transform(TransformInvocation transformInvocation)
            throws IOException, TransformException {
        long startTime = System.currentTimeMillis();
        mLogger.quiet("==================patched start===================");
        transformInvocation.getOutputProvider().deleteAll();
        File outDir = transformInvocation.getOutputProvider()
                .getContentLocation("main", getOutputTypes(), getScopes(), Format.DIRECTORY);
        List<CtClass> loadedClass = loadInputClasses(transformInvocation.getInputs());
        File opened = doTransform(loadedClass);
        long cost = (System.currentTimeMillis() - startTime) / 1000;
        mLogger.quiet("==================patched finish==================");
        finishWithDirectory(opened);
    }

    private static void finishWithDirectory(File directory) throws IOException {
        URL url = ClassLoader.getSystemClassLoader().getResource("./icon.png");
        Image image = Toolkit.getDefaultToolkit().createImage(url);
        image = image.getScaledInstance(40, 40, Image.SCALE_DEFAULT);
        ImageIcon imageIcon = new ImageIcon(image);
        JOptionPane.showConfirmDialog(null,
                "补丁生成完成", "提示",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                imageIcon);
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(directory);
        }
        System.exit(0);
    }

    private List<CtClass> loadInputClasses(Collection<TransformInput> inputs)
            throws IOException, TransformException {
        AppExtension android = mProject.getExtensions().getByType(AppExtension.class);
        String[] extension = {SdkConstants.EXT_CLASS};
        List<String> classNames = new LinkedList<>();
        try {
            for (File classpath : android.getBootClasspath()) {
                mClassPool.appendClassPath(classpath.getAbsolutePath());
            }
            for (TransformInput input : inputs) {
                for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                    String directory = directoryInput.getFile().getAbsolutePath();
                    mClassPool.insertClassPath(directory);
                    for (File file : FileUtils.listFiles(directoryInput.getFile(),
                            extension, true)) {
                        String className = file.getAbsolutePath().substring(
                                directory.length() + 1,
                                file.getAbsolutePath().length() -
                                        SdkConstants.DOT_CLASS.length()
                        ).replaceAll(Matcher.quoteReplacement(File.separator), ".");
                        classNames.add(className);
                    }
                }
                for (JarInput jarInput : input.getJarInputs()) {
                    mClassPool.insertClassPath(jarInput.getFile().getAbsolutePath());
                    JarFile jarFile = new JarFile(jarInput.getFile());
                    Enumeration<JarEntry> enumeration = jarFile.entries();
                    while (enumeration.hasMoreElements()) {
                        JarEntry jarEntry = enumeration.nextElement();
                        String className = jarEntry.getName();
                        if (className.endsWith(SdkConstants.DOT_CLASS)) {
                            className = className.substring(0,
                                    className.length() - SdkConstants.DOT_CLASS.length()
                            ).replaceAll("/", ".");
                            classNames.add(className);
                        }
                    }
                }
            }
        } catch (NotFoundException e) {
            throw new TransformException(e);
        }
        List<CtClass> classes = new ArrayList<>(classNames.size());
        for (String className : classNames) {
            try {
                classes.add(mClassPool.get(className));
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }
        return classes;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File doTransform(List<CtClass> loaded)
            throws TransformException, IOException {
        String path = mProject.getBuildDir().getAbsolutePath() + File.separator
                + "output" + File.separator
                + "patch" + File.separator;
        File output = new File(path);
        output.delete();
        List<CtClass> classes = new LinkedList<>();
        List<CtField> fields = new LinkedList<>();
        List<CtMethod> methods = new LinkedList<>();
        loadPatchedElements(loaded, classes, fields, methods);
        buildClasses(path, classes, fields, methods);
        return output;
    }

    private void loadPatchedElements(
            List<CtClass> loaded,
            List<CtClass> outClasses,
            List<CtField> outFields,
            List<CtMethod> outMethods)
            throws TransformException {
        for (CtClass ctClass : loaded) {
            //ClassPool使用了默认的类加载器
            boolean patched = ctClass.hasAnnotation(Patched.class);
            boolean hotfix = ctClass.hasAnnotation(Hotfix.class);
            if (patched && hotfix) {
                throw new TransformException("注解 " + Patched.class.getName()
                        + " 和注解 " + Hotfix.class.getName()
                        + " 不能同时在class上出现");
            }
            if (patched) {
                outClasses.add(ctClass);
                continue;
            }
            if (hotfix) {
                for (CtField field : ctClass.getDeclaredFields()) {
                    if (field.hasAnnotation(Patched.class)) {
                        outFields.add(field);
                    }
                }
                for (CtMethod method : ctClass.getDeclaredMethods()) {
                    if (method.hasAnnotation(Patched.class)) {
                        outMethods.add(method);
                    }
                }
            }
        }
    }

    private void buildClasses(
            String output,
            List<CtClass> classes,
            List<CtField> fields,
            List<CtMethod> methods)
            throws IOException, TransformException {
        try {
            for (CtClass clazz : classes) {
                clazz.writeFile(output);
            }
            CtClass executable = buildExecutable(fields, methods);
            executable.writeFile(output);
        } catch (CannotCompileException e) {
            throw new TransformException(e);
        }
    }

    private CtClass buildExecutable(
            List<CtField> fields,
            List<CtMethod> methods)
            throws TransformException {
        try {
            CtClass patch = mClassPool.makeClass(
                    PATCH_SUPER_CLASS_NAME + PATCH_CLASS_NAME_SUFFIX
            );
            patch.defrost();
            CtClass superClass = mClassPool.get(PATCH_SUPER_CLASS_NAME);
            StringBuilder builder
                    = new StringBuilder("protected java.lang.Object invokeDynamicMethod(" +
                    "int id, " +
                    "Object target, " +
                    "Object[] prams)" +
                    "throws Throwable {" +
                    "org.kexie.android.hotfix.internal.ExecutionEngine " +
                    "executionEngine = this.getExecutionEngine();" +
                    "switch (id) {");
            patch.subclassOf(superClass);
            Map<CtMethod, Integer> hashIds = new HashMap<>();
            for (CtMethod method : methods) {
                int id = hashMethodId(hashIds, method);
                builder.append("case ").append(id).append(": {");
                //TODO ......
                builder.append("}");
            }
            builder.append("default: {\n" +
                    "throw new java.lang.NoSuchMethodException();\n" +
                    "}\n" +
                    "}\n" +
                    "}");
            patch.addMethod(CtMethod.make(builder.toString(), patch));
            return patch;
        } catch (NotFoundException | CannotCompileException e) {
            throw new TransformException(e);
        }
    }

    /**
     * 开地址法确保散列始终不会碰撞
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
            id = id == Integer.MAX_VALUE ? 0 : id + 1;
        }
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }
}