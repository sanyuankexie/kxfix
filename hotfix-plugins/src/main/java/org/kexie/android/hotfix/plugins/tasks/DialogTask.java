package org.kexie.android.hotfix.plugins.tasks;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class DialogTask implements Task<File,Void> {
    @Override
    public Void apply(Context context, File input) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader()
                .getResource("./icon.png");
        Image image = Toolkit.getDefaultToolkit().createImage(url);
        image = image.getScaledInstance(40, 40, Image.SCALE_DEFAULT);
        ImageIcon imageIcon = new ImageIcon(image);
        JOptionPane.showConfirmDialog(null,
                "补丁生成完成", "提示",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                imageIcon);
        input = input.getParentFile();
        if (Desktop.isDesktopSupported() && input.isDirectory()) {
            Desktop.getDesktop().open(input);
        }
        System.exit(0);
        return null;
    }
}
