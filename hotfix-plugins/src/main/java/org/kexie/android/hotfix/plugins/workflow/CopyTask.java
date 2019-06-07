package org.kexie.android.hotfix.plugins.workflow;

import com.android.SdkConstants;
import com.android.utils.Pair;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.exceptions.Exceptions;
import javassist.CannotCompileException;
import javassist.CtClass;

public class CopyTask extends TempWorkflow<List<CtClass>,  List<Pair<CtClass, File>>> {
    @Override
    public ContextWith<List<Pair<CtClass, File>>>
    apply(ContextWith<List<CtClass>> contextWith) {
        String classOutputDir = getOutput(contextWith.getContext());
        List<Pair<CtClass, File>> files = new LinkedList<>();
        try {
            for (CtClass clazz : contextWith.getInput()) {
                clazz.writeFile(classOutputDir);
                String entryName = clazz.getName()
                        .replace(".",
                                File.separator);
                File file = new File(classOutputDir, entryName
                        + SdkConstants.DOT_CLASS);
                if (file.exists()) {
                    files.add(Pair.of(clazz, file));
                }
            }
        } catch (CannotCompileException | IOException e) {
            throw Exceptions.propagate(e);
        }
        return contextWith.getContext().with(files);
    }

    @Override
    protected String getOutput(Context context) {
        return super.getOutput(context) + "classes";
    }
}
