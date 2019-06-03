package org.keixe.android.hotfix.internal;

final class MemberInfo {
    private final Class[] mParameterTypes;
    private final String mCachedSignature;

    MemberInfo(Class[] parameterTypes,
               String cachedSignature) {
        this.mParameterTypes = parameterTypes;
        this.mCachedSignature = cachedSignature;
    }

    Class[] getParameterTypes() {
        return mParameterTypes;
    }

    String getSignature() {
        return mCachedSignature;
    }
}
