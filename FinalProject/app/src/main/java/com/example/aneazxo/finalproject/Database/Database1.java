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

public class Database1 extends SQLiteOpenHelper {

    //private String fname = "pointdata.csv";
    private String fpath = Environment.getExternalStorageDirectory() + "/" + "MapData";

    private static final String DB_NAME = "My Interest Data";
    private static final int DB_VERSION = 1;

    public static final String TABLE_NAME = "Interest";



    public static final String COL_ID = "ID";
    public static final String COL_NAME = "name";
    public static final String COL_LAT = "lat";
    public static final String COL_LNG = "lng";





    Context context;

    public Database1(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
        context = ctx;
    }

    public void onCreate(SQLiteDatabase db) {
        String fname = "";
        fname = Debug.ON? Tool.fname_debug1: Tool.fname_user1;

        db.execSQL(
                "CREATE TABLE " + TABLE_NAME + "("
                        + COL_ID + " INTEGER PRIMARY KEY, "
                        + COL_NAME + " TEXT, " + COL_LAT + " TEXT, "
                        + COL_LNG  + " TEXT);"
        );





        try {
            FileReader fileReader = new FileReader(fpath + "/" + fname);
            BufferedReader br = new BufferedReader(fileReader);
            String readLine = null;
            readLine = br.readLine();

            try {
                while ((readLine = br.readLine()) != null) {
                    String[] str = readLine.split(",");
                    db.execSQL("INSERT INTO " + TABLE_NAME
                            + " (" + COL_ID + ", " + COL_NAME + ", " + COL_LAT
                            + ", " + COL_LNG + ") VALUES (" + str[0]
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
                                + COL_LNG  + " TEXT " + ");"
                );

                // Copy Backup to point_data && Delete Backup file??
                fname = "";
                fname = Debug.ON? Tool.fname_debug: Tool.fname_user1;
                File mapData = new File(Tool.fpath + "/" + fname);
                File backupData = new File(Tool.fpath + "/" + "backup1.csv");
                Tool.copyFileUsingChannel(backupData, mapData);

                // INSERT
                fileReader = new FileReader(fpath + "/" + fname);
                br = new BufferedReader(fileReader);
                readLine = null;
                readLine = br.readLine();
                try {
                    while ((readLine = br.readLine()) != null) {
                        String[] str = readLine.split(",");
                        db.execSQL("INSERT INTO " + TABLE_NAME
                                + " (" + COL_ID + ", " + COL_NAME + ", " + COL_LAT
                                + ", " + COL_LNG +") VALUES (" + str[0]
                                + ", '" + str[1]
                                + "', '" + str[2]
                                + "', '" + str[3]
                                + "',);");
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
        onCreate(db);
    }
}
