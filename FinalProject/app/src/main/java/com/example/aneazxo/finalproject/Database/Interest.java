/**
 * Created by Windows on 19/3/2561.
 */
package com.example.aneazxo.finalproject.Database;

import android.provider.BaseColumns;

public class Interest {
    private int ID;
    private String LAT;
    private String LNG;
    private String string;

    public static  final String DATABASE_NAME = "Interest.db";
    public static  final  int DATABASE_VERSION = 1;
    public static  final  String Table = "Interest";

    public class Column{
        public static final String ID = BaseColumns._ID;
        public static final String LAT = "lat";
        public static final String LNG = "lng";
        public static final String string = "name";

    }

    public Interest()
    {

    }

    public Interest(int id,String lat,String lng,String string)
    {
        this.ID= id;
        this.LAT = lat;
        this.LNG = lng;
        this.string = string;
    }
}
