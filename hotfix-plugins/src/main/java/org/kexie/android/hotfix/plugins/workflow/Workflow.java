package org.kexie.android.hotfix.plugins.workflow;

import com.android.build.api.transform.TransformException;

import java.io.IOException;

import io.reactivex.functions.Function;

abstract class Workflow<I,O> implements Function<ContextWith<I>,ContextWith<O>> {
    @Override
    public final ContextWith<O> apply(ContextWith<I> in) throws Exception {
        //
        ContextWith<O> out = doWork(in);
        //
        return out;
    }

    abstract ContextWith<O> doWork(ContextWith<I> data) throws TransformException, IOException;
}