package org.keixe.android.hotfix.plugin;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import javassist.ClassPool;
import javassist.NotFoundException;

public final class PatchTransform extends Transform {

    private final String mWorkDir;
    private final Logger mLogger;
    private final ClassPool mClassPool = new ClassPool();

    PatchTransform(Project project) {
        mLogger = project.getLogger();
        mWorkDir = project.getRootDir() + File.separator + "patched" + File.separator;
        AppExtension android = project.getExtensions().getByType(AppExtension.class);
        android.getBootClasspath()
                .forEach(classpath -> {
                    try {
                        mClassPool.appendClassPath(classpath.getAbsolutePath());
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        long startTime = System.currentTimeMillis();
        mLogger.quiet("==================patched start===================");
        transformInvocation.getOutputProvider().deleteAll();
        File output = transformInvocation.getOutputProvider()
                .getContentLocation("main", getOutputTypes(), getScopes(), Format.DIRECTORY);

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
