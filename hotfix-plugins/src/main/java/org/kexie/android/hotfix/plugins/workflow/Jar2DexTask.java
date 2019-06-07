package org.kexie.android.hotfix.plugins.workflow;

import com.android.dx.command.Main;

import java.io.File;
import java.io.IOException;

import io.reactivex.exceptions.Exceptions;

public class Jar2DexTask extends TempWorkflow<File,File> {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public ContextWith<File>
    apply(ContextWith<File> contextWith) {
        String out = getOutput(contextWith.getContext());
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
        String[] cmd = {"--dex", "--output=" + out, contextWith.getInput().getAbsolutePath()};
        Main.main(cmd);
        return contextWith.getContext().with(outFile);
    }

    @Override
    protected String getOutput(Context context) {
        return super.getOutput(context) + "dex.jar";
    }
}
