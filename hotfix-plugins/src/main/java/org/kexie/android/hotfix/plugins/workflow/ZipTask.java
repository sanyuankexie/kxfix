package org.kexie.android.hotfix.plugins.workflow;

import com.android.utils.Pair;

import java.io.File;
import java.util.List;

import javassist.CtClass;

public class ZipTask implements Workflow<List<Pair<CtClass,File>>,File> {
    @Override
    public ContextWith<File>
    apply(ContextWith<List<Pair<CtClass, File>>> contextWith) throws Exception {
        return null;
    }
}
