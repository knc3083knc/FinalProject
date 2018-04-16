package com.example.aneazxo.finalproject.Activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aneazxo.finalproject.Database.DataModel;
import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Navigation;
import com.example.aneazxo.finalproject.core.Speaker;
import com.example.aneazxo.finalproject.core.Tool;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class NavCamDisActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener {

    private static final String TAG = "NavCamDisActivity";

    private GoogleApiClient googleApiClient;

    private TextView nearBy;
    private TextView destination;
    private TextView distance;
    private TextView poi;
    private TextView direction;
    private Button stopNav;

    private DataModel model;

    private Navigation nav;
    private Sensor sensor;
    private static SensorManager sensorService;
    private int deviceSelected = 0;

    //private Speaker speaker;
    private Vibrator vibrator;
    private CountDownTimer echoTimer;
    private boolean isExploreByTouchEnabled = false;

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
        setContentView(R.layout.activity_nav_cam_dis);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        init();
        if (deviceSelected == 1)
            bluetoothGPSinit();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            Log.d(TAG, "onCreate: " + bundle.getString("des"));
            destination.setText("" + bundle.getString("des"));
        }

        Cursor tempCursor = model.selectWhereName((String) destination.getText());

        tempCursor.moveToFirst();
        if (!tempCursor.isAfterLast()) {
            //none
        } else {
            stopTimer();
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "onCreate: " + Tool.msgNotFound + destination.getText() + Tool.msgTryAgain);
            notification(Tool.msgNotFound + destination.getText() + Tool.msgTryAgain);
            Intent intent = new Intent(NavCamDisActivity.this, SelectDesActivity.class);
            startActivity(intent);
            finish();
        }

        stopNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTimer();
                try {
                    if (br != null)
                        br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "onClick: " + stopNav);
                Intent intent = new Intent(NavCamDisActivity.this, MainActivity.class);
                intent.putExtra("finished", Tool.msgStopNav);
                intent.putExtra("from", NavCamDisActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void init() {
        nearBy = (TextView) findViewById(R.id.nearBy);
        destination = (TextView) findViewById(R.id.destination);
        distance = (TextView) findViewById(R.id.distance);
        direction = (TextView) findViewById(R.id.direction);
        poi = (TextView) findViewById(R.id.poi);
        stopNav = (Button) findViewById(R.id.stopNavBtn);

        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        isExploreByTouchEnabled = am.isTouchExplorationEnabled();

        //database
        model = new DataModel(this);

        nav = new Navigation(NavCamDisActivity.this);

        sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        echoTimer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long l) {
                ;
            }

            @Override
            public void onFinish() {

            }
        }.start();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Get instance of Vibrator from current Context
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        deviceSelected = Tool.getSettingInfo("device");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        ArrayList<String> activity = nav.sensorChanged(sensorEvent);
        doActivity(activity);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

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
            ArrayList<String> activity = nav.locationChanged(location, (String) destination.getText());
            doActivity(activity);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

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

        if (sensor != null) {
            sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(NavCamDisActivity.this, "Not supported!", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onPause() {

        Log.d("System", "onPause");
        super.onPause();
        stopEchoCompass();
        sensorService.unregisterListener(this);
    }

    @Override
    public void onBackPressed() {
        stopTimer();
        try {
            if (br != null)
                br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onBackPressed: ");
        Intent intent = new Intent(NavCamDisActivity.this, MainActivity.class);
        intent.putExtra("finished", "");
        intent.putExtra("from", NavCamDisActivity.class);
        startActivity(intent);
        finish();
    }

    private void delayAndEcho(int milli) {
        echoTimer = new CountDownTimer(milli, 1000) {
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                startEchoCompass();
            }
        };
        echoTimer.start();
    }

    private void startEchoCompass() {
        Log.d(TAG, "startEchoCompass");
        echoTimer = new CountDownTimer(5000, 5000) {
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                notification(nav.getCompassState());
                Log.d(TAG, "startEchoCompass: " + nav.getCompassState());
                startEchoCompass();
            }
        };
        echoTimer.start();
    }

    public void stopEchoCompass() {
        echoTimer.cancel();
    }

    public void doActivity (ArrayList<String> activity) { //have this method in NavCamEnActivity and NavCamDisActivity
        Intent intent = new Intent(NavCamDisActivity.this, MainActivity.class);
        for (int i = 0; i < activity.size(); i++) {
            Log.d(TAG, "doActivity: " + activity.get(i));
            switch (activity.get(i)) {
                case "poi":
                    i++;
                    poi.setText(activity.get(i));
                    break;
                case "nearby":
                    i++;
                    nearBy.setText(activity.get(i));
                    break;
                case "distance":
                    i++;
                    distance.setText(activity.get(i));
                    break;
                case "compass":
                    i++;
                    direction.setText(activity.get(i));
                    break;
                case "startActivity":
                    stopTimer();
                    try {
                        if (br != null)
                            br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "startActivity: ");
                    //intent = new Intent(NavCamDisActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                case "stopEcho":
                    stopEchoCompass();
                    break;
                case "echoCompass":
                    delayAndEcho(2000);
                    break;
                case "speak":
                    i++;
                    Log.d(TAG, "doActivity: speak=" + activity.get(i));
                    if (activity.get(i).contains(Tool.msgArrive) || activity.get(i).contains(Tool.msgTryAgain)) {
                        intent.putExtra("finished", activity.get(i));
                        stopEchoCompass();
                    } else {
                        Log.d(TAG, "doActivity(activity.get(i)): " + activity.get(i));
                        notification(activity.get(i));
                    }
                    break;
            }
        }
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
            notification(Tool.msgAccessory);
        } catch (IOException e) {
            e.printStackTrace();
            notification(Tool.msgAccessory);
        }
    }

    private void CheckBluetoothState() {
        if (btAdapter == null) {
            //textview1.append("\nBluetooth NOT supported. Aborting.");
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
                //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
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

                            Location location = new Location("");
                            location.setLatitude(latLog);
                            location.setLongitude(lngLog);

                            Log.d(TAG, "bugTest: " + location.getLatitude() + ", " + location.getLongitude());
                            ArrayList<String> activity = nav.locationChanged(location, (String) destination.getText());
                            doActivity(activity);

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

    public void notification (String text) {
        if (isExploreByTouchEnabled == true) {
            Toast.makeText(NavCamDisActivity.this, text,
                    Toast.LENGTH_LONG).show();
        } else {
            Speaker.getInstance(NavCamDisActivity.this).speak(text);
        }
    }

}