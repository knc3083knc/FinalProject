package com.example.aneazxo.finalproject.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * Created by AneazXo on 29/4/2560.
 */

public class Tool {

    public static final String fname_user = "pointdata_user.csv";
    public static final String fname_debug = "pointdata_debug.csv";
    public static final String settingFname = "settings.txt";
    public static final String fpath = Environment.getExternalStorageDirectory() + "/" + "MapData";
    public static final String msgNotFound = "ไม่พบ";
    public static final String msgNoPath = "ไม่พบเส้นทาง";
    public static final String msgWelcome = "ยินดีต้อนรับ, สวัสดีค่ะ";
    public static final String msgArrive = "มาถึง, ";
    public static final String msgAlready = " แล้ว";
    public static final String msgNearWhere = "อยู่ใกล้กับ ";
    public static final String msgRecordStart = "เริ่มการบันทึก";
    public static final String msgTurnBack = "หันหลังกลับ";
    public static final String msgTurnLeft = "เลี้ยวซ้าย";
    public static final String msgTurnRight = "เลี้ยวขวา";
    public static final String msgForward = "เดินหน้า";
    public static final String msgMeter = "เมตร";
    public static final String msgCareStreet = "ระวังวัตถุข้างหน้า";
    public static final String msgLeft = "ทางซ้าย";
    public static final String msgRight = "ทางขวา";
    public static final String msgPole = "เสา";
    public static final String msgDegree = "องศา";
    public static final String msgTryAgain = ", ลองใหม่อีกครั้ง";
    public static final String msgRecordComplete = "บันทึกสำเร็จแล้ว";
    public static final String msgNoNearBy = "ไม่พบสถานที่ใกล้เคียง";
    public static final String msgStopNav = "หยุดการนำทางแล้ว";
    public static final String msgStopRec = "หยุดการบันทึกเส้นทาง";
    public static final String msgAccessory = "ไม่สามารถเชื่อมต่ออุปกรณ์เสริม, กรุณาตรวจสอบการจับคู่อุปกรณ์เสริมของคุณ";
    public static final String msgNoDes = "ยังไม่มีเส้นทาง, กรุณาบันทึกเส้นทางก่อน";
    public static final String msgNoNearby = "ไม่พบสถานที่ใกล้เคียง";
    public static final String msgPrepare = "สามารถกดปุ่มเริ่มการบันทึกเมื่อพร้อม";

    public static final String DEVICE_KEY = "device";

    public static final int RADIUS = 5; //check lat, lon with target to point++
    public static final int RADIUS_FIRST_POINT = 150; //check lat, lon choose first point
    public static final int RADIUS_ARRIVED = 25; //check is arrived?

    public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float dist = (float) (earthRadius * c);

