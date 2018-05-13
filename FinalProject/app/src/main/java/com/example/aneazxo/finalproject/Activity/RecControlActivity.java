package com.example.aneazxo.finalproject.Activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.aneazxo.finalproject.Database.DataModel;
import com.example.aneazxo.finalproject.Database.DataModel1;
import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Debug;
import com.example.aneazxo.finalproject.core.Speaker;
import com.example.aneazxo.finalproject.core.Tool;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class RecControlActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "RecControlActivity";

    private GoogleApiClient googleApiClient;

    private GoogleMap mMap;

    private Button stopRec;
    private Button cancel;
    private Button update;
    private Button keyboard;
    private EditText text;
    private String destination;

    private double lat;
    private double lon;
    private String state;
    private String stateup;

    private DataModel1 model1;
    private DataModel model;
    private Location location;
    //private Speaker speaker;
    private boolean isExploreByTouchEnabled = false;
    private Vibrator vibrator;

    //global param for record state.
    private LatLng startPoint;
    private String recordName = "error";
    private String recordUpdate = "InterestPoint";
    private ArrayList<LatLng> recordLatLngList;
    private ArrayList<String> updateList;
    private ArrayList<LatLng> updateLatLngList;

    private boolean isExecuted;

    private int deviceSelected = 0;

    private boolean isStarted = false;

    //garmin glo
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter;
    private static final UUID DEFAULT_SPP_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice device;
    private CountDownTimer positionTimer;
    private BufferedReader br;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rec_control);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();
        if (deviceSelected == 1)
            bluetoothGPSinit();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            destination = bundle.getString("des");
        } else {
            destination = "เกิดข้อผิดพลาด";
        }

        execute(destination);

        stopRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isStarted == true) {
                    stopTimer();
                    try {
                        if (br != null)
                            br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    finishRecord();
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecControlActivity.this);
                    builder.setCancelable(true);
                    builder.setTitle(getString(R.string.reco));
                    builder.setMessage(getString(R.string.recor)+" "+recordName+" "+getString(R.string.yn));
                    builder.setPositiveButton(getString(R.string.confirm),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(RecControlActivity.this, ConfirmRecordActivity.class);
                                    intent.putExtra("LatLngList", recordLatLngList);
                                    intent.putExtra("recordName", recordName);
                                    intent.putExtra("updateName",updateList);
                                    intent.putExtra("upLatLngList",updateLatLngList);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                        }

                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();



                /*
                Intent intent = new Intent(RecControlActivity.this, MainActivity.class);
                intent.putExtra("finished", Tool.msgRecordComplete);
                intent.putExtra("from", RecControlActivity.class);
                startActivity(intent);
                finish();*/
                } else {
                    isStarted = true;
                    keyboard.setVisibility(View.VISIBLE);
                    stopRec.setText("ยืนยันการบันทึก");
                    notification(Tool.msgRecordStart + destination);
                    if (vibrator != null) {
                        // assume has vibrator, vibrate for 500 milliseconds
                        vibrator.vibrate(500);
                    }
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // return to RecordActivity
                Intent intent = new Intent(RecControlActivity.this, RecordActivity.class);
                startActivity(intent);
                finish();
            }
        });

        keyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyboard.setVisibility(View.GONE);
                text.setVisibility(View.VISIBLE);
                update.setVisibility(View.VISIBLE);
            }
        });
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = text.getText().toString();

                AlertDialog.Builder builder = new AlertDialog.Builder(RecControlActivity.this);
                builder.setCancelable(true);
                builder.setTitle(getString(R.string.reco));
                builder.setMessage(getString(R.string.POI)+" "+txt+" "+getString(R.string.yn));
                builder.setPositiveButton(getString(R.string.confirm),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String txt = text.getText().toString();
                                updateList.add(txt);
                                updateLocation();
                                Log.d(TAG,"LAT = "+lat+" LONG"+lon+" "+recordUpdate);
                            }
                        });
                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();

                    }

                });

                AlertDialog dialog = builder.create();
                dialog.show();
                Log.d(TAG,"LAT = "+lat+" LONG"+lon+" "+recordUpdate);

            }
        });



    }

   private void updateLocation() {
        stateup = "update";
       if(stateup.equals("update"))
       {
           startPoint = new LatLng(lat,lon);
           updateLatLngList.add(startPoint);

           Log.d(TAG,"SIZE = "+updateLatLngList.size());
           for(int i=0;i<updateLatLngList.size();i++)
           {
               Log.d(TAG,updateLatLngList.toString());
               Log.d(TAG,updateList.get(i));
           }
           stateup = "idle";
       }
    }


    private void init() {
        stopRec = (Button) findViewById(R.id.stopRecBtn);
        cancel = (Button) findViewById(R.id.cancel);
        update = (Button) findViewById(R.id.updateRecBtn);
        keyboard = (Button) findViewById(R.id.txtInput);
        text = (EditText) findViewById(R.id.editTxtInput);
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        isExploreByTouchEnabled = am.isTouchExplorationEnabled();

        //database
        model = new DataModel(this);
        /*model1 = new DataModel1(this);*/

        lat = 0;
        lon = 0;
        state = "idle";
        isExecuted = false;
        stateup = "idle";
        //speaker
        //speaker = new Speaker(RecControlActivity.this);
        // Get instance of Vibrator from current Context
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        getSettingInfo();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        LocationAvailability locationAvailability = LocationServices.FusedLocationApi.getLocationAvailability(googleApiClient);
        if (locationAvailability.isLocationAvailable()) {
            LocationRequest locationRequest = new LocationRequest()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(500);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        } else {
            // Do something when location provider not available
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (deviceSelected == 0) {
            lat = location.getLatitude();
            lon = location.getLongitude();
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 18));
            Log.d(TAG, "onLocationChanged: " + lat + ", " + lon);
            Log.d(TAG, "onLocationChanged: " + state + " state");
            Log.d(TAG,"state"+stateup);

            if (isStarted == true) {
                if (isExecuted == false) {
                    isExecuted = true;
                    startPoint = new LatLng(lat, lon);
                    recordLatLngList.add(startPoint);
                }

                if (state.equals("record")) {
                    double distance = Tool.distFrom(startPoint.latitude, startPoint.longitude, lat, lon);
                    if (distance > 5) {
                        startPoint = new LatLng(lat, lon); //changing startPoint
                        recordLatLngList.add(startPoint); //add next point
                    }
                }

            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(lat, lon), 18));
            }
        }

        if (isStarted == true) {
            //show path on map
            ArrayList<LatLng> tempSmoothPath = smoothPath();
            if (mMap != null)
                mMap.clear();
            PolylineOptions polylineOptions = new PolylineOptions();
            for (LatLng latlng : tempSmoothPath) {

                mMap.addMarker(new MarkerOptions().position(latlng));
                polylineOptions.add(latlng);
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(tempSmoothPath.get(tempSmoothPath.size() - 1).latitude,
                            tempSmoothPath.get(tempSmoothPath.size() - 1).longitude
                    ), 18));
            mMap.addPolyline(polylineOptions);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    public void clearForBegin() {
        //mMap.clear();
        //latlngList.clear();
        state = "idle";
    }

    @Override
    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onResume() {
        Log.d("System", "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d("System", "onPause");
        super.onPause();
    }

    private void execute(String text) {

        recordName = text;
        recordUpdate = "InterestPoint";
        state = "record";
        recordLatLngList = new ArrayList<LatLng>();
        updateList = new ArrayList<String>();
        updateLatLngList = new ArrayList<LatLng>();

        notification(Tool.msgPrepare);

    }

    private void finishRecord() {
        //reducing forward point
        recordLatLngList = smoothPath();

        /*
        //insert new path
        if (!addRecordList(recordName))
            notification("Can not record this path, please try again.");
        */
        //change state to idle
        state = "idle";
    }

    public ArrayList<LatLng> smoothPath () {
        ArrayList<LatLng> ans = recordLatLngList;
        //reducing forward point
        if (ans.size() >= 3) {
            for (int i = 1; i < ans.size() - 1; ) {
                double angle1 = Tool.angleFormNorth(
                        ans.get(i).latitude,
                        ans.get(i).longitude,
                        ans.get(i - 1).latitude,
                        ans.get(i - 1).longitude
                );
                double angle2 = Tool.angleFormNorth(
                        ans.get(i).latitude,
                        ans.get(i).longitude,
                        ans.get(i + 1).latitude,
                        ans.get(i + 1).longitude
                );
                double angleBetween12 = Math.abs(angle1 - angle2);
                if (angleBetween12 > 140 && angleBetween12 < 220) {
                    ans.remove(i);
                } else {
                    i++;
                }
            }

            //clean recordLatLngList
            for (int i = 0; i < ans.size(); i++) {
                int findIndex = ans.lastIndexOf(ans.get(i));
                if (findIndex != i) { // delete loop
                    for (int j = 0; j < findIndex - i; j++) {
                        ans.remove(i + 1);
                    }
                }
            }
        }
        return ans;
    }

    @Override
    public void onBackPressed() {

    }

    public void bluetoothGPSinit() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBluetoothState();
        BluetoothSocket bluetoothSocket = null;
        try {
            bluetoothSocket = device
                    .createRfcommSocketToServiceRecord(DEFAULT_SPP_UUID);

            bluetoothSocket.connect(); // Do this when you want to start data retrieval

            // After successful connect you can open InputStream
            InputStream in = bluetoothSocket.getInputStream();
            InputStreamReader isr = new InputStreamReader(in);
            br = new BufferedReader(isr);

            timerStart();

            // !!!CLOSE Streams!!!

        } catch (NullPointerException e) {
            e.printStackTrace();
            Intent intent = new Intent(RecControlActivity.this, MainActivity.class);
            intent.putExtra("finished", Tool.msgAccessory + ", " + Tool.msgStopRec);
            intent.putExtra("from", RecControlActivity.class);
            startActivity(intent);
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Intent intent = new Intent(RecControlActivity.this, MainActivity.class);
            intent.putExtra("finished", Tool.msgAccessory + ", " + Tool.msgStopRec);
            intent.putExtra("from", RecControlActivity.class);
            startActivity(intent);
            finish();
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
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
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

    public void timerStart() {
        positionTimer = new CountDownTimer(500, 50) {
            @Override
            public void onTick(long l) {
                try {
                    String nmeaMessage;
                    for (int i = 0; i < 3; i++) {
                        nmeaMessage = br.readLine();
                        if (nmeaMessage.contains("$GPGGA")) {
                            String[] stringSet = nmeaMessage.split(",");
                            //Log.d(TAG, "stringSet (2, 4): " + stringSet[2] + ", " + stringSet[4]);
                            double latLog = Double.parseDouble(stringSet[2]);
                            double lngLog = Double.parseDouble(stringSet[4]);
                            latLog = ((int) latLog / 100) + ((latLog - (((int) latLog / 100) * 100)) / 60.0);
                            lngLog = ((int) lngLog / 100) + ((lngLog - (((int) lngLog / 100) * 100)) / 60.0);
                            Log.d(TAG, "nmeaMessage: " + nmeaMessage);
                            Log.d(TAG, "(lat, lng): " + latLog + ", " + lngLog);

                            //show
                            /*
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 18));
                            */

                            lat = latLog;
                            lon = lngLog;
                            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 18));
                            Log.d(TAG, "onLocationChanged: " + lat + ", " + lon);
                            Log.d(TAG, "onLocationChanged: " + state + " state");

                            if (isExecuted == false) {
                                isExecuted = true;
                                startPoint = new LatLng(lat, lon);
                                recordLatLngList.add(startPoint);
                            }

                            if (state.equals("record")) {
                                double distance = Tool.distFrom(startPoint.latitude, startPoint.longitude, lat, lon);
                                if (distance > 5) {
                                    startPoint = new LatLng(lat, lon); //changing startPoint
                                    recordLatLngList.add(startPoint); //add next point
                                }
                            }

                            //break;
                        }
                    }
                    //String[] stringSet = nmeaMessage.split(",");
                    //Log.d(TAG, "stringSet 3, 5: " + stringSet[3] + ", " + stringSet[5]);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFinish() {
                timerStart();
            }
        }.start();
    }

    public void stopTimer () {
        if (positionTimer != null)
            positionTimer.cancel();
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
                        case "device":
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

    public void notification (String text) {
        if (isExploreByTouchEnabled == true) {
            Toast.makeText(RecControlActivity.this, text,
                    Toast.LENGTH_LONG).show();
        } else {
            Speaker.getInstance(RecControlActivity.this).speak(text);
        }
    }
}
