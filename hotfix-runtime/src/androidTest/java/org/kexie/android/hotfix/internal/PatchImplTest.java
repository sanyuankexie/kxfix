package org.kexie.android.hotfix.internal;

public class PatchImplTest extends ExtensionExecutor {



    @Override
    void receiveModifyById(int id, Object o, Object newValue) {

    }

    @Override
    Object receiveAccessById(int id, Object o) {
        return null;
    }

    @Override
    Object receiveInvokeById(int id, Object o, Object[] args) {
        return null;
    }

}
