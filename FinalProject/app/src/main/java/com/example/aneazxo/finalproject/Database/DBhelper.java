package com.example.aneazxo.finalproject.Database;

/**
 * Created by Windows on 19/3/2561.
 */
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBhelper extends SQLiteOpenHelper {
    private final String TAG = getClass().getSimpleName();

    public DBhelper(Context context) {
        super(context, "interest.db", null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_FRIEND_TABLE = String.format("CREATE TABLE %s " +
                        "(%s INTEGER PRIMARY KEY  AUTOINCREMENT, %s TEXT, %s TEXT, %s TEXT)",
                Interest.Table,
                Interest.Column.ID,
                Interest.Column.LAT,
                Interest.Column.LNG,
                Interest.Column.string
                );

        Log.i(TAG, CREATE_FRIEND_TABLE);

        // create friend table
        db.execSQL(CREATE_FRIEND_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String DROP_Interest_TABLE = "DROP TABLE IF EXISTS " + Interest.Table;

        db.execSQL(DROP_Interest_TABLE);

        Log.i(TAG, "Upgrade Database from " + oldVersion + " to " + newVersion);

        onCreate(db);
    }

}
