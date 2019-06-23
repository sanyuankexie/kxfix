package org.kexie.android.hotfix.internal;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import androidx.annotation.Keep;

@Keep
class DomainManageEngine extends DegradableExecutionEngine {

    private static final AtomicReferenceFieldUpdater<DomainManageEngine, Domain>
            sDomainUpdater = AtomicReferenceFieldUpdater
            .newUpdater(DomainManageEngine.class, Domain.class, "domain");

    volatile Domain domain;

    DomainManageEngine(ExecutionEngine base) {
        super(base);
    }

    void apply(Domain domain) {
        sDomainUpdater.set(this, domain);
    }

    final boolean isThat(Domain domain) {
        return domain == this.domain;
    }
}
