package org.keixe.android.hotfix;

import java.io.File;
import java.util.UUID;

import androidx.annotation.Keep;

@Keep
@SuppressWarnings("WeakerAccess")
public final class Patch {
    private final File mDexPath;
    private final UUID mUUID;

    public Patch(File dexPath, UUID uuid) {
        this.mDexPath = dexPath;
        this.mUUID = uuid;
    }

    public UUID getUUID() {
        return mUUID;
    }

    public File getDexPath() {
        return mDexPath;
    }
}
