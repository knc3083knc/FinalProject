package com.example.aneazxo.finalproject.Activity;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Debug;
import com.example.aneazxo.finalproject.core.Speaker;
import com.example.aneazxo.finalproject.core.Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    SharedPreferences prefs = null;
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 001;
    private Button nav;
    private Button rec;
    private Button setting;
    private Button del;

    private boolean isExploreByTouchEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);
        prefs = getSharedPreferences("com.example.aneazxo.finalproject", MODE_PRIVATE);
        nav = (Button) findViewById(R.id.navBtn);
        rec = (Button) findViewById(R.id.recBtn);
        setting = (Button) findViewById(R.id.settingฺBtn);
        del = (Button) findViewById(R.id.delBtn);

        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        isExploreByTouchEnabled = am.isTouchExplorationEnabled();

        checkPointdataWrapper();

        Bundle bundle = getIntent().getExtras();
        if (bundle == null) { // speak only the first time
            checkTalkbackEnable();
            Log.d(TAG, "cleanDatabaseFile(if): start");
            Tool.cleanDatabaseFile();
            Log.d(TAG, "cleanDatabaseFile(if): end");
        } else {
            String msg = bundle.getString("finished");
            if (msg != null) {
                notification(msg);
                Log.d(TAG, "msg: " + msg);
            } else {
                checkTalkbackEnable();
            }
            Log.d(TAG, "cleanDatabaseFile(else): start");
            Tool.cleanDatabaseFile();
            Log.d(TAG, "cleanDatabaseFile(else): end");
        }


        nav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SelectDesActivity.class);
                startActivity(intent);
                finish();
            }
        });

        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RecordActivity.class);
                startActivity(intent);
                finish();
            }
        });

        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                finish();
            }
        });

        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DeleteActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        showDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.getBoolean("firstrun", true)) {
            notification(Tool.msgWelcome);
            prefs.edit().putBoolean("firstrun", false).commit();
        }
    }

    private void checkPointdata() {
        String fname = "";
        fname = Debug.ON? Tool.fname_debug: Tool.fname_user;
        new File(Tool.fpath).mkdirs();
        File f = new File(Tool.fpath + "/" + fname);

        Log.d(TAG, "test3: " + f.getPath());

        if (f.exists()) {
            Log.d(TAG, "f.exists(): have file from Map data.");

            //nothing

        } else {
            Log.d(TAG, "f.exists(): no have file from Map data.");

            BufferedReader reader = null;
            FileOutputStream fOut;
            OutputStreamWriter outputStreamWriter = null;
            try {
                reader = new BufferedReader(
                        new InputStreamReader(getAssets().open("pointdata.csv")));
                fOut = new FileOutputStream(f);
                outputStreamWriter = new OutputStreamWriter(fOut);

                // do reading, usually loop until end of file reading
                String mLine;
                Log.d(TAG, "test3: from assets");
                while ((mLine = reader.readLine()) != null) {
                    //process line
                    Log.d(TAG, "mLine: " + mLine);
                    outputStreamWriter.write("" + mLine + "\n");

                    if (!Debug.ON) {
                        break;
                    }
                }
            } catch (IOException e) {
                //log the exception
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        //log the exception
                    }
                }
                if (outputStreamWriter != null) {
                    try {
                        outputStreamWriter.close();
                    } catch (IOException e) {
                        //log the exception
                    }
                }
            }
        }
    }

    private void checkSettingData() {
        new File(Tool.fpath).mkdirs();
        File f = new File(Tool.fpath + "/" + Tool.settingFname);


        Log.d(TAG, "test3: " + f.getPath());

        if (f.exists()) {
            Log.d(TAG, "f.exists(): have file from Map data.");

            //nothing

        } else {
            Log.d(TAG, "f.exists(): no have file from Map data.");

            BufferedReader reader = null;
            FileOutputStream fOut;
            OutputStreamWriter outputStreamWriter = null;
            try {
                reader = new BufferedReader(
                        new InputStreamReader(getAssets().open("settings.txt")));
                fOut = new FileOutputStream(f);
                outputStreamWriter = new OutputStreamWriter(fOut);

                // do reading, usually loop until end of file reading
                String mLine;
                Log.d(TAG, "test3: from assets");
                while ((mLine = reader.readLine()) != null) {
                    //process line
                    Log.d(TAG, "mLine: " + mLine);
                    outputStreamWriter.write("" + mLine + "\n");
                }
            } catch (IOException e) {
                //log the exception
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        //log the exception
                    }
                }
                if (outputStreamWriter != null) {
                    try {
                        outputStreamWriter.close();
                    } catch (IOException e) {
                        //log the exception
                    }
                }
            }
        }
    }

    private void checkPointdataWrapper() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE))
            permissionsNeeded.add("Read and write database file");
        if (!addPermission(permissionsList, Manifest.permission.READ_PHONE_STATE))
            permissionsNeeded.add("Cellular");
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("Location");
        if (!addPermission(permissionsList, Manifest.permission.CAMERA))
            permissionsNeeded.add("Camera");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);
                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this, permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                            }
                        });
                return;
            }
            ActivityCompat.requestPermissions(MainActivity.this, permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }
        checkPointdata();
        checkSettingData();
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission))
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);

                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                if (perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    /* Add future permission check here */) {
                    // All Permissions Granted
                    checkPointdata();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Some Permission is Denied, Application may not work", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void showDialog() {
        //AlertDialog.Builder builder = new AlertDialog.Builder(ConfirmActivity.this);
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.confirm_dialog);

        final TextView message = (TextView) dialog.findViewById(R.id.txt_dia);
        Button buttonConfirm = (Button) dialog.findViewById(R.id.btn_yes);
        Button buttonCancel = (Button) dialog.findViewById(R.id.btn_no);
        message.setText("ต้องการออกจากโปรแกรมหรือไม่");

        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask();
                } else {
                    finish();
                }
                Process.killProcess(Process.myPid());
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

    public void notification(String text) {
        if (isExploreByTouchEnabled == true) {
            Toast.makeText(MainActivity.this, text,
                    Toast.LENGTH_LONG).show();
        } else {
            Speaker.getInstance(MainActivity.this).speak(text);
        }
    }

    public void checkTalkbackEnable(){
        if (!isExploreByTouchEnabled) {
            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.setContentView(R.layout.confirm_dialog);

            final TextView message = (TextView) dialog.findViewById(R.id.txt_dia);
            Button buttonConfirm = (Button) dialog.findViewById(R.id.btn_yes);
            Button buttonCancel = (Button) dialog.findViewById(R.id.btn_no);
            message.setText("Talkback ไม่ได้เปิด ต้องการเปิดหรือไม่, หากเลือกไม่ใช่จะออกจากโปรแกรม");

            buttonConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                    dialog.dismiss();
                }
            });

            buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //close popup
                    dialog.dismiss();

                    //exit app
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        finishAndRemoveTask();
                    } else {
                        finish();
                    }
                    Process.killProcess(Process.myPid());
                }
            });

            dialog.setCancelable(false);

            dialog.show();
        }
    }
}
