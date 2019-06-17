package org.kexie.android.hotfix.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ResourceUtils;

import org.kexie.android.hotfix.HotfixManager;
import org.kexie.android.hotfix.Overload;
import org.kexie.android.hotfix.Patch;

import java.io.File;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity
        extends AppCompatActivity
        implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.text);
        findViewById(R.id.testButton).setOnClickListener(this);
        findViewById(R.id.load).setOnClickListener(this);
    }

    private int test(int s) {
        return s;
    }

    @Overload
    @Override
    public void onClick(View v) {
        if (R.id.testButton == v.getId()) {
            Log.d(TAG, "onClick: aadsas");
        } else if (R.id.load == v.getId()) {
            File file = getDir("cache", MODE_PRIVATE);
            file = new File(file, "classes-dex.jar");
            if (ResourceUtils.copyFileFromAssets("classes-dex.jar",
                    file.getAbsolutePath())) {
                HotfixManager manager = new HotfixManager(this.getApplicationContext());
                manager.apply(new Patch(file.getAbsolutePath(), UUID.randomUUID().toString()));
            } else {
                Toast.makeText(this, "拷贝失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class InnerTest {
        void text() {

        }
    }
}
