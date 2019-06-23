package org.kexie.gradle.hotfix.workflow;

import com.android.dx.command.Main;

import java.io.File;
import java.io.IOException;

import io.reactivex.exceptions.Exceptions;

/**
 * 将普通的jar转换成dex
 */
final class Jar2DexTask extends TempWork<File,File> {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    ContextWith<File> doWork(ContextWith<File> context) {
        String out = getOutput(context);
        File outFile = new File(out);
        File parent = outFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (outFile.exists()) {
            outFile.delete();
        }
        try {
            outFile.createNewFile();
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
        String[] cmd = {"--dex", "--output=" + out, context.getData().getAbsolutePath()};
        Main.main(cmd);
        return context.with(outFile);
    }

    @Override
    protected String getOutput(Context context) {
        return super.getOutput(context) + "jar" + File.separator + "classes-dex.jar";
    }
}
