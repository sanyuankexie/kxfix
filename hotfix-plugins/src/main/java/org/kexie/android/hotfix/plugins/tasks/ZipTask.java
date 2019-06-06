package org.kexie.android.hotfix.plugins.tasks;

import com.android.utils.Pair;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javassist.CtClass;

public class ZipTask implements Task<List<Pair<CtClass,File>>,File> {


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public File apply(Context context, List<Pair<CtClass, File>> inputs) throws IOException {
        File output = new File("");
        if (output.exists()) {
            output.delete();
        }
        output.createNewFile();
        byte[] buffer = new byte[4096];
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
        for (Pair<CtClass, File> input : inputs) {
            ZipEntry zipEntry = new ZipEntry(input.getFirst().getName()
                    .replaceAll(Matcher.quoteReplacement("."),
                            "/"));
            zipOutputStream.putNextEntry(zipEntry);
            FileInputStream inputStream = new FileInputStream(input.getSecond());
            IOUtils.copyLarge(inputStream, zipOutputStream, buffer);
            inputStream.close();
            zipOutputStream.closeEntry();
            zipOutputStream.flush();
        }
        return output;
    }
}
