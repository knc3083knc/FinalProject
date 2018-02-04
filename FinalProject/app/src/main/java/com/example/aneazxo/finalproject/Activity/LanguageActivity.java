package com.example.aneazxo.finalproject.Activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import java.util.Locale;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Speaker;

import java.util.Locale;

/**
 * Created by Windows on 7/1/2561.
 */

public class LanguageActivity extends AppCompatActivity {
    private Button Thai;
    private Button Eng;
    private Button back;
    private boolean isExploreByTouchEnabled = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        isExploreByTouchEnabled = am.isTouchExplorationEnabled();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);
        back = (Button) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LanguageActivity.this,SettingActivity.class);
                startActivity(intent);
                finish();
            }
        });
        Thai = (Button) findViewById(R.id.Thai);
        Thai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Configuration config = new Configuration();
                config.locale = new Locale("th");
                getResources().updateConfiguration(config,null);
                notification("คุณได้ทำการเปลี่ยนภาษาเป็นภาษาไทยแล้ว");

            }
        });

        Eng = (Button) findViewById(R.id.Eng);
        Eng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Configuration config = new Configuration();
                config.locale = Locale.ENGLISH;
                getResources().updateConfiguration(config,null);
                notification("You changed to English.");
            }
        });
        }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(LanguageActivity.this, SettingActivity.class);
        startActivity(intent);
        finish();
    }
    public void notification(String text) {
        if (isExploreByTouchEnabled == true) {
            Toast.makeText(LanguageActivity.this, text,
                    Toast.LENGTH_LONG).show();
        }
        else
        {
            Speaker.getInstance(LanguageActivity.this).speak(text);

        }
    }

}
