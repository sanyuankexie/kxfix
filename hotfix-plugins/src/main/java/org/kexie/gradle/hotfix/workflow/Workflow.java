package org.kexie.gradle.hotfix.workflow;

import com.android.build.api.transform.TransformInput;

import org.kexie.gradle.hotfix.workflow.beans.AddedOutput;
import org.kexie.gradle.hotfix.workflow.beans.AnnotatedOutput;

import java.io.File;
import java.util.Collection;
import java.util.List;

import io.reactivex.Single;
import javassist.CtClass;

/**
 * {@link Workflow#doWorks(Context, Collection)}由模块外部调用
 */
public final class Workflow {
    private Workflow() {
        throw new AssertionError();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckResult"})
    public static void doWorks(
            Context context,
            Collection<TransformInput> inputs
    ) {
        Single<ContextWith<AnnotatedOutput>> filterPresentResult = Single.just(context)
                .zipWith(Single.just(inputs), Context::with)
                .map(new LoadClassTask())
                .map(new FilterAnnotatedTask());
        Single<ContextWith<List<CtClass>>> addedClasses = filterPresentResult
                .map(it -> it.with(it.getData().added));
        Single<ContextWith<List<CtClass>>> needFixClasses = filterPresentResult
                .map(it -> it.with(it.getData().fixed));
        Single<ContextWith<List<CtClass>>> fixedClasses = needFixClasses
                .map(new CloneHotfixClassTask())
                .map(new AdjustCloneClassTask());
        Single<ContextWith<CtClass>> codeScope = needFixClasses
                .map(new BuildDomainTask());
        Single<ContextWith<AddedOutput>> filterAddedResult
                = addedClasses.map(new FilterAddedTask());
        Single<ContextWith<List<CtClass>>> directCopyClasses = filterAddedResult
                .map(it -> it.with(it.getData().topper));
        Single<ContextWith<List<CtClass>>> addedInnerClasses = filterAddedResult
                .map(it -> it.with(it.getData().inner));
        Single<ContextWith<List<CtClass>>> fixedAddedInnerClasses = addedInnerClasses
                .map(new AdjustInnerClassTask());
        Single<ContextWith<List<CtClass>>> allClasses = Single
                .zip(fixedClasses, codeScope, fixedAddedInnerClasses, directCopyClasses,
                        (context1, contextWith, context2, context3) -> {
                            List<CtClass> result = context1.getData();
                            result.addAll(context2.getData());
                            result.addAll(context3.getData());
                            result.add(contextWith.getData());
                            return context1.with(result);
                        });
        allClasses.map(new CopyClassFileTask())
                .map(new PreJarForDxTask())
                .map(new Jar2DexTask())
                .map(ContextWith::getData)
                .map(File::getParentFile)
                .subscribe(file -> {
                    String os = System.getProperty("os.name");
                    Process process;
                    if (os.toLowerCase().contains("win")) {
                        process = Runtime.getRuntime().exec("explorer " + file.getAbsolutePath());
                    } else if (os.toLowerCase().contains("mac")) {
                        process = Runtime.getRuntime().exec("open " + file.getAbsolutePath());
                    } else {
                        process = Runtime.getRuntime().exec("nautilus " + file.getAbsolutePath());
                    }
                    process.waitFor();
                    System.exit(0);
                }, throwable -> {
                    throw new RuntimeException("Plugin internal error", throwable);
                });
    }
}
