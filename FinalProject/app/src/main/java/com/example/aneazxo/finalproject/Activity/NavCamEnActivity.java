package com.example.aneazxo.finalproject.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aneazxo.finalproject.Database.DataModel;
import com.example.aneazxo.finalproject.ImgProc.ObstructionDetector;
import com.example.aneazxo.finalproject.ImgProc.PoleDetector;
import com.example.aneazxo.finalproject.ImgProc.VPDetector;
import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Debug;
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
import com.google.android.gms.maps.model.RuntimeRemoteException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NavCamEnActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener,
        SurfaceHolder.Callback,
        Camera.ShutterCallback,
        Camera.PictureCallback {

    private static final String TAG = "NavCamEnActivity";

    private GoogleApiClient googleApiClient;

    private TextView nearBy;
    private TextView destination;
    private TextView distance;

    private TextView direction;
    private Button stopNav;

    private static SensorManager sensorService;
    private Sensor sensor;
    private float currentDegree = 0f;

    private CountDownTimer echoTimer;

    private Navigation nav;
    private DataModel model;
    private int deviceSelected = 0;

    //private Speaker speaker;
    private Vibrator vibrator;
    private boolean isExploreByTouchEnabled = false;

    //camera
    private Camera mCamera;
    private SurfaceView mPreview;
    private boolean saveState = false;
    private CountDownTimer cameraTimer;
    private int mRotate = 0;  // if captured image rotates
    private String periodCapture = "12000";
    private double focal;

    //opencv
    private Mat mRgba;
    private BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    //jcv.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

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
        setContentView(R.layout.activity_nav_cam_en);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        init();
        initCamera();
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
            notification(Tool.msgNotFound + destination.getText() + Tool.msgTryAgain);
            Intent intent = new Intent(NavCamEnActivity.this, SelectDesActivity.class);
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
                Intent intent = new Intent(NavCamEnActivity.this, MainActivity.class);
                intent.putExtra("finished", Tool.msgStopNav);
                intent.putExtra("from", NavCamEnActivity.class);
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
        stopNav = (Button) findViewById(R.id.stopNavBtn);

        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        isExploreByTouchEnabled = am.isTouchExplorationEnabled();

        //database
        model = new DataModel(this);

        nav = new Navigation(NavCamEnActivity.this);

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
        deviceSelected = Tool.getSettingInfo("device");
    }

    private void initCamera() {
        mPreview = (SurfaceView) findViewById(R.id.preview);
        mPreview.getHolder().addCallback(this);
        mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //delayAndCamera(2000);

    }

    @Override
    public void onPictureTaken(byte[] arg0, Camera arg1) {
        Bitmap bitmap;
        bitmap = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);

        bitmap = Tool.convert(bitmap, Bitmap.Config.RGB_565);
        /*
        if (mRotate != 0) {
            bitmap = rotateImage(bitmap, mRotate);
        }
        */
        Log.d(TAG, "getConfig: " + bitmap.getConfig().toString());
        //imageView.setImageBitmap(bitmap);

        Log.d(TAG, "bitmap Width x Height: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        //test opencv
        //long startTime = System.nanoTime();
        detect(bitmap);
        Bitmap bm = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bm);
        //imageView.setImageBitmap(bm);
        //long estimatedTime = System.nanoTime() - startTime;
        //mytext2.setText("" + estimatedTime*10e6);

        Log.d("Camera", "Restart Preview");
        mCamera.stopPreview();
        mCamera.startPreview();
        saveState = false;
    }

    public Mat detect(Bitmap capcap) {
        double height = 1.5;
        double pixelSize = 1.13e-6;
        byte downSampling = 4;

//        mRgba = inputFrame.rgba();
//        mRgba = new Mat (capcap.getHeight(), capcap.getWidth(), CvType.CV_8UC1);
        mRgba = new Mat();
        Utils.bitmapToMat(capcap, mRgba);

        //Mat tmpMat = new Mat();
        //Imgproc.resize(mRgba, tmpMat, new Size(780, 1040));
        //mRgba.release();
        //mRgba = tmpMat;
        int rows = mRgba.rows();
        int cols = mRgba.cols();

        if (Debug.ON) {
            System.out.println("mRgba rows " + rows);
            System.out.println("mRgba cols " + cols);
        }

        //mytext2.setText("" + capcap.getHeight() + "x" + capcap.getWidth());

        Mat imGray = new Mat();
        Imgproc.cvtColor(mRgba, imGray, Imgproc.COLOR_RGBA2GRAY);

        int stop = ObstructionDetector.detect(imGray);

        if (stop != 1) {
            Log.i(TAG, "Nothing");
            //mytext3.setText("Nothing");


            //[vp, val] = vpdet(imgray);  % detect vanishing point
            ArrayList<Object> res = VPDetector.detect(imGray);

            if (res == null) {
                return mRgba;
            }
            Point vp = (Point) (res.get(0));
            double val = (double) (res.get(1));

            Mat vpLines = (Mat) (res.get(2));
            if (Debug.ON) {
                Mat lines = (Mat) (res.get(3));
            }

            if (vpLines != null) {
                VPDetector.drawLines(mRgba, vpLines, new Scalar(0, 255, 0, 255), 2);
            }
//            Imgproc.line(mRgba,
//                    new Point(mRgba.cols()/2, 0),
//                    new Point(mRgba.cols()/2, mRgba.rows()/2),
//                    new Scalar(0,255,255,255), 2);


            if (val == 1) {
//                System.out.println("X:" + vp.x + " " + "Y:" + vp.y);
                Imgproc.drawMarker(mRgba, vp,
                        new Scalar(255, 0, 0, 255), Imgproc.MARKER_CROSS,
                        30, 5, Imgproc.LINE_AA);
                //% detect locations of poles
                //poledet(imgray,vp,height,pix_size,down_samp,foc);
                ArrayList<Object> poles;
                poles = PoleDetector.detect(imGray, vp, height, pixelSize, downSampling, focal);
                if (poles != null) {
                    Point[] points = (Point[]) (poles.get(0));
                    double[] angles = (double[]) (poles.get(1));
                    double[] distances = (double[]) (poles.get(2));
                    int numPoles = points.length;

                    for (int i = 0; i < numPoles; i++) {
                        Imgproc.drawMarker(mRgba, points[i],
                                new Scalar(0, 255, 0, 255), Imgproc.MARKER_CROSS,
                                30, 5, Imgproc.LINE_AA);
                        String s;
                        if (angles[i] < 0) {
                            s = Tool.msgLeft;
                        } else {
                            s = Tool.msgRight;
                        }
                        s = Tool.msgPole
                                + Math.abs(Math.round(angles[i]))
                                + Tool.msgDegree + s + ", "
                                + String.format("%.1f", distances[i]) + " m";
                        //System.out.printf("Pole Detected: angle = %d degree %s, dist = %.2f m\n",Math.abs(Math.round(angles[i])), s, distances[i]);
                        //mytext3.setText(s);

                        //
                        stopEchoCompass();
                        notification(s);
                        Toast.makeText(NavCamEnActivity.this, s,
                                Toast.LENGTH_LONG).show();
                        delayAndEcho(6000);

                    }
                }

            }
        } else {
            notification(Tool.msgCareStreet);
            if (vibrator != null) {
                // assume has vibrator, vibrate for 500 milliseconds
                vibrator.vibrate(500);
            }
            Log.i(TAG, "Stop!!!");
            //mytext3.setText("Stop!!!");
        }

        return mRgba;
    }

    @Override
    public void onShutter() {

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
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1
            , int arg2, int arg3) {
        Log.d("CameraSystem", "surfaceChanged");
        try {
            setupCamera();
        } catch (RuntimeRemoteException e) {
            Log.e(TAG, "surfaceChanged: ", e);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

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

    private void startEchoCompass() {
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
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onResume() {

        Log.d("System", "onResume");
        super.onResume();

        setupCamera();
        delayAndCamera(2000);

        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded successuly");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallBack);
        }

        if (sensor != null) {
            sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(NavCamEnActivity.this, "Not supported!", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onPause() {

        Log.d("System", "onPause");
        super.onPause();
        stopEchoCompass();
        stopCamera();
        sensorService.unregisterListener(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }

    public void setupCamera() {
        Log.d("CameraSystem", "setup preview");
        if (mCamera == null) {
            mCamera = Camera.open();
        }
        Camera.Parameters params = mCamera.getParameters();
        //Log.d(TAG, "down sampling_start: " + params.getPictureSize().width + " x " + params.getPictureSize().height);
        List<Camera.Size> previewSize = params.getSupportedPreviewSizes();
        List<Camera.Size> pictureSize = params.getSupportedPictureSizes();
        int nSize, i;

        nSize = pictureSize.size();
        for (i = 0; i < nSize; i++) {
            int w = pictureSize.get(i).width;
            //Log.d(TAG, "down sampling-1: " + pictureSize.get(i).width + " x " + pictureSize.get(i).height);
            if (w == 1024) break;
        }
        if (i != nSize) {
            params.setPictureSize(pictureSize.get(i).width, pictureSize.get(i).height);
        } else {
            // will be different values for different camera
            params.setPictureSize(pictureSize.get(0).width, pictureSize.get(0).height);
        }

        nSize = previewSize.size();
        for (i = 0; i < nSize; i++) {
            int w = previewSize.get(i).width;
            if (w == 1024) break;
        }
        if (i != nSize) {
            params.setPreviewSize(previewSize.get(i).width, previewSize.get(i).height);
        } else {
            // will be different values for different camera
            params.setPreviewSize(previewSize.get(0).width, previewSize.get(0).height);
        }

        //test size
        /* get supported picture sizes list
        pictureSize.get(0): 4160x3120
        pictureSize.get(1): 4096x2160
        pictureSize.get(2): 3264x2448
        pictureSize.get(3): 3200x2400
        pictureSize.get(4): 3328x1872
        pictureSize.get(5): 2560x1920
        pictureSize.get(6): 2560x1440
        pictureSize.get(7): 2048x1536
        pictureSize.get(8): 1600x1200
        pictureSize.get(9): 1280x960
        pictureSize.get(10): 1280x768
        pictureSize.get(11): 1280x720
        pictureSize.get(12): 1024x768
        pictureSize.get(13): 800x480
        pictureSize.get(14): 720x480
        pictureSize.get(15): 640x480
        pictureSize.get(16): 320x240
        */

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        if (info.canDisableShutterSound) {
            mCamera.enableShutterSound(false);
        }

        mRotate = info.orientation;
        params.setRotation(90);
        params.setJpegQuality(100);

        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);

        //FocalLength
        Log.d(TAG, "getFocalLength(): " + params.getFocalLength());
        focal = Double.parseDouble("" + params.getFocalLength()) / 1000.0;
        //Log.d(TAG, "down sampling: " + params.getPictureSize().width + " x " + params.getPictureSize().height);

        try {
            mCamera.setPreviewDisplay(mPreview.getHolder());
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void startCamera() {
        Log.d(TAG, "startCamera: ");
        int period;
        try {
            period = Integer.parseInt(periodCapture);
            cameraTimer = new CountDownTimer(period, period) {
                public void onTick(long millisUntilFinished) {

                }

                public void onFinish() {
                    Log.d(TAG, "onFinish: ");
                    if (!saveState) {
                        try {
                            Log.d(TAG, "onTick: startCamera");
                            saveState = true;
                            mCamera.takePicture(null, null, null, NavCamEnActivity.this);
                        } catch (RuntimeException e) {
                            saveState = false;
                            stopCamera();
                            Log.e(TAG, "onTick: java.lang.RuntimeException: Camera is being used after Camera.release() was called");
                        }
                    }
                    startCamera();
                }
            };
            cameraTimer.start();
        } catch (NumberFormatException e) {
            Log.d(TAG, "startCamera: NumberFormatException");
            period = 6000;
            cameraTimer = new CountDownTimer(period, period) {
                public void onTick(long millisUntilFinished) {
                    //nothing
                }

                public void onFinish() {
                    startCamera();
                }
            };
            cameraTimer.start();
        }
    }

    private void delayAndCamera(int milli) {
        cameraTimer = new CountDownTimer(milli, 1000) {
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                startCamera();
            }
        };
        cameraTimer.start();
    }

    private void stopCamera() {
        notification("");
        cameraTimer.cancel();
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix,
                true);
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
        Intent intent = new Intent(NavCamEnActivity.this, MainActivity.class);
        intent.putExtra("finished", "");
        intent.putExtra("from", NavCamEnActivity.class);
        startActivity(intent);
        finish();
    }

    public void doActivity (ArrayList<String> activity) { //have this method in NavCamEnActivity and NavCamDisActivity
        Intent intent = new Intent(NavCamEnActivity.this, MainActivity.class);
        for (int i = 0; i < activity.size(); i++) {
            //Log.d(TAG, "doActivity: " + activity.get(i));
            switch (activity.get(i)) {
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
                        Log.d(TAG, "nmeaMessage: " + nmeaMessage);
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
            Toast.makeText(NavCamEnActivity.this, text,
                    Toast.LENGTH_LONG).show();
        } else {
            Speaker.getInstance(NavCamEnActivity.this).speak(text);
        }
    }
}
