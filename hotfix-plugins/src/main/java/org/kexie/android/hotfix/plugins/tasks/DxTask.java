package org.kexie.android.hotfix.plugins.tasks;

import com.android.dx.command.Main;

import java.io.File;

public class DxTask implements Task<File,File> {

    private static File getOutput(Context context) {
        return new File(context.mBaseWorkDir + "dex" + File.separator);
    }

    @Override
    public File apply(Context context, File in) {
        File out = getOutput(context);
        String[] cmd = {"--dex", "--output=" + in.getAbsolutePath(), out.getAbsolutePath()};
        Main.main(cmd);
        return out;
    }
}
