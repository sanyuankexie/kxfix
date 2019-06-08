package org.kexie.android.hotfix.plugins;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.Pair;

import org.gradle.api.Project;
import org.kexie.android.hotfix.plugins.imgui.Looper;
import org.kexie.android.hotfix.plugins.workflow.BuildTask;
import org.kexie.android.hotfix.plugins.workflow.Context;
import org.kexie.android.hotfix.plugins.workflow.ContextWith;
import org.kexie.android.hotfix.plugins.workflow.CopyTask;
import org.kexie.android.hotfix.plugins.workflow.Jar2DexTask;
import org.kexie.android.hotfix.plugins.workflow.LoadTask;
import org.kexie.android.hotfix.plugins.workflow.ScanTask;
import org.kexie.android.hotfix.plugins.workflow.ZipTask;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import javassist.CtClass;

/**
 * Android Studio Plugin 可以完全使用Java来进行开发
 * 而Java是强类型非动态的
 * 特别适合用来构建复杂臃肿但又可靠的的系统
 * 所以这里没有使用groovy
 */
public class PatchTransform extends Transform {

    private final Context context;

    PatchTransform(Project project) {
        context = Context.make(project);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }


    @Override
    public void transform(TransformInvocation transformInvocation)
            throws IOException {
        transformInvocation.getOutputProvider().deleteAll();
        doWorks(context, transformInvocation.getInputs());
        Looper looper = Looper.make(context);
        looper.loop();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckResult"})
    private static void doWorks(
            Context context,
            Collection<TransformInput> inputs) {
        Single<ContextWith<Pair<List<CtClass>, List<CtClass>>>>
                scanResult = Single.just(context)
                .zipWith(Single.just(inputs), Context::with)
                .observeOn(Schedulers.io())
                .map(new LoadTask())
                .observeOn(Schedulers.computation())
                .map(new ScanTask());
        Single<List<CtClass>> copyClasses = scanResult
                .map(it -> it.getData().getFirst());
        Single<ContextWith<CtClass>> buildClass = scanResult
                .map(it -> it.with(it.getData().getSecond()))
                .map(new BuildTask());
        copyClasses.zipWith(buildClass, (classes, contextWith) -> {
            classes.add(contextWith.getData());
            return contextWith.with(classes);
        }).observeOn(Schedulers.io())
                .map(new CopyTask())
                .map(new ZipTask())
                .map(new Jar2DexTask())
                .observeOn(Schedulers.computation())
                .subscribe(contextWith -> {
                }, it -> {
                    throw new RuntimeException(it);
                });
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
    public boolean isCacheable() {
        return true;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

}