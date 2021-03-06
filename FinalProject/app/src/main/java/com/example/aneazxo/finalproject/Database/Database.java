package com.example.aneazxo.finalproject.Database;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.example.aneazxo.finalproject.core.Debug;
import com.example.aneazxo.finalproject.core.Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by AneazXo on 30-Jan-17.
 */

public class Database extends SQLiteOpenHelper {

    //private String fname = "pointdata.csv";
    private String fpath = Environment.getExternalStorageDirectory() + "/" + "MapData";

    private static final String DB_NAME = "My Point Data";

    private static final int DB_VERSION = 9;

    public static final String TABLE_NAME = "Point";
    public static final String COL_ID = "pointId";
    public static final String COL_NAME = "name";
    public static final String COL_LAT = "lat";
    public static final String COL_LNG = "lng";
    public static final String COL_ADJ = "adj";

    public static final String TABLE_NAME1 = "POI";
    public static final String COL_ID1 = "InterestId";
    public static final String COL_NAME1 = "name";
    public static final String COL_LAT1 = "lat";
    public static final String COL_LNG1 = "lng";




    Context context;

    public Database(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
        context = ctx;
    }

    public void onCreate(SQLiteDatabase db) {
        String fname = "";
        String fname1 = "";
        fname = Debug.ON? Tool.fname_debug: Tool.fname_user;
        fname1 = Debug.ON? Tool.fname_debug1: Tool.fname_user1;

        db.execSQL(
                "CREATE TABLE " + TABLE_NAME + "("
                + COL_ID + " INTEGER PRIMARY KEY, "
                + COL_NAME + " TEXT, " + COL_LAT + " TEXT, "
                + COL_LNG  + " TEXT, " + COL_ADJ + " TEXT);"
        );

        db.execSQL(
                "CREATE TABLE " + TABLE_NAME1 + "("
                        + COL_ID1 + " INTEGER PRIMARY KEY, "
                        + COL_NAME1 + " TEXT, " + COL_LAT1 + " TEXT, "
                        + COL_LNG1  + " TEXT );"
        );


        try {
            FileReader fileReader = new FileReader(fpath + "/" + fname);
            BufferedReader br = new BufferedReader(fileReader);
            String readLine = null;
            readLine = br.readLine();

            FileReader fileReader1 = new FileReader(fpath + "/" + fname1);
            BufferedReader br1 = new BufferedReader(fileReader1);
            String readLine1 = null;
            readLine1 = br1.readLine();

            try {
                while ((readLine = br.readLine()) != null) {
                    String[] str = readLine.split(",");
                    db.execSQL("INSERT INTO " + TABLE_NAME
                            + " (" + COL_ID + ", " + COL_NAME + ", " + COL_LAT
                            + ", " + COL_LNG + ", " + COL_ADJ
                            + ") VALUES (" + str[0]
                            + ", '" + str[1]
                            + "', '" + str[2]
                            + "', '" + str[3]
                            + "', '" + str[4]
                            + "');");
                }
                while ((readLine1 = br1.readLine()) != null) {
                    String[] str = readLine1.split(",");
                    db.execSQL("INSERT INTO " + TABLE_NAME1
                            + " (" + COL_ID1 + ", " + COL_NAME1 + ", " + COL_LAT1
                            + ", " + COL_LNG1 +") VALUES (" + str[0]
                            + ", '" + str[1]
                            + "', '" + str[2]
                            + "', '" + str[3]
                            + "');");
                }
            } catch (SQLiteConstraintException e) {
                e.printStackTrace();
                // DROP TABLE
                db.execSQL("DROP TABLE " + TABLE_NAME + ";");

                // CREATE TABLE
                db.execSQL(
                        "CREATE TABLE " + TABLE_NAME + "("
                                + COL_ID + " INTEGER PRIMARY KEY, "
                                + COL_NAME + " TEXT, " + COL_LAT + " TEXT, "
                                + COL_LNG  + " TEXT, " + COL_ADJ + " TEXT);"
                );

                db.execSQL(
                        "CREATE TABLE " + TABLE_NAME1 + "("
                                + COL_ID1 + " INTEGER PRIMARY KEY, "
                                + COL_NAME1 + " TEXT, " + COL_LAT1 + " TEXT, "
                                + COL_LNG1  + " TEXT );"
                );

                // Copy Backup to point_data && Delete Backup file??
                fname = "";
                fname = Debug.ON? Tool.fname_debug: Tool.fname_user;
                File mapData = new File(Tool.fpath + "/" + fname);
                File backupData = new File(Tool.fpath + "/" + "backup.csv");
                Tool.copyFileUsingChannel(backupData, mapData);

                // INSERT
                fileReader = new FileReader(fpath + "/" + fname);
                br = new BufferedReader(fileReader);
                readLine = null;
                readLine = br.readLine();

                fname1 = "";
                fname1 = Debug.ON? Tool.fname_debug1: Tool.fname_user1;
                File mapData1 = new File(Tool.fpath + "/" + fname1);
                File backupData1 = new File(Tool.fpath + "/" + "backup1.csv");
                Tool.copyFileUsingChannel(backupData1, mapData1);

                // INSERT
                fileReader1 = new FileReader(fpath + "/" + fname1);
                br1 = new BufferedReader(fileReader1);
                readLine1 = null;
                readLine1 = br1.readLine();
                try {
                    while ((readLine = br.readLine()) != null) {
                        String[] str = readLine.split(",");
                        db.execSQL("INSERT INTO " + TABLE_NAME
                                + " (" + COL_ID + ", " + COL_NAME + ", " + COL_LAT
                                + ", " + COL_LNG + ", " + COL_ADJ
                                + ") VALUES (" + str[0]
                                + ", '" + str[1]
                                + "', '" + str[2]
                                + "', '" + str[3]
                                + "', '" + str[4]
                                + "');");
                    }
                    while ((readLine1 = br1.readLine()) != null) {
                        String[] str = readLine1.split(",");
                        db.execSQL("INSERT INTO " + TABLE_NAME1
                                + " (" + COL_ID1 + ", " + COL_NAME1 + ", " + COL_LAT1
                                + ", " + COL_LNG1 +") VALUES (" + str[0]
                                + ", '" + str[1]
                                + "', '" + str[2]
                                + "', '" + str[3]
                                + "');");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                //
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion
            , int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME1);

        onCreate(db);
    }
}
