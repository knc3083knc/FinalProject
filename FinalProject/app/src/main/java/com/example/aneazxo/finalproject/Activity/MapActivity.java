package com.example.aneazxo.finalproject.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aneazxo.finalproject.Database.DataModel;
import com.example.aneazxo.finalproject.Database.Database;
import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Debug;
import com.example.aneazxo.finalproject.core.Speaker;
import com.example.aneazxo.finalproject.core.Tool;
import com.example.aneazxo.finalproject.find_path.algs.AStar;
import com.example.aneazxo.finalproject.find_path.algs.Dijkstra;
import com.example.aneazxo.finalproject.find_path.graph.AdjacencyListWeightedDirectedGraph;
import com.example.aneazxo.finalproject.find_path.graph.Graph;
import com.example.aneazxo.finalproject.find_path.graph.WeightedEdge;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MapActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener{

    private static final String TAG = "MapActivity";

    private GoogleApiClient googleApiClient;

    private GoogleMap mMap;

    private Button ptt;
    private Button btnList;
    private TextView mytext;
    private TextView mytext2;
    private TextView mytext3;
    private ImageView imageView;
    private Spinner spinner;

    private double lat;
    private double lon;

    private ArrayList<LatLng> latlngList = new ArrayList<LatLng>();
    private static SensorManager sensorService;
    private Sensor sensor;
    private float currentDegree = 0f;
    int degree = 0;
    double angleMap = 0;
    int point = 0;
    int disFoward = 0;

    private LatLng destination;
    private CountDownTimer echoTimer;
    private String compassState;
    private String compassStateChange;
    private String state;

    //global param for record state.
    private LatLng startPoint;
    private String recordName = "error";
    private ArrayList<LatLng> recordLatLngList;

    //database and shortest part alg
    private DataModel model;
    private Graph g;
    private int source = -1;
    private int target = -1;
    private int nRow;
    private int mRotate = 0;  // if captured image rotates

    //for test
    private String resText = "none";
    private Speaker speaker;
    private Vibrator vibrator;
    private boolean isExploreByTouchEnabled;
    private final int algorithm = 1; //0 = Dijk, 1 = AStar

    //test bluetooth
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter;
    private static final UUID DEFAULT_SPP_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice device;
    private CountDownTimer positionTimer;
    private BufferedReader br;
    private int deviceSelected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //test2();

        checkPointdata();
        init();
        notification(Tool.msgWelcome);
        if (deviceSelected == 1)
            bluetoothGPSinit ();

        //test
        //test();


        ptt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Speaker.getInstance(MapsActivity.this).speak("Hello world");
                startPrompt();
            }
        });
        btnList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogListView();
            }
        });

        Bundle b = getIntent().getExtras();
        if (b != null) {
            String text = b.getString("command");
            //mytext2.setText(text);
        }
    }

    public void bluetoothGPSinit () {
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

            timerStart ();

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

    public void timerStart () {
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
                            lat = ((int)latLog/100)+((latLog-(((int)latLog/100)*100))/60.0);
                            lon = ((int)lngLog/100)+((lngLog-(((int)lngLog/100)*100))/60.0);
                            Log.d(TAG, "nmeaMessage: " + nmeaMessage);
                            Log.d(TAG, "(lat, lng): " + lat + ", " + lon);
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            );

                            if (latlngList.size() > 0) {
                                PolylineOptions polylineOptions = new PolylineOptions();
                                polylineOptions.add(new LatLng(lat, lon));
                                for (LatLng latlng : latlngList) {
                                    mMap.addMarker(new MarkerOptions().position(latlng));
                                    polylineOptions.add(latlng);
                                    Log.d(TAG, "test: " + latlng.toString());
                                }
                            }

                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 18));
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
                timerStart ();
            }
        }.start();
    }

    public void stopTimer () {
        if (positionTimer != null)
            positionTimer.cancel();
    }

    private void refreshDatapointFile() {
        String fname = "";
        fname = Debug.ON? Tool.fname_debug: Tool.fname_user;
        File folder = new File(Tool.fpath + "/" + fname);

        try {
            FileOutputStream fOut = new FileOutputStream(folder);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);

            ArrayList<String> tempRecArray = model.selectAllToArray();
            outputStreamWriter.write("PointId,Name,Lat,Lng,Adj\n");
            for (int i = 0; i < tempRecArray.size(); i++) {
                outputStreamWriter.write("" + tempRecArray.get(i) + "\n");
            }

            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void test2() {

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
                    //Log.d(TAG, "mLine: " + mLine);
                    outputStreamWriter.write("" + mLine + "\n");
                }
            } catch (IOException e) {
                //log the exception
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                        outputStreamWriter.close();
                    } catch (IOException e) {
                        //log the exception
                    }
                }
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        ArrayList<String> responds = null;

        if (resultCode == RESULT_OK && null != data) {
            responds = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String text = "";
            text = responds.get(0).trim().replace(" ", "");
            switch (requestCode) {
                case 1: {
                    Log.d(TAG, "Get response: " + text);
                    resText = text;
                    execute(text);
                    break;
                }
            }
        } else if (requestCode == REQUEST_ENABLE_BT) {
            CheckBluetoothState();
        }

    }

    private void startPrompt() {
        stopEchoCompass();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Now");    // user hint 24 char per line
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);    // setting recognition model, optimized for short phrases – search queries
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);    // quantity of results we want to receive
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        startActivityForResult(intent, 1);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
    public void onLocationChanged(Location location) {
        if (deviceSelected == 0) {
            lat = location.getLatitude();
            lon = location.getLongitude();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 18));
            mytext.setText("Location : " + lat + ", " + lon + " / " + resText + " / " + state);
            mytext2.setText("Accuracy: " + (int) location.getAccuracy());

            if (state.equals("navigate")) {
                if (latlngList.size() > 0 && point < latlngList.size()) {

                    LatLng latLng = (LatLng) latlngList.get(point);
                    LatLng prevLatLng = (LatLng) latlngList.get(point - 1);
                    angleMap = Tool.angleFormNorth(lat, lon, latLng.latitude, latLng.longitude);
                    disFoward = (int) Tool.distFrom(lat, lon, latLng.latitude, latLng.longitude);
                    double disPrevPointToTarget = Tool.distFrom(prevLatLng.latitude, prevLatLng.longitude, latLng.latitude, latLng.longitude);
                    double disPrevPointToCurrentPoint = Tool.distFrom(prevLatLng.latitude, prevLatLng.longitude, lat, lon);
                    if (disFoward < 3 || disPrevPointToTarget <= disPrevPointToCurrentPoint) {
                        point++;

                        stopEchoCompass();
                        notification(Tool.msgNearWhere + findNearby());
                        delayAndEcho(5000);

                        Log.d(TAG, "onLocationChanged: point=" + point);
                    }
                    compass(disFoward);
                } else {
                    clearForBegin();
                    stopEchoCompass();
                    notification(Tool.msgArrive + resText + Tool.msgAlready);
                }
            } else if (state.equals("record")) {
                double distance = Tool.distFrom(startPoint.latitude, startPoint.longitude, lat, lon);
                if (distance > 5) {
                    startPoint = new LatLng(lat, lon); //changing startPoint
                    recordLatLngList.add(startPoint); //add next point
                }
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onResume() {

        Log.d("System", "onResume");
        super.onResume();

        if (sensor != null) {
            sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(MapActivity.this, "Not supported!", Toast.LENGTH_SHORT).show();
        }

        Bundle b = getIntent().getExtras();
        if (b != null && b.getString("command") != null) {
            String text = b.getString("command").replace(" ", "");
            mytext2.setText(text);
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
    public void onSensorChanged(SensorEvent sensorEvent) {
        degree = Math.round(sensorEvent.values[0]);
        String name = sensorEvent.sensor.getName();

        if (state.equals("navigate")) {
            if (latlngList.size() > 0) {
                compass(disFoward);
            }

            //state is not change but dist is change
            if (compassState.contains(Tool.msgForward) && compassStateChange.contains(Tool.msgForward)) {
                compassState = compassStateChange;
            }
            //compassState is changed.
            else if (!compassState.equals(compassStateChange)) {
                //into forward state from another state is must vibrator
                if (compassStateChange.contains(Tool.msgForward) && vibrator != null)
                    // assume has vibrator, vibrate for 500 milliseconds
                    vibrator.vibrate(500);

                compassState = compassStateChange;
                stopEchoCompass();
                delayAndEcho(2000);
            }
        } else if (state.equals("record")) {
            //none
        }

        currentDegree = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void compass(int metre) {
        if (Math.abs(angleMap - degree) < 20 || Math.abs(angleMap - degree) > 340) {
            mytext.setText("Forward " + metre + " metre");
            compassStateChange = Tool.msgForward + metre + Tool.msgMeter;
        } else if (Math.abs(angleMap - degree) > 160 && Math.abs(angleMap - degree) < 200) {
            mytext.setText("U Turn.");
            compassStateChange = Tool.msgTurnBack;
        } else if (degree > angleMap && Math.abs(angleMap - degree) > 180) {
            mytext.setText("Turn right: " + (int) (360 - Math.abs(angleMap - degree)) + (char) 0x00b0);
            compassStateChange = Tool.msgTurnRight;
        } else if (degree < angleMap && Math.abs(angleMap - degree) > 180) {
            mytext.setText("Turn left: " + (int) (360 - Math.abs(angleMap - degree)) + (char) 0x00b0);
            compassStateChange = Tool.msgTurnLeft;
        } else if (degree > angleMap) {
            mytext.setText("Turn left: " + (int) Math.abs(angleMap - degree) + (char) 0x00b0);
            compassStateChange = Tool.msgTurnLeft;
        } else if (degree < angleMap) {
            mytext.setText("Turn right: " + (int) Math.abs(angleMap - degree) + (char) 0x00b0);
            compassStateChange = Tool.msgTurnRight;
        }
    }

    private void startEchoCompass() {
        echoTimer = new CountDownTimer(5000, 5000) {
            public void onTick(long millisUntilFinished) {
                notification(compassState);
                Log.d(TAG, "startEchoCompass: " + compassState);
            }

            public void onFinish() {
                startEchoCompass();
            }
        };
        echoTimer.start();
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

    private void stopEchoCompass() {
        echoTimer.cancel();
    }

    public void init() {

        ptt = (Button) findViewById(R.id.btn1);
        btnList = (Button) findViewById(R.id.btn2);
        mytext = (TextView) findViewById(R.id.textView3);
        mytext2 = (TextView) findViewById(R.id.textView4);

        lat = 0;
        lon = 0;
        compassState = "";
        compassStateChange = "";
        state = "idle";

        deviceSelected = Tool.getSettingInfo(Tool.DEVICE_KEY);

        //check is talkback
        /*
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        //boolean isAccessibilityEnabled = am.isEnabled();
        isExploreByTouchEnabled = am.isTouchExplorationEnabled();
        Log.d(TAG, "isExploreByTouchEnabled: " + isExploreByTouchEnabled);
        if (isExploreByTouchEnabled) {
            ptt.setText("เลือกจุดหมาย");
        } else {
            ptt.setText("สั่งงานด้วยเสียง");
        }
        */

        //speaker
        speaker = new Speaker(MapActivity.this);

        //database
        model = new DataModel(this);

        //Dijkstra or AStar no refresh graph
        switch (algorithm) {
            case 0: //Dijkstra
                refreshGraph(); //use to generate or refresh graph
                break;
            default:
        }


        echoTimer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long l) {
                ;
            }

            @Override
            public void onFinish() {

            }
        }.start();

        sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Get instance of Vibrator from current Context
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void refreshGraph() {
        //Dijkstra or AStar no refresh graph
        switch (algorithm) {
            case 0: //Dijkstra
                nRow = model.nRow();
                //gen graph
                //Log.d(TAG, "nRow = " + nRow);
                g = generateGraph(nRow);
                break;
            default:
        }
    }

    public void findSource() {
        Cursor tempCursor = model.selectAll();

        tempCursor.moveToFirst();

        double dist_temp = Tool.distFrom(
                lat,
                lon,
                Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT))),
                Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)))
        );
        source = Integer.parseInt(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ID)));
        tempCursor.moveToNext();

        while (!tempCursor.isAfterLast()) {
            double lat2 = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT)));
            double lng2 = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)));
            String name = tempCursor.getString(tempCursor.getColumnIndex(Database.COL_NAME));
            double dist = Tool.distFrom(lat, lon, lat2, lng2);

            //debug
            //int tempId = Integer.parseInt(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ID)));
            //Log.d(TAG, "dist < dist_temp: " + dist + ", " + dist_temp + " : tempId=" + tempId);

            if (dist < dist_temp && name.equals("point")) {
                dist_temp = dist;
                source = Integer.parseInt(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ID)));

                //debug
                //Log.d(TAG, "source: " + source);
            }

            tempCursor.moveToNext();
        }
    }

    public String findNearby() {
        Cursor tempCursor = model.selectAll();
        String nearbyName = "error";
        tempCursor.moveToFirst();

        double dist_temp = Tool.distFrom(
                lat,
                lon,
                Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT))),
                Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)))
        );
        source = Integer.parseInt(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ID)));
        tempCursor.moveToNext();

        while (!tempCursor.isAfterLast()) {
            double lat2 = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT)));
            double lng2 = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)));
            String name = tempCursor.getString(tempCursor.getColumnIndex(Database.COL_NAME));
            double dist = Tool.distFrom(lat, lon, lat2, lng2);

            //debug
            //int tempId = Integer.parseInt(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ID)));
            //Log.d(TAG, "dist < dist_temp: " + dist + ", " + dist_temp + " : tempId=" + tempId);

            if (dist < dist_temp && !name.equals("point")) {
                dist_temp = dist;
                nearbyName = tempCursor.getString(tempCursor.getColumnIndex(Database.COL_NAME));

                //debug
                //Log.d(TAG, "source: " + source);
            }

            tempCursor.moveToNext();
        }
        return nearbyName;
    }

    public AdjacencyListWeightedDirectedGraph<WeightedEdge> generateGraph(int nPoint) {
        // construct new graph with the passed number of vertices
        AdjacencyListWeightedDirectedGraph<WeightedEdge> g = new AdjacencyListWeightedDirectedGraph<WeightedEdge>(nPoint);

        Cursor tempCursor = model.selectAll();

        tempCursor.moveToFirst();
        ArrayList<Integer> pointId = new ArrayList<Integer>();
        ArrayList<String> name = new ArrayList<String>();
        ArrayList<Double> lat = new ArrayList<Double>();
        ArrayList<Double> lng = new ArrayList<Double>();
        ArrayList<String[]> adj = new ArrayList<String[]>();

        while (!tempCursor.isAfterLast()) {
            pointId.add(tempCursor.getInt(tempCursor.getColumnIndex(Database.COL_ID)));
            name.add(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_NAME)));
            lat.add(Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT))));
            lng.add(Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG))));
            adj.add(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ADJ)).split("-"));

            tempCursor.moveToNext();
        }

        for (int i = 0; i < pointId.size(); i++) {
            String[] adjArray = adj.get(i);
            for (int j = 0; j < adjArray.length; j++) {
                tempCursor = model.selectWhereId(adjArray[j]);
                tempCursor.moveToFirst();
                //Log.d(TAG, "adjArray[j]: " + adjArray[j]);
                //Log.d(TAG, "tempCursor.getColumnIndex(Database.COL_LAT): " + tempCursor.getColumnIndex(Database.COL_LAT));
                //Log.d(TAG, "tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT)): " + tempCursor.getString(2));
                double lat2 = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT)));
                double lng2 = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)));
                int dist = (int) Tool.distFrom(lat.get(i), lng.get(i), lat2, lng2);
                //g.addEdge(new WeightedEdge(0, 1, 2)); // 0 -> 1 : w = 2
                g.addEdge(new WeightedEdge(pointId.get(i), Integer.parseInt(adjArray[j]), dist));

                tempCursor.moveToNext();
            }
        }
        return g;
    }

    private void execute(String text) {

        Cursor tempCursor = model.selectWhereName(text);

        tempCursor.moveToFirst();
        if (!tempCursor.isAfterLast()) {
            Log.d(TAG, "Finded: " + text + ", target id = " + tempCursor.getInt(tempCursor.getColumnIndex(Database.COL_ID)));
            target = tempCursor.getInt(tempCursor.getColumnIndex(Database.COL_ID));
            findSource();
            point = 1; //first point to navigate
            clearForBegin();
            latlngList.add(new LatLng(lat, lon));

            //find Shortest Path.
            ArrayList<Integer> path = new ArrayList<Integer>();
            switch (algorithm) {
                case 0: //Dijkstra
                    Dijkstra shortestPath = new Dijkstra();
                    path = shortestPath.findShortestPath(source, target, g);
                    break;
                case 1: //AStar (not need to refresh graph)
                    AStar aStar = new AStar();
                    aStar.prepare(model.selectAllToArray());
                    path = aStar.findPath(source, target);
                default:
            }

            if (path.size() == 0) {
                clearForBegin();
                notification(Tool.msgNoPath + text + Tool.msgTryAgain);
            } else {
                Log.d(TAG, "path: " + path.toString());
                for (int i = 0; i < path.size(); i++) {
                    tempCursor = model.selectWhereId(path.get(i).toString());
                    tempCursor.moveToFirst();
                    if (!tempCursor.isAfterLast()) {
                        latlngList.add(new LatLng(
                                Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT))),
                                Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)))
                        ));
                        //Log.d(TAG, test: " + path.get(i));
                    }
                }
                state = "navigate";
                angleMap = Tool.angleFormNorth(lat, lon, latlngList.get(0).latitude, latlngList.get(0).longitude);

                int totalDistance = 0;
                for (int i = 0; i < latlngList.size() - 1; i++) {
                    totalDistance += (int) Tool.distFrom(latlngList.get(i).latitude, latlngList.get(i).longitude, latlngList.get(i + 1).latitude, latlngList.get(i + 1).longitude);
                }
                notification(text + " ระยะ " + totalDistance + "m");

                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.add(new LatLng(this.lat, lon));
                for (LatLng latlng : latlngList) {
                    mMap.addMarker(new MarkerOptions().position(latlng));
                    polylineOptions.add(latlng);
                    Log.d(TAG, "test: " + latlng.toString());
                }

                mMap.addPolyline(polylineOptions);
                delayAndEcho(10000);
            }
        } else {
            if (text.contains("บันทึก")) {
                mMap.clear();
                recordName = text.substring(text.indexOf("บันทึก") + 6);
                state = "record";
                startPoint = new LatLng(lat, lon);
                recordLatLngList = new ArrayList<LatLng>();
                recordLatLngList.add(startPoint);
                notification(Tool.msgRecordStart);

            } else if (text.contains("จบ") && state.equals("record")) {
                mMap.clear();
                //reducing forward point
                if (recordLatLngList.size() >= 3) {
                    for (int i = 1; i < recordLatLngList.size() - 1; ) {
                        double angle1 = Tool.angleFormNorth(
                                recordLatLngList.get(i).latitude,
                                recordLatLngList.get(i).longitude,
                                recordLatLngList.get(i - 1).latitude,
                                recordLatLngList.get(i - 1).longitude
                        );
                        double angle2 = Tool.angleFormNorth(
                                recordLatLngList.get(i).latitude,
                                recordLatLngList.get(i).longitude,
                                recordLatLngList.get(i + 1).latitude,
                                recordLatLngList.get(i + 1).longitude
                        );
                        double angleBetween12 = Math.abs(angle1 - angle2);
                        if (angleBetween12 > 140 && angleBetween12 < 220) {
                            recordLatLngList.remove(i);
                        } else {
                            i++;
                        }
                    }

                    //clean recordLatLngList
                    for (int i = 0; i < recordLatLngList.size(); i++) {
                        int findIndex = recordLatLngList.lastIndexOf(recordLatLngList.get(i));
                        if (findIndex != i) { // delete loop
                            for (int j = 0; j < findIndex - i; j++) {
                                recordLatLngList.remove(i + 1);
                            }
                        }
                    }
                }

                //insert new path
                if (!addRecordList(recordName))
                    notification("Can not record this path, please try again.");
                else
                    notification("Recording complete.");

                //change state to idle
                state = "idle";

            } else if (text.contains("ต่อไป") && state.equals("navigate")) {
                point++;
            } else if (text.contains("state") || text.contains("สเตจ") || text.contains("stage")) {
                notification(state + "state");
            } else if (text.contains("ใกล้เคียง") || text.contains("สถานที่ใกล้เคียง") || text.contains("nearby")) {
                String nearbyName = findNearby();
                notification(nearbyName + " is near by.");
            } else if (text.contains("เข็มทิศ")) {
                stopEchoCompass();
                delayAndEcho(1000);
            } else {
                clearForBegin();
                //notification("Not found, please try again.");
                notification(Tool.msgNotFound);
            }
        }
    }

    private boolean addRecordList(String recordName) {
        try {
            if (recordLatLngList.size() >= 2) {
                int nextId = model.maxId() + 1;
                int tempId = -1;
                int prevTempId = -1;
                String adj = "";
                String preAdj = "";
                int currentId = -1;

                for (int i = 0; i < recordLatLngList.size(); i++) {
                    if (i == 0) {
                        Cursor tempCursor = model.selectAll();

                        tempCursor.moveToFirst();
                        while (!tempCursor.isAfterLast()) {

                            double dbLat = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT)));
                            double dbLng = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)));
                            if (Tool.distFrom(dbLat, dbLng, recordLatLngList.get(i).latitude, recordLatLngList.get(i).longitude) < 4) {
                                tempId = tempCursor.getInt(tempCursor.getColumnIndex(Database.COL_ID));
                                preAdj = tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ADJ));
                                break;
                            } else {
                                tempId = nextId;
                                preAdj = "";
                            }

                            tempCursor.moveToNext();
                        }
                    } else if (i > 0) {
                        Cursor tempCursor = model.selectAll();

                        tempCursor.moveToFirst();
                        if (tempId == nextId)
                            currentId = nextId + 1;
                        else
                            currentId = nextId;
                        adj = "";
                        while (!tempCursor.isAfterLast()) {

                            double dbLat = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT)));
                            double dbLng = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)));
                            if (Tool.distFrom(dbLat, dbLng, recordLatLngList.get(i).latitude, recordLatLngList.get(i).longitude) < 4) {
                                currentId = tempCursor.getInt(tempCursor.getColumnIndex(Database.COL_ID));
                                adj = tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ADJ));
                                break;
                            }

                            tempCursor.moveToNext();
                        }
                        if (tempId == nextId) {
                            if (prevTempId != -1)
                                preAdj += prevTempId + "-" + currentId;
                            else
                                preAdj += currentId;
                            //INSERT INTO TABLE_NAME VALUES (value1,value2,value3,...valueN);
                            model.insertRow(tempId, "point",
                                    recordLatLngList.get(i - 1).latitude,
                                    recordLatLngList.get(i - 1).longitude, preAdj);
                            nextId++;

                        } else {
                            if (prevTempId != -1)
                                preAdj += "-" + prevTempId + "-" + currentId;
                            else
                                preAdj += "-" + currentId;
                            //UPDATE table_name SET col_name = '...' WHERE COL_ID = ...;
                            model.updateAdj(tempId, preAdj);
                        }
                        if (i == recordLatLngList.size() - 1) {
                            //last record
                            currentId = nextId;
                            preAdj = adj;
                            if (preAdj.equals(""))
                                preAdj += tempId;
                            else
                                preAdj += "-" + tempId;
                            //INSERT INTO TABLE_NAME VALUES (value1,value2,value3,...valueN);
                            model.insertRow(currentId, recordName,
                                    recordLatLngList.get(recordLatLngList.size() - 1).latitude,
                                    recordLatLngList.get(recordLatLngList.size() - 1).longitude, preAdj);
                        } else {
                            prevTempId = tempId;
                            tempId = currentId;
                            preAdj = adj;
                        }

                    }
                }

            } else if (recordLatLngList.size() == 1) {
                int currentId = -1;
                boolean isNewRecord = false;
                Cursor tempCursor = model.selectAll();
                tempCursor.moveToFirst();
                while (!tempCursor.isAfterLast()) {
                    double dbLat = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT)));
                    double dbLng = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)));
                    if (Tool.distFrom(dbLat, dbLng, recordLatLngList.get(0).latitude, recordLatLngList.get(0).longitude) < 4) {
                        isNewRecord = true;
                        currentId = tempCursor.getInt(tempCursor.getColumnIndex(Database.COL_ID));
                        break;
                    }

                    tempCursor.moveToNext();
                }

                if (isNewRecord == false) {
                    int nextId = model.maxId() + 1;
                    model.insertRow(nextId, recordName, recordLatLngList.get(0).latitude, recordLatLngList.get(0).longitude, "");
                } else {
                    model.updateName(currentId, recordName);
                }
            }
            refreshDatapointFile();
            //Dijkstra or AStar no refresh graph
            //refreshGraph();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "addRecordList: ", e);
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                //return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void onTextClick(View v) {
        startPrompt();
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix,
                true);
    }

    public void showDialogListView() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MapActivity.this);
        builderSingle.setTitle("กรุณาเลือกจุดหมายด้านล่าง");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MapActivity.this, android.R.layout.select_dialog_singlechoice);


        ArrayList<String> tempArray = model.selectAllTarget();

        for (int i = 0; i < tempArray.size(); i++) {
            arrayAdapter.add(tempArray.get(i));
        }

        builderSingle.setNegativeButton("ยกเลิก", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String listSelected = arrayAdapter.getItem(which);

                /* popup confirm
                AlertDialog.Builder builderInner = new AlertDialog.Builder(MapActivity.this);
                builderInner.setTitle("Your Selected Item is");
                builderInner.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        dialog.dismiss();
                    }
                });
                builderInner.show();
                */
                clearForBegin();
                stopEchoCompass();
                resText = listSelected;
                execute(listSelected);
            }
        });
        builderSingle.show();


    }

    public void clearForBegin() {
        mMap.clear();
        latlngList.clear();
        state = "idle";
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
        Intent intent = new Intent(MapActivity.this, MainActivity.class);
        intent.putExtra("finished", "");
        intent.putExtra("from", MapActivity.class);
        startActivity(intent);
        finish();
    }

    public void notification (String text) {
        if (isExploreByTouchEnabled == true) {
            Toast.makeText(MapActivity.this, text,
                    Toast.LENGTH_LONG).show();
        } else {
            Speaker.getInstance(MapActivity.this).speak(text);
        }
    }
}