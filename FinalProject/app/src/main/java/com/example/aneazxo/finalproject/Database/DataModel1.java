package com.example.aneazxo.finalproject.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

/**
 * Created by AneazXo on 04-Mar-17.
 */

public class DataModel1 {

    private SQLiteDatabase mDb;
    private Database1 mHelper;
    private Cursor mCursor;

    public DataModel1(Context ctx) {
        mHelper = new Database1(ctx);
        mDb = mHelper.getWritableDatabase();
        mHelper.onUpgrade(mDb, 1, 1);
    }

    public int nRow () {
        mCursor = mDb.rawQuery("SELECT COUNT(*) AS nRow FROM " + Database.TABLE_NAME, null);
        mCursor.moveToFirst();
        int nRow = Integer.parseInt(mCursor.getString(0));
        return nRow;
    }

    public ArrayList<String> selectAllToArray () {
        ArrayList<String> ansArray = new ArrayList<String>();
        mCursor = mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG +
                " FROM " + Database.TABLE_NAME, null);

        mCursor.moveToFirst();
        while (!mCursor.isAfterLast()) {

            ansArray.add("" +
                    mCursor.getInt(mCursor.getColumnIndex(Database.COL_ID)) + "," +
                    mCursor.getString(mCursor.getColumnIndex(Database.COL_NAME)) + "," +
                    mCursor.getString(mCursor.getColumnIndex(Database.COL_LAT)) + "," +
                    mCursor.getString(mCursor.getColumnIndex(Database.COL_LNG))
            );

            mCursor.moveToNext();
        }
        return ansArray;
    }

    public Cursor selectAll () {
        return mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG + " FROM " + Database.TABLE_NAME, null);
    }

    public Cursor selectWhereId(String id) {
        return mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG + " FROM " + Database.TABLE_NAME
                + " WHERE " + Database.COL_ID + " = " + id, null);
    }

    public Cursor selectWhereName(String name) {
        return mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG +
                " FROM " + Database.TABLE_NAME +
                " WHERE " + Database.COL_NAME + "='" + name + "'", null);
    }

    public ArrayList<String> selectAllTarget() {
        ArrayList<String> ansArray = new ArrayList<String>();
        mCursor = mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG +
                " FROM " + Database.TABLE_NAME +
                " WHERE " + Database.COL_NAME + "!='point'", null);

        mCursor.moveToFirst();
        while (!mCursor.isAfterLast()) {

            ansArray.add("" + mCursor.getString(mCursor.getColumnIndex(Database.COL_NAME)));

            mCursor.moveToNext();
        }
        return ansArray;

    }

    public int maxId () {
        mCursor = mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG +
                " FROM " + Database.TABLE_NAME +
                " WHERE " + Database.COL_ID + "=(SELECT MAX(" + Database.COL_ID + ") FROM " + Database.TABLE_NAME + ")", null);
        mCursor.moveToFirst();
        int maxId = mCursor.getInt(mCursor.getColumnIndex(Database.COL_ID));
        return maxId;
    }

    public void insertRow (int id, String name, double lat, double lng, String adj) {
        //INSERT INTO TABLE_NAME VALUES (value1,value2,value3,...valueN);
        mDb.execSQL("INSERT INTO " + Database.TABLE_NAME
                + " (" + Database.COL_ID + ", " + Database.COL_NAME + ", " + Database.COL_LAT
                + ", " + Database.COL_LNG + ") VALUES (" + id
                + ", '" + name
                + "', '" + lat
                + "', '" + lng
                + "');");
    }


    public void updateName (int id, String name) {
        mDb.rawQuery("UPDATE " + Database.TABLE_NAME +
                        " SET " + Database.COL_NAME + "='" + name + "'" +
                        " WHERE " + Database.COL_ID + "=" + id
                , null);
    }
}
