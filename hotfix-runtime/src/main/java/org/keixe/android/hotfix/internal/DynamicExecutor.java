package org.keixe.android.hotfix.internal;

import androidx.annotation.Keep;

@Keep
interface DynamicExecutor {

    void apply(Executable executable);

    boolean isExecuteThat(Executable executable);

    DynamicExecutor INSTANCE = HotfixExecutionEngine.INSTANCE;
}
