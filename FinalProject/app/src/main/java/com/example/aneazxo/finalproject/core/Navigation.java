package com.example.aneazxo.finalproject.core;

import android.content.Context;
import android.database.Cursor;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;

import com.example.aneazxo.finalproject.Database.DataModel;
import com.example.aneazxo.finalproject.Database.Database;
import com.example.aneazxo.finalproject.find_path.algs.AStar;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by AneazXo on 18/5/2560.
 */

public class Navigation {
    private static final String TAG = "Navigation";

    private double angleMap = 0;
    private float currentDegree = 0f;
    private int degree = 0;
    private String compassState;
    private String compassStateChange;
    private String state;
    private int distance =0;
    private int distance10 =0;
    private int point = 0;
    private int target = -1;
    private CountDownTimer echoTimer;
    private String POI = Tool.msgNoNearby;
    private double dist_temp;
    private double lat;
    private double lon;
    private int distance_total;
    private int distance_balance;
    private int distance_poi;
    private ArrayList<LatLng> latlngList = new ArrayList<LatLng>();
    private boolean isExecuted;
    private int disFoward = 0;

    private DataModel model;
    private Context context;
    private Vibrator vibrator;

    public Navigation (Context mContext) {

        this.context = mContext;

        //database
        model = new DataModel(this.context);

        lat = 0;
        lon = 0;
        compassState = "";
        compassStateChange = "";
        state = "idle";
        isExecuted = false;

        // Get instance of Vibrator from current Context
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public ArrayList<String> locationChanged(Location location, String text) {
        lat = location.getLatitude();
        lon = location.getLongitude();
        ArrayList<String> ans = new ArrayList<>();
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 18));
        Log.d(TAG, "onLocationChanged: " + lat + ", " + lon);
        Log.d(TAG, "onLocationChanged: " + state + " state");

        if (isExecuted == false) {
            ans.add("speak");
            String ansText = execute(text);
            ans.add(ansText);
            if (ansText.contains(Tool.msgTryAgain) || ansText.contains(Tool.msgArrive)) {
                ans.add("startActivity");
                return ans;
            }

            isExecuted = true;

            if(findNearby()==null)
            {

            }
            else if (!findNearby().equals("error")) {
                //nearBy.setText(Tool.msgNearWhere + findNearby());
                ans.add("nearby");
                ans.add(Tool.msgNearWhere + findNearby());
            }
            else {
                //nearBy.setText(Tool.msgNoNearBy);
                ans.add("nearby");
                ans.add(Tool.msgNoNearBy);
            }
        }

        distance_balance = totalDistance(point);
        //distance.setText("เหลือ: " + distance_balance + "เมตร, จาก " + distance_total + Tool.msgMeter);
        ans.add("distance");
        ans.add(Tool.remaining+" "+ distance_balance + Tool.msgMeter+" "+Tool.from+" " + distance_total + Tool.msgMeter);
        Log.d(TAG,"MAth = "+(Math.abs(distance-distance_balance)==10));
        if(Math.abs(distance-distance_balance)==10)
        {
            distance = distance_balance;
            ans.add("speak");
            ans.add(compass(disFoward));
        }
        ans.add("poi");
        ans.add(Tool.msgPOIWhere + findPOI() + Tool.distance+" "+ (int)dist_temp+Tool.msgMeter);

