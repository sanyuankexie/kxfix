package org.kexie.android.hotfix.plugins.tasks;

import com.android.SdkConstants;
import com.android.build.api.transform.TransformException;
import com.android.utils.Pair;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;

public class CopingTask implements Task<List<CtClass>, List<Pair<CtClass,File>>> {

    private static String getOutput(Context context) {
        return context.mBaseWorkDir + "classes" + File.separator;
    }

    @Override
    public List<Pair<CtClass, File>> apply(Context context, List<CtClass> inputs)
            throws IOException, TransformException {
        String classOutputDir = getOutput(context);
        List<Pair<CtClass, File>> files = new LinkedList<>();
        try {
            for (CtClass clazz : inputs) {
                clazz.writeFile(classOutputDir);
                String entryName = clazz.getName()
                        .replace('.', File.separatorChar);
                File file = new File(context.mClassPool
                        + entryName
                        + SdkConstants.DOT_CLASS);
                if (file.exists()) {
                    files.add(Pair.of(clazz, file));
                }
            }
        } catch (CannotCompileException e) {
            throw new TransformException(e);
        }
        return files;
    }
}
