package org.kexie.android.hotfix.sample;

import android.content.Context;
import android.widget.Toast;


public class TestSuper {
    private static final String TAG = "TestSuper";
    void test(Context context) {
        Toast.makeText(context,"in super",Toast.LENGTH_LONG).show();
    }
}