        if (latlngList.size() > 0 && point < latlngList.size()) {

            LatLng latLng = (LatLng) latlngList.get(point);
            LatLng prevLatLng = (LatLng) latlngList.get(point - 1);
            angleMap = Tool.angleFormNorth(lat, lon, latLng.latitude, latLng.longitude);
            disFoward = (int) Tool.distFrom(lat, lon, latLng.latitude, latLng.longitude);
            double disPrevPointToTarget = Tool.distFrom(prevLatLng.latitude, prevLatLng.longitude, latLng.latitude, latLng.longitude);
            double disPrevPointToCurrentPoint = Tool.distFrom(prevLatLng.latitude, prevLatLng.longitude, lat, lon);
            if (disFoward < Tool.RADIUS || disPrevPointToTarget <= disPrevPointToCurrentPoint) {
                point++;

                //stopEchoCompass();
                ans.add("stopEcho");
                if (!findNearby().equals("error")) {
                    //nearBy.setText(Tool.msgNearWhere + findNearby());
                    ans.add("nearby");
                    ans.add(Tool.msgNearWhere + findNearby());
                }
                if (!findPOI().equals("error")) {//เพิ่มใหม่
                    ans.add("poi");
                    ans.add(Tool.msgPOIWhere + findPOI());
                }


                Log.d(TAG, "onLocationChanged: point=" + point);
            }
            ans.add("compass");
            ans.add(compass(disFoward));
        } else if (point >= latlngList.size() && latlngList.size() != 0) {
            //Speaker.getInstance(context).speak(Tool.msgArrive + text + Tool.msgAlready);
            ans.add("speak");
            ans.add(Tool.msgArrive + text + Tool.msgAlready);
            clearForBegin();
            //stopEchoCompass();
            ans.add("stopEcho");
            ans.add("startActivity");
            /*
            Intent intent = new Intent(context, MainActivity.class);
            startActivity(intent);
            finish();
            */
        }
        return ans;
    }

    private String execute(String text) {
        //is arrived?
        if (isArrived(text))
            return Tool.msgArrive + text + Tool.msgAlready;

        String ans = "";
        Cursor tempCursor = model.selectWhereName(text);

        tempCursor.moveToFirst();
        if (!tempCursor.isAfterLast()) {
            Log.d(TAG, "Finded: " + text + ", target id = " + tempCursor.getInt(tempCursor.getColumnIndex(Database.COL_ID)));
            target = tempCursor.getInt(tempCursor.getColumnIndex(Database.COL_ID));
            ArrayList<Integer> sourceList = findSource();
            point = 1; //first point to navigate
            clearForBegin();
            latlngList.add(new LatLng(lat, lon));

            //find Shortest Path.
            ArrayList<Integer> path = new ArrayList<>();
            for (int i = 0; i < sourceList.size(); i++) {
                AStar aStar = new AStar();
                aStar.prepare(model.selectAllToArray());
                path = aStar.findPath(sourceList.get(i), target);

                if (path.size() != 0)
                    break;
            }

            if (path.size() == 0) {
                clearForBegin();
                //Speaker.getInstance(context).speak(Tool.msgNoPath + text + Tool.msgTryAgain);
                ans = Tool.msgNoPath + text + Tool.msgTryAgain;
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

                distance_total = totalDistance(point);
                distance = distance_total;
                //Speaker.getInstance(context).speak(text + " ระยะ " + distance_total + "m");
                ans = text + " ระยะ " + distance_total + "m";
/*
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.add(new LatLng(this.lat, lon));
                for (LatLng latlng : latlngList) {
                    //mMap.addMarker(new MarkerOptions().position(latlng));
                    polylineOptions.add(latlng);
                    Log.d(TAG, "test: " + latlng.toString());
                }
*/
                //mMap.addPolyline(polylineOptions);
                //delayAndEcho(10000);
            }
        } else {
            clearForBegin();
            //Speaker.getInstance(context).speak(Tool.msgNotFound + text + Tool.msgTryAgain);
            ans = Tool.msgNotFound + text + Tool.msgTryAgain;
            /*
            Intent intent = new Intent(context, SelectDesActivity.class);
            startActivity(intent);
            finish();
            */
        }
        return ans;
    }

    public void clearForBegin() {
        //mMap.clear();
        latlngList.clear();
        state = "idle";
    }
    public String findPOI() {
        Cursor tempPOI = model.selectAllPOI();
        Log.d(TAG,tempPOI.toString());
        tempPOI.moveToFirst();

         dist_temp = Tool.distFrom(
                lat,
                lon,
                Double.parseDouble(tempPOI.getString(tempPOI.getColumnIndex(Database.COL_LAT1))),
                Double.parseDouble(tempPOI.getString(tempPOI.getColumnIndex(Database.COL_LNG1)))
        );



        while (!tempPOI.isAfterLast()) {
            double lat2 = Double.parseDouble(tempPOI.getString(tempPOI.getColumnIndex(Database.COL_LAT1)));
            double lng2 = Double.parseDouble(tempPOI.getString(tempPOI.getColumnIndex(Database.COL_LNG1)));
            String name = tempPOI.getString(tempPOI.getColumnIndex(Database.COL_NAME1));
            double dist = Tool.distFrom(lat, lon, lat2, lng2);


            if (dist <= dist_temp) {
                dist_temp = dist;
                POI = name;


            }

            tempPOI.moveToNext();
        }
        Log.d(TAG,POI.toString());
        return POI;
    }
    public String findNearby() {
        Cursor tempCursor = model.selectAll();
        String nearbyName = Tool.msgNoNearby;
        tempCursor.moveToFirst();

        double dist_temp = Tool.distFrom(
                lat,
                lon,
                Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT))),
                Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)))
        );
        //source = Integer.parseInt(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ID)));
        //tempCursor.moveToNext();

        while (!tempCursor.isAfterLast()) {
            double lat2 = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT)));
            double lng2 = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)));
            String name = tempCursor.getString(tempCursor.getColumnIndex(Database.COL_NAME));
            Log.d("Nav","NAme :" +name);
            double dist = Tool.distFrom(lat, lon, lat2, lng2);

            //debug
            //int tempId = Integer.parseInt(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ID)));
            //Log.d(TAG, "dist < dist_temp: " + dist + ", " + dist_temp + " : tempId=" + tempId);

            if (dist <= dist_temp && !name.equals("point")) {
                dist_temp = dist;
                nearbyName = name;

                //debug
                //Log.d(TAG, "source: " + source);
            }

            tempCursor.moveToNext();
        }
        return nearbyName;
    }

    public int totalDistance (int point) {
        int totalDistance = 0;

        if (point < latlngList.size())
            totalDistance += (int) Tool.distFrom(lat, lon, latlngList.get(point).latitude, latlngList.get(point).longitude);

        for (int i = point; i < latlngList.size() - 1; i++) {
            totalDistance += (int) Tool.distFrom(latlngList.get(i).latitude, latlngList.get(i).longitude, latlngList.get(i + 1).latitude, latlngList.get(i + 1).longitude);
        }
        return totalDistance;
    }


    public boolean isArrived (String target) {
        Cursor cursor = model.selectWhereName(target);
        cursor.moveToFirst();
        double latTarget = Double.parseDouble(cursor.getString(cursor.getColumnIndex(Database.COL_LAT)));
        double lngTarget = Double.parseDouble(cursor.getString(cursor.getColumnIndex(Database.COL_LNG)));
        int dist = (int)Tool.distFrom(lat, lon, latTarget, lngTarget);

        if (dist < Tool.RADIUS_ARRIVED) {
            return true;
        }

        return false;
    }

    public ArrayList<Integer> findSource() {
        ArrayList obj = new ArrayList();
        Cursor tempCursor = model.selectAll();

        tempCursor.moveToFirst();

        //source = Integer.parseInt(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ID)));
        tempCursor.moveToNext();

        while (!tempCursor.isAfterLast()) {
            double lat2 = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LAT)));
            double lng2 = Double.parseDouble(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_LNG)));
            String name = tempCursor.getString(tempCursor.getColumnIndex(Database.COL_NAME));
            double dist = Tool.distFrom(lat, lon, lat2, lng2);

            //debug
            //int tempId = Integer.parseInt(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ID)));
            //Log.d(TAG, "dist < dist_temp: " + dist + ", " + dist_temp + " : tempId=" + tempId);

            if (dist < Tool.RADIUS_FIRST_POINT && name.equals("point")) {
                int tempId = Integer.parseInt(tempCursor.getString(tempCursor.getColumnIndex(Database.COL_ID)));

                ArrayList<Integer> tempArray = new ArrayList<>();
                tempArray.add(tempId);
                tempArray.add((int) dist);
                obj.add(tempArray);

                //debug
                //Log.d(TAG, "source: " + source);
            }

            tempCursor.moveToNext();
        }

        obj = Tool.insertionSort(obj);
        ArrayList<Integer> ans = new ArrayList<>();
        for (Object al : obj) {
            ans.add(((ArrayList<Integer>) al).get(0));
        }

        return ans;
    }

    private String compass(int metre) {
        String ans = "";
        if (Math.abs(angleMap - degree) < 20 || Math.abs(angleMap - degree) > 340) {
            ans = Tool.msgForward + metre + Tool.msgMeter;
            compassStateChange = Tool.msgForward + metre + Tool.msgMeter;
        } else if (Math.abs(angleMap - degree) > 160 && Math.abs(angleMap - degree) < 200) {
            ans = Tool.msgTurnBack;
            compassStateChange = Tool.msgTurnBack;
        } else if (degree > angleMap && Math.abs(angleMap - degree) > 180) {
            ans = Tool.msgTurnRight + ": " + (int) (360 - Math.abs(angleMap - degree)) + (char) 0x00b0;
            compassStateChange = Tool.msgTurnRight;
        } else if (degree < angleMap && Math.abs(angleMap - degree) > 180) {
            ans = Tool.msgTurnLeft + ": " + (int) (360 - Math.abs(angleMap - degree)) + (char) 0x00b0;
            compassStateChange = Tool.msgTurnLeft;
        } else if (degree > angleMap) {
            ans = Tool.msgTurnLeft + ": " + (int) Math.abs(angleMap - degree) + (char) 0x00b0;
            compassStateChange = Tool.msgTurnLeft;
        } else if (degree < angleMap) {
            ans = Tool.msgTurnRight + ": " + (int) Math.abs(angleMap - degree) + (char) 0x00b0;
            compassStateChange = Tool.msgTurnRight;
        }
        return ans;
    }

    public ArrayList<String> sensorChanged(SensorEvent sensorEvent) {
        degree = Math.round(sensorEvent.values[0]);
        String name = sensorEvent.sensor.getName();
        ArrayList<String> ans = new ArrayList<>();

        if (state.equals("navigate")) {
            if (latlngList.size() > 0) {
                ans.add("compass");
                ans.add(compass(disFoward));
            }

            //state is not change but dist is change

            //compassState is changed.
            else if (!compassState.equals(compassStateChange)) {
                //into forward state from another state is must vibrator
                if (compassStateChange.contains(Tool.msgForward) && vibrator != null)
                    // assume has vibrator, vibrate for 500 milliseconds
                    vibrator.vibrate(500);

                compassState = compassStateChange;
                //stopEchoCompass();
                ans.add("stopEcho");
                //delayAndEcho(2000);
                ans.add("echoCompass");
            }
        } else if (state.equals("record")) {
            //none
        }

        currentDegree = -degree;
        return ans;
    }

    public String getCompassState () {
        return compassState;
    }
}
