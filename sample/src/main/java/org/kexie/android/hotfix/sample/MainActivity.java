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
    MainActivity() {

    }

    @Overload
    private static final String TAG = "MainActivity";
    @Overload
    private static String fl;

    @Overload
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: " + Environment.getDataDirectory() + savedInstanceState);
        test(0, null);
        test2();
    }

    @Overload
    public static Object test(int xx, Test activity) {
        return 0;
    }

    public void test2() {

    }
}

final class Test {

}
