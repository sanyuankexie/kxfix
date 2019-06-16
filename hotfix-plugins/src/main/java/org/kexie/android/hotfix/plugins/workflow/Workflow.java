package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformInput;
import com.android.utils.Pair;

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
        Single<ContextWith<Pair<List<CtClass>, List<CtClass>>>>
                filterPresentResult = Single.just(context)
                .zipWith(Single.just(inputs), Context::with)
                .map(new LoadClassesTask())
                .map(new FilterPresentClassTask());
        Single<ContextWith<List<CtClass>>> addedClasses = filterPresentResult
                .map(it -> it.with(it.getData().getFirst()));
        Single<ContextWith<List<CtClass>>> needFixClasses = filterPresentResult
                .map(it -> it.with(it.getData().getSecond()));
        Single<ContextWith<List<CtClass>>> fixedClasses = needFixClasses
                .map(new CloneHotfixClassTask())
                .map(new FixCloneClassTask());
        Single<ContextWith<CtClass>> codeScope = needFixClasses
                .map(new BuildCodeScopeTask());
        Single<ContextWith<Pair<List<CtClass>, List<CtClass>>>> filterAddedResult
                = addedClasses.map(new FilterAddedClassTask());
        Single<ContextWith<List<CtClass>>> directCopyClasses = filterAddedResult
                .map(it -> it.with(it.getData().getFirst()));
        Single<ContextWith<List<CtClass>>> addedInnerClasses = filterAddedResult
                .map(it -> it.with(it.getData().getSecond()));
        Single<ContextWith<List<CtClass>>> fixedAddedInnerClasses = addedInnerClasses
                .map(new FixAddedInnerClassTask());
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
                .map(new PreDxJarTask())
                .map(new Jar2DexTask())
                .map(ContextWith::getData)
                .map(File::getParentFile)
                .subscribe(file -> {
                    String os = System.getProperty("os.name");
                    Process process;
                    if (os.toLowerCase().startsWith("win")) {
                        process = Runtime.getRuntime().exec("explorer " + file.getAbsolutePath());
                    } else {
                        process = Runtime.getRuntime().exec("nautilus " + file.getAbsolutePath());
                    }
                    process.waitFor();
                    System.exit(0);
                });
    }
}
