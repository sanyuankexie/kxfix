package org.keixe.android.hotfix.plugins


import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import org.gradle.api.Project
import org.gradle.api.logging.Logger

import javax.swing.*
import java.awt.*

final class PatchTransform extends Transform {

    private final String mWorkDir
    private final Logger mLogger
    private final ClassPool mClassPool = new ClassPool()

    PatchTransform(Project project) {
        mLogger = project.logger
        mWorkDir = project.rootProject + File.separator + "patched" + File.separator
    }

    @Override
    String getName() {
        return getClass().getName()
    }

    @Override
    String toString() {
        return super.toString()
    }

    @Override
    void transform(TransformInvocation transformInvocation) {
        AppExtension android = project.extensions.getByType(AppExtension)
        android.getBootClasspath().each { classpath ->
            mClassPool.appendClassPath(classpath.absolutePath)
        }
        long startTime = System.currentTimeMillis()
        mLogger.quiet "==================patched start==================="
        transformInvocation.outputProvider.deleteAll()
        File output = transformInvocation.outputProvider
                .getContentLocation("main", getOutputTypes(), getScopes(), Format.DIRECTORY)

        long cost = (System.currentTimeMillis() - startTime) / 1000
        mLogger.quiet "==================patched finish=================="
        finishWithDirectory(new File(""))
    }

    private static void finishWithDirectory(File directory) {
        URL url = ClassLoader.systemClassLoader.getResource("./icon.png")
        Image image = Toolkit.defaultToolkit.createImage(url)
        image = image.getScaledInstance(40, 40, Image.SCALE_DEFAULT)
        ImageIcon imageIcon = new ImageIcon(image)
        JOptionPane.showConfirmDialog(null,
                "补丁生成完成", "提示",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                imageIcon)
        if (Desktop.isDesktopSupported()) {
            Desktop.desktop.open(directory)
        }
        System.exit(0)
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }
}
