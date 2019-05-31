package org.keixe.android.hotfix.internal;

interface ExecutableHolder {

    void apply(Executable executable);

    boolean isExecuteThat(Executable executable);

    ExecutableHolder INSTANCE = HotfixExecutionEngine.INSTANCE;
}
