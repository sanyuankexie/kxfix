package org.kexie.android.hotfix.plugins;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.Pair;

import org.gradle.api.Project;
import org.kexie.android.hotfix.plugins.tasks.BuildExTask;
import org.kexie.android.hotfix.plugins.tasks.Context;
import org.kexie.android.hotfix.plugins.tasks.CopingTask;
import org.kexie.android.hotfix.plugins.tasks.DxTask;
import org.kexie.android.hotfix.plugins.tasks.DialogTask;
import org.kexie.android.hotfix.plugins.tasks.LoadingTask;
import org.kexie.android.hotfix.plugins.tasks.PackageTask;
import org.kexie.android.hotfix.plugins.tasks.ScanningTask;
import org.kexie.android.hotfix.plugins.tasks.ZipTask;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javassist.CtClass;

/**
 * Android Studio Plugin 可以完全使用Java来进行开发
 * 而Java是强类型非动态的
 * 特别适合用来构建复杂臃肿但又可靠的的系统
 * 所以这里没有使用groovy
 */
public class PatchTransform extends Transform {

    private final Context mContext;

    PatchTransform(Project project) {
        mContext = new Context(project);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void transform(TransformInvocation transformInvocation)
            throws IOException, TransformException {
        long startTime = System.currentTimeMillis();
        transformInvocation.getOutputProvider().deleteAll();
        LoadingTask loadingTask = new LoadingTask();
        List<CtClass> loaded = loadingTask.apply(mContext, transformInvocation.getInputs());
        ScanningTask scanningTask = new ScanningTask();
        ScanningTask.Result scanning = scanningTask.apply(mContext, loaded);
        List<CtClass> copySrc = new LinkedList<>(scanning.mClasses);
        BuildExTask buildExTask = new BuildExTask();
        CtClass ctClass = buildExTask.apply(mContext, Pair.of(scanning.mFields, scanning.mMethods));
        copySrc.add(ctClass);
        CopingTask copingTask = new CopingTask();
        List<Pair<CtClass, File>> copyResult = copingTask.apply(mContext, copySrc);
        ZipTask zipTask = new ZipTask();
        File zipFile = zipTask.apply(mContext, copyResult);
        DxTask dxTask = new DxTask();
        File dexFile = dxTask.apply(mContext, zipFile);
        PackageTask packageTask = new PackageTask();
        File packageFile = packageTask.apply(mContext, dexFile);
        long cost = (System.currentTimeMillis() - startTime) / 1000;
        DialogTask dialogTask = new DialogTask();
        dialogTask.apply(mContext, packageFile);
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }
}