package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;

@Keep
interface CodeScopeManager {
    boolean isThatScope(CodeScope codeScope);
}
