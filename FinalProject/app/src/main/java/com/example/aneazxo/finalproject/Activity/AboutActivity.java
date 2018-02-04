package com.example.aneazxo.finalproject.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.example.aneazxo.finalproject.R;

/**
 * Created by AneazXo on 25/6/2560.
 */

public class AboutActivity extends AppCompatActivity {

    private TextView version;
    private TextView dev;
    private TextView commit;

    final private String VERSION = "1.00";
    final private String PRE_COMMIT = "3f3cfec";
    final private String DEV_1 = "นายทีปกร  วงศ์แสนสุขเจริญ";
    final private String DEV_2 = "นายวุฒินันต์  หลงเจริญ";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        version = (TextView) findViewById(R.id.version);
        dev = (TextView) findViewById(R.id.developer);
        commit = (TextView) findViewById(R.id.commit);

        version.setText("เวอร์ชัน: " + VERSION);
        commit.setText("Previous Commit: " + PRE_COMMIT);
        dev.setText("ผู้พัฒนา : \n"
                + DEV_1 + "\n"
                + DEV_2);

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AboutActivity.this, SettingActivity.class);
        startActivity(intent);
        finish();
    }
}