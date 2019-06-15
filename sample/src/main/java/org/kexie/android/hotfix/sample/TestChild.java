package org.kexie.android.hotfix.sample;

import android.util.Log;

import org.kexie.android.hotfix.Hotfix;
import org.kexie.android.hotfix.Overload;

@Hotfix
public class TestChild extends TestSuper {
    private static final String TAG = "TestChild";

    @Overload
    @Override
    void test() {
        super.test();
        Log.d(TAG, "test: sadasdasdasdasdas");
    }
}
