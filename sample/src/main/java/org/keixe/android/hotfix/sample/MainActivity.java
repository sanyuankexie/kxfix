package org.keixe.android.hotfix.sample;

import android.os.Bundle;
import android.view.View;

import org.keixe.android.hotfix.Hotfix;

import androidx.appcompat.app.AppCompatActivity;

@Hotfix
public class MainActivity extends AppCompatActivity {

    int xl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.root)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        xxx();
                        xl++;
                    }
                });
    }


    public void xxx() {

    }
}
