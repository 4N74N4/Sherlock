package com.bgu.agent.sensors.AppsData;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.bgu.congeor.Constants;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by shedan on 17/09/2014.
 */
public class SQLiteHelper extends SQLiteOpenHelper {

    private static int DATABASE_VERSION = 1;

    public SQLiteHelper(Context context)
    {
        super(context, Constants.APPS_DATA_DB , null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        try{
            Log.i("SQLiteHelper Log", "Creating DB");
            String CREATE_CONTACTS_TABLE = "CREATE TABLE " + Constants.BANDWIDTH_TABLE + "("
                    + Constants.PACKAGE_NAME + " TEXT PRIMARY KEY," + Constants.UPLOAD_RATE_BYTE + " " + Constants.INT_TYPE + ", "
                    + Constants.UPLOAD_RATE_PACKET+ " " + Constants.INT_TYPE+", "+ Constants.DOWNLOAD_RATE_BYTE + Constants.INT_TYPE
                    + Constants.DOWNLOAD_RATE_PACKET+ " " + Constants.INT_TYPE+ ")";
            db.execSQL(CREATE_CONTACTS_TABLE);
            Log.i("SQLiteHelper Log", "Creating DB Successful");
        }
        catch(SQLiteException ex)
        {
            Log.e("sqlCreationError", ex.getMessage());
        }

    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("SQLiteHelper Log", "Upgrading Database " + oldVersion + " to " + newVersion);
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + Constants.BANDWIDTH_TABLE);
        DATABASE_VERSION = newVersion;
        // Create tables again
        onCreate(db);
    }

    // Adding new App
    public void addApp( String PackName,Long[] data )
    {
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(Constants.PACKAGE_NAME, PackName);
            values.put(Constants.UPLOAD_RATE_BYTE, data[0]);
            values.put(Constants.UPLOAD_RATE_PACKET, data[1]);
            values.put(Constants.DOWNLOAD_RATE_BYTE, data[2]);
            values.put(Constants.DOWNLOAD_RATE_PACKET, data[3]);
            db.insert(Constants.BANDWIDTH_TABLE, null, values);
            db.close();
        }
        catch(SQLiteException ex)
        {
            Log.e("sqlInsertionError", ex.getMessage());
        }

    }

    // Getting single App
    public Long[] getApp( String PackName)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        try{


            Cursor cursor = db.query(Constants.BANDWIDTH_TABLE, new String[] { Constants.PACKAGE_NAME,
                            Constants.UPLOAD_RATE_BYTE, Constants.UPLOAD_RATE_PACKET, Constants.DOWNLOAD_RATE_BYTE,Constants.DOWNLOAD_RATE_PACKET }, Constants.PACKAGE_NAME + "=?",
                    new String[] {  Constants.PACKAGE_NAME }, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                Long[] data = new Long[]{Long.parseLong(cursor.getString(1)), Long.parseLong(cursor.getString(2)), Long.parseLong(cursor.getString(3)), Long.parseLong(cursor.getString(4))};
                db.close();
                // return contact
                return data;
            }
            else {return null;}
        }
        catch(SQLiteException ex)
        {
            Log.e("error getting app", ex.getMessage());
            return null;
        }
        finally {
            db.close();
        }


    }

    // Getting All App Names in db
    public List<String> getAllApps()
    {
        List<String> appsList = new ArrayList<String>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + Constants.BANDWIDTH_TABLE;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                appsList.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        return appsList;
    }


    public void updateApp( String packName , Long[] data)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        try{
            ContentValues values = new ContentValues();
            values.put(Constants.PACKAGE_NAME, packName);
            values.put(Constants.UPLOAD_RATE_BYTE, data[0]);
            values.put(Constants.UPLOAD_RATE_BYTE, data[1]);
            values.put(Constants.UPLOAD_RATE_BYTE, data[2]);
            values.put(Constants.UPLOAD_RATE_BYTE, data[3]);

            db.update(Constants.BANDWIDTH_TABLE, values, Constants.PACKAGE_NAME + " = ?",
                    new String[] {packName });
        }
        finally {
            db.close();
        }

        // updating row
        db.close();
    }


    public void RemoveApp( String packName)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(Constants.BANDWIDTH_TABLE, Constants.PACKAGE_NAME + " = ?",
                new String[] { packName });
        db.close();
    }
}
