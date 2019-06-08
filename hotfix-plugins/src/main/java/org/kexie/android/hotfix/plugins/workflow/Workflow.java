package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformInput;
import com.android.utils.Pair;

import java.util.Collection;
import java.util.List;

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
                .observeOn(Schedulers.computation())
                .map(new Jar2DexTask())
                .subscribe(contextWith -> {
                }, it -> {
                    throw new RuntimeException(it);
                });
    }
}
