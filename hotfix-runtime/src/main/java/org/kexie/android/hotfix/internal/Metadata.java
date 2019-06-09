package org.kexie.android.hotfix.internal;

import androidx.annotation.Keep;

@Keep
abstract class Metadata {

    static final int ID_NOT_FOUND = Integer.MIN_VALUE;

    static final Metadata EMPTY = new Metadata() {
        @Override
        int hasField(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        int hasMethod(String name, Class[] parameterTypes) {
            throw new UnsupportedOperationException();
        }

        @Override
        Object getObject(CodeContext codeContext) {
            throw new UnsupportedOperationException();
        }
    };

    abstract int hasField(String name);

    abstract int hasMethod(String name, Class[] parameterTypes);

    abstract Object getObject(CodeContext codeContext);
}
