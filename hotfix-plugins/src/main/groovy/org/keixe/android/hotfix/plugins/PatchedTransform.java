package org.keixe.android.hotfix.plugins;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.gradle.api.logging.Logger;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public final class PatchedTransform extends Transform {

    private final Logger mLogger;

    public PatchedTransform(Logger logger) {
        this.mLogger = logger;
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        long startTime = System.currentTimeMillis();
        mLogger.quiet("==================patched start===================");
        transformInvocation.getOutputProvider().deleteAll();
        long cost = (System.currentTimeMillis() - startTime) / 1000;
        mLogger.quiet("==================patched finish==================");
        URL url = ClassLoader.getSystemClassLoader().getResource("./icon.png");
        Image image = Toolkit.getDefaultToolkit().createImage(url);
        image = image.getScaledInstance(40, 40, Image.SCALE_DEFAULT);
        ImageIcon imageIcon = new ImageIcon(image);
        JOptionPane.showConfirmDialog(null,
                "补丁生成完成", "提示",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                imageIcon);
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
