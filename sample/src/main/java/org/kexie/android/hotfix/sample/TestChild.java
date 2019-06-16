package org.kexie.android.hotfix.sample;

import android.content.Context;
import android.widget.Toast;

import org.kexie.android.hotfix.Hotfix;
import org.kexie.android.hotfix.Overload;

@Hotfix
public class TestChild extends TestSuper {
    private static final String TAG = "TestChild";

    @Overload
    @Override
    void test(Context context) {
        super.test(context);
        Toast.makeText(context,"has error",Toast.LENGTH_SHORT).show();
    }
}
