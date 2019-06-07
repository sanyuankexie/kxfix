package org.kexie.android.hotfix.plugins.workflow;

import com.android.SdkConstants;
import com.android.utils.Pair;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javassist.CtClass;

public class ZipTask implements Task<List<Pair<CtClass,File>>,File> {

    private static File getOutput(Context context) {
        return new File(context.mBaseWorkDir, "tmp" + File.separator + "jar" + File.separator + "classes.jar");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public File apply(Context context, List<Pair<CtClass, File>> inputs) throws IOException {
        File output = getOutput(context);
        File file = output.getParentFile();
        if (!file.exists()) {
            file.mkdirs();
        }
        if (output.exists()) {
            output.delete();
        }
        output.createNewFile();
        byte[] buffer = new byte[4096];
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
        for (Pair<CtClass, File> input : inputs) {
            String entryName = input.getFirst().getName()
                    .replace(".",
                            File.separator)
                    + SdkConstants.DOT_CLASS;
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipOutputStream.putNextEntry(zipEntry);
            FileInputStream inputStream = new FileInputStream(input.getSecond());
            System.out.println(IOUtils.copyLarge(inputStream, zipOutputStream, buffer));
            inputStream.close();
            zipOutputStream.closeEntry();
            zipOutputStream.flush();
        }
        zipOutputStream.close();
        return output;
    }
}
