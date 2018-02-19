package com.example.aneazxo.finalproject.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aneazxo.finalproject.Database.DataModel;
import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Speaker;
import com.example.aneazxo.finalproject.core.Tool;

import java.util.ArrayList;

public class SelectDesActivity extends AppCompatActivity {

    private static final String TAG = "SelectDesActivity";

    private final int SELECT_DESTINATON = 1;
    private ListView desList;
    private Button ptt;

    private boolean isExploreByTouchEnabled = false;

    //private Speaker speaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_des);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        isExploreByTouchEnabled = am.isTouchExplorationEnabled();

        desList = (ListView) findViewById(R.id.pointList);

        DataModel model = new DataModel(SelectDesActivity.this);
        ArrayList<String> valuesList = model.selectAllTarget();
        Log.d(TAG, "valuesList: " + valuesList.size());
        if (valuesList.size() == 0) {
            notification(Tool.msgNoDes);
            goToRecord();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(SelectDesActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, valuesList);

        desList.setAdapter(adapter);

        desList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // ListView Clicked item index
                //int itemPosition     = position;

                // ListView Clicked item value
                String itemValue = (String) desList.getItemAtPosition(position);

                Intent intent = new Intent(SelectDesActivity.this, CamOptionActivity.class);
                intent.putExtra("des", itemValue);
                startActivity(intent);
                finish();
            }

        });
        ptt = (Button) findViewById(R.id.ptt);
        ptt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPrompt();
            }
        });
    }

    private void startPrompt() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Now");    // user hint 24 char per line
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);    // setting recognition model, optimized for short phrases – search queries
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);    // quantity of results we want to receive
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        startActivityForResult(intent, SELECT_DESTINATON);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        ArrayList<String> responds = null;
        DataModel model = new DataModel(SelectDesActivity.this);
        ArrayList<String> valuesList = model.selectAllTarget();
        if (resultCode == RESULT_OK && null != data) {
            responds = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String text = "";
            text = responds.get(0).trim().replace(" ", "");
            if(requestCode==SELECT_DESTINATON)
            {
                Log.d(TAG, "text ="+text);
                for(int i=0;i<valuesList.size();i++)
                {
                    if (valuesList.get(i).equals(text))
                    {
                        Log.d(TAG, "onActivityResult: "+requestCode);
                        Intent intent = new Intent(SelectDesActivity.this, ConfirmActivity.class);
                        intent.putExtra("des", text);
                        intent.putExtra("from", SELECT_DESTINATON);
                        startActivity(intent);
                        finish();
                    }

                }
                notification("ไม่พบเส้นทาง"+text+"กรุณาลองใหม่อีกครั้ง");



            }

        }
        else {

        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(SelectDesActivity.this, MainActivity.class);
        intent.putExtra("finished", "");
        intent.putExtra("from", SelectDesActivity.class);
        startActivity(intent);
        finish();
    }

    public void notification(String text) {
        if (isExploreByTouchEnabled == true) {
            Toast.makeText(SelectDesActivity.this, text,
                    Toast.LENGTH_LONG).show();
        } else {
            Speaker.getInstance(SelectDesActivity.this).speak(text);
        }
    }

    public void goToRecord() {
        final Dialog dialog = new Dialog(SelectDesActivity.this);
        dialog.setContentView(R.layout.confirm_dialog);

        final TextView message = (TextView) dialog.findViewById(R.id.txt_dia);
        Button buttonConfirm = (Button) dialog.findViewById(R.id.btn_yes);
        Button buttonCancel = (Button) dialog.findViewById(R.id.btn_no);
        message.setText(getString(R.string.record1));

        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SelectDesActivity.this, RecordActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SelectDesActivity.this, MainActivity.class);
                intent.putExtra("finished", "");
                intent.putExtra("from", SelectDesActivity.class);
                startActivity(intent);
                finish();
            }
        });

        dialog.setCancelable(false);

        dialog.show();

    }
}
