package com.example.work.mapmarker.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Work on 2017/4/15.
 */

public class MarkerContract {

    //Create a private constructor to prevent accidental instantiation.
    private MarkerContract() {}

    /**
     * The "Content authority" is a name for the entire content provider, similar to the
     * relationship between a domain name and its website.  A convenient string to use for the
     * content authority is the package name for the app, which is guaranteed to be unique on the
     * device.
     */
    public static final String CONTENT_AUTHORITY = "com.example.work.mapmarker";

    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Possible path (appended to base content URI for possible URI's)
     */
    public static final String PATH_MARKERS = "markers";

    /**
     * Inner class that defines constant values for the markers database table.
     * Each entry in the table represents a single marker.
     */
    public static final class MarkerEntry implements BaseColumns {

        /** The content URI to access the pet data in the provider */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_MARKERS);

        /** Name of database table for markers */
        public final static String TABLE_NAME = "markers";

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of markers.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MARKERS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single markers.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MARKERS;

        /**
         * Unique ID number for the marker (only for use in the database table).
         *
         * Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Name of the marker.
         *
         * Type: TEXT
         */
        public final static String COLUMN_MARKER_NAME ="name";

        /**
         * Content of the marker.
         *
         * Type: TEXT
         */
        public final static String COLUMN_MARKER_CONTENT = "content";

        /**
         * Color of the marker.
         *
         * TODO: Add the comment about all possible type of color
         *
         * Type: INTEGER
         */
        public final static String COLUMN_MARKER_COLOR = "color";

        /**
         * Latitude of the marker
         *
         * Type: Long
         */
        public final static String COLUMN_MARKER_LATITUDE = "latitude";

        /**
         * Longitude of the marker
         *
         * Type: Long
         */
        public final static String COLUMN_MARKER_LONGITUDE = "longitude";

        /**
         * Possible values for the color of marker.
         * TODO: Add color here if more color is allowed
         */
        public static final int COLOR_RED = 0;


        /**
         * Returns whether or not the given color is valid.
         *
         * @return true if color is valid
         * TODO: Add check condition here if more color is allowed
         */
        public static boolean isValidColor(int color) {
            if (color == COLOR_RED) {
                return true;
            }
            return false;
        }
    }
}

