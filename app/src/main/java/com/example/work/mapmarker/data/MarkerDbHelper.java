package com.example.work.mapmarker.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.work.mapmarker.data.MarkerContract.MarkerEntry;

public class MarkerDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = MarkerDbHelper.class.getSimpleName();

    // Database version
    // ** If you change the database schema, you MUST increment the database version.**
    public static final int DATABASE_VERSION = 1;

    // Name of the database file
    public static final String DATABASE_NAME = "mapmarker.db";

    public MarkerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table markers
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop table markers and recreate table markers
        // **MUST implement this method when changing the database schema**
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    // SQL statement to create table markers
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + MarkerEntry.TABLE_NAME + " (" +
                    MarkerEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    MarkerEntry.COLUMN_MARKER_NAME + " TEXT NOT NULL, " +
                    MarkerEntry.COLUMN_MARKER_CONTENT + " TEXT, " +
                    MarkerEntry.COLUMN_MARKER_LATITUDE + " FLOAT NOT NULL, " +
                    MarkerEntry.COLUMN_MARKER_LONGITUDE + " FLOAT NOT NULL, " +
                    MarkerEntry.COLUMN_MARKER_COLOR + " INTEGER NOT NULL DEFAULT 0);";

    // SQL statement to drop table markers
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE" + MarkerEntry.TABLE_NAME;
}
