package org.kexie.android.hotfix.plugins.workflow;

import com.android.dx.command.Main;

import java.io.File;
import java.io.IOException;

public class DxTask implements Task<File,File> {

    private static File getOutput(Context context) {
        return new File(context.mBaseWorkDir + "dex" + File.separator + "dex.jar");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public File apply(Context context, File in) throws IOException {
        File out = getOutput(context);
        File file = out.getParentFile();
        if (!file.exists()) {
            file.mkdirs();
        }
        if (out.exists()) {
            out.delete();
        }
        out.createNewFile();
        String[] cmd = {"--dex", "--output=" + out.getAbsolutePath(), in.getAbsolutePath()};
        Main.main(cmd);
        return out;
    }
}
