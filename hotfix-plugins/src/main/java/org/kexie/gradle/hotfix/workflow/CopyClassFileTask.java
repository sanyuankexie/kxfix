package org.kexie.gradle.hotfix.workflow;

import com.android.SdkConstants;

import org.kexie.gradle.hotfix.workflow.beans.CopyMapping;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.exceptions.Exceptions;
import javassist.CannotCompileException;
import javassist.CtClass;


/**
 * 拷贝所有类型到指定目录
 */
final class CopyClassFileTask extends TempWork<List<CtClass>,  List<CopyMapping>> {
    @Override
    ContextWith<List<CopyMapping>> doWork(ContextWith<List<CtClass>> context) {
        String classOutputDir = getOutput(context);
        List<CopyMapping> files = new LinkedList<>();
        try {
            for (CtClass clazz : context.getData()) {
                clazz.writeFile(classOutputDir);
                String entryName = clazz.getName()
                        .replace(".",
                                File.separator);
                File file = new File(classOutputDir, entryName
                        + SdkConstants.DOT_CLASS);
                if (file.exists()) {
                    files.add(new CopyMapping(clazz, file));
                }
            }
        } catch (CannotCompileException | IOException e) {
            throw Exceptions.propagate(e);
        }
        return context.with(files);
    }

    @Override
    protected String getOutput(Context context) {
        return super.getOutput(context) + "classes";
    }
}
