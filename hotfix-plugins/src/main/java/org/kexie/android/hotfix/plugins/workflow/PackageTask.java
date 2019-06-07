package org.kexie.android.hotfix.plugins.workflow;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackageTask implements Task<File,File> {

    private static File getOutput(Context context) {
        return new File(context.mBaseWorkDir + "patch" + File.separator + "patch.jar");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public File apply(Context context, File input) throws IOException {
        File file = getOutput(context);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileInputStream inputStream = new FileInputStream(input);
        FileOutputStream outputStream = new FileOutputStream(file);
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        zipOutputStream.setLevel(Deflater.NO_COMPRESSION);
        byte[] bytes = new byte[4096];
        ZipEntry zipEntry = new ZipEntry("classes.dex");
        zipOutputStream.putNextEntry(zipEntry);
        IOUtils.copyLarge(inputStream, zipOutputStream, bytes);
        inputStream.close();
        zipOutputStream.closeEntry();
        zipOutputStream.finish();
        zipOutputStream.close();
        return file;
    }
}
