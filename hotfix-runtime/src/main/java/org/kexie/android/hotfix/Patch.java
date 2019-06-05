package org.kexie.android.hotfix;

import androidx.annotation.Keep;

@Keep
@SuppressWarnings("WeakerAccess")
public final class Patch {
    private final String mDexPath;
    private final String mUUID;

    public Patch(String dexPath, String uuid) {
        this.mDexPath = dexPath;
        this.mUUID = uuid;
    }

    public String getUUID() {
        return mUUID;
    }

    public String getDexPath() {
        return mDexPath;
    }
}
