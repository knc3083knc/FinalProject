package com.example.aneazxo.finalproject.Activity;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.example.aneazxo.finalproject.Database.DataModel;
import com.example.aneazxo.finalproject.Database.DataModel1;
import com.example.aneazxo.finalproject.R;
import com.example.aneazxo.finalproject.core.Debug;
import com.example.aneazxo.finalproject.core.Tool;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class ConfirmRecordActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final String TAG = "ConfirmRecordActivity";

    private Button confirm;
    private Button cancel;
    private DataModel model;
    private DataModel1 model1;
    private String recordName = "error";
    private ArrayList<LatLng> latlngList = new ArrayList<LatLng>();
    private ArrayList<LatLng> updateLatLngList = new ArrayList<LatLng>();
    private ArrayList<String> updateName = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_confirm_record);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        model = new DataModel(this);

        confirm = (Button) findViewById(R.id.confirm);
        cancel = (Button) findViewById(R.id.cancel);

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Write Backup MapData
                String fname = "";
                fname = Debug.ON? Tool.fname_debug: Tool.fname_user;

                File oldFile = new File(Tool.fpath + "/" + fname);
                File backupFileName = new File(Tool.fpath + "/" + "backup.csv");
                Tool.copyFileUsingChannel(oldFile, backupFileName);

                // write file
                boolean b = addUpdateList(updateName);
                Log.d(TAG,"update"+b);
                addRecordList(recordName);
                Log.d(TAG,"add record success");


                Intent intent = new Intent(ConfirmRecordActivity.this, MainActivity.class);
                intent.putExtra("finished", Tool.msgRecordComplete);
                intent.putExtra("from", RecControlActivity.class);
                startActivity(intent);
                finish();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // return to RecordActivity
                Intent intent = new Intent(ConfirmRecordActivity.this, RecordActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

   private boolean addUpdateList(ArrayList<String> updateName) {
        try {
            //new method
            ArrayList<String> al = model.selectPOI();
            Log.d(TAG,"Select POI");
            Log.d(TAG,al.toString());
            refreshDatapointFileFromArrayList1(al);
            Log.d(TAG,"Do refresh");
            String sPoint ="";
            for(int i=0;i<updateName.size();i++)
            {
                Log.d(TAG,updateName.get(i));
                sPoint = "" + al.size() + ","+updateName.get(i)+","
                        + latlngList.get(i).latitude + ","
                        + latlngList.get(i).longitude ;
                Log.d(TAG,sPoint);
                al.add(sPoint);

            }
            refreshDatapointFileFromArrayList1(al);
            Log.d(TAG,al.toString());

            return true;

        } catch (Exception e) {
        }
        return false;
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

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            latlngList = (ArrayList<LatLng>) bundle.get("LatLngList");
            recordName = bundle.getString("recordName");
            updateLatLngList = (ArrayList<LatLng>) bundle.get("upLatLngList");
            updateName = (ArrayList<String>) bundle.get("updateName");
            for(int i=0;i<updateLatLngList.size();i++)
            {
                Log.d(TAG, updateLatLngList.get(i).toString());
                Log.d(TAG, updateName.get(i));
            }
        }

        PolylineOptions polylineOptions = new PolylineOptions();
        for (LatLng latlng : latlngList) {

            mMap.addMarker(new MarkerOptions().position(latlng));
            polylineOptions.add(latlng);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(latlngList.get(latlngList.size()-1).latitude,
                        latlngList.get(latlngList.size()-1).longitude
                ), 18));
        mMap.addPolyline(polylineOptions);
    }

    private  void addRecordList(String recordName) {
        try {
            //new method
            ArrayList<String> al = model.selectAllToArray();
            int[] checkPoint = new int[latlngList.size()];

            //prepare -1 set and nearby point set
            for (int i = 0; i < latlngList.size(); i++) {
                double recLat = latlngList.get(i).latitude;
                double recLng = latlngList.get(i).longitude;
                checkPoint[i] = -1;
                for (int j = 0; j < al.size(); j++) {
                    String[] alSet = al.get(j).split(","); // id[0], name[1], lat[2], lng[3], adj[4]
                    double alLat = Double.parseDouble(alSet[2]); //lat[2]
                    double alLng = Double.parseDouble(alSet[3]); //lng[3]

                    //check nearby in 6 metre
                    if (Tool.distFrom(recLat, recLng, alLat, alLng) < 6) {
                        checkPoint[i] = Integer.parseInt(alSet[0]); //id[0]
                    }
                }
            }

            for (int i = 0; i < checkPoint.length; i++) {
                //first point
                if (i == 0) {
                    //start with new point
                    if (checkPoint[i] == -1) {
                        String sPoint = "" + al.size() + ",point,"
                                + latlngList.get(i).latitude + ","
                                + latlngList.get(i).longitude + ",";

                        //single point
                        if (checkPoint.length == 1) {
                            sPoint = "" + al.size() + "," +
                                    recordName + "," +
                                    latlngList.get(i).latitude + "," +
                                    latlngList.get(i).longitude + ",s";
                        }
                        //adj to new point
                        else if (checkPoint[i + 1] == -1) {
                            sPoint += al.size() + 1;
                        }
                        //adj to ArrayList al point
                        else {
                            sPoint += checkPoint[i + 1];
                        }
                        Log.d("test", "addRecordList: " + sPoint);
                        al.add(sPoint);
                    } //start with ArrayList al point
                    else {
                        String[] alSet = al.get(checkPoint[i]).split(",");
                        String sPoint = al.get(checkPoint[i]);

                        //single point
                        if (checkPoint.length == 1) {
                            //sPoint = "" + al.size() + "," + recordName + "," + latlngList.get(i).latitude + "," + latlngList.get(i).longitude + ",s";
                        }

                        //adj to new point
                        else if (checkPoint[i + 1] == -1) //single point to new point
                        {
                            if (alSet[4].equals("s")) {
                                sPoint = alSet[0] + "," + alSet[1] + "," +
                                        alSet[2] + "," + alSet[3] + "," +
                                        al.size();
                                //point to new point
                            } else {
                                sPoint += "-" + al.size();
                            }
                        } //adj to ArrayList al point
                        else {
                            String[] adjSet = alSet[4].split("-");
                            boolean isHavingAdj = false;
                            for (int j = 0; j < adjSet.length; j++) {
                                if (adjSet[j].equals(checkPoint[i + 1])) {
                                    isHavingAdj = true;
                                }
                            }
                            if (isHavingAdj == true) {
                                ;//none
                            } else {
                                if (checkPoint[i] != checkPoint[i + 1])
                                    sPoint += checkPoint[i + 1];
                            }

                        }
                        al.remove(checkPoint[i]);
                        al.add(sPoint);
                    }
                }
                //last point
                else if (i == checkPoint.length - 1) {
                    if (checkPoint[i] == -1) {
                        String sPoint = "" + al.size() + "," +
                                recordName + "," +
                                latlngList.get(i).latitude + "," +
                                latlngList.get(i).longitude + ",";

                        //single point
                        if (checkPoint.length == 1)
                            ;//none
                            //adj from new point
                        else if (checkPoint[i - 1] == -1) {
                            sPoint += al.size() - 1;
                        }
                        //adj from ArrayList al point
                        else {
                            sPoint += checkPoint[i - 1];
                        }

                        al.add(sPoint);
                    } else {
                        String sPoint = al.get(checkPoint[i]);
                        String[] alSet = sPoint.split(",");
                        String[] adjSet = alSet[4].split("-");
                        sPoint = alSet[0] + "," + recordName + "," + alSet[2] + "," + alSet[3] + "," + alSet[4];
                        if (checkPoint[i - 1] == -1) {
                            sPoint += "-" + (al.size() - 1);
                        } else {
                            boolean isHavingAdj = false;
                            for (int j = 0; j < adjSet.length; j++) {
                                if (adjSet[j].equals("" + checkPoint[i - 1])) {
                                    isHavingAdj = true;
                                }
                            }
                            if (isHavingAdj == true) {
                                ;//none
                            } else {
                                if (checkPoint[i] != checkPoint[i - 1])
                                    sPoint += "-" + checkPoint[i - 1];
                            }
                        }
                        al.set(checkPoint[i], sPoint);
                    }

                }

                //anothor point
                else {
                    String sPoint = "";

                    //single point
                    if (checkPoint.length == 1)
                        ;//none

                        // new point
                    else if (checkPoint[i] == -1) {
                        sPoint = "" + al.size() + ",point," +
                                latlngList.get(i).latitude + "," +
                                latlngList.get(i).longitude + ",s";
                        String[] alSet = sPoint.split(",");
                        String[] adjSet = alSet[4].split("-");

                        // check previous point
                        if (checkPoint[i - 1] == -1)
                            if (alSet[4].equals("s"))
                                sPoint = sPoint.substring(0, sPoint.length() - 1) + (al.size() - 1);
                            else
                                sPoint += "-" + (al.size() - 1);
                        else {

                            boolean isHavingAdj = false;
                            for (int j = 0; j < adjSet.length; j++) {
                                if (adjSet[j].equals("" + checkPoint[i - 1])) {
                                    isHavingAdj = true;
                                }
                            }
                            if (isHavingAdj == true) {
                                ;//none
                            } else {
                                if (alSet[4].equals("s"))
                                    sPoint = sPoint.substring(0, sPoint.length() - 1) + checkPoint[i - 1];
                                else
                                    sPoint += "-" + checkPoint[i - 1];
                            }
                        }

                        alSet = sPoint.split(",");
                        adjSet = alSet[4].split("-");

                        // check next point
                        if (checkPoint[i + 1] == -1)
                            if (alSet[4].equals("s"))
                                sPoint = sPoint.substring(0, sPoint.length() - 1) + (al.size() + 1);
                            else
                                sPoint += "-" + (al.size() + 1);
                        else {
                            alSet = sPoint.split(",");
                            adjSet = alSet[4].split("-");
                            boolean isHavingAdj = false;
                            for (int j = 0; j < adjSet.length; j++) {
                                if (adjSet[j].equals("" + checkPoint[i + 1])) {
                                    isHavingAdj = true;
                                }
                            }
                            if (isHavingAdj == true) {
                                ;//none
                            } else {
                                if (alSet[4].equals("s"))
                                    sPoint = sPoint.substring(0, sPoint.length() - 1) + checkPoint[i + 1];
                                else
                                    sPoint += "-" + checkPoint[i + 1];
                            }
                        }

                        al.add(sPoint);
                    }

                    //point from al
                    else {
                        sPoint = al.get(checkPoint[i]);

                        // check previous point
                        if (checkPoint[i - 1] == -1)
                            sPoint += "-" + (al.size() - 1);
                        else {
                            String[] alSet = al.get(checkPoint[i]).split(",");
                            String[] adjSet = alSet[4].split("-");
                            boolean isHavingAdj = false;
                            for (int j = 0; j < adjSet.length; j++) {
                                if (adjSet[j].equals("" + checkPoint[i - 1])) {
                                    isHavingAdj = true;
                                }
                            }
                            if (isHavingAdj == true) {
                                ;//none
                            } else {
                                sPoint += checkPoint[i - 1];
                            }
                        }

                        // check next point
                        if (checkPoint[i + 1] == -1)
                            sPoint += "-" + al.size();
                        else {
                            String[] alSet = al.get(checkPoint[i]).split(",");
                            String[] adjSet = alSet[4].split("-");
                            boolean isHavingAdj = false;
                            for (int j = 0; j < adjSet.length; j++) {
                                if (adjSet[j].equals("" + checkPoint[i + 1])) {
                                    isHavingAdj = true;
                                }
                            }
                            if (isHavingAdj == true) {
                                ;//none
                            } else {
                                sPoint += checkPoint[i + 1];
                            }
                        }


                        al.set(checkPoint[i], sPoint);
                    }
                }
            }

            refreshDatapointFileFromArrayList(al);
            //Dijkstra or AStar no refresh graph
            //refreshGraph();

        } catch (Exception e) {
        }

    }
    private void refreshDatapointFileFromArrayList1(ArrayList<String> al) {
        String fname1 = "";
        fname1 = Debug.ON ? Tool.fname_debug1 : Tool.fname_user1;
        File folder = new File(Tool.fpath + "/" + fname1);

        try {
            FileOutputStream fOut = new FileOutputStream(folder);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);

            outputStreamWriter.write("Id,Name,Lat,Lng\n");
            for (int i = 0; i < al.size(); i++) {
                outputStreamWriter.write("" + al.get(i) + "\n");
            }

            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshDatapointFileFromArrayList(ArrayList<String> al) {
        String fname = "";
        fname = Debug.ON? Tool.fname_debug: Tool.fname_user;
        File folder = new File(Tool.fpath + "/" + fname);

        try {
            FileOutputStream fOut = new FileOutputStream(folder);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);

            outputStreamWriter.write("PointId,Name,Lat,Lng,Adj\n");
            for (int i = 0; i < al.size(); i++) {
                outputStreamWriter.write("" + al.get(i) + "\n");
            }

            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}