package org.kexie.android.hotfix.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ResourceUtils;

import org.kexie.android.hotfix.Hotfix;
import org.kexie.android.hotfix.HotfixManager;
import org.kexie.android.hotfix.Overload;
import org.kexie.android.hotfix.sample.ui.login.LoginActivity;

import java.io.File;


@Hotfix
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

    private int[][][] test(int[][][][] arr, int sds, Object c) {
        return null;
    }

    public void start() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    @Overload
    @Override
    public void onClick(View v) {
        if (R.id.testButton == v.getId()) {
            //start();
            Toast.makeText(this, "打开界面出错", Toast.LENGTH_SHORT).show();
        } else if (R.id.load == v.getId()) {
            File file = getDir("cache", MODE_PRIVATE);
            file = new File(file, "classes-dex.jar");
            if (ResourceUtils.copyFileFromAssets("classes-dex.jar",
                    file.getAbsolutePath())) {
                new HotfixManager().load(getApplicationContext(), file.getAbsolutePath());
            } else {
                Toast.makeText(this, "拷贝失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
