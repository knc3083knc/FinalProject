package com.example.aneazxo.finalproject.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.aneazxo.finalproject.Database.DataModel;
import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Debug;
import com.example.aneazxo.finalproject.core.Tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class ConfirmActivity extends AppCompatActivity {

    private final String TAG = "ConfirmActivity";

    private final int SELECT_DESTINATON = 1;
    private final int RECORD_SOUND = 2;
    private final int DELETE = 3;
    private final int RECORD_TXT = 4;

    private String destination;
    private int from;
    private DataModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        model = new DataModel(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            destination = bundle.getString("des");
            from = bundle.getInt("from");
        }

        showDialog(destination, from);
    }

    private void showDialog(final String des, int from){
        //AlertDialog.Builder builder = new AlertDialog.Builder(ConfirmActivity.this);
        final Dialog dialog = new Dialog(ConfirmActivity.this);
        dialog.setContentView(R.layout.confirm_dialog);

        final TextView message = (TextView) dialog.findViewById(R.id.txt_dia);
        Button buttonConfirm = (Button) dialog.findViewById(R.id.btn_yes);
        Button buttonCancel = (Button) dialog.findViewById(R.id.btn_no);

        if (from == SELECT_DESTINATON){
            message.setText("ต้องการไปที่" + des + "ใช่หรือไม่");

            buttonConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ConfirmActivity.this, CamOptionActivity.class);
                    intent.putExtra("des", destination);
                    startActivity(intent);
                    finish();
                }
            });

            buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startPrompt(SELECT_DESTINATON);
                }
            });

            dialog.setCancelable(false);

            dialog.show();

        } else if (from == RECORD_SOUND) {
            message.setText("ต้องการบันทึกเส้นทางสำหรับไปที่" + des + "ใช่หรือไม่");

            buttonConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ConfirmActivity.this, RecControlActivity.class);
                    intent.putExtra("des", destination);
                    startActivity(intent);
                    finish();
                }
            });

            buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startPrompt(RECORD_SOUND);
                }
            });

            dialog.setCancelable(false);
            dialog.show();
        } else if (from == DELETE) {
            message.setText("ต้องการลบ " + des + " ใช่หรือไม่");
            final String name = des;
            buttonConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    delete (name);
                    Intent intent = new Intent(ConfirmActivity.this, DeleteActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ConfirmActivity.this, DeleteActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            dialog.setCancelable(false);

            dialog.show();
        } else if (from == RECORD_TXT) {
            message.setText("ต้องการบันทึกเส้นทางสำหรับไปที่" + des + "ใช่หรือไม่");

            buttonConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ConfirmActivity.this, RecControlActivity.class);
                    intent.putExtra("des", destination);
                    startActivity(intent);
                    finish();
                }
            });

            buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ConfirmActivity.this, RecordActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            dialog.setCancelable(false);
            dialog.show();
        }
    }

    private void startPrompt(int OP) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Now");    // user hint 24 char per line
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);    // setting recognition model, optimized for short phrases – search queries
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);    // quantity of results we want to receive
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        startActivityForResult(intent, OP);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        ArrayList<String> responds = null;

        if (resultCode == RESULT_OK && null != data) {
            responds = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String text = "";
            text = responds.get(0).trim().replace(" ", "");
            switch (requestCode) {
                case SELECT_DESTINATON: {
                    Intent intent = new Intent(ConfirmActivity.this, ConfirmActivity.class);
                    intent.putExtra("des", text);
                    intent.putExtra("from", SELECT_DESTINATON);
                    startActivity(intent);
                    finish();
                    break;
                }
                case RECORD_SOUND: {
                    Intent intent = new Intent(ConfirmActivity.this, ConfirmActivity.class);
                    intent.putExtra("des", text);
                    intent.putExtra("from", RECORD_SOUND);
                    startActivity(intent);
                    finish();
                    break;
                }
            }
        } else {
            switch (requestCode) {
                case SELECT_DESTINATON: {
                    Intent intent = new Intent(ConfirmActivity.this, SelectDesActivity.class);
                    startActivity(intent);
                    break;
                }
                case RECORD_SOUND: {
                    Intent intent = new Intent(ConfirmActivity.this, RecordActivity.class);
                    startActivity(intent);
                    break;
                }
            }

        }
    }

    public void delete (String name) {
        ArrayList<String> al = model.selectAllToArray();
        String ans = "";
        for (int i = 0; i < al.size(); i++) {
            if (al.get(i).contains(name)) {
                String[] stringAl = al.get(i).split(",");
                ans += stringAl[0] + ",point";
                for (int j = 2; j < stringAl.length; j++) {
                    ans += "," + stringAl[j];
                }
                al.set(i, ans);
            }
        }
        String fname = "";
        fname = Debug.ON? Tool.fname_debug: Tool.fname_user;
        File folder = new File(Tool.fpath + "/" + fname);

        try {
            FileOutputStream fOut = new FileOutputStream(folder);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);

            outputStreamWriter.write("PointId,Name,Lat,Lng,Adj\n");
            for (int i = 0; i < al.size(); i++) {
                Log.d(TAG, "al: " + al.get(i));
                outputStreamWriter.write("" + al.get(i) + "\n");
            }

            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
