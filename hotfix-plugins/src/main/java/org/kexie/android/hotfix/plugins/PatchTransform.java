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

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * Java特别适合用来构建复杂臃肿但又可靠的的系统
 */
public class PatchTransform extends Transform {

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
    public void transform(TransformInvocation transformInvocation) throws IOException, TransformException {
        long startTime = System.currentTimeMillis();
        mLogger.quiet("==================patched start===================");
        transformInvocation.getOutputProvider().deleteAll();
        File outDir = transformInvocation.getOutputProvider()
                .getContentLocation("main", getOutputTypes(), getScopes(), Format.DIRECTORY);
        List<CtClass> boxClass = loadCtClasses(transformInvocation.getInputs());
        long cost = (System.currentTimeMillis() - startTime) / 1000;
        mLogger.quiet("==================patched finish==================");
        finishWithDirectory(new File(""));
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

    private List<CtClass> loadCtClasses(Collection<TransformInput> inputs) throws IOException, TransformException {
        AppExtension android = mProject.getExtensions().getByType(AppExtension.class);
        List<String> classNames = new LinkedList<>();
        try {
            for (File classpath : android.getBootClasspath()) {
                mClassPool.appendClassPath(classpath.getAbsolutePath());
            }
            String[] extension = {SdkConstants.EXT_CLASS};
            for (TransformInput input : inputs) {
                for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                    String directory = directoryInput.getFile().getAbsolutePath();
                    mClassPool.insertClassPath(directory);
                    for (File file : FileUtils.listFiles(directoryInput.getFile(),
                            extension, true)) {
                        String className = file.getAbsolutePath().substring(directory.length() + 1,
                                file.getAbsolutePath().length() - SdkConstants.DOT_CLASS.length())
                                .replaceAll(Matcher.quoteReplacement(File.separator), ".");
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

    private void readAnnotation(List<CtClass> loaded) {

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