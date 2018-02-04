package com.example.aneazxo.finalproject.Activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aneazxo.finalproject.ImgProc.ObstructionDetector;
import com.example.aneazxo.finalproject.ImgProc.PoleDetector;
import com.example.aneazxo.finalproject.ImgProc.VPDetector;
import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Debug;
import com.example.aneazxo.finalproject.core.Speaker;
import com.example.aneazxo.finalproject.core.Tool;
import com.google.android.gms.maps.model.RuntimeRemoteException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by AneazXo on 17/7/2560.
 */

public class CameraModeActivity extends AppCompatActivity implements
        SurfaceHolder.Callback,
        Camera.ShutterCallback,
        Camera.PictureCallback {

    private final String TAG = "CameraModeActivity";

    private ImageView imageView;
    private SurfaceView surface;
    private TextView textView;

    //private Speaker speaker;
    private Vibrator vibrator;
    private boolean isExploreByTouchEnabled = false;

    //camera
    private Camera mCamera;
    private SurfaceView mPreview;
    private boolean saveState = false;
    private CountDownTimer cameraTimer;
    private int mRotate = 0;  // if captured image rotates
    private String periodCapture = "7000";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        init();
        initCamera();
    }

    private void init() {
        imageView = (ImageView) findViewById(R.id.image_view);
        surface = (SurfaceView) findViewById(R.id.preview);
        textView = (TextView) findViewById(R.id.line1);

        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        isExploreByTouchEnabled = am.isTouchExplorationEnabled();

        // Get instance of Vibrator from current Context
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
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
        textView.setText("bitmap Width x Height: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        //test opencv
        //long startTime = System.nanoTime();
        detect(bitmap);
        Bitmap bm = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bm);
        imageView.setImageBitmap(bm);
        //long estimatedTime = System.nanoTime() - startTime;
        //mytext2.setText("" + estimatedTime*10e6);

        Log.d("Camera", "Restart Preview");
        mCamera.stopPreview();
        mCamera.startPreview();
        saveState = false;
    }

    @Override
    public void onShutter() {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("CameraSystem", "surfaceChanged");
        try {
            setupCamera();
        } catch (RuntimeRemoteException e) {
            Log.e(TAG, "surfaceChanged: ", e);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

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

                        notification(s);
                        Toast.makeText(CameraModeActivity.this, s,
                                Toast.LENGTH_LONG).show();

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

    }

    @Override
    public void onPause() {

        Log.d("System", "onPause");
        super.onPause();
        stopCamera();
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
                            mCamera.takePicture(null, null, null, CameraModeActivity.this);
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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(CameraModeActivity.this, MainActivity.class);
        intent.putExtra("finished", "");
        intent.putExtra("from", NavCamEnActivity.class);
        startActivity(intent);
        finish();
    }

    public void notification (String text) {
        if (isExploreByTouchEnabled == true) {
            Toast.makeText(CameraModeActivity.this, text,
                    Toast.LENGTH_LONG).show();
        } else {
            Speaker.getInstance(CameraModeActivity.this).speak(text);
        }
    }
}
