package org.kexie.android.hotfix;

import androidx.annotation.Keep;

@Keep
@SuppressWarnings("WeakerAccess")
public final class Patch {
    private final String dexPath;
    private final String uuid;

    public Patch(String dexPath, String uuid) {
        this.dexPath = dexPath;
        this.uuid = uuid;
    }

    public String getUUID() {
        return uuid;
    }

    public String getDexPath() {
        return dexPath;
    }
}
