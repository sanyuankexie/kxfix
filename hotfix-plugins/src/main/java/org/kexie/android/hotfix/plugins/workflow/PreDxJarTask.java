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


/**
 * 将classes 用zip打包到指定文件夹
 */
final class PreDxJarTask extends TempWork<List<Pair<CtClass,File>>,File> {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    ContextWith<File> doWork(ContextWith<List<Pair<CtClass, File>>> context) throws IOException {
        String out = getOutput(context);
        File output = new File(out);
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
        for (Pair<CtClass, File> input : context.getData()) {
            String entryName = input.getFirst().getName()
                    .replace(".",
                            File.separator)
                    + SdkConstants.DOT_CLASS;
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipOutputStream.putNextEntry(zipEntry);
            FileInputStream inputStream = new FileInputStream(input.getSecond());
            IOUtils.copyLarge(inputStream, zipOutputStream, buffer);
            inputStream.close();
            zipOutputStream.closeEntry();
            zipOutputStream.flush();
        }
        zipOutputStream.close();
        return context.with(output);
    }

    @Override
    protected String getOutput(Context context) {
        return super.getOutput(context) + "jar" + File.separator + "classes.jar";
    }
}
