package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformInput;
import com.android.utils.Pair;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import javassist.CtClass;

/**
 * {@link Workflow#doWorks(Context, Collection)}由模块外部调用
 */
public final class Workflow {
    private Workflow() {
        throw new AssertionError();
    }

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckResult"})
    public static void doWorks(
            Context context,
            Collection<TransformInput> inputs
    ) {
        Single<ContextWith<Pair<List<CtClass>, List<CtClass>>>>
                scanResult = Single.just(context)
                .zipWith(Single.just(inputs), Context::with)
                .observeOn(Schedulers.io())
                .map(new LoadTask())
                .observeOn(Schedulers.from(EXECUTOR))
                .map(new ScanTask());
        Single<ContextWith<List<CtClass>>> copyClasses = scanResult
                .map(it -> it.with(it.getData().getFirst()));
        Single<ContextWith<List<CtClass>>> needFixClasses = scanResult
                .map(it -> it.with(it.getData().getSecond()));
        Single<ContextWith<List<CtClass>>> fixedClasses = needFixClasses
                .map(new CloneTask())
                .map(new FixCloneTask());
        Single<ContextWith<CtClass>> codeScope = needFixClasses.map(new BuildScopeTask());
        Single<ContextWith<List<CtClass>>> buildClass = fixedClasses
                .zipWith(codeScope, (cs, c) -> {
                    List<CtClass> classes = cs.getData();
                    classes.add(c.getData());
                    return cs.with(classes);
                });
        Single<ContextWith<List<CtClass>>> allClasses = copyClasses
                .zipWith(buildClass, (copy, build) -> {
                    List<CtClass> classes = copy.getData();
                    classes.addAll(build.getData());
                    return copy.with(classes);
                });
        allClasses.observeOn(Schedulers.io())
                .map(new CopyTask())
                .map(new ZipTask())
                .observeOn(Schedulers.computation())
                .map(new Jar2DexTask())
                .blockingGet();
    }
}
