package com.apolloyang.bathroommaps;

import android.animation.Animator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.apolloyang.bathroommaps.model.BathroomMapsAPI;
import com.apolloyang.bathroommaps.model.GoogleDirectionsAPI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
    implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String DIALOG_ADDBATHROOM = "addbathroom";

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap;
    private boolean mGoogleApiConnected;
    private Marker mCurrentMarker;
    private Toolbar mToolbar;
    private View mWalkButton;
    private TextView mWalkingTimeTextView;
    private View mAddButton;
    private Marker mAddMarker;
    private HashMap<Marker, BathroomMapsAPI.Bathroom> mMarkers = new HashMap<Marker, BathroomMapsAPI.Bathroom>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar)findViewById(R.id.my_toolbar_bathroom);
        //setSupportActionBar(mToolbar);
        //getSupportActionBar().hide();

        mWalkingTimeTextView = (TextView)findViewById(R.id.time_textview);

        mWalkButton = findViewById(R.id.walk_button);
        mWalkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uriString = String.format("google.navigation:q=%s&mode=w&z=%d",
                        Uri.encode(String.format("%f,%f",
                                mCurrentMarker.getPosition().latitude,
                                mCurrentMarker.getPosition().longitude)),
                        15);

                Uri uri = Uri.parse(uriString);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            }
        });

        mAddButton = findViewById(R.id.add_button);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateToolbar(false);

                if (mAddMarker != null) {
                    //mMarkers.get(mAddMarker).clearMarker();
                    //mMarkers.remove(mAddMarker);
                    //mAddMarker = null;
                    mAddMarker.remove();
                }

                mAddMarker = mGoogleMap.addMarker(new MarkerOptions()
                    .position(mGoogleMap.getCameraPosition().target)
                    .draggable(true));
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);

        /*
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (code != ConnectionResult.SUCCESS) {
            int i = 2;
            i++;
        }
        */

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mGoogleMap = map;
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mCurrentMarker = marker;
                if ((mAddMarker != null) && (mCurrentMarker.getId().equals(mAddMarker.getId()))) {
                    AddBathroomDialogFragment dialogFragment = new AddBathroomDialogFragment(marker.getPosition());
                    dialogFragment.show(getSupportFragmentManager(), DIALOG_ADDBATHROOM);
                } else {
                    BathroomMapsAPI.Bathroom bathroom = mMarkers.get(marker);
                    ((TextView)findViewById(R.id.name_textview)).setText(bathroom.getName());

                    int count = bathroom.getRatingCount();

                    ((TextView)findViewById(R.id.rating_textview)).setText(
                            (count > 0) ? String.format("%.1f stars (%d reviews)",
                                            bathroom.getRatingAverage(),
                                            bathroom.getRatingCount()) : "No reviews");
                    animateToolbar(true);
                    ensureMapCentered(marker.getPosition());

                    Location hereLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (hereLoc != null) {
                        GetWalkingTimeTask task = new GetWalkingTimeTask(mWalkingTimeTextView);
                        task.execute(
                                new LatLng(hereLoc.getLatitude(), hereLoc.getLongitude()),
                                marker.getPosition()
                        );
                    }
                }

                return true;
            }
        });

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (mCurrentMarker != null) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(mCurrentMarker.getTitle())
                            .setMessage("Remove this bathroom?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    RemoveBathroomTask task = new RemoveBathroomTask(MainActivity.this,
                                            mCurrentMarker.getPosition(), mCurrentMarker.getTitle());
                                    task.execute();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                }
            }
        });

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                animateToolbar(false);
            }
        });

        /*
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if ((mCurrentMarker != null) &&
                        ((cameraPosition.target.latitude != mCurrentMarker.getPosition().latitude) ||
                         (cameraPosition.target.longitude != mCurrentMarker.getPosition().longitude))) {
                    LatLng x = mCurrentMarker.getPosition();
                    animateToolbar(false);
                }
            }
        });
        */

        AddMarkersTask task = new AddMarkersTask(mGoogleApiClient, map, mMarkers);
        task.execute();
        ensureMapCentered(null /* use current location */);
    }

    private void animateToolbar(boolean show) {
        final long ANIMATION_DURATION = 200;
        if (show) {
            mWalkButton.setVisibility(View.VISIBLE);
            mWalkButton.animate()
                    .alpha(1)
                    .setDuration(ANIMATION_DURATION);

            mToolbar.setVisibility(View.VISIBLE);
            //getSupportActionBar().show();
            mToolbar.animate()
                    .alpha(1)
                    .setDuration(ANIMATION_DURATION)
                    .setListener(null);
        } else {
            mWalkButton.animate()
                    .alpha(0)
                    .setDuration(ANIMATION_DURATION);
            mToolbar.animate()
                    .alpha(0)
                    .setDuration(ANIMATION_DURATION)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mToolbar.setVisibility(View.GONE);
                            //getSupportActionBar().hide();
                            mWalkButton.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
        }
    }

    private void ensureMapCentered(LatLng latLng) {
        // handle race between retrieving googlemap (onMapReady) and connecting to google api (onConnected)
        if (mGoogleApiConnected && (mGoogleMap != null)) {

            // If no location, use current location
            if (latLng == null) {
                Location hereLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (hereLoc != null) {
                    latLng = new LatLng(hereLoc.getLatitude(), hereLoc.getLongitude());
                }
            }

            if (latLng != null) {
                mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder()
                        .target(latLng)
                        .zoom(15)
                        .build()), 200, null);
            }
        }
    }

    // GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnected(Bundle var1)
    {
        onDoneConnecting("connected");
        mGoogleApiConnected = true;
        ensureMapCentered(null /* use current location */);
    }

    @Override
    public void onConnectionSuspended(int var1)
    {
        onDoneConnecting("suspended");
    }

    // GoogleApiClient.OnConnectionFailedListener
    @Override
    public void onConnectionFailed(ConnectionResult var1)
    {
        onDoneConnecting("failed");
    }

    private void onDoneConnecting(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    // Parameters
    // Progress
    // Result
    private static class AddMarkersTask extends AsyncTask<Void, Void, List<BathroomMapsAPI.Bathroom>> {

        private GoogleApiClient mGoogleApiClient;
        private GoogleMap mMap;
        private HashMap<Marker, BathroomMapsAPI.Bathroom> mMarkers;

        public AddMarkersTask(GoogleApiClient googleApiClient, GoogleMap map, HashMap<Marker, BathroomMapsAPI.Bathroom> markers) {
            mGoogleApiClient = googleApiClient;
            mMap = map;
            mMarkers = markers;
        }

        @Override
        protected List<BathroomMapsAPI.Bathroom> doInBackground(Void... params) {
            BathroomMapsAPI api = new BathroomMapsAPI();
            List<BathroomMapsAPI.Bathroom> bathrooms = null;
            try {
                bathrooms = api.getBathrooms();
            } catch (Exception e) {
                System.err.println(e);
            }
            return bathrooms;
        }

        @Override
        protected void onPostExecute(List<BathroomMapsAPI.Bathroom> result) {
            if (result != null) {
                for (BathroomMapsAPI.Bathroom bathroom : result) {
                    mMarkers.put(bathroom.addMarker(mMap), bathroom);
                }
            }
        }
    }

    private static class GetWalkingTimeTask extends AsyncTask<LatLng, Void, String> {
        private TextView mTimeTextView;

        public GetWalkingTimeTask(TextView timeTextView) {
            mTimeTextView = timeTextView;
        }

        @Override
        protected void onPreExecute() {
            mTimeTextView.setText(null);
        }

        @Override
        protected String doInBackground(LatLng... params) {
            try {
                return GoogleDirectionsAPI.getWalkingTime(params[0], params[1]);
            } catch (Exception e) {
                Log.e("BathroomMaps", "GetWalkingTimeTask exception: " + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            mTimeTextView.setText(result);
        }
    }

    // Parameters
    // Progress
    // Result
    private static class RemoveBathroomTask extends AsyncTask<Void, Void, String> {

        private Context mContext;
        private LatLng mPosition;
        private String mName;

        public RemoveBathroomTask(Context context, LatLng position, String name) {
            mContext = context;
            mPosition = position;
            mName = name;
        }

        @Override
        protected String doInBackground(Void... params) {
            BathroomMapsAPI api = new BathroomMapsAPI();
            try {
                return api.removeBathroom(mPosition, mName);
            } catch (Exception e) {
                System.err.println(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast toast = Toast.makeText(mContext, result, Toast.LENGTH_LONG);
            toast.show();
        }
    }
}
