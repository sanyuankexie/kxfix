package org.keixe.android.hotfix.internal;

import androidx.annotation.Keep;

@Keep
interface ExecutableHolder {

    void apply(Executable executable);

    boolean isExecuteThat(Executable executable);

    ExecutableHolder INSTANCE = HotfixExecutionEngine.INSTANCE;
}
