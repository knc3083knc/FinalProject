package com.example.aneazxo.finalproject.Activity;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Tool;

import java.util.ArrayList;

public class RecordActivity extends AppCompatActivity {

    private static final String TAG = "RecordActivity";
    private static final int REQUEST_ENABLE_BT = 1;

    private final int RECORD_SOUND = 2;
    private final int RECORD_TXT = 4;

    private Button ptt;
    private Button txtInput;
    private Button startRec;
    private Button toMain;
    private EditText editTxtInput;

    private int deviceSelected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        deviceSelected = Tool.getSettingInfo(Tool.DEVICE_KEY);
        if (deviceSelected == 1) {
            checkBluetooth();
        }
        else {
            //check location services
            checkLocationServices();
        }

        editTxtInput = (EditText) findViewById(R.id.editTxtInput);
        ptt = (Button) findViewById(R.id.ptt);
        txtInput = (Button) findViewById(R.id.txtInput);
        toMain = (Button) findViewById(R.id.toMainBtn);
        startRec = (Button) findViewById(R.id.startRecTxtInput);

        ptt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPrompt();
            }
        });

        txtInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTxtInput.setVisibility(View.VISIBLE);
                startRec.setVisibility(View.VISIBLE);
            }
        });

        toMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RecordActivity.this, MainActivity.class);
                intent.putExtra("finished", "");
                intent.putExtra("from", RecordActivity.class);
                startActivity(intent);
                finish();
            }
        });

        startRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = editTxtInput.getText().toString();
                if (!text.equals("")) {
                    Intent intent = new Intent(RecordActivity.this, ConfirmActivity.class);
                    intent.putExtra("des", text);
                    intent.putExtra("from", RECORD_TXT);
                    startActivity(intent);
                    finish();
                } else {
                    // alert user to input txt first
                }
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
        startActivityForResult(intent, RECORD_SOUND);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        ArrayList<String> responds = null;

        if (resultCode == RESULT_OK && null != data) {
            responds = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String text = "";
            text = responds.get(0).trim().replace(" ", "");
            switch (requestCode) {
                case RECORD_SOUND: {
                    Intent intent = new Intent(RecordActivity.this, ConfirmActivity.class);
                    intent.putExtra("des", text);
                    intent.putExtra("from", RECORD_SOUND);
                    startActivity(intent);
                    finish();
                    break;
                }
            }
        } else {

        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(RecordActivity.this, MainActivity.class);
        intent.putExtra("finished", "");
        intent.putExtra("from", RecordActivity.class);
        startActivity(intent);
        finish();
    }

    public void checkLocationServices () {

        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        //Toast.makeText(RecordActivity.this, "gps_enabled: " + gps_enabled, Toast.LENGTH_LONG).show();

        if(!gps_enabled) {
            goToLocationSetting();
        }
    }

    public void goToLocationSetting() {
        final Dialog dialog = new Dialog(RecordActivity.this);
        dialog.setContentView(R.layout.confirm_dialog);

        final TextView message = (TextView) dialog.findViewById(R.id.txt_dia);
        Button buttonConfirm = (Button) dialog.findViewById(R.id.btn_yes);
        Button buttonCancel = (Button) dialog.findViewById(R.id.btn_no);
        message.setText(getString(R.string.location1));

        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
                dialog.dismiss();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.setCancelable(false);

        dialog.show();

    }

    public void checkBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d(TAG, "checkBluetooth: Device does not support Bluetooth");
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enable :)
            } else {
                Log.d(TAG, "checkBluetooth: Bluetooth is not enable");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

}
