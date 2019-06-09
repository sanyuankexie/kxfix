package org.kexie.android.hotfix.sample;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import org.kexie.android.hotfix.Hotfix;
import org.kexie.android.hotfix.Overload;

import androidx.appcompat.app.AppCompatActivity;

@Hotfix
public class MainActivity extends AppCompatActivity {

    @Overload
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: " + Environment.getDataDirectory() + savedInstanceState);
        test(0, null);
    }

    @Overload
    public static void test(int xx, Test activity) {

    }
}

final class Test {

}
