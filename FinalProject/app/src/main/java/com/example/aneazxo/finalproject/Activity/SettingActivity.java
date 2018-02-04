package com.example.aneazxo.finalproject.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class SettingActivity extends AppCompatActivity {

    private ListView settingList;
    private Button toMain;

    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter btAdapter;

    private static final UUID DEFAULT_SPP_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice device;
    private boolean isCheckBluetooth = false;
    private int deviceSelected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        settingList = (ListView) findViewById(R.id.settingList);
        toMain = (Button) findViewById(R.id.back);

        initAdapter();

        settingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Set Operation Each Item Here
                String  itemValue    = (String) settingList.getItemAtPosition(position);
                switch (itemValue) {
                    case "ใช้อุปกรณ์เสริม":
                        btAdapter = BluetoothAdapter.getDefaultAdapter();
                        CheckBluetoothState();
                        initAdapter();
                        break;
                    case "ไม่ใช้อุปกรณ์เสริม":
                        setting(Tool.DEVICE_KEY, 0);
                        initAdapter();
                        break;
                    case "โหมดแผนที่":
                        Intent MapActivity = new Intent(SettingActivity.this, MapActivity.class);
                        startActivity(MapActivity);
                        finish();
                        break;
                    case "โหมดกล้อง":
                        Intent CameraModeActivity = new Intent(SettingActivity.this, CameraModeActivity.class);
                        startActivity(CameraModeActivity);
                        finish();
                        break;
                    case "เปิด/ปิด talkback":
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivity(intent);
                        //finish();
                        break;
                    case "เกี่ยวกับ":
                        Intent about = new Intent(SettingActivity.this, AboutActivity.class);
                        startActivity(about);
                        finish();
                        break;
                }
            }

        });

        toMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                intent.putExtra("finished", "");
                intent.putExtra("from", SettingActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public void initAdapter () {
        getSettingInfo();
        ArrayList<String> valuesList = new ArrayList<>();
        valuesList.add("เปิด/ปิด talkback");
        valuesList.add("การระบุทิศทาง");
        valuesList.add("ภาษา");
        if (deviceSelected == 0)
            valuesList.add("ใช้อุปกรณ์เสริม");
        else
            valuesList.add("ไม่ใช้อุปกรณ์เสริม");
        valuesList.add("โหมดแผนที่");
        valuesList.add("โหมดกล้อง");
        valuesList.add("เกี่ยวกับ");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(SettingActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, valuesList);

        settingList.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(SettingActivity.this, MainActivity.class);
        intent.putExtra("finished", "");
        intent.putExtra("from", SettingActivity.class);
        startActivity(intent);
        finish();
    }

    public void getSettingInfo() {
        try {
            FileReader fileReader = new FileReader(Tool.fpath + "/" + Tool.settingFname);
            BufferedReader br = new BufferedReader(fileReader);
            String readLine = null;

            try {
                while ((readLine = br.readLine()) != null) {
                    String[] str = readLine.split(":");
                    switch (str[0]) {
                        case Tool.DEVICE_KEY:
                            deviceSelected = Integer.parseInt(str[1]);
                            break;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setting (String key, int value) {
        ArrayList<String> al = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(Tool.fpath + "/" + Tool.settingFname);
            BufferedReader br = new BufferedReader(fileReader);
            String readLine = null;

            try {
                while ((readLine = br.readLine()) != null) {
                    al.add(readLine);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String ans = "";
        for (int i = 0; i < al.size(); i++) {
            if (al.get(i).contains(key)) {
                ans = key + ":" + value;
                al.set(i, ans);
            }
        }
        File folder = new File(Tool.fpath + "/" + Tool.settingFname);

        try {
            FileOutputStream fOut = new FileOutputStream(folder);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);

            for (int i = 0; i < al.size(); i++) {
                outputStreamWriter.write("" + al.get(i) + "\n");
            }

            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            CheckBluetoothState();
        }
    }

    private void CheckBluetoothState() {
        if (btAdapter == null) {
            //textview1.append("\nBluetooth NOT supported. Aborting.");
            return;
        } else {
            if (btAdapter.isEnabled()) {
                //textview1.append("\nBluetooth is enabled...");
                //textview1.append("\nPaired Devices are:");
                Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
                for (BluetoothDevice device : devices) {
                    //textview1.append("\n  Device: " + device.getName() + ", " + device);
                    this.device = device;
                }
                setting(Tool.DEVICE_KEY, 1);
                if (isCheckBluetooth == true) {
                    Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                    intent.putExtra("finished", "");
                    intent.putExtra("from", SettingActivity.class);
                    startActivity(intent);
                    finish();
                }
            } else if (isCheckBluetooth == false) {
                isCheckBluetooth = true;
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                intent.putExtra("finished", "");
                intent.putExtra("from", SettingActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