        return dist;
    }

    public static double angleFormNorth(double lat1, double lng1, double lat2, double lng2) {
        double rLat = lat2;
        double rLng = lng1;
        double angle = 0;
        double dis = distFrom(lat2, lng2, rLat, rLng) / distFrom(lat2, lng2, lat1, lng1);
        angle = Math.asin(dis) * (180 / Math.PI);

        if (lat2 >= lat1 && lng2 >= lng1) { //Q1 +,+
            ;
        } else if (lat2 < lat1 && lng2 >= lng1) { //Q2 -,+
            angle = 90 + (90 - angle);
        } else if (lat2 >= lat1 && lng2 < lng1) { //Q4 +,-
            angle = 360 - angle;
        } else { //Q3 -,-
            angle = angle + 180;
        }

        return angle;
    }

    public static ArrayList insertionSort(ArrayList A) {
        int holePosition;
        int valueToInsert;

        for (int i = 1; i < A.size(); i++) {

            /* select value to be inserted */
            ArrayList<Integer> tempArray = (ArrayList<Integer>) A.get(i);
            valueToInsert = tempArray.get(1);
            holePosition = i;

            /*locate hole position for the element to be inserted */
            ArrayList<Integer> tempArray2 = (ArrayList<Integer>) A.get(holePosition - 1);
            while (holePosition > 0 && tempArray2.get(1) > valueToInsert) {
                A.set(holePosition, A.get(holePosition - 1));
                holePosition = holePosition - 1;
                if (holePosition > 0)
                    tempArray2 = (ArrayList<Integer>) A.get(holePosition - 1);
            }

            /* insert the number at hole position */
            A.set(holePosition, tempArray);
        }
        return A;
    }

    public static Bitmap convert(Bitmap bitmap, Bitmap.Config config) {
        Bitmap convertedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config);
        Canvas canvas = new Canvas(convertedBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return convertedBitmap;
    }

    public static int getSettingInfo(String setSelect) {
        try {
            FileReader fileReader = new FileReader(Tool.fpath + "/" + Tool.settingFname);
            BufferedReader br = new BufferedReader(fileReader);
            String readLine = null;

            try {
                while ((readLine = br.readLine()) != null) {
                    String[] str = readLine.split(":");
                    switch (str[0]) {
                        case DEVICE_KEY:
                            if (setSelect.equals(DEVICE_KEY))
                                return Integer.parseInt(str[1]);
                        //add case for setting here
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void cleanDatabaseFile() {
        ArrayList<String> readLineArray = new ArrayList<String>();
        String readLineHead;
        String fname = "";
        fname = Debug.ON? fname_debug: fname_user;
        OutputStreamWriter outputStreamWriter;
        try {
            FileReader fileReader = new FileReader(fpath + "/" + fname);
            BufferedReader br = new BufferedReader(fileReader);
            String readLine = null;
            readLineHead = br.readLine();

            while ((readLine = br.readLine()) != null) {

                readLineArray.add(readLine);
            }

            for (int i = 0; i < readLineArray.size(); i++) {

                String[] str = readLineArray.get(i).split(",");
                String[] adj = null;
                try {
                    adj = str[4].split("-");
                } catch (ArrayIndexOutOfBoundsException e) {
                    adj = fixEmptyAdj(readLineArray, i);
                }
                String adjStr = "";
                int adjInt;
                for (int j = 0; j < adj.length; j++) {
                    try {
                        adjInt = Integer.parseInt(adj[j]);
                        if (adjInt >= readLineArray.size()) {
                            ;
                        } else {
                            adjStr += adj[j] + "-";
                        }
                    } catch (NumberFormatException e) {
                        adjStr += adj[j] + "-";
                    }
                }
                if (adjStr.length() > 0) {
                    adjStr = adjStr.substring(0, adjStr.length() - 1);
                }
                readLineArray.set(i, str[0] + "," + str[1] + "," + str[2] + "," + str[3] + "," + adjStr);

            }
            readLineArray = checkAdj(readLineArray);

            File folder = new File(fpath + "/" + fname);
            FileOutputStream fOut = new FileOutputStream(folder);
            outputStreamWriter = new OutputStreamWriter(fOut);

            outputStreamWriter.write("PointId,Name,Lat,Lng,Adj\n");
            for (int i = 0; i < readLineArray.size(); i++) {
                Log.d("Tool", "readLineArray: " + readLineArray.get(i));
                outputStreamWriter.write("" + readLineArray.get(i) + "\n");
            }

            outputStreamWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> checkAdj(ArrayList<String> readLineArray) {
        for (int i = 0; i < readLineArray.size(); i++) {

            String[] str = readLineArray.get(i).split(",");
            String[] adj = null;
            try {
                adj = str[4].split("-");
            } catch (ArrayIndexOutOfBoundsException e) {
                //adj = fixEmptyAdj(readLineArray, i);
            }
            String adjStr = "";
            int adjInt;
            for (int j = 0; j < adj.length; j++) {
                try {
                    boolean isTwoWay = false;
                    String strTemp = readLineArray.get(Integer.parseInt(adj[j]));
                    String[] pointSet = strTemp.split(",");
                    String[] tempAdj = pointSet[4].split("-");
                    for (int k = 0; k < tempAdj.length; k++) {
                        if (tempAdj[k].equals(""+i))
                            isTwoWay = true;
                    }
                    if (isTwoWay == false) {
                        readLineArray.set(Integer.parseInt(adj[j]), pointSet[0] + "," + pointSet[1] + "," +
                                pointSet[2] + "," + pointSet[3] + "," + pointSet[4] + "-" + i);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

        }

        return readLineArray;
    }

    public static String[] fixEmptyAdj (ArrayList<String> readLineArray, int index) {
        String strIndex = "" + index;
        String ans = null;
        String adjStr = "";
        for (int i = 0; i < readLineArray.size(); i++) {
            if (i != index) {
                String[] str = readLineArray.get(i).split(",");
                String[] adj = null;
                try {
                    adj = str[4].split("-");
                } catch (ArrayIndexOutOfBoundsException e) {
                    ;
                }
                int adjInt;
                for (int j = 0; j < adj.length; j++) {
                    try {
                        adjInt = Integer.parseInt(adj[j]);
                        if (adjInt >= readLineArray.size()) {
                            ;
                        } else {
                            if (adj[j].equals(strIndex))
                                adjStr += i + "-";
                        }
                    } catch (NumberFormatException e) {
                        if (adj[j].equals(strIndex))
                            adjStr += i + "-";
                    }
                }
            }
        }
        if (adjStr.length() > 0) {
            adjStr = adjStr.substring(0, adjStr.length() - 1);
        }
        return adjStr.split("-");
    }

    public static void copyFileUsingChannel(File source, File dest) {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            try {
                sourceChannel.close();
                destChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}