package org.kexie.android.hotfix.sample;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import org.kexie.android.hotfix.Hotfix;
import org.kexie.android.hotfix.Patched;

import androidx.appcompat.app.AppCompatActivity;

@Hotfix
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Patched
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: " + Environment.getDataDirectory());
    }

    @Patched
    public static void text(int xx, Test activity) {

    }
}

final class Test {

}
