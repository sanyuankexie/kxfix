package org.kexie.android.hotfix.plugins.tasks;

import com.android.build.api.transform.TransformException;
import com.android.dx.command.Main;

import java.io.File;
import java.io.IOException;

public class DxTask implements Task<File,File> {
    @Override
    public File apply(Context context, File input) throws IOException, TransformException {
        String[] cmd = {"--dex"};
        Main.main(cmd);
        return null;
    }
}
