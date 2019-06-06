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
import com.android.dx.command.Main;
import com.android.utils.Pair;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.kexie.android.hotfix.Hotfix;
import org.kexie.android.hotfix.Patched;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private final String mWorkDir;
    private final String mClassOutput;
    private final String mZipOutput;

    PatchTransform(Project project) {
        mProject = project;
        mLogger = project.getLogger();
        mWorkDir = mProject.getBuildDir()
                .getAbsolutePath() + File.separator +
                "output" + File.separator +
                "hotfix" + File.separator;
        mClassOutput = mWorkDir + "classes" + File.separator;
        mZipOutput = mWorkDir + "patch" + File.separator + "classes.jar";
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
        List<CtClass> loadedClass = loadInput(transformInvocation.getInputs());
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

    private List<CtClass> loadInput(Collection<TransformInput> inputs)
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
        List<CtClass> classes = new LinkedList<>();
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
        File output = new File(mClassOutput);
        if (output.exists()) {
            output.delete();
        }
        output.mkdirs();
        List<CtClass> classes = new LinkedList<>();
        List<CtField> fields = new LinkedList<>();
        List<CtMethod> methods = new LinkedList<>();
        findPatchedElements(loaded, classes, fields, methods);
        List<Pair<String, File>> outFiles = buildClasses(classes, fields, methods);
        zipToFile(outFiles);
        return output;
    }

    private void findPatchedElements(
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

    private List<Pair<String, File>> buildClasses(
            List<CtClass> classes,
            List<CtField> fields,
            List<CtMethod> methods)
            throws IOException, TransformException {
        List<Pair<String, File>> files = new LinkedList<Pair<String, File>>();
        try {
            for (CtClass clazz : classes) {

                clazz.writeFile(mClassOutput);
                String entryName = clazz.getName()
                        .replace('.', File.separatorChar);
                File file = new File(mClassOutput
                        + entryName
                        + SdkConstants.DOT_CLASS);
                if (file.exists()) {
                    files.add(Pair.of(entryName, file));
                }
            }
            CtClass executable = buildExecutable(fields, methods);
            executable.writeFile(mClassOutput);
            String entryName = executable.getName()
                    .replace('.', File.separatorChar);
            File file = new File(mClassOutput
                    + entryName
                    + SdkConstants.DOT_CLASS);
            if (file.exists()) {
                files.add(Pair.of(entryName, file));
            }
            return files;
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
            patch.subclassOf(superClass);
            Map<CtMethod, Integer> hashIds = new HashMap<>();
            for (CtMethod method : methods) {
                int id = hashMethodId(hashIds, method);
                methodsBuilder.append("case ").append(id).append(": {");
                //TODO ......
                methodsBuilder.append("}");
            }
            methodsBuilder.append("default: {throw new java.lang.NoSuchMethodException();}}}");
            patch.addMethod(CtMethod.make(methodsBuilder.toString(), patch));
            methodsBuilder = new StringBuilder("protected void " +
                    "onLoad(org.kexie.android.hotfix.internal.Metadata metadata){");
            for (CtField field : fields) {
                methodsBuilder.append("metadata.addFiled(\"")
                        .append(field.getDeclaringClass().getName())
                        .append("\",\"")
                        .append(field.getName())
                        .append("\");");
            }
            for (CtMethod method : methods) {
                methodsBuilder.append("metadata.addMethod(")
                        .append(hashIds.get(method))
                        .append(",\"")
                        .append(method.getDeclaringClass().getName())
                        .append("\",\"")
                        .append(method.getName())
                        .append("\",");
                CtClass[] pramTypes = method.getParameterTypes();
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
            patch.addMethod(CtMethod.make(methodsBuilder.toString(), patch));
            patch.freeze();
            return patch;
        } catch (NotFoundException | CannotCompileException e) {
            throw new TransformException(e);
        }
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void zipToFile(List<Pair<String, File>> inputs) throws IOException {
        File output = new File(mZipOutput);
        if (output.exists()) {
            output.delete();
        }
        output.createNewFile();
        byte[] buffer = new byte[4096];
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
        for (Pair<String, File> input : inputs) {
            ZipEntry zipEntry = new ZipEntry(input.getFirst());
            zipOutputStream.putNextEntry(zipEntry);
            FileInputStream inputStream = new FileInputStream(input.getSecond());
            IOUtils.copyLarge(inputStream, zipOutputStream, buffer);
            inputStream.close();
            zipOutputStream.closeEntry();
            zipOutputStream.flush();
        }
    }

    private void zipToDex() {
        String[] command = {"--dex", "--output=" + mZipOutput, "classes.dex"};
        Main.main(command);

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