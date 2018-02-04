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
    private static final int DB_VERSION = 1;

    public static final String TABLE_NAME = "Point";

    public static final String COL_ID = "pointId";
    public static final String COL_NAME = "name";
    public static final String COL_LAT = "lat";
    public static final String COL_LNG = "lng";
    public static final String COL_ADJ = "adj";

    Context context;

    public Database(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
        context = ctx;
    }

    public void onCreate(SQLiteDatabase db) {
        String fname = "";
        fname = Debug.ON? Tool.fname_debug: Tool.fname_user;
        db.execSQL(
                "CREATE TABLE " + TABLE_NAME + "("
                + COL_ID + " INTEGER PRIMARY KEY, "
                + COL_NAME + " TEXT, " + COL_LAT + " TEXT, "
                + COL_LNG  + " TEXT, " + COL_ADJ + " TEXT);"
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
                            + ", " + COL_LNG + ", " + COL_ADJ
                            + ") VALUES (" + str[0]
                            + ", '" + str[1]
                            + "', '" + str[2]
                            + "', '" + str[3]
                            + "', '" + str[4]
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
