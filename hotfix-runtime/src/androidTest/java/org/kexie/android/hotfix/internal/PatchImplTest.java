package org.kexie.android.hotfix.internal;

public class PatchImplTest extends OverloadObject {

    public PatchImplTest(CodeContext inner) {
        super(inner);
    }

    @Override
    void modifyWithId(int id, Object o, Object newValue) {

    }

    @Override
    Object accessWithId(int id, Object o) {
        return null;
    }

    @Override
    Object invokeWithId(int id, Object o, Object[] args) {
        return null;
    }

}
