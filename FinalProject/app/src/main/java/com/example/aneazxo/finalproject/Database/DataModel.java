package com.example.aneazxo.finalproject.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

/**
 * Created by AneazXo on 04-Mar-17.
 */

public class DataModel {

    private SQLiteDatabase mDb;
    private Database mHelper;
    private Cursor mCursor;

    public DataModel (Context ctx) {
        mHelper = new Database(ctx);
        mDb = mHelper.getWritableDatabase();
        mHelper.onUpgrade(mDb, 1, 1);
    }

    public int nRow () {
        mCursor = mDb.rawQuery("SELECT COUNT(*) AS nRow FROM " + Database.TABLE_NAME, null);
        mCursor.moveToFirst();
        int nRow = Integer.parseInt(mCursor.getString(0));
        return nRow;
    }

    public ArrayList<String> selectPOI () {
        ArrayList<String> ansArray = new ArrayList<String>();
        mCursor = mDb.rawQuery("SELECT " + Database.COL_ID1 + ", " + Database.COL_NAME1 + ", "
                + Database.COL_LAT1 + ", " + Database.COL_LNG1 +
                " FROM " + Database.TABLE_NAME1, null);

        mCursor.moveToFirst();
        while (!mCursor.isAfterLast()) {

            ansArray.add("" +
                    mCursor.getInt(mCursor.getColumnIndex(Database.COL_ID1)) + "," +
                    mCursor.getString(mCursor.getColumnIndex(Database.COL_NAME1)) + "," +
                    mCursor.getString(mCursor.getColumnIndex(Database.COL_LAT1)) + "," +
                    mCursor.getString(mCursor.getColumnIndex(Database.COL_LNG1))
            );

            mCursor.moveToNext();
        }
        return ansArray;
    }
    public ArrayList<String> selectAllToArray () {
        ArrayList<String> ansArray = new ArrayList<String>();
        mCursor = mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG + ", "
                + Database.COL_ADJ +
                " FROM " + Database.TABLE_NAME, null);

        mCursor.moveToFirst();
        while (!mCursor.isAfterLast()) {

            ansArray.add("" +
                    mCursor.getInt(mCursor.getColumnIndex(Database.COL_ID)) + "," +
                    mCursor.getString(mCursor.getColumnIndex(Database.COL_NAME)) + "," +
                    mCursor.getString(mCursor.getColumnIndex(Database.COL_LAT)) + "," +
                    mCursor.getString(mCursor.getColumnIndex(Database.COL_LNG)) + "," +
                    mCursor.getString(mCursor.getColumnIndex(Database.COL_ADJ))
            );

            mCursor.moveToNext();
        }
        return ansArray;
    }

    public Cursor selectAll () {
        return mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG + ", "
                + Database.COL_ADJ + " FROM " + Database.TABLE_NAME, null);
    }

    public Cursor selectWhereId(String id) {
        return mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG + ", "
                + Database.COL_ADJ + " FROM " + Database.TABLE_NAME
                + " WHERE " + Database.COL_ID + " = " + id, null);
    }

    public Cursor selectWhereName(String name) {
        return mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG + ", "
                + Database.COL_ADJ +
                " FROM " + Database.TABLE_NAME +
                " WHERE " + Database.COL_NAME + "='" + name + "'", null);
    }

    public ArrayList<String> selectAllTarget() {
        ArrayList<String> ansArray = new ArrayList<String>();
        mCursor = mDb.rawQuery("SELECT " + Database.COL_ID + ", " + Database.COL_NAME + ", "
                + Database.COL_LAT + ", " + Database.COL_LNG + ", "
                + Database.COL_ADJ +
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
                + Database.COL_LAT + ", " + Database.COL_LNG + ", "
                + Database.COL_ADJ +
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
                + ", " + Database.COL_LNG + ", " + Database.COL_ADJ
                + ") VALUES (" + id
                + ", '" + name
                + "', '" + lat
                + "', '" + lng
                + "', '" + adj
                + "');");
    }

    public void updateAdj (int id, String adj) {
        mDb.rawQuery("UPDATE " + Database.TABLE_NAME +
                        " SET " + Database.COL_ADJ + "='" + adj + "'" +
                        " WHERE " + Database.COL_ID + "=" + id
                , null);
    }

    public void updateName (int id, String name) {
        mDb.rawQuery("UPDATE " + Database.TABLE_NAME +
                        " SET " + Database.COL_NAME + "='" + name + "'" +
                        " WHERE " + Database.COL_ID + "=" + id
                , null);
    }
}
