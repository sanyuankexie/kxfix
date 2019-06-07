package org.kexie.android.hotfix.plugins.workflow;

import io.reactivex.functions.Function;

interface Workflow<I,O> extends Function<ContextWith<I>,ContextWith<O>> { }
