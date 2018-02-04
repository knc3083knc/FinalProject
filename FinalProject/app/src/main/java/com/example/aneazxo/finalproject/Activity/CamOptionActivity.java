package com.example.aneazxo.finalproject.Activity;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Tool;

public class CamOptionActivity extends AppCompatActivity {

    private static final String TAG = "CamOptionActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private Button enCam;
    private Button disCam;
    private String destination;
    private int deviceSelected = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam_option);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        deviceSelected = Tool.getSettingInfo(Tool.DEVICE_KEY);
        if (deviceSelected == 1) {
            checkBluetooth();
        }
        else {
            //check location services
            checkLocationServices();
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            destination = bundle.getString("des");
        }

        enCam = (Button)findViewById(R.id.enableCamBtn);
        disCam = (Button)findViewById(R.id.disableCamBtn);

        enCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CamOptionActivity.this, NavCamEnActivity.class);
                intent.putExtra("des", destination);
                startActivity(intent);
                finish();
            }
        });

        disCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CamOptionActivity.this, NavCamDisActivity.class);
                intent.putExtra("des", destination);
                startActivity(intent);
                finish();
            }
        });
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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(CamOptionActivity.this, SelectDesActivity.class);
        startActivity(intent);
        finish();
    }

    public void checkLocationServices () {

        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        /*Toast.makeText(CamOptionActivity.this, "gps_enabled: " + gps_enabled,
                Toast.LENGTH_LONG).show();*/

        if(!gps_enabled) {
            goToLocationSetting();
        }
    }

    public void goToLocationSetting() {
        final Dialog dialog = new Dialog(CamOptionActivity.this);
        dialog.setContentView(R.layout.confirm_dialog);

        final TextView message = (TextView) dialog.findViewById(R.id.txt_dia);
        Button buttonConfirm = (Button) dialog.findViewById(R.id.btn_yes);
        Button buttonCancel = (Button) dialog.findViewById(R.id.btn_no);
        message.setText("ท่านยังไม่ได้เปิด Location ต้องการไปที่ตั่งค่าเพื่อเปิด Location หรือไม่");

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
}
