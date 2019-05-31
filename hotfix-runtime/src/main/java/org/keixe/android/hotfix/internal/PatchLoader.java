package org.keixe.android.hotfix.internal;

import org.hashids.Hashids;

import dalvik.system.DexClassLoader;

final class PatchLoader {

    public Class<? extends Patch> load() {
        DexClassLoader classLoader = new DexClassLoader(
                "", "", "",
                ClassLoader.getSystemClassLoader());
        //dalvik.system.DelegateLastClassLoader
        Hashids hashids = new Hashids();
        return null;
    }

}
