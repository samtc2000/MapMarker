package com.example.work.mapmarker;

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.work.mapmarker.data.MarkerContract.MarkerEntry;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import static com.example.work.mapmarker.R.id.map;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, OnMarkerClickListener, OnMapClickListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Tag for log messages
     */
    private final static String LOG_TAG = MainActivity.class.getName();

    /**
     * Identifies a particular Loader being used in this component
     */
    private static final int MARKERS_LOADER = 0;

    /**
     * Request code of the ACCESS_FINE_LOCATION permission, used in onRequestPermissionsResult() call back method to indicate which permission result it is related to.
     */
    private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 0;

    /**
     * Google map object
     */
    private GoogleMap mMap;

    /**
     * List of marker on the map
     */
    private List<Marker> mMarkers;

    /**
     * Marker used when adding new position
     */
    private Marker mAddPositionMarker;

    /**
     * Boolean indicator on whether it is in add position mode.
     */
    private boolean mIsAddPosition;

    /**
     * Boolean indicator on whether it is in edit mode.
     */
    private boolean mIsEdit;

    /**
     * GoogleAPIClient
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Current location as location object
     */
    private Location mMyLocation;

    /**
     * Reference for views
     */
    private SupportMapFragment mMapFragment;
    private FloatingActionButton mMyLocationFab;
    private FloatingActionButton mAddLocationFab;
    private ImageView mDoneButton;
    private ImageView mCancelButton;
    private ImageView mDeleteButton;
    private ImageView mDirectionsButton;
    private ImageView mEditButton;
    private EditText mNameEditText;
    private TextView mNameTextView;
    private LinearLayout mInfoCardView;
    private LinearLayout mEditCardView;
    private ProgressBar mLoadingIndicator;

    /**
     * Indicator to keep track of the id of the marker last clicked
     */
    private int mMarkerClickedID = 0;

    /**
     * Cursor of the markers table
     */
    private Cursor mCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize mIsAddPosition and mIsEdit to false, since we are not adding position nor editing when the activity is created
        mIsAddPosition = false;
        mIsEdit = false;

        // Initialize the list of markers
        mMarkers = new ArrayList<>();

        // Check internet connection
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        // Connect to Google API Client if connected to internet
        if (isConnected) {
            // Create an instance of GoogleAPIClient.
            if (mGoogleApiClient == null) {
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();
            }
        } else {
            // Else, toast a message and close the app
            Log.e(LOG_TAG, "Fail to connect internet");
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
            finish();
        }

        // Get reference for all the views
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(map);
        mMyLocationFab = (FloatingActionButton) findViewById(R.id.my_location_fab);
        mAddLocationFab = (FloatingActionButton) findViewById(R.id.add_location_fab);
        mDoneButton = (ImageView) findViewById(R.id.done_button);
        mCancelButton = (ImageView) findViewById(R.id.cancel_button);
        mDeleteButton = (ImageView) findViewById(R.id.delete_button);
        mDirectionsButton = (ImageView) findViewById(R.id.directions_button);
        mEditButton = (ImageView) findViewById(R.id.edit_button);
        mNameEditText = (EditText) findViewById(R.id.name_edit);
        mNameTextView = (TextView) findViewById(R.id.name);
        mInfoCardView = (LinearLayout) findViewById(R.id.card_info);
        mEditCardView = (LinearLayout) findViewById(R.id.card_edit);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.loading_indicator);

        // Set map fragment to invisible and set up the map
        mMapFragment.getView().setVisibility(View.INVISIBLE);
        mMapFragment.getMapAsync(this);

        // Setup all on click listeners
        mMyLocationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                centerMapOnMyLocation();
            }
        });
        mAddLocationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAddLocationFabClick();
            }
        });
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDoneClick(mMarkerClickedID);
            }
        });
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelClick();
            }
        });
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDeleteClick(mMarkerClickedID);
            }
        });
        mDirectionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDirectionsClick(mMarkerClickedID);
            }
        });
        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEditClick(mMarkerClickedID);
            }
        });

        // Initializes the CursorLoader. The URL_LOADER value (ie. MARKERS_LOADER) is eventually passed to onCreateLoader().
        getLoaderManager().initLoader(MARKERS_LOADER, null, this);
    }

    /**
     * Called when activity start.
     */
    protected void onStart() {
        // Connect to GoogleAPIClient
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        // Restart the CursorLoader. (so then rotating the device will not make markers disappear)
        getLoaderManager().restartLoader(MARKERS_LOADER, null, this);

        super.onStart();
    }

    /**
     * Called when activity stop.
     */
    protected void onStop() {
        // Disconnect from GoogleAPIClient
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    /**
     * Helper method to handle action when add location FAB is pressed
     */
    private void onAddLocationFabClick() {

        // Change mIsAddPosition to true
        mIsAddPosition = true;

        // Get the center view of the map as LatLng
        LatLng centerView = mMap.getCameraPosition().target;

        // Hide add location FAB and show edit card
        mAddLocationFab.setVisibility(View.GONE);
        mEditCardView.setVisibility(View.VISIBLE);

        // Show add position marker
        mAddPositionMarker = mMap.addMarker(new MarkerOptions().position(centerView).icon(defaultMarker(HUE_AZURE)));
    }

    /**
     * Helper method to handle action when done button is pressed
     *
     * @param id id of the marker last clicked (only used in edit mode)
     */
    private void onDoneClick(int id) {

        // Get name from edit text view
        String name = mNameEditText.getText().toString().trim();

        // Check is name is empty
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.name_needed, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();

        // Handle action in add position mode
        if (mIsAddPosition) {

            // Put data into the ContentValues
            values.put(MarkerEntry.COLUMN_MARKER_NAME, name);
            values.put(MarkerEntry.COLUMN_MARKER_LATITUDE, mAddPositionMarker.getPosition().latitude);
            values.put(MarkerEntry.COLUMN_MARKER_LONGITUDE, mAddPositionMarker.getPosition().longitude);

            // Insert the new row, returning the uri of the new row
            // Catch the exception and toast the error message
            try {
                Uri uri = getContentResolver().insert(MarkerEntry.CONTENT_URI, values);

                // Show different toast message depending on whether insert is successful
                // Uri is null if the insertion failed
                if (uri == null) {
                    Toast.makeText(this, R.string.error_saving_marker, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.marker_saved, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            // Remove add position marker, show add location FAB and hide edit card
            mAddPositionMarker.remove();
            mAddLocationFab.setVisibility(View.VISIBLE);
            mEditCardView.setVisibility(View.GONE);

            // Set mIsAddPosition to false
            mIsAddPosition = false;

            // Reset EditText view
            mNameEditText.setText("");
        }

        // Handle action in edit mode
        else {
            // Put data into the ContentValues
            values.put(MarkerEntry.COLUMN_MARKER_NAME, name);

            // Update the row and get the number of row updated
            // Catch the exception and toast the error message
            try {
                int rowUpdated = getContentResolver().update(ContentUris.withAppendedId(MarkerEntry.CONTENT_URI, id), values, null, null);

                // Show different toast message depending on whether insert is successful
                // Uri is null if the insertion failed
                if (rowUpdated == 0) {
                    Toast.makeText(this, R.string.error_saving_marker, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.marker_saved, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            // Set text for the name text view
            mNameTextView.setText(name);

            // Show info card and hide edit card
            mInfoCardView.setVisibility(View.VISIBLE);
            mEditCardView.setVisibility(View.GONE);

            // Exit edit mode
            mIsEdit = false;

            // Reset EditText view
            mNameEditText.setText("");

            // Hide delete button
            mDeleteButton.setVisibility(View.GONE);
        }
    }

    /**
     * Helper method to handle action when cancel button is pressed
     */
    private void onCancelClick() {
        // Handle actions in add position mode
        if (mIsAddPosition) {
            // Remove add location marker, show add location FAB and hide edit card
            mAddPositionMarker.remove();
            mAddLocationFab.setVisibility(View.VISIBLE);
            mEditCardView.setVisibility(View.GONE);

            // Exit add position mode
            mIsAddPosition = false;

            // Reset EditText view
            mNameEditText.setText("");
        }
        // Handle actions in edit mode
        else {
            // Show add location FAB and hide edit card
            mEditCardView.setVisibility(View.GONE);
            mAddLocationFab.setVisibility(View.VISIBLE);

            // Exit edit mode
            mIsEdit = false;

            // Reset EditText view
            mNameEditText.setText("");

            // Hide delete button
            mDeleteButton.setVisibility(View.GONE);
        }
    }

    /**
     * Helper method to handle action when cancel button is pressed
     *
     * @param id the id of the marker to be deleted
     */
    private void onDeleteClick(int id) {

        int rowDeleted = getContentResolver().delete(ContentUris.withAppendedId(MarkerEntry.CONTENT_URI, id), null, null);

        if (rowDeleted == 0) {
            Toast.makeText(this, R.string.error_delete_marker, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.marker_deleted, Toast.LENGTH_SHORT).show();
        }

        //Show add location FAB and hide edit card
        mAddLocationFab.setVisibility(View.VISIBLE);
        mEditCardView.setVisibility(View.GONE);

        // Exit edit mode
        mIsEdit = false;

        // Reset EditText view
        mNameEditText.setText("");

        // Hide delete button
        mDeleteButton.setVisibility(View.GONE);
    }

    /**
     * Helper method to handle action when edit button is pressed
     *
     * @param id the id of the marker being edited
     */
    private void onEditClick(int id) {
        mIsEdit = true;

        // Get name of the marker
        Cursor cursor = getContentResolver().query(ContentUris.withAppendedId(MarkerEntry.CONTENT_URI, id), new String[]{MarkerEntry.COLUMN_MARKER_NAME}, null, null, null);
        cursor.moveToFirst();
        String name = cursor.getString(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_MARKER_NAME));
        cursor.close();

        // Set the text of EditText view to the name of the marker
        mNameEditText.setText(name);

        // Show delete button
        mDeleteButton.setVisibility(View.VISIBLE);

        // Hide info card and show edit card
        mInfoCardView.setVisibility(View.GONE);
        mEditCardView.setVisibility(View.VISIBLE);

        // Focus on edit text
        mNameEditText.requestFocus();
    }

    /**
     * Helper method to handle action when directions button is pressed
     *
     * @param id the id of the marker which needed to get direction on.
     */
    private void onDirectionsClick(int id) {

        // Get name of the marker
        Cursor nameCursor = getContentResolver().query(ContentUris.withAppendedId(MarkerEntry.CONTENT_URI, id), new String[]{MarkerEntry.COLUMN_MARKER_NAME, MarkerEntry.COLUMN_MARKER_LATITUDE, MarkerEntry.COLUMN_MARKER_LONGITUDE}, null, null, null);
        nameCursor.moveToFirst();
        String name = nameCursor.getString(nameCursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_MARKER_NAME));
        Double latitude = nameCursor.getDouble(nameCursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_MARKER_LATITUDE));
        Double longitude = nameCursor.getDouble(nameCursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_MARKER_LONGITUDE));
        nameCursor.close();

        // Create an intent to open map and get direction
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("geo:0,0?q=" + latitude + "," + longitude + "(" + name + ")"));

        // Send the intent to launch a new activity
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    /**
     * Helper method to center map on user location
     */
    private void centerMapOnMyLocation() {
        // Check for location permission then get last location from Google API
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //Permission to access the location is missing.
            ActivityCompat.requestPermissions(this,
            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);
        }

        mMyLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        LatLng latLng = new LatLng(mMyLocation.getLatitude(), mMyLocation.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18);
        mMap.animateCamera(cameraUpdate);
    }

    /**
     * Helper method to center map on user location with no animation
     */
    private void centerMapOnMyLocationNoAnimate() {
        // Check for location permission then get last location from Google API
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //Permission to access the location is missing.
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);
        } else {
            mMyLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            LatLng latLng = new LatLng(mMyLocation.getLatitude(), mMyLocation.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14);
            mMap.moveCamera(cameraUpdate);
        }
    }

    /**
     * Call back method called when map is ready
     *
     * @param map the GoogleMap object returned
     */
    @Override
    public void onMapReady(GoogleMap map) {

        // Get reference to Google map
        mMap = map;

        // Set listener for marker events.
        mMap.setOnMarkerClickListener(this);

        // Set listener for on map click
        mMap.setOnMapClickListener(this);

        enableMyLocation();
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    /**
     * Helper method to add markers to map from the database
     *
     * @param  cursor the cursor of the database
     */
    private void addMarkersToMap(Cursor cursor) {

        // Move to each row and add a marker for each row
        while (cursor.moveToNext()) {

            // Get the name, latitude and longitude of the mark of that row
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(MarkerEntry._ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_MARKER_NAME));
            Double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_MARKER_LATITUDE));
            Double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_MARKER_LONGITUDE));
            LatLng laglng = new LatLng(latitude, longitude);

            // Add the marker to the map
            Marker marker = mMap.addMarker(new MarkerOptions().position(laglng).title(name));
            marker.setTag(id);
            mMarkers.add(marker);
        }
    }

    /**
     * Markers on click listener
     *
     * @param marker the marker being clicked
     */
    @Override
    public boolean onMarkerClick(Marker marker) {

        // If it is in add position mode or edit mode, do nothing
        if (mIsAddPosition || mIsEdit) {
            return true;
        }

        // Change marker clicked indicator
        mMarkerClickedID = (int) marker.getTag();

        // Set text for the name text view
        mNameTextView.setText(marker.getTitle());

        // Show info card and hide add location FAB
        mInfoCardView.setVisibility(View.VISIBLE);
        mAddLocationFab.setVisibility(View.GONE);

        // We return true to indicate that we have consumed the event and that we do not wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return true;
    }

    /**
     * Map on click listener
     *
     * @param latLng where the map is clicked
     */
    @Override
    public void onMapClick(LatLng latLng) {

        // If it is in add position mode, move the app position marker to the clicked position
        if (mIsAddPosition) {
            mAddPositionMarker.setPosition(latLng);
            return;
        }

        // Else if it is in edit mode, do nothing
        else if (mIsEdit) {
            return;
        }

        // Else, it is on view mode. Hide card and show add location fab when map is clicked
        else {
            if (mInfoCardView.getVisibility() == View.VISIBLE) {
                mInfoCardView.setVisibility(View.GONE);
            }
            if (mAddLocationFab.getVisibility() == View.GONE) {
                mAddLocationFab.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Call back method when permissions request result is return
     *
     * @param requestCode the request code passed in requestPermissions()
     * @param permissions the requested permissions
     * @param grantResults the grant results for the corresponding permissions which is either PERMISSION_GRANTED or PERMISSION_DENIED.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                    centerMapOnMyLocationNoAnimate();
                    enableMyLocation();

                } else {
                    Toast.makeText(this, R.string.location_permissionion_needed, Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
            }
        }
    }

    /**
     * Empty on click handler for edit card.
     * This is set so that the map on click will not be trigger when the user click on the card
     *
     * @param view the view being clicked
     */
    public void onCardEditClick(View view) {
    }

    /**
     * Empty on click handler for info card.
     * This is set so that the map on click will not be trigger when the user click on the card
     *
     * @param view the view being clicked
     */
    public void onCardInfoClick(View view) {
    }

    /**
     * Call back method when Connection to Google API is completed.
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Hide loading indicator because the data has been loaded
        mLoadingIndicator.setVisibility(View.GONE);

        // Center the view on current location
        centerMapOnMyLocationNoAnimate();

        // Show map and FAB
        mMapFragment.getView().setVisibility(View.VISIBLE);
        mMyLocationFab.setVisibility(View.VISIBLE);
        mAddLocationFab.setVisibility(View.VISIBLE);
    }

    /**
     * Call back method when Connection to Google API is suspended.
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.e(LOG_TAG, "Connection to Google API suspended");
    }

    /**
     * Call back method when Connection to Google API fail.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Fail to connect to Google API");
    }

    /**
     * Instantiate and return a new Loader<Cursor>.
     *
     * @param i the ID whose loader is to be created. Not used for now cause only one loader is used in this case
     * @param bundle arguments supplied by the caller.
     * @return  a new Loader<Cursor> instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies which columns from the database will actually be used after this query
        String[] projection = {
                MarkerEntry._ID,
                MarkerEntry.COLUMN_MARKER_NAME,
                MarkerEntry.COLUMN_MARKER_LATITUDE,
                MarkerEntry.COLUMN_MARKER_LONGITUDE};

        // Returns a new CursorLoader
        return new CursorLoader(
                this,                         // Parent activity context
                MarkerEntry.CONTENT_URI,  // Table to query
                projection,                   // Projection to return
                null,                         // No selection clause
                null,                         // No selection arguments
                null                          // Default sort order
        );
    }

    /**
     * Called when the loader has finished its load.
     *
     * @param loader the Loader that has finished.
     * @param cursor the cursor returned by the loader
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        // Call onLoaderReset () to clear map and markers and close cursor
        onLoaderReset(loader);

        // Initiate mCursor
        mCursor = cursor;

        // Add markers to the map from the database
        addMarkersToMap(mCursor);
    }

    /**
     * Called when a previously created loader is being reset
     *
     * @param loader the Loader that has been reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mMap != null) {
            // Clear the map and markers if map is ready
            mMap.clear();
            mMarkers.clear();
        }
        // Close the cursor if exist
        if(mCursor != null) {
            mCursor.close();
        }
    }


}
