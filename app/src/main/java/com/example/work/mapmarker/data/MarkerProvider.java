package com.example.work.mapmarker.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.work.mapmarker.data.MarkerContract.MarkerEntry;

/**
 * Created by Work on 2017/4/15.
 */

public class MarkerProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = MarkerProvider.class.getSimpleName();

    //Instant of our subclass of SQLiteOpenHelper which will be instantiate in onCreate()
    private MarkerDbHelper mDbHelper;

    //UriMatcher IDs
    private static final int MARKERS = 100;
    private static final int MARKER_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(MarkerContract.CONTENT_AUTHORITY, MarkerContract.PATH_MARKERS, MARKERS);
        sUriMatcher.addURI(MarkerContract.CONTENT_AUTHORITY, MarkerContract.PATH_MARKERS + "/#", MARKER_ID);
    }

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        // Instantiate our subclass of SQLiteOpenHelper to gain access to the mapmarker database.
        mDbHelper = new MarkerDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case MARKERS:
                // For the MARKERS code, query the markers table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the markers table.
                cursor = database.query(MarkerEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case MARKER_ID:
                // For the MARKER_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.work.mapmarker/markers/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = MarkerEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the markers table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(MarkerEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Register to watch URI for changes. The listener attached to the content resolver will be notified.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Get the MIME type of data for the content URI.
     *
     * @returns the MIME type of data for the content URI.
     */
    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MARKERS:
                return MarkerEntry.CONTENT_LIST_TYPE;
            case MARKER_ID:
                return MarkerEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     *
     * @return  the new content URI for that specific row in the database.
     */
    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MARKERS:
                return insertMarker(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Delete the data at the given selection and selection arguments.
     *
     * @return the number of rows that were successfully deleted
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case MARKERS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(MarkerEntry.TABLE_NAME, selection, selectionArgs);

                // If 1 or more rows were deleted, then notify all listeners that the data at the
                // given URI has changed
                if (rowsDeleted != 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }

                //Return the number of rows deleted
                return rowsDeleted;

            case MARKER_ID:
                // Delete a single row given by the ID in the URI
                selection = MarkerEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(MarkerEntry.TABLE_NAME, selection, selectionArgs);

                // If 1 or more rows were deleted, then notify all listeners that the data at the
                // given URI has changed
                if (rowsDeleted != 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }

                //Return the number of rows deleted (likely 1)
                return rowsDeleted;

            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     *
     * @return the number of rows that were successfully updated from updateMarker()
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MARKERS:
                return updateMarker(uri, values, selection, selectionArgs);
            case MARKER_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = MarkerEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // For debugging
                Log.v(LOG_TAG,"case MARKER_ID");
                Log.v(LOG_TAG, "selection = " + selection);
                Log.v(LOG_TAG, "selectionArgs[0] = " + selectionArgs[0]);

                return updateMarker(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Helper method to update markers in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more markers).
     *
     * @return the number of rows that were successfully updated.
     */
    private int updateMarker(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // If the {@link MarkerEntry#COLUMN_MARKER_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(MarkerEntry.COLUMN_MARKER_NAME)) {
            String name = values.getAsString(MarkerEntry.COLUMN_MARKER_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Marker requires a name");
            }
        }

        // If the {@link MarkerEntry#COLUMN_MARKER_COLOR} key is present,
        // check that the color value is valid.
        if (values.containsKey(MarkerEntry.COLUMN_MARKER_COLOR)) {
            Integer color = values.getAsInteger(MarkerEntry.COLUMN_MARKER_COLOR);
            if (color == null || !MarkerEntry.isValidColor(color)) {
                throw new IllegalArgumentException("Marker requires valid color");
            }
        }

        // If the {@link MarkerEntry#COLUMN_MARKER_LATITUDE} key is present,
        // check that the latitude value is valid.
        if (values.containsKey(MarkerEntry.COLUMN_MARKER_LATITUDE)) {
            Double latitude = values.getAsDouble(MarkerEntry.COLUMN_MARKER_LATITUDE);
            if (latitude == null) {
                throw new IllegalArgumentException("Marker requires a latitude");
            }
        }

        // If the {@link MarkerEntry#COLUMN_MARKER_LONGITUDE} key is present,
        // check that the latitude value is valid.
        if (values.containsKey(MarkerEntry.COLUMN_MARKER_LONGITUDE)) {
            Double longitude = values.getAsDouble(MarkerEntry.COLUMN_MARKER_LONGITUDE);
            if (longitude == null) {
                throw new IllegalArgumentException("Marker requires a longitude");
            }
        }

        // No need to check the content, any value is valid (including null).

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // For Debugging
        Log.v(LOG_TAG, "values = " + values);
        Log.v(LOG_TAG, "selection = " + selection);
        Log.v(LOG_TAG, "selectionArgs = " + selectionArgs[0]);

        // Update all rows that match the selection and selection args
        int rowsUpdated = database.update(MarkerEntry.TABLE_NAME, values, selection, selectionArgs);

        Log.v(LOG_TAG, "rowsUpdated = " + rowsUpdated);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Returning the number of rows updated
        return rowsUpdated;
    }

    /**
     * Helper method to nsert a marker into the database with the given content values.
     *
     * @return  the new content URI for that specific row in the database.
     */
    private Uri insertMarker(Uri uri, ContentValues values) {

        // Check that the name is not null
        String name = values.getAsString(MarkerEntry.COLUMN_MARKER_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Marker requires a name");
        }

        // Check that the color is valid if it is provided
        Integer color = values.getAsInteger(MarkerEntry.COLUMN_MARKER_COLOR);
        if (color != null && !MarkerEntry.isValidColor(color)) {
            throw new IllegalArgumentException("Marker requires valid color");
        }

        // Check that the latitude is not null
        Double latitude = values.getAsDouble(MarkerEntry.COLUMN_MARKER_LATITUDE);
        if (latitude == null) {
            throw new IllegalArgumentException("Marker requires a latitude");
        }

        // Check that the longitude is not null
        Double longitude = values.getAsDouble(MarkerEntry.COLUMN_MARKER_LONGITUDE);
        if (longitude == null) {
            throw new IllegalArgumentException("Marker requires a longitude");
        }

        // No need to check the content, any value is valid (including null).

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new row with values, returning the primary key value of the new row as id
        long id = database.insert(MarkerEntry.TABLE_NAME, null, values);

        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify change to the listener attached to the content resolver with the URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }
}
